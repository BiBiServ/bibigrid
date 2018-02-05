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
 * Creates user data and other shell scripts that are executed on instances during startup.
 *
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public final class ShellScriptCreator {
    private static final Logger LOG = LoggerFactory.getLogger(ShellScriptCreator.class);

    public static String getUserData(Configuration config, ClusterKeyPair keypair, boolean base64, boolean log){
        StringBuilder userData = new StringBuilder();
        userData.append("#!/bin/bash\n");
        // redirect output
        userData.append("exec > /var/log/userdata.log\n");
        userData.append("exec 2>&1\n");
        // source shell configuration
        userData.append("source /home/").append(config.getSshUser()).append("/.bashrc\n");
        // simple log function
        userData.append("function log { date +\"%x %R:%S - ${1}\";}\n");
        // iplookup function
        userData.append("function iplookup { nslookup ${1} | grep name | cut -f 2 -d '=' | cut -f 1 -d '.' | xargs; }\n");
        // Log local & resolved hostname
        userData.append("localip=$(curl --silent --show-error http://169.254.169.254/latest/meta-data/local-ipv4)\n");
        userData.append("log \"hostname is $(hostname)\"\n");
        userData.append("log \"hostname should be $(iplookup $localip)\"\n");
        // force set hostname
        userData.append("log \"set hostname\"\n");
        userData.append("hostname -b $(iplookup $localip)\n");
        userData.append("log \"disable apt-daily.(service|timer)\"\n");
        appendDisableAptDailyService(userData);
        userData.append("log \"configure ssh\"\n");
        appendSshConfiguration(config, userData, keypair);
        userData.append("log \"run early executescript\"\n");
        appendEarlyExecuteScript(userData, config.getEarlyMasterShellScriptFile());
        userData.append("log \"userdata.finished\"\n");
        userData.append("exit 0\n");
        if (log) {
            LOG.info(V, "Userdata:\n{}", userData.toString());
        }
        return base64 ? new String(Base64.encodeBase64(userData.toString().getBytes())) : userData.toString();
    }


    private static void appendDisableAptDailyService(StringBuilder userData){
        userData.append("systemctl stop apt-daily.service\n" +
                        "systemctl disable apt-daily.service\n" +
                        "systemctl stop apt-daily.timer\n" +
                        "systemctl disable apt-daily.timer\n" +
                        "systemctl kill --kill-who=all apt-daily.service\n" +
                        "while ! (systemctl list-units --all apt-daily.service | fgrep -q dead)\n" +
                        "do\n" +
                        "  sleep 1;\n" +
                        "done\n");
    }

    private static void appendSshConfiguration(Configuration config, StringBuilder userData, ClusterKeyPair keypair) {
        String user = config.getSshUser();
        String userSshPath = "/home/" + user + "/.ssh/";
        userData.append("echo '").append(keypair.getPrivateKey()).append("' > ").append(userSshPath).append("id_rsa\n");
        userData.append("chown ").append(user).append(":").append(user).append(" ").append(userSshPath)
                .append("id_rsa\n");
        userData.append("chmod 600 ").append(userSshPath).append("id_rsa\n");
        userData.append("echo '").append(keypair.getPublicKey()).append("' >> ").append(userSshPath)
                .append("authorized_keys\n");
        userData.append("cat > ").append(userSshPath).append("config << SSHCONFIG\n");
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

    public static String getMasterAnsibleExecutionScript() {
        StringBuilder script = new StringBuilder();
        // apt-get update
        script.append("sudo apt-get update | sudo tee -a /var/log/ssh_exec.log\n");
        // install python2
        script.append("sudo apt-get --yes  install apt-transport-https ca-certificates ")
                .append("software-properties-common python python-pip |sudo tee -a /var/log/ssh_exec.log\n");
        // Install ansible from pypi using pip
        script.append("sudo pip install ansible | sudo tee -a /var/log/ssh_exec.log\n" );
        // Install python2 on slaves instances
        script.append("ansible slaves -i ~/playook/ansible_hosts --sudo -m raw \"apt-get --yes install python\" | sudo tee -a /var/log/ansible.log\n");

        // Execute ansible playbook
        script.append("ansible-playbook ~/playbook/site.yml -i ~/playbook/ansible_hosts | sudo tee -a /var/log/ansible-playbook.log\n");
        script.append("echo \"CONFIGURATION_FINISHED\"\n");
        return script.toString();
    }
}
