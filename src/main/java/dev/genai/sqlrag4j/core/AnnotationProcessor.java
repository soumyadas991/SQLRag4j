package dev.genai.sqlrag4j.core;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import dev.genai.sqlrag4j.annotation.LLMQuery;
import dev.genai.sqlrag4j.api.Assistant;

/**
 * Responsible for scanning classes in a package and creating proxies for interfaces
 * with methods annotated using @LLMQuery.
 */
public class AnnotationProcessor {

    private final Assistant assistant;

    public AnnotationProcessor(Assistant assistant) {
        this.assistant = assistant;
    }

    /**
     * Creates a proxy for an interface in the given package that contains at least one method annotated with @LLMQuery.
     *
     * @param packageName package to scan
     * @param <T>         type of the interface
     * @return proxy instance implementing the annotated interface
     * @throws ClassNotFoundException if no class is found
     * @throws IOException            if package scanning fails
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(String packageName) throws ClassNotFoundException, IOException {
    	Class<?> clazz = findClassWithMethodAnnotatedByLLMQuery(packageName);
        return (T) Proxy.newProxyInstance(
            clazz.getClassLoader(),
            new Class[]{clazz},
            new Handler(assistant)
        );
    }

    private static class Handler implements InvocationHandler {

        private final Assistant assistant;

        public Handler(Assistant assistant) {
            this.assistant = assistant;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.isAnnotationPresent(LLMQuery.class)) {
                if (args == null || args.length == 0 || !(args[0] instanceof String)) {
                    throw new IllegalArgumentException("Method must have a String parameter for the query");
                }
                String query = (String) args[0];
                return assistant.answer(query);
            }
            return null; // or throw new UnsupportedOperationException("Method not annotated");
        }
    }
    
    /**
     * Invocation handler that routes @LLMQuery-annotated methods to the Assistant.
     */
    public static Class<?> findClassWithMethodAnnotatedByLLMQuery(String packageName)
            throws IOException, ClassNotFoundException {

        Set<Class<?>> matchedClasses = new HashSet<>();

        String path = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL packageURL = classLoader.getResource(path);

        if (packageURL == null) {
            throw new IllegalArgumentException("Package " + packageName + " not found.");
        }

        File folder = new File(packageURL.getFile());
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".class"));

        if (files != null) {
            for (File file : files) {
                String className = packageName + "." + file.getName().replace(".class", "");
                Class<?> clazz = Class.forName(className);

                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(LLMQuery.class)) {
                        matchedClasses.add(clazz);
                        break;
                    }
                }
            }
        }

        return  matchedClasses.iterator().next();
    }
}
