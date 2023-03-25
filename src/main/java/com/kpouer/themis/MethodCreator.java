/*
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
package com.kpouer.themis;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *  Component creator.
 *  It will create the component using it's constructor.
 * @param <T>
 */
class MethodCreator<T> extends AbstractCreator<T> {
    private final ComponentDefinition<T> componentDefinition;
    private final Method method;

    public MethodCreator(ThemisImpl themis, ComponentDefinition<T> componentDefinition, Method method) {
        super(themis);
        this.componentDefinition = componentDefinition;
        this.method = method;
    }

    @Override
    public T create() {
        var parameterTypes = method.getParameterTypes();
        try {
            T instance = componentDefinition.getInstance();
            T component;
            if (parameterTypes.length == 0) {
                component = (T) method.invoke(instance);
            } else {
                component = (T) method.invoke(instance, themis.getArgs(parameterTypes));
            }
            invokePostConstruct(component);
            return component;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ComponentIocException(e);
        }
    }
}
