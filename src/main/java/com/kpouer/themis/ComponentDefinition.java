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

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;

/**
 * @author Matthieu Casanova
 */
@Getter
@Setter
class ComponentDefinition<T> {
    private final Class<T>   clazz;
    private       Creator<T> creator;
    private final boolean    singleton;
    private final boolean lazy;
    private T instance;

    public ComponentDefinition(Class<T> clazz, Creator<T> creator, boolean singleton, boolean lazy) {
        this.clazz = clazz;
        this.creator = creator;
        this.singleton = singleton;
        this.lazy = lazy;
    }

    private ComponentDefinition(T instance) {
        clazz = (Class<T>) instance.getClass();
        this.instance = instance;
        singleton = true;
        lazy = false;
    }

    /**
     * Create a singleton component definition.
     * It can be used when adding a singleton to the components
     *
     * @param instance the instance of the component
     * @return the component definition
     * @param <T> the type of the component
     */
    public static <T> ComponentDefinition<T> createSingleton(T instance) {
        return new ComponentDefinition<>(instance);
    }

    public static <T> ComponentDefinition<T> create(DefaultThemisImpl themis, ComponentDefinition<T> componentDefinition, Method method, boolean singleton, boolean lazy) {
        var creator = new MethodCreator<T>(themis, componentDefinition, method);
        var clazz = (Class<T>) method.getReturnType();
        return new ComponentDefinition<>(clazz, creator, singleton, lazy);
    }

    public static <T> ComponentDefinition<T> create(DefaultThemisImpl themis, Class<T> clazz, boolean singleton, boolean lazy) {
        ConstructorCreator<T> creator = new ConstructorCreator<>(themis, clazz);
        return new ComponentDefinition<>(clazz, creator, singleton, lazy);
    }

    public T getInstance() {
        synchronized (this) {
            if (instance == null) {
                var instance = creator.create();
                if (singleton) {
                    // if it is a singleton then we store the instance and release the creator
                    this.instance = instance;
                    creator = null;
                }
            }
        }
        return instance;
    }
}
