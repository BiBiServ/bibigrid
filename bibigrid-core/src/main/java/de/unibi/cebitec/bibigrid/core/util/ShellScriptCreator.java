package de.unibi.cebitec.bibigrid.core.util;

import de.unibi.cebitec.bibigrid.core.model.Configuration;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates user data and other shell scripts that are executed on instances.
 * <p/>
 * User data script steps executed on instance initialisation:
 * <ol>
 * <li>Add the generated keypair to ssh config</li>
 * <li>Execute early shell script provided by user</li>
 * </ol>
 *
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 * @author alueckne(at)cebitec.uni-bielefeld.de
 */
public final class ShellScriptCreator {
    private static final Logger LOG = LoggerFactory.getLogger(ShellScriptCreator.class);

    public static String getSlaveUserData(Configuration cfg, ClusterKeyPair keypair, boolean base64) {
        StringBuilder userData = new StringBuilder();
        userData.append("#!/bin/bash\n");
        userData.append("exec > /var/log/userdata.log\n");
        userData.append("exec 2>&1\n");
        userData.append("source /home/ubuntu/.bashrc\n");
        userData.append("function log { date +\"%x %R:%S - ${1}\";}\n");
        appendSshConfiguration(userData, keypair);
        appendEarlyExecuteScript(userData, cfg.getEarlySlaveShellScriptFile());
        // Log the finished message to notify the read loop
        userData.append("log \"userdata.finished\"\n");
        userData.append("exit 0\n");
        LOG.info(V, "Slave userdata:\n{}", userData.toString());
        return base64 ? new String(Base64.encodeBase64(userData.toString().getBytes())) : userData.toString();
    }

    private static void appendSshConfiguration(StringBuilder userData, ClusterKeyPair keypair) {
        userData.append("echo '").append(keypair.getPrivateKey()).append("' > /home/ubuntu/.ssh/id_rsa\n");
        userData.append("chown ubuntu:ubuntu /home/ubuntu/.ssh/id_rsa\n");
        userData.append("chmod 600 /home/ubuntu/.ssh/id_rsa\n");
        userData.append("echo '").append(keypair.getPublicKey()).append("' >> /home/ubuntu/.ssh/authorized_keys\n");
        userData.append("cat > /home/ubuntu/.ssh/config << SSHCONFIG\n");
        userData.append("Host *\n");
        userData.append("\tCheckHostIP no\n");
        userData.append("\tStrictHostKeyChecking no\n");
        userData.append("\tUserKnownHostsfile /dev/null\n");
        userData.append("SSHCONFIG\n");
    }

    private static void appendEarlyExecuteScript(StringBuilder userData, Path earlyShellScriptFile) {
        if (earlyShellScriptFile == null) {
            return;
        }
        try {
            String base64 = new String(Base64.encodeBase64(Files.readAllBytes(earlyShellScriptFile)));
            if (base64.length() > 10000) {
                LOG.info("Early shell script file too large (base64 encoded size exceeds 10000 chars)");
            } else {
                userData.append("echo ").append(base64);
                userData.append(" | base64 --decode | bash - 2>&1 > /var/log/earlyshellscript.log\n");
                userData.append("log \"earlyshellscript executed\"\n");
            }
        } catch (IOException e) {
            LOG.info("Early shell script could not be read.");
        }
    }

    public static String getMasterUserData(Configuration cfg, ClusterKeyPair keypair, boolean base64) {
        StringBuilder userData = new StringBuilder();
        userData.append("#!/bin/bash\n");
        userData.append("exec > /var/log/userdata.log\n");
        userData.append("exec 2>&1\n");
        userData.append("source /home/ubuntu/.bashrc\n");
        userData.append("function log { date +\"%x %R:%S - ${1}\";}\n");
        appendSshConfiguration(userData, keypair);
        appendEarlyExecuteScript(userData, cfg.getEarlyMasterShellScriptFile());
        // Log the finished message to notify the read loop
        userData.append("log \"userdata.finished\"\n");
        userData.append("exit 0\n");
        LOG.info(V, "Master userdata:\n{}", userData.toString());
        return base64 ? new String(Base64.encodeBase64(userData.toString().getBytes())) : userData.toString();
    }

    public static String getMasterAnsibleExecutionScript() {
        StringBuilder script = new StringBuilder();
        script.append("echo \"Update apt-get\"\n");
        script.append("sudo apt-get update\n");
        script.append("echo \"Install python2\"\n");
        script.append("sudo apt-get --yes --force-yes install apt-transport-https ca-certificates ")
                .append("software-properties-common python python-pip\n"); // TODO: -qq
        script.append("echo \"Install ansible from pypi using pip\"\n");
        script.append("sudo pip install ansible\n"); // TODO: -q
        script.append("echo \"Execute ansible playbook\"\n");
        script.append("sudo -E ansible-playbook ~/playbook/site.yml -i ~/playbook/ansible_hosts\n");
        script.append("echo \"CONFIGURATION_FINISHED\"\n");
        script.append("");
        script.append("");
        return script.toString();
    }
}
