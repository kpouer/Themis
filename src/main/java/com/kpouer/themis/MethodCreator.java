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

    MethodCreator(ThemisImpl themis, ComponentDefinition<T> componentDefinition, Method method) {
        super(themis);
        this.componentDefinition = componentDefinition;
        this.method = method;
    }

    @Override
    public T create() {
        var parameters = method.getParameters();
        try {
            T instance = componentDefinition.getInstance();
            T component;
            if (parameters.length == 0) {
                component = (T) method.invoke(instance);
            } else {
                component = (T) method.invoke(instance, themis.getArgs(parameters));
            }
            invokePostConstruct(component);
            return component;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ComponentIocException(e);
        }
    }
}
