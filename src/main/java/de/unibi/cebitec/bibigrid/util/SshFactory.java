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
import de.unibi.cebitec.bibigrid.meta.openstack.CreateClusterOpenstack;
import de.unibi.cebitec.bibigrid.model.Configuration;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
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

    public static String buildSshCommand(String asGroupName, Configuration cfg, Instance masterInstance, List<Instance> slaveInstances) {
        StringBuilder sb = new StringBuilder();

        sb.append("sudo sed -i s/MASTER_IP/$(hostname)/g /etc/ganglia/gmond.conf\n");

        if (cfg.getShellScriptFile() != null) {
            try {
                List<String> lines = Files.readAllLines(cfg.getShellScriptFile(), StandardCharsets.UTF_8);
                sb.append("cat > shellscript.sh << EOFCUSTOMSCRIPT \n");

                for (String e : lines) {
                    sb.append(e);
                    sb.append("\n");
                }
                sb.append("\nEOFCUSTOMSCRIPT\n");
                sb.append("bash shellscript.sh &> shellscript.log &\n");
            } catch (IOException e) {
                log.info("Shell script could not be read.");
            }
        }

        if (cfg.isOge()) {
            sb.append("qconf -as $(hostname)\n");
            // clean-up possible previous configuration (could be happend if you use a configured masterimage snapshot as image)
            sb.append("for i in `qconf -sel`; do qconf -dattr hostgroup hostlist $i \\@allhosts 2>&1; qconf -de $i 2>&1; done;\n");
            
            if (cfg.isUseMasterAsCompute()) {
                sb.append("./add_exec ");
                sb.append(masterInstance.getPrivateDnsName());
                sb.append(" ");
                sb.append(cfg.getMasterInstanceType().getSpec().instanceCores);
                sb.append("\n");
                //sb.append("sudo service gridengine-exec start\n");
            }
            if (slaveInstances != null) {
                for (Instance instance : slaveInstances) {
                    sb.append("./add_exec ");
                    sb.append(instance.getPrivateDnsName());
                    sb.append(" ");
                    sb.append(cfg.getSlaveInstanceType().getSpec().instanceCores);
                    sb.append("\n");
                }
            }
        } else {
            //sb.append("sudo service gridengine-master stop\n");
        }
        sb.append("sudo service gmetad restart \n");
        sb.append("sudo service ganglia-monitor restart \n");
        sb.append("echo CONFIGURATION_FINISHED \n");


        return sb.toString();
    }

    public static String buildSshCommandOpenstack(String asGroupName, Configuration cfg, CreateClusterOpenstack.Instance master, Collection<CreateClusterOpenstack.Instance> slaves) {
        StringBuilder sb = new StringBuilder();
        
        UserDataCreator.updateHostname(sb);
        UserDataCreator.shellFct(sb);


        //Install Ansible (code is only temporarily, since there is no ansible installation right now...)

        sb.append("sudo apt-get -y install software-properties-common > /dev/null 2>&1\n");
        sb.append("sudo -E apt-add-repository -y ppa:ansible/ansible > /dev/null 2>&1\n");
        sb.append("sudo apt-get update > /dev/null 2>&1\n");
        sb.append("sudo apt-get -y install ansible > /dev/null 2>&1\n");


        //Configure Ansible and build hosts file.
        sb.append("sudo rm /etc/ansible/ansible.cfg\n");
        sb.append("sudo sh -c \"echo [defaults] >> /etc/ansible/ansible.cfg\"\n");
        sb.append("sudo sh -c \"echo host_key_checking = False >> /etc/ansible/ansible.cfg\"\n");
        sb.append("sudo sh -c \"echo [ssh_connection] >> /etc/ansible/ansible.cfg\"\n");
        sb.append("sudo sh -c \"echo pipelining = True >> /etc/ansible/ansible.cfg\"\n");
        sb.append("sudo sh -c \"echo [master] >> /etc/ansible/hosts\"\n");
        sb.append("sudo sh -c \"echo ").append(master.getIp()).append(" >> /etc/ansible/hosts\"\n\n");
        sb.append("sudo sh -c \"echo [slaves] >> /etc/ansible/hosts\"\n");
        for(CreateClusterOpenstack.Instance instance : slaves){
            sb.append("sudo sh -c \"echo ").append(instance.getIp()).append(" >> /etc/ansible/hosts\"\n");
        }





        
        sb.append("sudo sed -i s/MASTER_IP/").append(master.getIp()).append("/g /etc/ganglia/gmond.conf\n");

        if (cfg.getShellScriptFile() != null) {
            try {
                List<String> lines = Files.readAllLines(cfg.getShellScriptFile(), StandardCharsets.UTF_8);
                sb.append("cat > shellscript.sh << EOFCUSTOMSCRIPT \n");

                for (String e : lines) {
                    sb.append(e);
                    sb.append("\n");
                }
                sb.append("\nEOFCUSTOMSCRIPT\n");
                sb.append("bash shellscript.sh &> shellscript.log &\n");
            } catch (IOException e) {
                log.info("Shell script could not be read.");
            }
        }
        if (cfg.isOge()) {
            // wait for sge_master started
            sb.append("check ").append(master.getIp()).append(" 6444\n");
            // configure submit host
            sb.append("qconf -as ").append(master.getIp()).append(" 2>&1\n");
            // clean-up possible previous configuration (could be happend if you use a configured masterimage snapshot as image)
            sb.append("for i in `qconf -sel`; do qconf -dattr hostgroup hostlist $i \\@allhosts 2>&1; qconf -de $i 2>&1; done;\n");
            
            // add master as exec host  if set and start execd
            if (cfg.isUseMasterAsCompute()) {
                sb.append("./add_exec ").append(master.getIp()).append(" ").append(cfg.getMasterInstanceType().getSpec().instanceCores).append(" 2>&1 \n");
                sb.append("sudo service gridengine-exec start\n");
                
            }
            // add slaves as exec hosts
            for (CreateClusterOpenstack.Instance slave : slaves) {              
                sb.append("./add_exec ").append(slave.getIp()).append(" ").append(cfg.getSlaveInstanceType().getSpec().instanceCores).append(" 2>&1 \n");
            }
        }
        
        if (cfg.isHdfs()) {
            for (CreateClusterOpenstack.Instance slave : slaves) {
                sb.append("echo ").append(slave.getIp()).append(" >> /opt/hadoop/etc/hadoop/slaves\n");
            }
            sb.append("/opt/hadoop/sbin/start-dfs.sh\n");
            
        }
        
        sb.append("sudo service gmetad restart \n");
        sb.append("sudo service ganglia-monitor restart \n");


        //Test running ansible installer
        sb.append("chmod +x /home/ubuntu/tmp/playbookinstall.sh\n");
        sb.append("sh /home/ubuntu/tmp/playbookinstall.sh /home/ubuntu/tmp/ansible 10\n");



        sb.append("echo CONFIGURATION_FINISHED \n");


        return sb.toString();
    }
    

}
