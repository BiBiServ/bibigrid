package de.unibi.cebitec.bibigrid;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Factory scanning the classpath for mapping implementations.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public final class Factory {
    private static Factory instance;
    private Map<String, List<Class<?>>> interfaceClassMap;
    private Map<String, List<Class<?>>> baseClassMap;

    private Factory() {
        interfaceClassMap = new HashMap<>();
        baseClassMap = new HashMap<>();
        loadAllClasses();
    }

    public static Factory getInstance() {
        return instance != null ? instance : (instance = new Factory());
    }

    /**
     * Load all classes in the classpath and search for usable implementations.
     */
    private void loadAllClasses() {
        Set<String> allClassPaths = new HashSet<>();
        URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        for (URL url : classLoader.getURLs()) {
            try {
                File file = new File(url.toURI());
                if (file.isDirectory()) {
                    iterateFileSystem(file, allClassPaths, url.toString());
                } else if (file.isFile() &&
                        file.getName().toLowerCase(Locale.US).endsWith(".jar") &&
                        !file.getName().contains("rt.jar") &&
                        !file.getName().contains("idea_rt.jar") &&
                        !file.getName().contains("aws-java-sdk-ec2") &&
                        !file.getName().contains("proto-") &&
                        !file.getName().contains("google-cloud-") &&
                        !file.getName().contains("google-api-") &&
                        !file.getName().contains("openstack4j-core") &&
                        !file.getName().contains("selenium-") &&
                        !file.getName().contains("google-api-client") &&
                        !file.getName().contains("jackson-") &&
                        !file.getName().contains("guava") &&
                        !file.getName().contains("jetty") &&
                        !file.getName().contains("netty-") &&
                        !file.getName().contains("junit-")) {
                    iterateJarFile(file, allClassPaths);
                }
            } catch (URISyntaxException | IOException e) {
                e.printStackTrace();
            }
        }
        for (String classPath : allClassPaths) {
            loadClass(classLoader, classPath);
        }
    }

    private void iterateFileSystem(File directory, Set<String> allClassPaths, String rootPath) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    iterateFileSystem(file, allClassPaths, rootPath);
                } else if (file.isFile()) {
                    collectUrl(allClassPaths, file.toURI().toURL().toString(), rootPath);
                }
            }
        }
    }

    private void iterateJarFile(File file, Set<String> allClassPaths) throws IOException {
        JarFile jarFile = new JarFile(file);
        for (Enumeration<JarEntry> je = jarFile.entries(); je.hasMoreElements(); ) {
            JarEntry j = je.nextElement();
            if (!j.isDirectory()) {
                collectUrl(allClassPaths, j.getName(), null);
            }
        }
    }

    private void collectUrl(Set<String> allClassPaths, String url, String rootPath) {
        if (url.endsWith(".class") && url.contains("de/unibi/cebitec/bibigrid")) {
            if (rootPath != null) {
                url = url.replace(rootPath, "");
            }
            allClassPaths.add(url.replace("/", ".").replace(".class", ""));
        }
    }

    private void loadClass(URLClassLoader classLoader, String classPath) {
        Class<?> clazz = null;
        try {
            clazz = classLoader.loadClass(classPath);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (clazz == null) {
            return;
        }
        for (Class<?> classInterface : clazz.getInterfaces()) {
            String interfaceName = classInterface.getName();
            if (!interfaceClassMap.containsKey(interfaceName)) {
                interfaceClassMap.put(interfaceName, new ArrayList<>());
            }
            interfaceClassMap.get(interfaceName).add(clazz);
        }
        if (clazz.getSuperclass() != null) {
            String superclassName = clazz.getSuperclass().getName();
            if (!baseClassMap.containsKey(superclassName)) {
                baseClassMap.put(superclassName, new ArrayList<>());
            }
            baseClassMap.get(superclassName).add(clazz);
        }
    }

    public <T> List<Class<T>> getImplementations(Class<T> type) {
        String typeName = type.getName();
        if (interfaceClassMap.containsKey(typeName)) {
            List<Class<T>> result = new ArrayList<>();
            for (Class<?> clazz : interfaceClassMap.get(typeName)) {
                //noinspection unchecked
                result.add((Class<T>) clazz);
            }
            return result;
        }
        if (baseClassMap.containsKey(typeName)) {
            List<Class<T>> result = new ArrayList<>();
            for (Class<?> clazz : baseClassMap.get(typeName)) {
                //noinspection unchecked
                result.add((Class<T>) clazz);
            }
            return result;
        }
        return Collections.emptyList();
    }
}
