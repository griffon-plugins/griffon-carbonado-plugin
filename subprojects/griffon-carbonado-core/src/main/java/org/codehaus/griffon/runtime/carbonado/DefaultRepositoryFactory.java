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

import com.amazon.carbonado.Repository;
import com.amazon.carbonado.RepositoryException;
import com.amazon.carbonado.repo.jdbc.JDBCRepositoryBuilder;
import com.amazon.carbonado.repo.map.MapRepositoryBuilder;
import com.amazon.carbonado.repo.sleepycat.BDBRepositoryBuilder;
import griffon.core.Configuration;
import griffon.core.GriffonApplication;
import griffon.core.injection.Injector;
import griffon.exceptions.GriffonException;
import griffon.plugins.carbonado.CarbonadoBootstrap;
import griffon.plugins.carbonado.RepositoryFactory;
import griffon.plugins.datasource.DataSourceFactory;
import griffon.plugins.datasource.DataSourceStorage;
import org.codehaus.griffon.runtime.core.storage.AbstractObjectFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static griffon.util.ConfigUtils.getConfigValue;
import static griffon.util.ConfigUtils.getConfigValueAsString;
import static griffon.util.GriffonClassUtils.setPropertiesNoException;
import static griffon.util.GriffonNameUtils.requireNonBlank;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * @author Andres Almiray
 */
public class DefaultRepositoryFactory extends AbstractObjectFactory<Repository> implements RepositoryFactory {
    private static final String ERROR_REPOSITORY_BLANK = "Argument 'repositoryName' must not be blank";

    private final Set<String> repositoryNames = new LinkedHashSet<>();

    @Inject
    private DataSourceFactory dataSourceFactory;

    @Inject
    private DataSourceStorage dataSourceStorage;

    @Inject
    private Injector injector;

    @Inject
    public DefaultRepositoryFactory(@Nonnull @Named("carbonado") Configuration configuration, @Nonnull GriffonApplication application) {
        super(configuration, application);
        repositoryNames.add(KEY_DEFAULT);

        if (configuration.containsKey(getPluralKey())) {
            Map<String, Object> carbonados = (Map<String, Object>) configuration.get(getPluralKey());
            repositoryNames.addAll(carbonados.keySet());
        }
    }

    @Nonnull
    @Override
    public Set<String> getRepositoryNames() {
        return repositoryNames;
    }

    @Nonnull
    @Override
    public Map<String, Object> getConfigurationFor(@Nonnull String repositoryName) {
        requireNonBlank(repositoryName, ERROR_REPOSITORY_BLANK);
        return narrowConfig(repositoryName);
    }

    @Nonnull
    @Override
    protected String getSingleKey() {
        return "repository";
    }

    @Nonnull
    @Override
    protected String getPluralKey() {
        return "repositories";
    }

    @Nonnull
    @Override
    public Repository create(@Nonnull String name) {
        requireNonBlank(name, ERROR_REPOSITORY_BLANK);
        Map<String, Object> config = narrowConfig(name);

        if (config.isEmpty()) {
            throw new IllegalArgumentException("Repository '" + name + "' is not configured.");
        }

        event("CarbonadoConnectStart", asList(name, config));

        Repository repository = null;
        try {
            repository = createRepository(config, name);
        } catch (Exception e) {
            throw new GriffonException(e);
        }

        for (Object o : injector.getInstances(CarbonadoBootstrap.class)) {
            ((CarbonadoBootstrap) o).init(name, repository);
        }

        event("CarbonadoConnectEnd", asList(name, config, repository));

        return repository;
    }

    @Override
    public void destroy(@Nonnull String name, @Nonnull Repository instance) {
        requireNonBlank(name, ERROR_REPOSITORY_BLANK);
        requireNonNull(instance, "Argument 'instance' must not be null");
        Map<String, Object> config = narrowConfig(name);

        if (config.isEmpty()) {
            throw new IllegalArgumentException("Repository '" + config + "' is not configured.");
        }

        event("CarbonadoDisconnectStart", asList(name, config, instance));

        for (Object o : injector.getInstances(CarbonadoBootstrap.class)) {
            ((CarbonadoBootstrap) o).destroy(name, instance);
        }

        destroyCarbonado(config, name, instance);

        event("CarbonadoDisconnectEnd", asList(name, config));
    }

    @Nonnull
    private Repository createRepository(@Nonnull Map<String, Object> config, @Nonnull String name) throws RepositoryException {
        String type = getConfigValueAsString(config, "type", "map");
        if ("jdbc".equalsIgnoreCase(type)) {
            return createJDBCRepository(getConfigValue(config, "jdbc", Collections.<String, Object>emptyMap()), name);
        } else if ("bdb".equalsIgnoreCase(type)) {
            return createBDBRepository(getConfigValue(config, "bdb", Collections.<String, Object>emptyMap()), name);
        } else if ("map".equalsIgnoreCase(type)) {
            return createMapRepository(getConfigValue(config, "map", Collections.<String, Object>emptyMap()), name);
        } else {
            throw new IllegalArgumentException("Unknown repository type '" + type + "'. Valid values are jdbc, bdb, map");
        }
    }

    @Nonnull
    private Repository createJDBCRepository(Map<String, Object> properties, String name) throws RepositoryException {
        JDBCRepositoryBuilder builder = new JDBCRepositoryBuilder();
        builder.setName(name);
        builder.setDataSource(getDataSource(name));
        setPropertiesNoException(builder, properties);
        return builder.build();
    }

    @Nonnull
    private Repository createBDBRepository(Map<String, Object> properties, String name) throws RepositoryException {
        BDBRepositoryBuilder builder = new BDBRepositoryBuilder();
        builder.setName(name);
        setPropertiesNoException(builder, properties);
        return builder.build();
    }

    @Nonnull
    private Repository createMapRepository(Map<String, Object> properties, String name) throws RepositoryException {
        MapRepositoryBuilder builder = new MapRepositoryBuilder();
        builder.setName(name);
        setPropertiesNoException(builder, properties);
        return builder.build();
    }

    private void destroyCarbonado(@Nonnull Map<String, Object> config, @Nonnull String name, @Nonnull Repository repository) {
        String type = getConfigValueAsString(config, "type", "map");
        if ("jdbc".equalsIgnoreCase(type)) {
            closeDataSource(name);
        }
    }

    private void closeDataSource(@Nonnull String dataSourceName) {
        DataSource dataSource = dataSourceStorage.get(dataSourceName);
        if (dataSource != null) {
            dataSourceFactory.destroy(dataSourceName, dataSource);
            dataSourceStorage.remove(dataSourceName);
        }
    }

    @Nonnull
    private DataSource getDataSource(@Nonnull String dataSourceName) {
        DataSource dataSource = dataSourceStorage.get(dataSourceName);
        if (dataSource == null) {
            dataSource = dataSourceFactory.create(dataSourceName);
            dataSourceStorage.set(dataSourceName, dataSource);
        }
        return dataSource;
    }
}
