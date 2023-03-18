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

import java.util.Map;

public interface Themis {
    <T> T getComponentOfType(Class<T> requiredType) throws ComponentIocException;

    <T> T getComponentOfType(String name, Class<T> requiredType) throws ComponentIocException;

    /**
     * Return all the components of the given type.
     * Lazy components will be initialized.
     * Singleton components will be initialized only once.
     *
     * @param requiredType the type of the components
     * @return a map of the components
     * @param <T> the type of the components
     * @throws ComponentIocException if a component can't be created
     */
    <T> Map<String, T> getComponentsOfType(Class<T> requiredType) throws ComponentIocException;
}
