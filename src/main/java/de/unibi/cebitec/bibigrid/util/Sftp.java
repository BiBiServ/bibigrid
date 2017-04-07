package de.unibi.cebitec.bibigrid.util;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple usage of sftp for copying local files to remote hosts.
 *
 * @author Alex Walender <awalende@cebitec.uni-bielefeld.de>
 */
public class Sftp {

    public static final Logger LOG = LoggerFactory.getLogger(Sftp.class);
    private final String SSH_MODE = "sftp";
    private String currentRemotePath = "~/";

    private Session session;
    private ChannelSftp channelSftp;



    private List<File> fileList;


    public Sftp(Session session) throws JSchException {
        this.session = session;
        this.channelSftp = (ChannelSftp) this.session.openChannel(SSH_MODE);
        this.channelSftp.connect();
        fileList = new ArrayList<>();
    }

    public void putSingle(String localFilePath, String remoteFilePath){
        File localFile = new File(localFilePath);
        //@TODO Work here...

    }

    public void mkdir(String path) {
        try {
            channelSftp.mkdir(path);
            currentRemotePath = path;
        } catch (SftpException e) {
            LOG.error("Could not make a directory in remote host.");
        }
    }

    public void disconnectChannel(){
        channelSftp.disconnect();
    }

    public void disconnectSession(){
        session.disconnect();
    }


    public List<File> getFileList() {
        return fileList;
    }
}
