package de.unibi.cebitec.bibigrid.ansible;

import de.unibi.cebitec.bibigrid.model.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;

/**
 * Created by awalende on 11.04.17.
 * This class gathers ansible playbooks from various local directorys.
 */
public class PlaybookBuilder {


    public static final Logger LOG = LoggerFactory.getLogger(PlaybookBuilder.class);


    /**
     * Gathers local playbook files together to a list.
     * @param path The local path on where ansible playbooks are stored.
     * @return A list of all local playbook files from the given path.
     */
    public static List<File> extractPlaybooks(String path){
        String jarPath = (PlaybookBuilder.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        jarPath = transformToPath(jarPath);
        LOG.info(V, "Extracting Playbooks from: " + jarPath + path);

        File folder = new File(jarPath + path);
        File[] files = folder.listFiles();
        List<File> listOfFiles = new ArrayList<>();
        for(int i = 0; i < files.length; i++){
            if(files[i].isFile()){
                listOfFiles.add(files[i]);
            }
        }
        return listOfFiles;
    }

    /**
     * Gathers local playbook files together to a list.
     * @param conf BiBiGrid configuration object.
     * @return A list of all local playbook files from the given path inside the properties file or cl input.
     */
    public static List<File> extractPlaybooks(Configuration conf){
        List<File> listOfFiles = new ArrayList<>();
        LOG.info(V, "Extracting Playbooks from: "  + conf.getAdditionalPlaybookPath());
        File folder = new File(conf.getAdditionalPlaybookPath().toString());
        File[] files = folder.listFiles();
        for(int i = 0; i < files.length; i++){
            if(files[i].isFile()){
                listOfFiles.add(files[i]);
            }
        }
        return listOfFiles;
    }


    /**
     *
     * @param str The pathstring to the actual running jarfile which needs to be cleaned up.
     * @return A cleaned string, containing the path of this running jar.
     */
    public static String transformToPath(String str){
        String newstr = null;
        if (null != str && str.length() > 0 ) {
            int endIndex = str.lastIndexOf("/");
            if (endIndex != -1) {
                newstr = str.substring(0, endIndex);
                int dex = newstr.lastIndexOf("/");
                newstr = newstr.substring(0, dex+1);
            }
        }
        return newstr;
    }




}
