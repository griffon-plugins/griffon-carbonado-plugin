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
package org.codehaus.griffon.runtime.carbonado;

import griffon.core.Configuration;
import griffon.core.addon.GriffonAddon;
import griffon.core.injection.Module;
import griffon.inject.DependsOn;
import griffon.plugins.carbonado.CarbonadoHandler;
import griffon.plugins.carbonado.RepositoryFactory;
import griffon.plugins.carbonado.RepositoryStorage;
import org.codehaus.griffon.runtime.core.injection.AbstractModule;
import org.codehaus.griffon.runtime.util.ResourceBundleProvider;
import org.kordamp.jipsy.ServiceProviderFor;

import javax.inject.Named;
import java.util.ResourceBundle;

import static griffon.util.AnnotationUtils.named;

/**
 * @author Andres Almiray
 */
@DependsOn("datasource")
@Named("carbonado")
@ServiceProviderFor(Module.class)
public class CarbonadoModule extends AbstractModule {
    @Override
    protected void doConfigure() {
        // tag::bindings[]
        bind(ResourceBundle.class)
            .withClassifier(named("carbonado"))
            .toProvider(new ResourceBundleProvider("Carbonado"))
            .asSingleton();

        bind(Configuration.class)
            .withClassifier(named("carbonado"))
            .to(DefaultCarbonadoConfiguration.class)
            .asSingleton();

        bind(RepositoryStorage.class)
            .to(DefaultRepositoryStorage.class)
            .asSingleton();

        bind(RepositoryFactory.class)
            .to(DefaultRepositoryFactory.class)
            .asSingleton();

        bind(CarbonadoHandler.class)
            .to(DefaultCarbonadoHandler.class)
            .asSingleton();

        bind(GriffonAddon.class)
            .to(CarbonadoAddon.class)
            .asSingleton();
        // end::bindings[]
    }
}
