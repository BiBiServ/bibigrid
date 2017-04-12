package de.unibi.cebitec.bibigrid.ansible;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

/**
 * Created by awalende on 11.04.17.
 */
public class Playbook {

    private String content;
    private int priority = Integer.MAX_VALUE;
    private File file;
    private String fileName;

    public Playbook(File file, int priority){
        this.file = file;
        this.priority = priority;
        this.fileName = file.getName();
        //@TODO Read file to content?
    }

    public Playbook(String content, String fileName, int priority){
        this.content = content;
        this.fileName = fileName;
        this.priority = priority;
    }

    public File getFile(){
        if(file != null){
            return file;
        }
        try {
            PrintWriter printWriter = new PrintWriter("tmp/"+fileName, "UTF-8");
            printWriter.println(content);
            printWriter.flush();
            printWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return new File("tmp/"+fileName);

    }

    public String getContent() {
        return content;
    }

    public int getPriority() {
        return priority;
    }

    public String getFileName() {
        return fileName;
    }
}
