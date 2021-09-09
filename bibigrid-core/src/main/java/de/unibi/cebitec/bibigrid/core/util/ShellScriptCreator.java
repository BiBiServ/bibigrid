package de.unibi.cebitec.bibigrid.core.util;

import de.unibi.cebitec.bibigrid.core.model.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import de.unibi.cebitec.bibigrid.core.model.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates user data and other shell scripts that are executed on instances during startup.
 *
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public final class ShellScriptCreator {
    private static final Logger LOG = LoggerFactory.getLogger(ShellScriptCreator.class);

    public static String getUserData(Configuration config, boolean base64) {
        StringBuilder userData = new StringBuilder();
        userData.append("#!/bin/bash\n");
        // redirect output
        userData.append("exec > /var/log/userdata.log\n");
        userData.append("exec 2>&1\n");
        // disableUpgrades
        // appendDisableAptDailyService(userData);
        appendDisableUpgrades(userData);
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
        // configure SSH Config
        userData.append("log \"configure ssh\"\n");
        appendSshConfiguration(config, userData);
        // finished
        userData.append("log \"userdata.finished\"\n");
        userData.append("exit 0\n");
        return base64 ? Base64.getEncoder().encodeToString(userData.toString().getBytes()) : userData.toString();
    }


    private static void appendDisableUpgrades(StringBuilder userData){
        userData.append("echo > /etc/apt/apt.conf.d/20auto-upgrades << \"END\"\n" +
                "APT::Periodic::Update-Package-Lists \"0\";\n" +
                "APT::Periodic::Unattended-Upgrade \"0\";\n" +
                "END\n");
    }

    private static void appendSshConfiguration(Configuration config, StringBuilder userData) {
        String user = config.getSshUser();
        String userSshPath = "/home/" + user + "/.ssh/";
        // place private cluster ssh-key as 'default' on all instances to allow instance interconnection between all instances
        userData.append("echo '").append(config.getClusterKeyPair().getPrivateKey()).append("' > ").append(userSshPath).append("id_rsa\n");
        userData.append("chown ").append(user).append(":").append(user).append(" ").append(userSshPath).append("id_rsa\n");
        userData.append("chmod 600 ").append(userSshPath).append("id_rsa\n");

        // place *all* additional public keys within authorized keys
        // public keys
        List<String> pks = new ArrayList<>(config.getSshPublicKeys());
        // public key files
        List<String> pkfs = new ArrayList<>(config.getSshPublicKeyFiles());
        if (config.getSshPublicKeyFile() != null) {
            pkfs.add(config.getSshPublicKeyFile());
        }
        // read each key and add content to overall list
        for (String pkf :  pkfs) {
            try {
                String pk = new String(Files.readAllBytes(Paths.get(pkf)));
                pks.add(pk);
            } catch (IOException e){
                LOG.error("Failed to read public key : {}",pkf);
            }
        }

        // add all keys to authorized keys
        for (String pk : pks) {
            // Check if we have a public key like putty saves them
            if (pk.startsWith("----")) {
                String tmp = "tmp.pub";
                userData.append("echo '").append(pk.trim()).append("' > ").append(userSshPath)
                        .append(tmp).append("\n");
                userData.append("ssh-keygen -i -f ").append(userSshPath).append(tmp).append(" >> ")
                        .append(userSshPath).append("authorized_keys\n");
                userData.append("rm ").append(userSshPath).append(tmp).append("\n");
            } else {
                userData.append("echo '").append(pk.trim()).append("' >> ").append(userSshPath)
                        .append("authorized_keys\n");
            }
        }

        // ssh config
        userData.append("cat > ").append(userSshPath).append("config << SSHCONFIG\n");
        userData.append("Host *\n");
        userData.append("\tCheckHostIP no\n");
        userData.append("\tStrictHostKeyChecking no\n");
        userData.append("\tUserKnownHostsfile /dev/null\n");
        userData.append("SSHCONFIG\n");
    }

    /**
     * Builds script to configure ansible and execute ansible commands to install (galaxy) roles / playbooks.
     * @param config Configuration
     * @return script String to execute in CreateCluster
     */
    public static String getMasterAnsibleExecutionScript(final Configuration config) {
        StringBuilder script = new StringBuilder();
        // wait until /var/lib/dpkg/lock is not locked by apt/dpkg
        script.append("while sudo lsof /var/lib/dpkg/lock 2> null; do echo \"/var/lib/dpkg/lock locked - wait for 10 seconds\"; sleep 10; done;\n");
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

        // Test ansible
        script.append("echo \"Testing Ansible...\n\";");
        script.append("ansible -i ~/" + AnsibleResources.HOSTS_CONFIG_FILE + " all -m ping | sudo tee -a /var/log/ansible.log \n");
        script.append("if [ $? -eq 0 ]; then echo \"Ansible configuration seems to work properly.\"; else echo\"Ansible hosts not reachable. " +
                "There seems to be a misconfiguration.\"; fi\n");

        // Run ansible-galaxy to install ansible-galaxy roles from galaxy, git or url (.tar.gz)
        if (config.hasCustomAnsibleGalaxyRoles()) {
            script.append("ansible-galaxy install --roles-path ~/"
                    + AnsibleResources.ADDITIONAL_ROLES_ROOT_PATH
                    + " -r ~/" + AnsibleResources.REQUIREMENTS_CONFIG_FILE + "\n");
        }
        // Extract ansible roles from files (.tar.gz, .tgz)
        script.append("cd ~/" + AnsibleResources.ADDITIONAL_ROLES_ROOT_PATH + "\n");
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
        return script.toString();
    }

    /**
     * ansible-playbook script to execute scale (up or down) tasks on master.
     * @return script
     */
    public static String executeScaleTasksOnMaster(Scale scale) {
        StringBuilder script = new StringBuilder();
        script.append("python3 ${HOME}/playbook/tools/tee.py --cmd \"$(which ansible-playbook)").
                append(" ${HOME}/").append(AnsibleResources.SITE_CONFIG_FILE).
                append(" -i ${HOME}/").append(AnsibleResources.HOSTS_CONFIG_FILE).
                append(" -t ").append(scale.toString()).
                append(" -l master").
                append("\" --outfile /var/log/ansible-playbook.log \n");
        return script.toString();
    }

    /**
     * ansible-playbook script to execute whole site.yml on specified worker nodes.
     * @param workers worker nodes the playbook should be rolled out
     * @return script
     */
    public static String executePlaybookOnWorkers(List<Instance> workers) {
        StringBuilder script = new StringBuilder();
        script.append("sleep 30\n");
        script.append("python3 ${HOME}/playbook/tools/tee.py --cmd \"$(which ansible-playbook)").
                append(" ${HOME}/").append(AnsibleResources.SITE_CONFIG_FILE).
                append(" -i ${HOME}/").append(AnsibleResources.HOSTS_CONFIG_FILE).
                append(" -l ");
        for (Instance worker : workers) {
            script.append(worker.getPrivateIp());
            if (workers.indexOf(worker) != workers.size() - 1) {
                script.append(",");
            }
        }
        script.append("\" --outfile /var/log/ansible-playbook.log \n");
        return script.toString();
    }
}
