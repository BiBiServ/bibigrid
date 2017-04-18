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



    public static List<File> extractPlaybooks(String path){


        File folder = new File(path);
        File[] files = folder.listFiles();
        List<File> listOfFiles = new ArrayList<>();
        for(int i = 0; i < files.length; i++){
            if(files[i].isFile()) listOfFiles.add(files[i]);
        }
        return listOfFiles;
    }


}
