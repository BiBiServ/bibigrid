/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.monitor.util;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import de.unibi.cebitec.bibigrid.util.JSchLogger;
import de.unibi.cebitec.bibigrid.util.SshFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author alueckne
 */
public class GridCommands {

    public static final Logger log = LoggerFactory.getLogger(GridCommands.class);
    
    private static final String HOST_GROUP_DELETE = "qconf -dattr hostgroup hostlist ";
    private static final String HOST_QUEUE_DELETE = "qconf -purge queue slots main.q@";
    private static final String DISABLE_HOST = "qmod -d main.q@";
    private static final String ALLHOSTS = "@allhosts";
    private static final String DELETE_NODE = "qconf -de ";
    private static final String FULL_QSTAT = "qstat -f -xml";

    public static final String deleteNodeFromQueue(String nodeIp) {
        return (HOST_QUEUE_DELETE + nodeIp + "\n");
    }

    public static final String deleteNodeFromHostGroup(String nodeIp) {
        return (HOST_GROUP_DELETE + nodeIp + " " + ALLHOSTS + "\n");
    }

    public static final String disableNode(String nodeIp) {
        return (DISABLE_HOST + nodeIp + "\n");
    }

    public static final String deleteNodeFromGrid(String nodeIp) {
        return (DELETE_NODE + nodeIp + "\n");
    }

    public static final String qStatXML() {
        return FULL_QSTAT;
    }

}
