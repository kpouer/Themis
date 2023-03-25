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

import lombok.ToString;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Component creator.
 * It will create the component using it's constructor.
 *
 * @param <T>
 */
@ToString
class ConstructorCreator<T> extends AbstractCreator<T> {
    private final Class<T> clazz;

    ConstructorCreator(ThemisImpl themis, Class<T> clazz) {
        super(themis);
        this.clazz = clazz;
    }

    @Override
    public T create() {
        var constructors = (Constructor<T>[]) clazz.getConstructors();
        if (constructors.length != 1)
            throw new ComponentIocException(clazz.getName() + " must have only one constructor");

        var constructor = constructors[0];
        var parameters = constructor.getParameters();
        try {
            T component;
            if (parameters.length == 0) {
                component = constructor.newInstance();
            } else {
                var args = themis.getArgs(parameters);
                component = constructor.newInstance(args);
            }
            invokePostConstruct(component);
            return component;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new ComponentIocException(e);
        }
    }
}
