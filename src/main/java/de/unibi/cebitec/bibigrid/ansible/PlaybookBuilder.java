package de.unibi.cebitec.bibigrid.ansible;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by awalende on 11.04.17.
 */
public class PlaybookBuilder {


    public static final Logger LOG = LoggerFactory.getLogger(PlaybookBuilder.class);


    public static List<File> extractPlaybooks(String path){



        String jarPath = (PlaybookBuilder.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        jarPath = transformToPath(jarPath);
        LOG.info("Extracting Playbooks from: " + jarPath + path);

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
