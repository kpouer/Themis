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

import lombok.RequiredArgsConstructor;

import javax.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@RequiredArgsConstructor
public abstract class AbstractCreator<T> implements Creator<T> {
    protected final DefaultThemisImpl themis;

    protected void invokePostConstruct(T component) {
        var methods = component.getClass().getMethods();
        for (Method method : methods) {
            if (method.getAnnotation(PostConstruct.class) != null) {
                try {
                    method.setAccessible(true);
                    method.invoke(component);
                } catch (IllegalAccessException e) {
                    throw new ComponentIocException("Method " + method.getName() + " of " + component.getClass().getName() + " is should be public", e);
                } catch (InvocationTargetException e) {
                    throw new ComponentIocException("Unable to call " + method.getName() + " of " + component.getClass().getName(), e);
                }
            }
        }
    }
}
