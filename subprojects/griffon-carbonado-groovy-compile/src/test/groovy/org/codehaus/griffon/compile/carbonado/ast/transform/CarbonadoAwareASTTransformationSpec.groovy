/*
 * Copyright 2016 the original author or authors.
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
package org.codehaus.griffon.compile.carbonado.ast.transform

import griffon.plugins.carbonado.CarbonadoHandler
import spock.lang.Specification

import java.lang.reflect.Method

/**
 * @author Andres Almiray
 */
class CarbonadoAwareASTTransformationSpec extends Specification {
    def 'CarbonadoAwareASTTransformation is applied to a bean via @CarbonadoAware'() {
        given:
        GroovyShell shell = new GroovyShell()

        when:
        def bean = shell.evaluate('''import griffon.transform.CarbonadoAware
        @CarbonadoAware
        class Bean { }
        new Bean()
        ''')

        then:
        bean instanceof CarbonadoHandler
        CarbonadoHandler.methods.every { Method target ->
            bean.class.declaredMethods.find { Method candidate ->
                candidate.name == target.name &&
                    candidate.returnType == target.returnType &&
                    candidate.parameterTypes == target.parameterTypes &&
                    candidate.exceptionTypes == target.exceptionTypes
            }
        }
    }

    def 'CarbonadoAwareASTTransformation is not applied to a CarbonadoHandler subclass via @CarbonadoAware'() {
        given:
        GroovyShell shell = new GroovyShell()

        when:
        def bean = shell.evaluate('''import griffon.plugins.carbonado.RepositoryCallback
        import griffon.plugins.carbonado.CarbonadoHandler
        import griffon.transform.CarbonadoAware

        import javax.annotation.Nonnull
        @CarbonadoAware
        class CarbonadoHandlerBean implements CarbonadoHandler {
            @Override
            public <R> R withCarbonado(@Nonnull RepositoryCallback<R> callback)  {
                return null
            }
            @Override
            public <R> R withCarbonado(@Nonnull String repositoryName, @Nonnull RepositoryCallback<R> callback) {
                 return null
            }
            @Override
            void closeCarbonado(){}
            @Override
            void closeCarbonado(@Nonnull String repositoryName){}
        }
        new CarbonadoHandlerBean()
        ''')

        then:
        bean instanceof CarbonadoHandler
        CarbonadoHandler.methods.every { Method target ->
            bean.class.declaredMethods.find { Method candidate ->
                candidate.name == target.name &&
                    candidate.returnType == target.returnType &&
                    candidate.parameterTypes == target.parameterTypes &&
                    candidate.exceptionTypes == target.exceptionTypes
            }
        }
    }
}
