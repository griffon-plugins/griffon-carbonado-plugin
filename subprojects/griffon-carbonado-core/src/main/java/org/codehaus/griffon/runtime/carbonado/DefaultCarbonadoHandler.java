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
import griffon.plugins.carbonado.CarbonadoHandler;
import griffon.plugins.carbonado.RepositoryCallback;
import griffon.plugins.carbonado.RepositoryFactory;
import griffon.plugins.carbonado.RepositoryStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

import static griffon.util.GriffonNameUtils.requireNonBlank;
import static java.util.Objects.requireNonNull;

/**
 * @author Andres Almiray
 */
public class DefaultCarbonadoHandler implements CarbonadoHandler {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultCarbonadoHandler.class);
    private static final String ERROR_VOLUME_NAME_BLANK = "Argument 'repositoryName' must not be blank";
    private static final String ERROR_REPOSITORY_NULL = "Argument 'repository' must not be null";
    private static final String ERROR_CALLBACK_NULL = "Argument 'callback' must not be null";

    private final RepositoryFactory repositoryFactory;
    private final RepositoryStorage repositoryStorage;

    @Inject
    public DefaultCarbonadoHandler(@Nonnull RepositoryFactory repositoryFactory, @Nonnull RepositoryStorage repositoryStorage) {
        this.repositoryFactory = requireNonNull(repositoryFactory, "Argument 'repositoryFactory' must not be null");
        this.repositoryStorage = requireNonNull(repositoryStorage, "Argument 'repositoryStorage' must not be null");
    }

    @Nullable
    @Override
    public <R> R withCarbonado(@Nonnull RepositoryCallback<R> callback) {
        return withCarbonado(DefaultRepositoryFactory.KEY_DEFAULT, callback);
    }

    @Nullable
    @Override
    public <R> R withCarbonado(@Nonnull String repositoryName, @Nonnull RepositoryCallback<R> callback) {
        requireNonBlank(repositoryName, ERROR_VOLUME_NAME_BLANK);
        requireNonNull(callback, ERROR_CALLBACK_NULL);

        Repository repository = getRepository(repositoryName);
        return doWithCarbonado(repositoryName, repository, callback);
    }

    @Nullable
    static <R> R doWithCarbonado(@Nonnull String repositoryName, @Nonnull Repository repository, @Nonnull RepositoryCallback<R> callback) {
        requireNonBlank(repositoryName, ERROR_VOLUME_NAME_BLANK);
        requireNonNull(repository, ERROR_REPOSITORY_NULL);
        requireNonNull(callback, ERROR_CALLBACK_NULL);

        LOG.debug("Executing statements on carbonado '{}'", repositoryName);
        return callback.handle(repositoryName, repository);
    }

    @Override
    public void closeCarbonado() {
        closeCarbonado(DefaultRepositoryFactory.KEY_DEFAULT);
    }

    @Override
    public void closeCarbonado(@Nonnull String repositoryName) {
        Repository repository = repositoryStorage.get(repositoryName);
        if (repository != null) {
            repositoryFactory.destroy(repositoryName, repository);
            repositoryStorage.remove(repositoryName);
        }
    }

    @Nonnull
    private Repository getRepository(@Nonnull String repositoryName) {
        Repository repository = repositoryStorage.get(repositoryName);
        if (repository == null) {
            repository = repositoryFactory.create(repositoryName);
            repositoryStorage.set(repositoryName, repository);
        }
        return repository;
    }
}
