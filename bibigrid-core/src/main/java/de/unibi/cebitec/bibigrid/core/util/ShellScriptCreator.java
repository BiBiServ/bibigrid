package de.unibi.cebitec.bibigrid.core.util;

import de.unibi.cebitec.bibigrid.core.model.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates user data and other shell scripts that are executed on instances during startup.
 *
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public final class ShellScriptCreator {
    private static final Logger LOG = LoggerFactory.getLogger(ShellScriptCreator.class);

    public static String getUserData(Configuration config, ClusterKeyPair keypair, boolean base64) {
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
        userData.append("log \"umount possibly mounted ephemeral\"\n");
        userData.append("umount /mnt\n");
        userData.append("log \"userdata.finished\"\n");
        userData.append("exit 0\n");
        return base64 ? Base64.getEncoder().encodeToString(userData.toString().getBytes()) : userData.toString();
    }


    private static void appendDisableAptDailyService(StringBuilder userData) {
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
        if (config.getSshPublicKeyFile() != null) {
            Path publicKeyFile = Paths.get(config.getSshPublicKeyFile());
            try {
                String publicKey = new String(Files.readAllBytes(publicKeyFile));
                // Check if we have a public key like putty saves them
                if (publicKey.startsWith("----")) {
                    String publicKeyFilename = "bibigrid-ssh-public-key-file.pub";
                    userData.append("echo '").append(publicKey).append("' > ").append(userSshPath)
                            .append(publicKeyFilename).append("\n");
                    userData.append("ssh-keygen -i -f ").append(userSshPath).append(publicKeyFilename).append(" >> ")
                            .append(userSshPath).append("authorized_keys\n");
                    userData.append("rm ").append(userSshPath).append(publicKeyFilename).append("\n");
                } else {
                    userData.append("echo '").append(publicKey).append("' >> ").append(userSshPath)
                            .append("authorized_keys\n");
                }
            } catch (IOException e) {
                LOG.error("Failed to add ssh public key file '{}'. {}", config.getSshPublicKeyFile(), e);
            }
        }
        userData.append("cat > ").append(userSshPath).append("config << SSHCONFIG\n");
        userData.append("Host *\n");
        userData.append("\tCheckHostIP no\n");
        userData.append("\tStrictHostKeyChecking no\n");
        userData.append("\tUserKnownHostsfile /dev/null\n");
        userData.append("SSHCONFIG\n");
    }

    /**
     * Builds script to configure ansible and execute ansible commands to install (galaxy) roles / playbooks.
     * @param prepare true, if still preparation necessary
     * @return script String to execute in CreateCluster
     */
    public static String getMasterAnsibleExecutionScript(final boolean prepare) {
        StringBuilder script = new StringBuilder();
        // apt-get update
        script.append("sudo apt-get update | sudo tee -a /var/log/ssh_exec.log\n");
        // install python2
        script.append("sudo DEBIAN_FRONTEND=noninteractive apt-get --yes  install apt-transport-https ca-certificates ")
                .append("software-properties-common python python-pip |sudo tee -a /var/log/ssh_exec.log\n");
        // Update pip to latest version
        script.append("sudo pip install --upgrade pip | sudo tee -a /var/log/ssh_exec.log\n");

        // Upgrade OpenSSL to fix ssl version problems on Ubuntu 16.04
        script.append("sudo python -m easy_install --upgrade pyOpenSSL\n");

        // Install setuptools from pypi using pip
        script.append("sudo pip install setuptools | sudo tee -a /var/log/ssh_exec.log\n");
        // Install ansible from pypi using pip
        script.append("sudo pip install ansible | sudo tee -a /var/log/ssh_exec.log\n");
        // Install python2 on slaves instances
        script.append("ansible slaves -i ~/" + AnsibleResources.HOSTS_CONFIG_FILE
                + " --become -m raw -a \"apt-get update && apt-get --yes install python\" | sudo tee -a /var/log/ansible.log\n");

        // Fix line endings to ensure windows files being used correctly
        script.append("for file in $(find " + AnsibleResources.ROOT_PATH + " -name '*.*'); do sed -i 's/\\r$//' \"$file\"; done\n");

        // Run ansible-galaxy to install ansible-galaxy roles from galaxy, git or url (.tar.gz)
        script.append("ansible-galaxy install --roles-path ~/"
                + AnsibleResources.ROLES_ROOT_PATH
                + " -r ~/" + AnsibleResources.REQUIREMENTS_CONFIG_FILE + "\n");

        // Extract ansible roles from files (.tar.gz, .tgz)
        script.append("cd ~/" + AnsibleResources.ROLES_ROOT_PATH + "\n");
        script.append("tar -xzf *.tgz\n");
        script.append("tar -xzf *.tar.gz\n");
        script.append("rm -rf *.tgz\n");
        script.append("rm -rf *.tar.gz\n");
        script.append("cd ~\n");

        // Execute ansible playbook
        script.append("ansible-playbook ~/" + AnsibleResources.SITE_CONFIG_FILE
                + " -i ~/" + AnsibleResources.HOSTS_CONFIG_FILE)
                .append(prepare ? " -t install" : "")
                .append(" | sudo tee -a /var/log/ansible-playbook.log\n");

        script.append("echo \"CONFIGURATION_FINISHED\"\n");
        return script.toString();
    }
}
