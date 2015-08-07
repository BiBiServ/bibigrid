/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.test;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import static de.unibi.cebitec.bibigrid.ctrl.CreateIntent.log;
import de.unibi.cebitec.bibigrid.util.JSchLogger;
import de.unibi.cebitec.bibigrid.util.SshFactory;
import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 *
 * @author jkrueger
 */
public class TestSSH {

    public static void main(String[] main) throws Exception {
        String execCommand = "echo Bla  > bla.txt;\n"
                + "echo Hello World!;\n"
                + "echo Hello World! 1>&2;\n";

        JSch ssh = new JSch();
        JSch.setLogger(new JSchLogger());
        ssh.addIdentity("/homes/jkrueger/.ssh/amazon/AWSbibiserv2.pem");
        Session sshSession = SshFactory.createNewSshSession(ssh, "52.19.66.74", "ubuntu", new File("/homes/jkrueger/.ssh/amazon/AWSbibiserv2.pem").toPath());

        sshSession.connect();

        ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");

        BufferedReader br = new BufferedReader(new InputStreamReader(channel.getInputStream()));

        channel.setCommand(execCommand);

        channel.connect();

        String line;

        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }

        // upload
        String remoteDirectory = "/home/ubuntu/.ssh";
        String filename = "id_rsa";
        String localFile = "/homes/jkrueger/.ssh/amazon/AWSbibiserv2.pem";
        log.info(V, "Uploading key");
        ChannelSftp channelPut = (ChannelSftp) sshSession.openChannel("sftp");
        channelPut.connect();
        channelPut.cd(remoteDirectory);
        channelPut.put(new FileInputStream(localFile), filename);
        channelPut.disconnect();
        log.info(V, "Upload done");

        channel.disconnect();
        sshSession.disconnect();
    }
}
