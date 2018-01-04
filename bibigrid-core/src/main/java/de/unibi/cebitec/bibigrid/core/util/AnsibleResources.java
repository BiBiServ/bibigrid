package de.unibi.cebitec.bibigrid.core.util;

import java.io.File;
import java.net.URL;
import java.util.*;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public final class AnsibleResources {
    private static final String ROOT_PATH = "playbook";
    public static final String HOSTS_CONFIG_FILE = ROOT_PATH + "/ansible_hosts";
    public static final String COMMONS_CONFIG_FILE = ROOT_PATH + "/vars/common.yml";
    private final Map<String, File> files = new HashMap<>();

    public AnsibleResources() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        processFolder(loader, ROOT_PATH);
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
                files.put(filepath, file);
            }
        }
    }

    public Map<String, File> getFiles() {
        return files;
    }

    public List<String> getDirectories() {
        List<String> folderSet = new ArrayList<>();
        for (String filepath : files.keySet()) {
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
