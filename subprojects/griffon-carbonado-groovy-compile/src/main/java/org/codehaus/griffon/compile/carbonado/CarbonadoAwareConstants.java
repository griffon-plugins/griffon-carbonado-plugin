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
package org.codehaus.griffon.compile.carbonado;

import org.codehaus.griffon.compile.core.BaseConstants;
import org.codehaus.griffon.compile.core.MethodDescriptor;

import static org.codehaus.griffon.compile.core.MethodDescriptor.annotatedMethod;
import static org.codehaus.griffon.compile.core.MethodDescriptor.annotatedType;
import static org.codehaus.griffon.compile.core.MethodDescriptor.annotations;
import static org.codehaus.griffon.compile.core.MethodDescriptor.args;
import static org.codehaus.griffon.compile.core.MethodDescriptor.method;
import static org.codehaus.griffon.compile.core.MethodDescriptor.type;
import static org.codehaus.griffon.compile.core.MethodDescriptor.typeParams;
import static org.codehaus.griffon.compile.core.MethodDescriptor.types;

/**
 * @author Andres Almiray
 */
public interface CarbonadoAwareConstants extends BaseConstants {
    String REPOSITORY_TYPE = "com.amazon.carbonado.Repository";
    String CARBONADO_HANDLER_TYPE = "griffon.plugins.carbonado.CarbonadoHandler";
    String REPOSITORY_CALLBACK_TYPE = "griffon.plugins.carbonado.RepositoryCallback";
    String CARBONADO_HANDLER_PROPERTY = "carbonadoHandler";
    String CARBONADO_HANDLER_FIELD_NAME = "this$" + CARBONADO_HANDLER_PROPERTY;

    String METHOD_WITH_CARBONADO = "withCarbonado";
    String METHOD_CLOSE_CARBONADO = "closeCarbonado";
    String REPOSITORY_NAME = "repositoryName";
    String CALLBACK = "callback";

    MethodDescriptor[] METHODS = new MethodDescriptor[]{
        method(
            type(VOID),
            METHOD_CLOSE_CARBONADO
        ),
        method(
            type(VOID),
            METHOD_CLOSE_CARBONADO,
            args(annotatedType(types(type(JAVAX_ANNOTATION_NONNULL)), JAVA_LANG_STRING))
        ),

        annotatedMethod(
            annotations(JAVAX_ANNOTATION_NONNULL),
            type(R),
            typeParams(R),
            METHOD_WITH_CARBONADO,
            args(annotatedType(annotations(JAVAX_ANNOTATION_NONNULL), REPOSITORY_CALLBACK_TYPE, R))
        ),
        annotatedMethod(
            types(type(JAVAX_ANNOTATION_NONNULL)),
            type(R),
            typeParams(R),
            METHOD_WITH_CARBONADO,
            args(
                annotatedType(annotations(JAVAX_ANNOTATION_NONNULL), JAVA_LANG_STRING),
                annotatedType(annotations(JAVAX_ANNOTATION_NONNULL), REPOSITORY_CALLBACK_TYPE, R))
        )
    };
}
