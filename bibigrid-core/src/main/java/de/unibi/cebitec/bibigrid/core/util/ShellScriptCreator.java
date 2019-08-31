package de.unibi.cebitec.bibigrid.core.util;

import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.*;


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
        // log local hostname
        userData.append("localip=$(wget -O - --quiet http://169.254.169.254/latest/meta-data/local-ipv4 |cat)\n");
        userData.append("log \"hostname is $(hostname)\"\n");
        // check if resolved  differs from local hostname and force set resolved hostname as local name, needs nslookup
        userData.append("which nslookup\n");
        userData.append("if [ $? -eq 0 ]; then \n");
        userData.append("log \"hostname should be $(iplookup $localip)\"\n");
        userData.append("log \"set hostname\"\n");
        userData.append("hostname -b $(iplookup $localip)\n");
        userData.append("fi\n");
        // disableAptDailyService
        userData.append("log \"disable apt-daily.(service|timer)\"\n");
        appendDisableAptDailyService(userData);
        // configure SSH Config
        userData.append("log \"configure ssh\"\n");
        appendSshConfiguration(config, userData, keypair);
        // finished
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
     * @param config Configuration
     * @return script String to execute in CreateCluster
     */
    public static String getMasterAnsibleExecutionScript(final boolean prepare, final Configuration config) {
        StringBuilder script = new StringBuilder();
        // apt-get update
        script.append("sudo apt-get update | sudo tee -a /var/log/ssh_exec.log\n");
        // install python3
        script.append("sudo DEBIAN_FRONTEND=noninteractive apt-get --yes  install apt-transport-https ca-certificates ")
                .append("software-properties-common python3 python3-pip libffi-dev libssl-dev |sudo tee -a /var/log/ssh_exec.log\n");
        // Update pip to latest version
        script.append("sudo pip3 install --upgrade pip | sudo tee -a /var/log/ssh_exec.log\n");

        // Upgrade OpenSSL to fix ssl version problems on Ubuntu 16.04
        script.append("sudo python3 -m easy_install --upgrade pyOpenSSL\n");    // doesn't work

        // Install setuptools from pypi using pip
        script.append("sudo pip3 install setuptools | sudo tee -a /var/log/ssh_exec.log\n");
        // Install ansible from pypi using pip
        script.append("sudo pip3 install ansible | sudo tee -a /var/log/ssh_exec.log\n");
        // Install python3 on workers instances
        script.append("ansible workers -i ~/" + AnsibleResources.HOSTS_CONFIG_FILE
                + " --become -m raw -a \"apt-get update && apt-get --yes install python3\" | sudo tee -a /var/log/ansible.log\n");

        // Run ansible-galaxy to install ansible-galaxy roles from galaxy, git or url (.tar.gz)
        if (config.hasCustomAnsibleGalaxyRoles()) {
            script.append("ansible-galaxy install --roles-path ~/"
                    + AnsibleResources.ROLES_ROOT_PATH
                    + " -r ~/" + AnsibleResources.REQUIREMENTS_CONFIG_FILE + "\n");
        }
        // Extract ansible roles from files (.tar.gz, .tgz)
        script.append("cd ~/" + AnsibleResources.ROLES_ROOT_PATH + "\n");
        script.append("for f in $(find /tmp/roles -type f -regex '.*\\.t\\(ar\\.\\)?gz'); do tar -xzf $f; done\n");
        script.append("cd ~\n");

        // Fix line endings for all text based ansible file to ensure windows files being used correctly
        script.append("files=$(for f in $( find ~/playbook -type f); do  file ${f} | grep ASCII | cut -f 1 -d ':'; done;)\n");
        script.append("for file in ${file}; do sed -i 's/\\r$//' \"${file}\"; done\n");

        script.append("echo Execute ansible-playbook\n");
        script.append("sudo touch /var/log/ansible-playbook.log\n");
        script.append("sudo chown ${USER}:${USER} /var/log/ansible-playbook.log\n");
        script.append("python3 ${HOME}/playbook/tools/tee.py --cmd \"$(which ansible-playbook)").
                append(" ${HOME}/").append(AnsibleResources.SITE_CONFIG_FILE).
                append(" -i ${HOME}/").append(AnsibleResources.HOSTS_CONFIG_FILE).
                append("\" --outfile /var/log/ansible-playbook.log \n");

        // Execute ansible playbook using tee
        //script.append("ansible-playbook ~/" + AnsibleResources.SITE_CONFIG_FILE
        //        + " -i ~/" + AnsibleResources.HOSTS_CONFIG_FILE)
        //        .append(prepare ? " -t install" : "")
        //       .append(" | sudo tee -a /var/log/ansible-playbook.log")
        //        .append("\n");

        script.append("if [ $? == 0 ]; then echo CONFIGURATION FINISHED; else echo CONFIGURATION FAILED; fi\n");
        return script.toString();
    }
}
