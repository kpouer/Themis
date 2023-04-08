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

import com.kpouer.themis.annotation.Component;
import com.kpouer.themis.annotation.Qualifier;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

/**
 * The default implementation of Themis.
 *
 * @author Matthieu Casanova
 */
public class ThemisImpl implements Themis {
    /**
     * The registered components
     * key: the name of the component
     * value: the component definition.
     */
    private final Map<String, ComponentDefinition<?>> components = new ConcurrentHashMap<>();
    private List<ComponentDefinition<?>> pendingInitialization;

    public ThemisImpl(String pkg) {
        pendingInitialization = new ArrayList<>();
        registerSingletonInstance(Themis.class.getName(), this);
        loadPackage(pkg);
        initPendingComponents();
        pendingInitialization = null;
    }

    private void initPendingComponents() {
        while (!pendingInitialization.isEmpty()) {
            var iterator = pendingInitialization.iterator();
            boolean hasChanged = false;
            while (iterator.hasNext()) {
                var componentDefinition = iterator.next();
                try {
                    componentDefinition.getInstance();
                    iterator.remove();
                    hasChanged = true;
                } catch (ComponentIocException ignored) {
                    // the component is not ready yet
                }
            }
            if (!hasChanged) {
                throw new ComponentIocException("There is a circular dependency + " + pendingInitialization);
            }
        }
    }

    @Override
    public <T> T getComponentOfType(Class<T> requiredType) throws ComponentIocException {
        var annotation = requiredType.getAnnotation(Component.class);
        var name = annotation == null || annotation.value().isEmpty() ? requiredType.getSimpleName() : annotation.value();
        var definition = components.get(name.toLowerCase());
        if (definition == null) {
            for (ComponentDefinition<?> value : components.values()) {
                if (requiredType.isAssignableFrom(value.getClazz())) {
                    definition = value;
                    break;
                }
            }
        }
        if (definition == null) {
            throw new ComponentIocException("No bean with type " + requiredType + " is not registered");
        }
        return (T) definition.getInstance();
    }

    @Override
    public <T> T getComponentOfType(String name, Class<T> requiredType) throws ComponentIocException {
        ComponentDefinition<?> definition = components.get(name.toLowerCase());
        if (definition == null) {
            throw new ComponentIocException("The bean " + name + " is not registered");
        }

        if (!requiredType.isAssignableFrom(definition.getClazz())) {
            throw new ComponentIocException("The bean " + name + " is not of type " + requiredType.getName());
        }
        return (T) definition.getInstance();
    }

    @Override
    public <T> Map<String, T> getComponentsOfType(Class<T> requiredType) throws ComponentIocException {
        var result = new HashMap<String, T>();
        for (var entry : components.entrySet()) {
            ComponentDefinition<?> definition = entry.getValue();
            if (requiredType.isAssignableFrom(definition.getClazz())) {
                var instance = (T) definition.getInstance();
                result.put(entry.getKey(), instance);
            }
        }
        return result;
    }

    Object[] getArgs(Parameter[] parameters) {
        var args = new Object[parameters.length];
        for (var i = 0; i < parameters.length; i++) {
            var parameter = parameters[i];
            try {
                Class<?> type = parameter.getType();
                var qualifier = parameter.getAnnotation(Qualifier.class);
                Object component = null;
                if (qualifier != null) {
                    var name = qualifier.value();
                    if (name.isEmpty()) {
                        name = parameter.getName();
                    }
                    try {
                        component = getComponentOfType(name, type);
                    } catch (ComponentIocException e) {
                        // unable to find a component with the qualifier name
                    }
                }
                if (component == null) {
                    component = getComponentOfType(type);
                }
                args[i] = component;
            } catch (ComponentIocException e) {
                throw new ComponentIocException("Unable to create component of type " + parameter, e);
            }
        }
        return args;
    }

    /**
     * Register a singleton instance.
     * It's beans will be initialized.
     *
     * @param name     the name of the component
     * @param instance the instance to register
     */
    private void registerSingletonInstance(String name, Object instance) {
        ComponentDefinition<?> componentDefinition = ComponentDefinition.createSingleton(instance);
        registerComponentDefinition(name, componentDefinition);
    }

    private void registerComponentDefinition(String name, ComponentDefinition<?> componentDefinition) {
        if (components.put(name.toLowerCase(), componentDefinition) != null) {
            throw new ComponentIocException("The component " + name + " is already registered");
        }
        if (!componentDefinition.isLazy()) {
            try {
                componentDefinition.getInstance();
            } catch (ComponentIocException e) {
                if (e.getCause() != null) {
                    // there is an external cause, we will throw it
                    throw e;
                }
                // unable to create the instance now, we will try later
                pendingInitialization.add(componentDefinition);
            }
        }
        initMethodComponents(componentDefinition);
    }

    private void initMethodComponents(ComponentDefinition<?> componentDefinition) {
        var declaredMethods = componentDefinition.getClazz().getDeclaredMethods();
        for (var declaredMethod : declaredMethods) {
            var annotation = declaredMethod.getAnnotation(Component.class);
            if (annotation != null) {
                if (declaredMethod.getReturnType().equals(Void.TYPE)) {
                    throw new ComponentIocException("The method " + declaredMethod.getName() + " must return a value");
                }
                var name = annotation.value().isEmpty() ? declaredMethod.getName() : annotation.value();

                var definition = ComponentDefinition.create(this,
                                                            componentDefinition,
                                                            declaredMethod,
                                                            annotation.singleton(),
                                                            annotation.lazy());
                registerComponentDefinition(name, definition);
            }
        }
    }

    /**
     * Load the classes from a package.
     * Components are not instantiated at this time
     * @param pkg the package name
     */
    private void loadPackage(String pkg) {
        var classes = getClasses(pkg);
        for (var aClass : classes) {
            Component annotation = aClass.getAnnotation(Component.class);
            if (annotation != null) {
                var value = annotation.value();
                var name = value.isEmpty() ? aClass.getSimpleName() : value;
                registerComponentDefinition(name, ComponentDefinition.create(this, aClass, annotation.singleton(), annotation.lazy()));
            }
        }
    }

    private static List<Class<?>> getClasses(String packageName) {
        // Get name of package and turn it to a relative path
        // Get a File object for the package
        try {
            var relPath = packageName.replace('.', '/');
            var resources = ThemisImpl.class.getClassLoader().getResources(relPath);
            var classes = new ArrayList<Class<?>>();
            if (!resources.hasMoreElements()) {
                throw new ComponentIocException("Unexpected problem: No resource for " + relPath);
            } else {
                do {
                    var resource = resources.nextElement();
                    // If the resource is a jar get all classes from jar
                    if (resource.toString().startsWith("jar:")) {
                        classes.addAll(processJarfile(resource, packageName));
                    } else {
                        var dir = new File(resource.getPath());
                        classes.addAll(processDirectory(dir, packageName));
                    }
                } while (resources.hasMoreElements());
            }
            return classes;
        } catch (IOException e) {
            throw new ComponentIocException("Unexpected error loading resources", e);
        }
    }

    private static List<Class<?>> processJarfile(URL resource, String packageName) {
        var classes = new ArrayList<Class<?>>();
        // Turn package name to relative path to jar file
        var relPath = packageName.replace('.', '/');
        var resPath = resource.getPath();
        var jarPath = resPath.replaceFirst(".jar!.*", ".jar").replaceFirst("file:", "");

        try (var jarFile = new JarFile(jarPath)) {
            // attempt to load jar file

            // get contents of jar file and iterate through them
            var entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();

                // Get content name from jar file
                var    entryName = entry.getName();
                String className = null;

                // If content is a class save class name.
                if (entryName.endsWith(".class") && entryName.startsWith(relPath)
                    && entryName.length() > (relPath.length() + "/".length())) {
                    className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
                }

                // If content is a class add class to List
                if (className != null) {
                    classes.add(loadClass(className));
                }
            }
        } catch (IOException e) {
            throw new ComponentIocException("Unexpected IOException reading JAR File " + jarPath, e);
        }
        return classes;
    }

    private static List<Class<?>> processDirectory(File dir, String packageName) {
        var classes = new ArrayList<Class<?>>();
        var list = dir.list();
        if (list != null) {
            for (var file : list) {
                // we are only interested in .class files
                if (file.endsWith(".class")) {
                    // removes the .class extension
                    var cls = packageName + '.' + file.substring(0, file.length() - 6);
                    classes.add(loadClass(cls));
                }
                // If the file is a directory recursively class this method.
                var subdir = new File(dir, file);
                if (subdir.isDirectory()) {
                    classes.addAll(processDirectory(subdir, packageName + '.' + file));
                }
            }
        }
        return classes;
    }

    private static Class<?> loadClass(String cls) {
        try {
            return Class.forName(cls);
        } catch (ClassNotFoundException e) {
            throw new ComponentIocException(String.format("Unexpected ClassNotFoundException loading class [%s]", cls), e);
        }
    }
}
