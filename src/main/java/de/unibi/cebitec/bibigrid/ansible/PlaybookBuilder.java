package de.unibi.cebitec.bibigrid.ansible;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by awalende on 11.04.17.
 */
public class PlaybookBuilder {



    public static Map<String, File> extractBuiltInPlaybooks(){


        Map<String, File> map =  new HashMap<>();
        File folder = new File("playbooks/");
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++){
            if(listOfFiles[i].isFile()) map.put(listOfFiles[i].getName(), listOfFiles[i]);
            System.out.println("ADDED: " + listOfFiles[i].getName());
        }

        return map;
    }
}
