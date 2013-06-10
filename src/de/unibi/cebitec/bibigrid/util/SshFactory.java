/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.util;

import com.amazonaws.services.ec2.model.Instance;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import de.unibi.cebitec.bibigrid.model.Configuration;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SshFactory {

    public static final Logger log = LoggerFactory.getLogger(SshFactory.class);

    public static Session createNewSshSession(JSch ssh, String dns, String user, Path identity) {
        try {
            Session sshSession = ssh.getSession(user, dns, 22);
            ssh.addIdentity(identity.toString());

            UserInfo userInfo = new UserInfo() {
                @Override
                public String getPassphrase() {
                    return null;
                }

                @Override
                public String getPassword() {
                    return null;
                }

                @Override
                public boolean promptPassword(String string) {
                    return false;
                }

                @Override
                public boolean promptPassphrase(String string) {
                    return false;
                }

                @Override
                public boolean promptYesNo(String string) {
                    return true;
                }

                @Override
                public void showMessage(String string) {
                    log.info("SSH: {}", string);
                }
            };
            sshSession.setUserInfo(userInfo);
            return sshSession;
        } catch (JSchException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public static String buildSshCommand(Instance masterInstance, List<Instance> slaveInstances, Configuration cfg) {
        StringBuilder sb = new StringBuilder();
        String command = "./add_exec ";
        int slaveCores = InstanceInformation.getSpecs(cfg.getSlaveInstanceType()).instanceCores;
        ////////////////////////////////////////////////////////////////////
        //// Add master as submission host and add slaves as exec hosts ////
        sb.append("qconf -as ");
        sb.append(masterInstance.getPrivateDnsName());
        sb.append("\n");

        for (Instance e : slaveInstances) {
            sb.append(command);
            sb.append(e.getPrivateDnsName());
            sb.append(" ");
            sb.append(slaveCores);
            sb.append("\n");
        }

        ///////////////////////////////////////////////////////////////////////////////////
        //// Add master als execution host if there are only 2 or less slave instances ////
        if (cfg.getSlaveInstanceCount() < 3) {
            sb.append(command);
            sb.append(masterInstance.getPrivateDnsName());
            sb.append(" ");
            sb.append(InstanceInformation.getSpecs(cfg.getMasterInstanceType()).instanceCores);
            sb.append("\n");
            sb.append("sudo service gridengine-exec start\n");
        }

        if (cfg.getShellScriptFile() != null) {
            try {
                List<String> lines = Files.readAllLines(cfg.getShellScriptFile(), StandardCharsets.UTF_8);
                sb.append("cat > shellscript.sh << EOFCUSTOMSCRIPT \n");

                for (String e : lines) {
                    sb.append(e);
                    sb.append("\n");
                }
                sb.append("\nEOFCUSTOMSCRIPT\n");
                sb.append("bash shellscript.sh\n");
            } catch (IOException e) {
                log.info("Shell script could not be read.");
            }
        }

       
        return sb.toString();
    }
}
