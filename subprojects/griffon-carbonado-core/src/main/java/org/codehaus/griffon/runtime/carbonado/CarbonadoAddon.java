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
package org.codehaus.griffon.runtime.carbonado;

import com.amazon.carbonado.Repository;
import griffon.core.GriffonApplication;
import griffon.core.env.Metadata;
import griffon.inject.DependsOn;
import griffon.plugins.carbonado.CarbonadoHandler;
import griffon.plugins.carbonado.RepositoryCallback;
import griffon.plugins.carbonado.RepositoryFactory;
import griffon.plugins.carbonado.RepositoryStorage;
import griffon.plugins.monitor.MBeanManager;
import org.codehaus.griffon.runtime.core.addon.AbstractGriffonAddon;
import org.codehaus.griffon.runtime.jmx.RepositoryStorageMonitor;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

import static griffon.util.ConfigUtils.getConfigValueAsBoolean;

/**
 * @author Andres Almiray
 */
@DependsOn("datasource")
@Named("carbonado")
public class CarbonadoAddon extends AbstractGriffonAddon {
    @Inject
    private CarbonadoHandler carbonadoHandler;

    @Inject
    private RepositoryFactory repositoryFactory;

    @Inject
    private RepositoryStorage repositoryStorage;

    @Inject
    private MBeanManager mbeanManager;

    @Inject
    private Metadata metadata;

    @Override
    public void init(@Nonnull GriffonApplication application) {
        mbeanManager.registerMBean(new RepositoryStorageMonitor(metadata, repositoryStorage));
    }

    public void onStartupStart(@Nonnull GriffonApplication application) {
        for (String repositoryName : repositoryFactory.getRepositoryNames()) {
            Map<String, Object> config = repositoryFactory.getConfigurationFor(repositoryName);
            if (getConfigValueAsBoolean(config, "connect_on_startup", false)) {
                carbonadoHandler.withCarbonado(new RepositoryCallback<Object>() {
                    @Override
                    public Object handle(@Nonnull String repositoryName, @Nonnull Repository repository) {
                        return null;
                    }
                });
            }
        }
    }

    public void onShutdownStart(@Nonnull GriffonApplication application) {
        for (String repositoryName : repositoryFactory.getRepositoryNames()) {
            carbonadoHandler.closeCarbonado(repositoryName);
        }
    }
}
