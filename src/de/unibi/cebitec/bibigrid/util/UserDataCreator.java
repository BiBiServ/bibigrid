/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.util;

import de.unibi.cebitec.bibigrid.model.Configuration;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates UserData for master and slave instances.
 * 
 * 
 * @author jkrueger(at)cebitec.uni-bielefeld.de, alueckne(at)cebitec.uni-bielefeld.de
 */
public class UserDataCreator {

    public static final Logger log = LoggerFactory.getLogger(UserDataCreator.class);

    /**
     * Creates slaveUserData content.
     *
     *
     * Changes JK : <ul> <li> use StringBuilder instead of String concatenation
     * ;-) </li> <li> remove unnecessary sudo's - UserData is executed as root
     * </li> </ul>
     *
     *
     * @param masterIp
     * @param masterDns
     * @param slaveDeviceMapper
     * @param cfg
     * @param keypair

     * @return
     */
    public static String forSlave(String masterIp, String masterDns, DeviceMapper slaveDeviceMapper, Configuration cfg,KEYPAIR keypair) {
        StringBuilder slaveUserData = new StringBuilder();

        slaveUserData.append("#!/bin/sh\n");
        //slaveUserData.append("sleep 5\n");
        
        /* "Hack" for CeBiTec OpenStack setup - set hostname */
        if (cfg.getMode().equals(Configuration.MODE.OPENSTACK)) {
            updateHostname(slaveUserData);
        }
        
        slaveUserData.append("echo '").append(keypair.getPrivateKey()).append("' > /home/ubuntu/.ssh/id_rsa\n");
        slaveUserData.append("chown ubuntu:ubuntu /home/ubuntu/.ssh/id_rsa\n");
        slaveUserData.append("chmod 600 /home/ubuntu/.ssh/id_rsa\n");
        slaveUserData.append("echo '").append(keypair.getPublicKey()).append("' >> /home/ubuntu/.ssh/authorized_keys\n");
        
        slaveUserData.append("mkdir -p /vol/spool/\n");
        slaveUserData.append("mkdir -p /vol/scratch/\n");
       
        /*
         * GridEngine Block
         */
        if (cfg.isOge()) {
            slaveUserData.append("echo ").append(masterIp).append(" > /var/lib/gridengine/default/common/act_qmaster\n");
            slaveUserData.append("pid=`ps acx | grep sge_execd | cut -c1-6`\n");
            slaveUserData.append("if [ -n $pid ]; then\n");
            slaveUserData.append("        kill $pid;\n");
            slaveUserData.append("fi;\n");
            slaveUserData.append("sleep 1\n");
            slaveUserData.append("while test $(ps acx | grep sge_execd | wc -l) -eq 0; do\n");
            slaveUserData.append("        service gridengine-exec start\n");
            slaveUserData.append("        sleep 35\n");
            slaveUserData.append("done\n");
        } else {
            slaveUserData.append("service gridengine-exec stop");
        }

        /*
         * Ganglia service monitor
         */
        slaveUserData.append("sed -i s/MASTER_IP/").append(masterIp).append("/g /etc/ganglia/gmond.conf\n");
        slaveUserData.append("service ganglia-monitor restart \n");

       

        int ephemeralamount = cfg.getSlaveInstanceType().getSpec().ephemerals;

        /*
         * Ephemeral Block
         */
        String blockDeviceBase = "";
        switch (cfg.getMode()) {
            case AWS:
                blockDeviceBase = "/dev/xvd";
                break;
            case OPENSTACK:
                blockDeviceBase = "/dev/vd";
                break;
        }

        if (ephemeralamount  == 1) {
            slaveUserData.append("umount /mnt\n");
            switch (cfg.getLocalFS()) {
                case EXT2 : {
                    slaveUserData.append("mkfs.ext2 ").append(blockDeviceBase).append("b\n");
                    break;
                }
                case EXT3 : {
                    slaveUserData.append("mkfs.ext3 ").append(blockDeviceBase).append("b\n");
                    break;
                }
                case EXT4 : {
                    slaveUserData.append("mkfs.ext4 ").append(blockDeviceBase).append("b\n");
                    break;    
                }
                default : {
                    slaveUserData.append("mkfs.xfs -f /").append(blockDeviceBase).append("b\n");           
                }
            }       
            slaveUserData.append("mount ").append(blockDeviceBase).append("b /vol/scratch\n");
            
        } else if (ephemeralamount >= 2) {
            // if 2 or more ephemerals are available use all of them in a RAID 0 system
            slaveUserData.append("umount /mnt\n");
            slaveUserData.append("yes | mdadm --create /dev/md0 --level=0 -c256 --raid-devices=").append(ephemeralamount).append(" ");
            for (int i = 0; i < ephemeralamount; i++) {
                slaveUserData.append(blockDeviceBase).append(ephemeral(i)).append(" ");
            }
            slaveUserData.append("\n");

            slaveUserData.append("echo 'DEVICE ");
            for (int i = 0; i < ephemeralamount; i++) {
                slaveUserData.append(blockDeviceBase).append(ephemeral(i)).append(" ");
            }
            slaveUserData.append("'> /etc/mdadm.conf \n");

            slaveUserData.append("mdadm --detail --scan >> /etc/mdadm.conf\n");
            slaveUserData.append("blockdev --setra 65536 /dev/md0\n");
            
            switch (cfg.getLocalFS()) {
                case EXT2 : {
                    slaveUserData.append("mkfs.ext2 /dev/md0\n");
                    slaveUserData.append("mount -t ext2 -o noatime /dev/md0 /vol/scratch\n");
                    break;
                }
                case EXT3 : {
                    slaveUserData.append("mkfs.ext3 /dev/md0\n");
                    slaveUserData.append("mount -t ext3 -o noatime /dev/md0 /vol/scratch\n");
                    break;
                }
                case EXT4 : {
                    slaveUserData.append("mkfs.ext4 /dev/md0\n");
                    slaveUserData.append("mount -t ext4 -o noatime /dev/md0 /vol/scratch\n");
                    break;    
                }
                default : {
                    slaveUserData.append("mkfs.xfs -f /dev/md0\n");
                    slaveUserData.append("mount -t xfs -o noatime /dev/md0 /vol/scratch\n");
                }
            }
        }
        
        slaveUserData.append("chown ubuntu:ubuntu /vol/scratch \n");
        slaveUserData.append("chmod -R 777 /vol/scratch \n");
        
        
         /*
         * Cassandra Block
         */
        if (cfg.isCassandra()) {
            
            slaveUserData.append("mkdir /vol/scratch/cassandra\n");
            slaveUserData.append("mkdir /vol/scratch/cassandra/commitlog\n");
            slaveUserData.append("mkdir /vol/scratch/cassandra/data\n");
            slaveUserData.append("mkdir /vol/scratch/cassandra/saved_caches\n");
            slaveUserData.append("chown cassandra:cassandra -R /vol/scratch/cassandra\n");
            slaveUserData.append("sed -i s/##PRIVATE_IP##/$(curl http://instance-data/latest/meta-data/local-ipv4)/g /opt/cassandra/conf/cassandra.yaml\n");
            slaveUserData.append("sed -i s/##MASTER_IP##/").append(masterIp).append("/g  /opt/cassandra/conf/cassandra.yaml\n");
            // slaveUserData.append("service cassandra start\n"); --> start cassandra database as daemon process

        }
        
        /*
         * HDFS Block
         */
        if (cfg.isHdfs()) {
            slaveUserData.append("mkdir /vol/scratch/hadoop\n");
            slaveUserData.append("chown hadoop:hadoop -R /vol/scratch/hadoop\n");
            
            
        }
        
        /*
         * NFS//Mount Block
         */

        slaveUserData.append("chown ubuntu:ubuntu /vol/ \n");
        if (cfg.isNfs()) {
            slaveUserData.append(
                    "mount -t nfs4 -o proto=tcp,port=2049 ").append(masterIp).append(":/vol/spool /vol/spool\n");

            for (String e
                    : slaveDeviceMapper.getSnapshotIdToMountPoint()
                    .keySet()) {
                slaveUserData.append("mkdir -p ").append(slaveDeviceMapper.getSnapshotIdToMountPoint().get(e)).append("\n");
                slaveUserData.append("mount ").append(slaveDeviceMapper.getRealDeviceNameforMountPoint(slaveDeviceMapper.getSnapshotIdToMountPoint().get(e))).append(" ").append(slaveDeviceMapper.getSnapshotIdToMountPoint().get(e)).append("\n");
            }
            List<String> slaveNfsMounts = cfg.getNfsShares();

            if (!slaveNfsMounts.isEmpty()) {
                for (String share : slaveNfsMounts) {
                    slaveUserData.append("mkdir -p ").append(share).append("\n");
                    slaveUserData.append("mount -t nfs4 -o proto=tcp,port=2049 ").append(masterIp).append(":").append(share).append(" ").append(share).append("\n");
                }
            }
        }
        /**
         * route all traffic to master-instance (inet access) if slaves not configured with public ip address
         */
        switch (cfg.getMode()) {
            case AWS:
                if (!cfg.isPublicSlaveIps()) {
                    slaveUserData.append("route del default gw `ip route | grep default | awk '{print $3}'` eth0 \n");
                    slaveUserData.append("route add default gw ").append(masterIp).append(" eth0 \n");
                }
                break;
            case OPENSTACK:             
                break;
        }


        /* 
         * Mesos Block
         */
        
        if (cfg.isMesos()) {
            slaveUserData.append("service mesos-master stop\n");
            // configure zk
            slaveUserData.append("echo zk://").append(masterIp).append(":2181/mesos > /etc/mesos/zk\n");
            //configure mesos-slave
            slaveUserData.append("echo /vol/spool/mesos > /etc/mesos-slave/work_dir\n");
            slaveUserData.append("echo mesos,docker > /etc/mesos-slave/containerizers\n");
            slaveUserData.append("echo false > /etc/mesos-slave/switch_user\n");
            slaveUserData.append("service mesos-slave restart\n");
        } else {
            slaveUserData.append("service mesos-slave stop\n");
            slaveUserData.append("service mesos-master stop\n");
            slaveUserData.append("service zookeeper stop\n");
        }

        slaveUserData.append("/usr/bin/curl http://169.254.169.254/latest/meta-data/local-ipv4 -w '\\n'>> /vol/spool/slaves.finished \n");
        switch (cfg.getMode()) {
            case AWS:
                return new String(Base64.encodeBase64(slaveUserData.toString().getBytes()));
            default:
                return slaveUserData.toString();
        }
    }

    /**
     * Creates masterUserData content.
     *
     *
     * Changes JK : <ul> <li> Encode earlyscript as base64 string to avoid shell
     * interpretation of special chars like ($,
     *
     * @,`,&) </li> <li> Limit earlyscript length to 10K chars (base64 encoded),
     * roughly 6.6K absolute size </li> <li> use StringBuilder instead of String
     * concatenation ;-) </li> <li> remove unnecessary sudo's - UserData is
     * executed as root </li> <li> execute earlyscript as ubuntu user </li>
     * </ul>
     *
     * @param masterDeviceMapper
     * @param cfg
     * @return
     */
    public static String masterUserData(DeviceMapper masterDeviceMapper, Configuration cfg, KEYPAIR keypair) {
        StringBuilder masterUserData = new StringBuilder();

        int ephemeralamount = cfg.getMasterInstanceType().getSpec().ephemerals;
        List<String> masterNfsShares = cfg.getNfsShares();
        masterUserData.append("#!/bin/sh\n").append("sleep 5\n");

        masterUserData.append("echo '").append(keypair.getPrivateKey()).append("' > /home/ubuntu/.ssh/id_rsa\n");
        masterUserData.append("chown ubuntu:ubuntu /home/ubuntu/.ssh/id_rsa\n");
        masterUserData.append("chmod 600 /home/ubuntu/.ssh/id_rsa\n");
        masterUserData.append("echo '").append(keypair.getPublicKey()).append("' >> /home/ubuntu/.ssh/authorized_keys\n");
        

        /* "Hack" for CeBiTec OpenStack setup - set hostname */
        if (cfg.getMode().equals(Configuration.MODE.OPENSTACK)) {
            updateHostname(masterUserData);
        }


        /*
         * Ephemeral/RAID Preperation
         */
        String blockDeviceBase = "";
        switch (cfg.getMode()) {
            case AWS:
                blockDeviceBase = "/dev/xvd";
                break;
            case OPENSTACK:
                blockDeviceBase = "/dev/vd";
                break;
        }
        // if 1 ephemeral is available mount it as /vol/spool
        if (ephemeralamount == 1) {
            masterUserData.append("umount /mnt\n"); // 
            switch (cfg.getLocalFS()) {
                case EXT2 : {
                    masterUserData.append("mkfs.ext2 ").append(blockDeviceBase).append("b\n");
                    break;
                }
                case EXT3 : {
                    masterUserData.append("mkfs.ext3 ").append(blockDeviceBase).append("b\n");
                    break;
                }
                case EXT4 : {
                    masterUserData.append("mkfs.ext4 ").append(blockDeviceBase).append("b\n");
                    break;    
                }
                default : {
                    masterUserData.append("mkfs.xfs -f /").append(blockDeviceBase).append("b\n");           
                }
            }
            masterUserData.append("mount ").append(blockDeviceBase).append("b /vol/\n");
        } else if (ephemeralamount >= 2) {
            masterUserData.append("umount /mnt\n");
            // if 2 or more ephemerals are available use all of them in a RAID 0 system
            masterUserData.append("yes | mdadm --create /dev/md0 --level=0 -c256 --raid-devices=").append(ephemeralamount).append(" ");
            for (int i = 0; i < ephemeralamount; i++) {
                masterUserData.append(blockDeviceBase).append(ephemeral(i)).append(" ");
            }
            masterUserData.append("\n");
            masterUserData.append("echo 'DEVICE ");
            for (int i = 0; i < ephemeralamount; i++) {
                masterUserData.append(blockDeviceBase).append(ephemeral(i)).append(" ");
            }
            masterUserData.append("'> /etc/mdadm.conf \n");
            masterUserData.append("mdadm --detail --scan >> /etc/mdadm.conf\n");
            masterUserData.append("blockdev --setra 65536 /dev/md0\n");
            switch (cfg.getLocalFS()) {
                case EXT2 : {
                    masterUserData.append("mkfs.ext2 -f /dev/md0\n");
                    masterUserData.append("mount -t ext2 -o noatime /dev/md0 /vol/\n");
                    break;
                }
                case EXT3 : {
                    masterUserData.append("mkfs.ext3 -f /dev/md0\n");
                    masterUserData.append("mount -t ext3 -o noatime /dev/md0 /vol/\n");
                    break;
                }
                case EXT4 : {
                    masterUserData.append("mkfs.ext4 -f /dev/md0\n");
                    masterUserData.append("mount -t ext4 -o noatime /dev/md0 /vol/\n");
                    break;    
                }
                default : {
                    masterUserData.append("mkfs.xfs -f /dev/md0\n");
                    masterUserData.append("mount -t xfs -o noatime /dev/md0 /vol/\n");
                }
            }
            
        }

        /*
         * create spool and scratch
         */
        masterUserData.append("mkdir -p /vol/spool/\n");
        masterUserData.append("chmod 777 /vol/spool/\n");
        masterUserData.append("mkdir -p /vol/scratch/\n");
        masterUserData.append("chown ubuntu:ubuntu /vol/ \n");
        masterUserData.append("chown ubuntu:ubuntu /vol/scratch \n");
        masterUserData.append("chmod -R 777 /vol/\n");
        
        /*
         * Cassandra Bloock
         */
        if (cfg.isCassandra()) {
            masterUserData.append("mkdir -p /vol/scratch/cassandra\n");
            masterUserData.append("chown -R cassandra:cassandra /vol/scratch/cassandra\n");
            masterUserData.append("chmod -R 777 /vol/scratch/cassandra\n");
            masterUserData.append("sed -i s/##MASTER_IP##/$(curl http://instance-data/latest/meta-data/local-ipv4)/g /opt/cassandra/conf/cassandra.yaml\n");
            masterUserData.append("sed -i s/##PRIVATE_IP##/$(curl http://instance-data/latest/meta-data/local-ipv4)/g /opt/cassandra/conf/cassandra.yaml\n");
            masterUserData.append("service cassandra start\n"); // @ToDo : this will not work with latest version
        }
        
        /*
         * HDFS Block
         */
        if (cfg.isHdfs()) {
            masterUserData.append("mkdir -p /vol/scratch/hadoop\n");
            masterUserData.append("chown -R hadoop:hadoop /vol/scratch/hdfs\n");
            masterUserData.append("chmod -R 777 /vol/scratch/hadoop\n");
            // @ToDo: Update configuration
            masterUserData.append("service hdfs start\n"); // @ToDo: start hdfs namenode
            masterUserData.append("service hdfs start\n"); // @ToDo: start hdfs datanode
        }
        
        
        
        /*
         * OGE Block
         */
        masterUserData.append("service gridengine-master stop\n");
        if (cfg.isOge()) {
            masterUserData.append("curl http://169.254.169.254/latest/meta-data/local-ipv4 > /var/lib/gridengine/default/common/act_qmaster\n");
            masterUserData.append("chown sgeadmin:sgeadmin /var/lib/gridengine/default/common/act_qmaster\n");
            masterUserData.append("service gridengine-master start\n");
        }
        

        /* 
         * Mesos Block
         */
        if (cfg.isMesos()) {
            // stop master & slave for Reconfigauation
            masterUserData.append("service mesos-slave stop\n");
            masterUserData.append("service mesos-master stop\n");
            // configure mesos master
            masterUserData.append("mkdir -p /vol/spool/mesos\n");
            masterUserData.append("chmod -R 777 /vol/spool/mesos\n");
            masterUserData.append("echo bibigrid > /etc/mesos-master/cluster\n");
            masterUserData.append("curl http://169.254.169.254/latest/meta-data/local-ipv4 > /etc/mesos-master/ip\n");
            masterUserData.append("echo /vol/spool/mesos > /etc/mesos-master/work_dir\n");          
            masterUserData.append("service mesos-master start\n");
            if (cfg.isUseMasterAsCompute()) {
                //configure mesos-slave
                masterUserData.append("echo /vol/spool/mesos > /etc/mesos-slave/work_dir\n");
                masterUserData.append("echo mesos,docker > /etc/mesos-slave/containerizers\n");
                masterUserData.append("echo false > /etc/mesos-slave/switch_user\n");
                masterUserData.append("service mesos-slave start\n");
            }
        } else {
            // shutdown possible running mesos components
            masterUserData.append("service zookeeper stop\n");
            masterUserData.append("service mesos-slave stop\n");
            masterUserData.append("service chronos stop\n");
            masterUserData.append("service mesos-master stop\n");
        }

        /*
         * NFS//Mounts Block
         */
        if (cfg.isNfs()) {
            // export spool dir
            masterUserData.append("ipbase=`curl http://169.254.169.254/latest/meta-data/local-ipv4 | cut -f 1-3 -d .`\n");
            masterUserData.append("echo \"/vol/spool/ $ipbase.0/24(rw,nohide,insecure,no_subtree_check,async)\" >> /etc/exports\n");

            if (masterDeviceMapper != null) {
                for (String e : masterDeviceMapper.getSnapshotIdToMountPoint().keySet()) {
                    masterUserData.append("mkdir -p ").append(masterDeviceMapper.getSnapshotIdToMountPoint().get(e)).append("\n");
                    masterUserData.append("mount ").append(masterDeviceMapper.getRealDeviceNameforMountPoint(masterDeviceMapper.getSnapshotIdToMountPoint().get(e))).append(" ").append(masterDeviceMapper.getSnapshotIdToMountPoint().get(e)).append("\n");
                }
                for (String mastershare : masterNfsShares) {
                    masterUserData.append("mkdir -p ").append(mastershare).append("\n");
                    masterUserData.append("chmod 777 ").append(mastershare).append("\n");
                    masterUserData.append("echo \"").append(mastershare).append(" $ipbase.0/24(rw,nohide,insecure,no_subtree_check,async)\">> /etc/exports\n");
                }
                masterUserData.append("/etc/init.d/nfs-kernel-server restart\n");
            } else {
                log.error("MasterDeviceMapper is null ...");
            }
        }
        /**
         * Enabling nat functions of master-instance (slave inet access) if slaves configured without public ip address
         * WARNING! 10.10.0.0 is a hardcoded SUBNET-proto...ensure generic
         * access later.
         */

        switch (cfg.getMode()) {
            case AWS:
                if (!cfg.isPublicSlaveIps()) {
                    masterUserData.append("sysctl -q -w net.ipv4.ip_forward=1 net.ipv4.conf.eth0.send_redirects=0\n"
                        + "iptables -t nat -C POSTROUTING -o eth0 -s 10.10.0.0/24 -j MASQUERADE 2> /dev/null || iptables -t nat -A POSTROUTING -o eth0 -s 10.10.0.0/24 -j MASQUERADE\n");
                }
                break;
            case OPENSTACK:
                // currently nothing todo
                break;

        }
        /*
         * Early Execute Script
         */
        if (cfg.getEarlyShellScriptFile() != null) {
            try {

                String base64 = new String(Base64.encodeBase64(Files.readAllBytes(cfg.getEarlyShellScriptFile())));

                if (base64.length() > 10000) {
                    log.info("Early shell script file too large  (base64 encoded size exceeds 10000 chars)");
                } else {
                    masterUserData.append("echo ").append(base64).append(" | base64 --decode  | sudo -u ubuntu bash - 2>&1 >> /var/log/earlyshellscript.log &\n");
                }

            } catch (IOException e) {
                log.info("Early shell script could not be read.");
            }
        }
        masterUserData.append("touch /vol/spool/masteruserdata.finished \n");

        switch (cfg.getMode()) {
            case AWS:
                return new String(Base64.encodeBase64(masterUserData.toString().getBytes()));
            default:
                return masterUserData.toString();
        }
    }

    private static char ephemeral(int i) {
        return (char) (i + 98);
    }
    
    
    private static void updateHostname(StringBuilder sb) {
        sb.append("t=`curl http://169.254.169.254/latest/meta-data/local-ipv4 | sed 's/\\./-/g'`\n");
        sb.append("hostname host-$t\n");
        
    }
}
