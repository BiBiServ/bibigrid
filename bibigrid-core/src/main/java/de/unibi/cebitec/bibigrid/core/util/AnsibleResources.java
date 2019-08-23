package de.unibi.cebitec.bibigrid.core.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility class AnsibleResources provides some resources (const + function)
 * concerning the ansible setup on master node.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public final class AnsibleResources {
    public static final String UPLOAD_PATH = "/tmp/roles/";
    public static final String ROOT_PATH = "playbook/";
    public static final String ANSIBLE_HOSTS = "ansible_hosts";
    public static final String VARS_PATH = "vars/";
    public static final String ROLES_PATH = "roles/";
    public static final String COMMON_YML = VARS_PATH + "common.yml";
    public static final String SITE_YML = "site.yml";
    public static final String REQUIREMENTS_YML = "requirements.yml";
    public static final String HOSTS_CONFIG_FILE = ROOT_PATH + ANSIBLE_HOSTS;
    public static final String CONFIG_ROOT_PATH = ROOT_PATH + VARS_PATH;
    public static final String ROLES_ROOT_PATH = ROOT_PATH + ROLES_PATH;
    public static final String COMMONS_CONFIG_FILE = ROOT_PATH + COMMON_YML;
    public static final String SITE_CONFIG_FILE = ROOT_PATH + SITE_YML;
    public static final String REQUIREMENTS_CONFIG_FILE = ROOT_PATH + REQUIREMENTS_YML;
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

    /**
     * Walks through folder structure to add to files list.
     *
     * @param loader ClassLoader to gather path
     * @param path actual path, starts with root path
     */
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

    /**
     * Adds file paths to files list.
     *
     * @param path
     */
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

    /**
     * Creates list of paths to folders given file list.
     *
     * @param files list of files in file structure
     * @return list of folders
     */
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
