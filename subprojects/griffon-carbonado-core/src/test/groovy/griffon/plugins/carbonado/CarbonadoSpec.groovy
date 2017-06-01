/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package griffon.plugins.carbonado

import com.amazon.carbonado.Cursor
import com.amazon.carbonado.Repository
import com.amazon.carbonado.Storage
import griffon.core.CallableWithArgs
import griffon.core.GriffonApplication
import griffon.core.test.GriffonUnitRule
import griffon.inject.BindTo
import org.junit.Rule
import spock.lang.Specification
import spock.lang.Unroll

import javax.inject.Inject

@Unroll
class CarbonadoSpec extends Specification {
    static {
        System.setProperty('org.slf4j.simpleLogger.defaultLogLevel', 'trace')
    }

    @Rule
    public final GriffonUnitRule griffon = new GriffonUnitRule()

    @Inject
    private CarbonadoHandler carbonadoHandler

    @Inject
    private GriffonApplication application

    void 'Open and close default repository'() {
        given:
        List eventNames = [
            'CarbonadoConnectStart', 'CarbonadoConnectEnd',
            'CarbonadoDisconnectStart', 'CarbonadoDisconnectEnd'
        ]
        List events = []
        eventNames.each { name ->
            application.eventRouter.addEventListener(name, { Object... args ->
                events << [name: name, args: args]
            } as CallableWithArgs)
        }

        when:
        carbonadoHandler.withCarbonado { String repositoryName, Repository repository ->
            true
        }
        carbonadoHandler.closeCarbonado()
        // second call should be a NOOP
        carbonadoHandler.closeCarbonado()

        then:
        events.size() == 4
        events.name == eventNames
    }

    void 'Connect to default repository'() {
        expect:
        carbonadoHandler.withCarbonado { String repositoryName, Repository repository ->
            repositoryName == 'default' && repository
        }
    }

    void 'Bootstrap init is called'() {
        given:
        assert !bootstrap.initWitness

        when:
        carbonadoHandler.withCarbonado { String repositoryName, Repository repository -> }

        then:
        bootstrap.initWitness
        !bootstrap.destroyWitness
    }

    void 'Bootstrap destroy is called'() {
        given:
        assert !bootstrap.initWitness
        assert !bootstrap.destroyWitness

        when:
        carbonadoHandler.withCarbonado { String repositoryName, Repository repository -> }
        carbonadoHandler.closeCarbonado()

        then:
        bootstrap.initWitness
        bootstrap.destroyWitness
    }

    void 'Can connect to #name repository'() {
        expect:
        carbonadoHandler.withCarbonado(name) { String repositoryName, Repository repository ->
            repositoryName == name && repository
        }

        where:
        name       | _
        'default'  | _
        'internal' | _
        'people'   | _
    }

    void 'Bogus repository name (#name) results in error'() {
        when:
        carbonadoHandler.withCarbonado(name) { String repositoryName, Repository repository ->
            true
        }

        then:
        thrown(IllegalArgumentException)

        where:
        name    | _
        null    | _
        ''      | _
        'bogus' | _
    }

    void 'Execute statements on people repository'() {
        when:
        List peopleIn = carbonadoHandler.withCarbonado('people') { String repositoryName, Repository repository ->
            Storage<Person> people = repository.storageFor(Person)
            [[id: '1', name: 'Danno', lastname: 'Ferrin'],
             [id: '2', name: 'Andres', lastname: 'Almiray'],
             [id: '3', name: 'James', lastname: 'Williams'],
             [id: '4', name: 'Guillaume', lastname: 'Laforge'],
             [id: '5', name: 'Jim', lastname: 'Shingler'],
             [id: '6', name: 'Alexander', lastname: 'Klein'],
             [id: '7', name: 'Rene', lastname: 'Groeschke']].collect { data ->
                Person person = people.prepare()
                data.each { propName, propValue ->
                    person[propName] = propValue
                }
                person.insert()
                person
            }
        }

        List peopleOut = carbonadoHandler.withCarbonado('people') { String repositoryName, Repository repository ->
            List<Person> list = []
            Storage<Person> people = repository.storageFor(Person)
            Cursor<Person> peopleCursor = people.query().fetch()
            while (peopleCursor.hasNext()) {
                list << peopleCursor.next()
            }
            list
        }

        then:
        peopleIn == peopleOut

        cleanup:
        carbonadoHandler.closeCarbonado()
    }

    @BindTo(CarbonadoBootstrap)
    private TestCarbonadoBootstrap bootstrap = new TestCarbonadoBootstrap()
}
