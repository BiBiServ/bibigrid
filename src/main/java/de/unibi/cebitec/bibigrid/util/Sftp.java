package de.unibi.cebitec.bibigrid.util;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import de.unibi.cebitec.bibigrid.ansible.PlaybookBuilder;


import java.io.*;
import java.util.ArrayList;
import java.util.List;



/**
 * Simple usage of sftp for copying local files to remote hosts.
 *
 * @author Alex Walender <awalende@cebitec.uni-bielefeld.de>
 */
public class Sftp {

    public static String jarPath = PlaybookBuilder.transformToPath(Sftp.class.getProtectionDomain().getCodeSource().getLocation().getPath());
    private static List<String> alreadyCreatedFolders = new ArrayList<>();

    private final String SSH_MODE = "sftp";
    private Session session;
    private ChannelSftp channelSftp;


    public Sftp(Session session) throws JSchException {
        this.session = session;
        this.channelSftp = (ChannelSftp) this.session.openChannel(SSH_MODE);
        this.channelSftp.connect();
    }

    /**
     * Copys a local file to a remote host over sftp. It uses the local filename as remote filename.
     * Will also create the target directory if it's not nested.
     * @param localFilePath The absolute path of the local file.
     * @param remoteFilePath The absolute path of where the file should be copied to.
     */
    public void putSingleToRemote(String localFilePath, String remoteFilePath){
        File localFile = new File(localFilePath);
        mkdir(remoteFilePath);
        try {
            channelSftp.cd(remoteFilePath);
            channelSftp.put(new FileInputStream(localFile), localFile.getName());
        } catch (SftpException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Transfers all files at once, which are contained in the worker list, to the remote host.
     * @param fileList The list of local files to be copied.
     * @param targetPath The target path on the remote host where the copied files will be stored.
     */
    public void putListToRemote(List<File> fileList, String targetPath){
        for(File e : fileList){
            mkdir(targetPath);
            try{
                channelSftp.cd(targetPath);
                channelSftp.put(new FileInputStream(e), e.getName());
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            } catch (SftpException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * Transfers all files at once, which are contained in the worker list, to the remote host.
     * @param fileList The list of local files to be copied.
     * @param targetPath The target path on the remote host where the copied files will be stored.
     * @param targetFileSuffix Adds a suffix to the target file name.
     */
    public void putListToRemote(List<File> fileList, String targetPath, String targetFileSuffix){
        for(File e : fileList){
            mkdir(targetPath);
            try{
                channelSftp.cd(targetPath);
                channelSftp.put(new FileInputStream(e), targetFileSuffix + e.getName());
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            } catch (SftpException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * Creates a directory in the given absolute path on a remote host.
     * Warning: Don't try to create nested directorys, right now you have to create every directory manually.
     * It maybe featured in future releases.
     * @param path The absolute path which shall be created on the remote host.
     **/
    public void mkdir(String path) {
        if(alreadyCreatedFolders.contains(path)){
            return;
        }
        try {
            channelSftp.mkdir(path);
            alreadyCreatedFolders.add(path);
        } catch (SftpException e) {

        }
    }

    /**
     * Disconnects the ssh channel to the remote host.
     */
    public void disconnectChannel(){
        channelSftp.disconnect();
    }

    /**
     * Disconnects the whole ssh connection to the host.
     */
    public void disconnectSession(){
        session.disconnect();
    }


    /*
    public class EntryFile {
        private File file;
        private String remotePath;


        public EntryFile(File file, String remotePath){
            this.file = file;
            this.remotePath = remotePath;
        }

        public File getFile() {
            return file;
        }

        public String getRemotePath() {
            return remotePath;
        }
    }
    */
}


