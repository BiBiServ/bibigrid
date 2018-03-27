package de.unibi.cebitec.bibigrid.core.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public final class AnsibleResources {
    private static final String ROOT_PATH = "playbook/";
    public static final String HOSTS_CONFIG_FILE = ROOT_PATH + "ansible_hosts";
    public static final String CONFIG_ROOT_PATH = ROOT_PATH + "vars/";
    public static final String ROLES_ROOT_PATH = ROOT_PATH + "roles/";
    public static final String COMMONS_CONFIG_FILE = CONFIG_ROOT_PATH + "common.yml";
    public static final String SITE_CONFIG_FILE = ROOT_PATH + "site.yml";
    private final List<String> files = new ArrayList<>();

    public AnsibleResources() {
        ClassLoader loader = getClass().getClassLoader();
        // Check if the resources are in a jar file or in a classpath
        URL resolvedRootUrl = loader.getResource(ROOT_PATH);
        String resolvedRootPath = resolvedRootUrl != null ? resolvedRootUrl.toExternalForm() : null;
        if (resolvedRootPath != null && resolvedRootPath.startsWith("jar:")) {
            String jarPath = resolvedRootPath.split("!")[0].substring(4);
            if (jarPath.startsWith("file:")) {
                jarPath = new File(jarPath.substring(5)).getAbsolutePath();
            }
            processJar(jarPath);
        } else {
            processFolder(loader, ROOT_PATH);
        }
    }

    private void processFolder(ClassLoader loader, String path) {
        URL url = loader.getResource(path);
        if (url == null) {
            return;
        }
        File directory = new File(url.getPath());
        File[] directoryFiles = directory.listFiles();
        if (directoryFiles == null) {
            return;
        }
        for (File file : directoryFiles) {
            String filepath = path + "/" + file.getName();
            if (file.isDirectory()) {
                processFolder(loader, filepath);
            } else {
                files.add(filepath);
            }
        }
    }

    private void processJar(String path) {
        try {
            JarFile jarFile = new JarFile(new File(path));
            for (Enumeration<JarEntry> je = jarFile.entries(); je.hasMoreElements(); ) {
                JarEntry j = je.nextElement();
                if (!j.isDirectory() && j.getName().startsWith(ROOT_PATH)) {
                    files.add(j.getName());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getFiles() {
        return files;
    }

    public InputStream getFileStream(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }

    public List<String> getDirectories(List<String> files) {
        List<String> folderSet = new ArrayList<>();
        for (String filepath : files) {
            String folderPath = new File(filepath).getParent().replace("\\", "/");
            String[] parts = folderPath.split("/");
            String path = "";
            for (int i = 0; i < parts.length; i++) {
                path += (i > 0 ? "/" : "") + parts[i];
                if (!folderSet.contains(path)) {
                    folderSet.add(path);
                }
            }
        }
        folderSet.sort(Comparator.comparingInt(a -> a.split("/").length));
        return folderSet;
    }
}
