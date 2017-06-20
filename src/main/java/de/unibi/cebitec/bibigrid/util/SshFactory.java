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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SshFactory {

    public static final Logger LOG = LoggerFactory.getLogger(SshFactory.class);
    
    private static final String SSH="ssh  -o CheckHostIP=no -o StrictHostKeyChecking=no ";

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
                    LOG.info("SSH: {}", string);
                }
            };
            sshSession.setUserInfo(userInfo);
            return sshSession;
        } catch (JSchException e) {
            LOG.error(e.getMessage());
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
                LOG.info("Shell script could not be read.");
            }
        }

        if (cfg.isOge()) {
            sb.append("qconf -as $(hostname)\n");
            // clean-up possible previous configuration (could be happend if you use a configured masterimage snapshot as image)
            sb.append("for i in `qconf -sel 2>/dev/null`; do qconf -dattr hostgroup hostlist $i \\@allhosts 2>&1; qconf -de $i 2>&1; done;\n");
            
            if (cfg.isUseMasterAsCompute()) {
                sb.append("./add_exec ");
                sb.append(masterInstance.getPrivateDnsName());
                sb.append(" ");
                sb.append(cfg.getMasterInstanceType().getSpec().instanceCores);
                sb.append("\n");
                sb.append("sudo service gridengine-exec start\n");
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
            sb.append("sudo service gridengine-master stop\n");
        }
        sb.append("sudo service gmetad restart \n");
        sb.append("sudo service ganglia-monitor restart \n");
        sb.append("echo CONFIGURATION_FINISHED \n");
        return sb.toString();
    }

    public static String buildSshCommandOpenstack(String asGroupName, Configuration cfg, CreateClusterOpenstack.Instance master, Collection<CreateClusterOpenstack.Instance> slaves) {
        StringBuilder sb = new StringBuilder();
        
        
        UserDataCreator.shellFct(sb);
        
        
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
                LOG.info("Shell script could not be read.");
            }
        }
        if (cfg.isOge()) {
            // get masters fqdn
            sb.append("MFQDN=$(fqdn ").append(master.getIp()).append(")\n");
            // configure and starts sge master
            sb.append("echo ${MFQDN} | sudo tee /var/lib/gridengine/default/common/act_qmaster > /dev/null\n");
            sb.append("sudo chown sgeadmin:sgeadmin /var/lib/gridengine/default/common/act_qmaster\n");
            sb.append("ch_p sge_qmaster 5 'sudo service gridengine-master start'\n");          
            // and wait for sge_master available
            sb.append("ch_s ").append(master.getIp()).append(" 6444\n");
            sb.append("log \"gridengine-master configured and started.\"\n");
            // add master as submit host
            sb.append("sudo qconf -as ${MFQDN} 2>&1\n");
            sb.append("log \"Master:${MFQDN} added as submit host.\"\n");
            // clean-up possible previous configuration (could be happend if you use a configured masterimage snapshot as image)
            sb.append("for i in `qconf -sel 2>/dev/null`; do sudo qconf -dattr hostgroup hostlist $i \\@allhosts 2>&1; sudo qconf -de $i 2>&1; done;\n");
            
            // add master as execution host  if set and start execution daemon
            if (cfg.isUseMasterAsCompute()) {
                //sb.append("./add_exec ").append(master.getNeutronHostname()).append(" ").append(cfg.getMasterInstanceType().getSpec().instanceCores).append(" 2>&1 \n");
                sb.append("/home/ubuntu/add_exec ${MFQDN} ").append(cfg.getMasterInstanceType().getSpec().instanceCores).append(" 2>&1 \n");
                sb.append("ch_p sge_execd 5 \"sudo service gridengine-exec start\"\n");
                sb.append("log \"Master:${MFQDN} configured as execution host.\"\n");
                
            }
            // add slaves as execution hosts
            for (CreateClusterOpenstack.Instance slave : slaves) {       
               // wait for slave instance ready
               sb.append("ch_s ").append(slave.getIp()).append(" 22\n");
               // configure sge, start execution daemon and get slaves fqdn
               sb.append("while \n");
               sb.append(SSH).append(slave.getIp()).append(" -E /dev/null \"echo ${MFQDN} | sudo tee /var/lib/gridengine/default/common/act_qmaster 2>&1 >/dev/null; sudo service gridengine-exec start\"\n");
               sb.append("(( $? != 0 ));\ndo sleep 10;done;\n");
               
               sb.append("SFQDN=$(fqdn  ").append(slave.getIp()).append(")\n");
               sb.append("/home/ubuntu/add_exec ${SFQDN} ").append(cfg.getSlaveInstanceType().getSpec().instanceCores).append(" 2>&1 \n");
               sb.append("log \"Slave:${SFQDN} configured as execution host.\"\n");
              
            }
        }
        
        if (cfg.isCassandra()) {
              
  
            List<String> cassandra_hosts = new ArrayList<>();
            // add master
            cassandra_hosts.add(master.getIp());
            // add add all slaves
            for (CreateClusterOpenstack.Instance slave : slaves) {              
                cassandra_hosts.add(slave.getIp());
            }
            // now configure cassandra on all hosts and starts it afterwards
            String ch = String.join(",",cassandra_hosts);
            
            for (String host : cassandra_hosts) {
                sb.append("ch_f /var/log/bibigrid/").append(host).append("\n");
                sb.append(SSH).append(host).append(" \"sudo -u cassandra /opt/cassandra/bin/create_cassandra_config.sh  /opt/cassandra/ /vol/scratch/cassandra/ cassandra ").append(ch).append(" \"\n");
                sb.append(SSH).append(host).append(" \"sudo service cassandra start\"\n");   
            }
        }
        
        if (cfg.isHdfs()) {
            sb.append("/opt/hadoop/bin/hdfs namenode -format bibigrid 2>&1\n");
            
            sb.append("echo ").append(master.getIp()).append(" > /opt/hadoop/etc/hadoop/slaves\n");
            for (CreateClusterOpenstack.Instance slave : slaves) {
                sb.append("echo ").append(slave.getIp()).append(" >> /opt/hadoop/etc/hadoop/slaves\n");
            }
            // sb.append("/opt/hadoop/sbin/hadoop-daemon.sh --config /opt/hadoop/etc/hadoop/ --script hdfs start namenode\n");
            // sb.append("/opt/hadoop/sbin/hadoop-daemon.sh --config /opt/hadoop/etc/hadoop/ --script hdfs start datanode\n");
            sb.append("/opt/hadoop/sbin/start-dfs.sh\n");
            
        }
        
        if (cfg.isSpark()) {
            sb.append("echo SPARK_MASTER_OPTS='\"-Dspark.ui.reverseProxyUrl=http://").append(master.getPublicIp()).append("/spark/ -Dspark.ui.reverseProxy=true\"' >> /opt/spark/conf/spark-env.sh\n");
            sb.append("echo SPARK_WORKER_OPTS='\"-Dspark.ui.reverseProxyUrl=http://").append(master.getPublicIp()).append("/spark/ -Dspark.ui.reverseProxy=true\"' >> /opt/spark/conf/spark-env.sh\n");
            for (CreateClusterOpenstack.Instance slave : slaves) {
                sb.append("echo ").append(slave.getIp()).append(" >> /opt/spark/conf/slaves\n");
            }
            sb.append("sudo /opt/spark/sbin/start-all.sh\n");
            sb.append("sudo /usr/sbin/a2enconf spark\n");
                    
        }
        
        sb.append("sudo /usr/sbin/a2enconf result\n");
        sb.append("sudo service apache2 restart\n");
        
        sb.append("sudo service gmetad restart \n");
        sb.append("sudo service ganglia-monitor start \n");
        sb.append("echo CONFIGURATION_FINISHED \n");
        return sb.toString();
    }
    

}
