/*
The MIT License (MIT)
Copyright (c) 2023 Matthieu Casanova

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
    private final Class<T> clazz;
    private final boolean singleton;
    private final boolean lazy;
    private Creator<T> creator;
    private T instance;

    public ComponentDefinition(Class<T> clazz, boolean singleton, boolean lazy, Creator<T> creator) {
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

    public static <T> ComponentDefinition<T> create(ThemisImpl themis, ComponentDefinition<T> componentDefinition, Method method, boolean singleton, boolean lazy) {
        var creator = new MethodCreator<T>(themis, componentDefinition, method);
        var clazz = (Class<T>) method.getReturnType();
        return new ComponentDefinition<>(clazz, singleton, lazy, creator);
    }

    public static <T> ComponentDefinition<T> create(ThemisImpl themis, Class<T> clazz, boolean singleton, boolean lazy) {
        ConstructorCreator<T> creator = new ConstructorCreator<>(themis, clazz);
        return new ComponentDefinition<>(clazz, singleton, lazy, creator);
    }

    public T getInstance() {
        synchronized (this) {
            if (instance == null) {
                var newInstance = creator.create();
                if (singleton) {
                    // if it is a singleton then we store the instance and release the creator
                    this.instance = newInstance;
                    creator = null;
                }
                return newInstance;
            }
        }
        return instance;
    }
}
