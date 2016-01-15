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
 *
 * @author alueckne
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
     * @param slaveNfsMounts
     * @param ephemerals
     * @return
     */
    public static String forSlave(String masterIp, String masterDns, DeviceMapper slaveDeviceMapper, Configuration cfg, String publicKey) {
        StringBuilder slaveUserData = new StringBuilder();

        slaveUserData.append("#!/bin/sh\n");
        slaveUserData.append("sleep 5\n");
        slaveUserData.append("mkdir -p /vol/spool/\n");
        slaveUserData.append("mkdir -p /vol/scratch/\n");
        slaveUserData.append("echo '").append(publicKey).append("' >> /home/ubuntu/.ssh/authorized_keys\n");

        /*
         * GridEngine Block
         */
        if (cfg.isOge()) {
            slaveUserData.append("echo ").append(masterIp).append(" > /var/lib/gridengine/default/common/act_qmaster\n");
            slaveUserData.append("echo ").append(masterIp).append(" ").append(masterDns).append(" >> /etc/hosts\n");
            slaveUserData.append("pid=`ps acx | grep sge_execd | cut -c1-6`\n");
            slaveUserData.append("if [ -n $pid ]; then\n");
            slaveUserData.append("        kill $pid;\n");
            slaveUserData.append("fi;\n");
            slaveUserData.append("sleep 1\n");
            slaveUserData.append("while test $(ps acx | grep sge_execd | wc -l) -eq 0; do\n");
            slaveUserData.append("        service gridengine-exec start\n");
            slaveUserData.append("        sleep 35\n");
            slaveUserData.append("done\n");
        }

        /*
         * Ganglia service monitor
         */
        slaveUserData.append("sed -i s/MASTER_IP/").append(masterIp).append("/g /etc/ganglia/gmond.conf\n");
        slaveUserData.append("service ganglia-monitor restart \n");

        /*
         * Cassandra Block
         */
        if (cfg.isCassandra()) {
            slaveUserData.append("service cassandra stop\n");
            slaveUserData.append("mkdir /vol/cassandra\n");
            slaveUserData.append("mkdir /vol/cassandra/commitlog\n");
            slaveUserData.append("mkdir /vol/cassandra/data\n");
            slaveUserData.append("mkdir /vol/cassandra/saved_caches\n");
            slaveUserData.append("chown cassandra:cassandra -R /vol/cassandra\n");
            slaveUserData.append("sed -i s/##PRIVATE_IP##/$(curl http://instance-data/latest/meta-data/local-ipv4)/g /etc/cassandra/cassandra.yaml\n");
            slaveUserData.append("sed -i s/##MASTER_IP##/").append(masterIp).append("/g  /etc/cassandra/cassandra.yaml\n");
            slaveUserData.append("service cassandra start\n");

        }

        int ephemeralamount = cfg.getSlaveInstanceType().getSpec().ephemerals;

        /*
         * Ephemeral Block
         */
        String blockDeviceBase = "";
        switch (cfg.getMode()) {
            case AWS_EC2:
                blockDeviceBase = "/dev/xvd";
                break;
            case OPENSTACK:
                blockDeviceBase = "/dev/vd";
                break;
        }

        if (ephemeralamount  == 1) {
            slaveUserData.append("sudo umount /mnt\n");
            slaveUserData.append("sudo mount ").append(blockDeviceBase).append("b /vol/scratch\n");
        } else if (ephemeralamount >= 2) {
            // if 2 or more ephemerals are available use 2 as a RAID system
            slaveUserData.append("umount /mnt\n");
            // if 2 or more ephemerals are available use all of them in a RAID 0 system

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
            slaveUserData.append("mkfs.xfs -f /dev/md0\n");

            slaveUserData.append("mount -t xfs -o noatime /dev/md0 /vol/scratch\n");
            slaveUserData.append("chown ubuntu:ubuntu /vol/scratch \n");
            slaveUserData.append("chmod -R 777 /vol/scratch \n");

        }
        /*
         * NFS//Mount Block
         */

        slaveUserData.append(
                "chown ubuntu:ubuntu /vol/ \n");
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
                    slaveUserData.append("sudo mkdir -p ").append(share).append("\n");
                    slaveUserData.append("mount -t nfs4 -o proto=tcp,port=2049 ").append(masterIp).append(":").append(share).append(" ").append(share).append("\n");
                }
            }
        }
        /**
         * route all traffic to master-instance (inet access)
         */
        // @TODO
        switch (cfg.getMode()) {
            case AWS_EC2:
                slaveUserData.append("route del default gw `ip route | grep default | awk '{print $3}'` eth0 \n");
                slaveUserData.append("route add default gw ").append(masterIp).append(" eth0 \n");
                break;
            case OPENSTACK:
                // TESTING!!! @TODO
                slaveUserData.append("echo 'http_proxy=http://proxy.cebitec.uni-bielefeld.de:3128' >> /etc/environment \n")
                        .append("echo 'https_proxy=https://proxy.cebitec.uni-bielefeld.de:3128' >> /etc/environment \n")
                        .append("echo 'ftp_proxy=http://proxy.cebitec.uni-bielefeld.de:3128' >> /etc/environment \n")
                        .append("echo 'no_proxy=localhost,127.0.0.1,169.254.169.254' >> /etc/environment \n")
                        .append("echo 'HTTP_PROXY=http://proxy.cebitec.uni-bielefeld.de:3128' >> /etc/environment \n")
                        .append("echo 'HTTPS_PROXY=https://proxy.cebitec.uni-bielefeld.de:3128' >> /etc/environment \n")
                        .append("echo 'FTP_PROXY=http://proxy.cebitec.uni-bielefeld.de:3128' >> /etc/environment \n")
                        .append("echo 'NO_PROXY=localhost,127.0.0.1,169.254.169.254' >> /etc/environment \n");
                break;
        }


        /* 
         * Mesos Block
         */
        slaveUserData.append("service mesos-master stop\n");
        slaveUserData.append("service mesos-slave stop\n");
        if (cfg.isMesos()) {
            //slaveUserData.append("rm /etc/mesos/zk\n"); // currently no zk supported
            slaveUserData.append("echo /vol/spool/mesos > /etc/mesos-slave/work_dir\n");
            slaveUserData.append("echo ").append(masterIp).append(":5050 > /etc/mesos-slave/master\n");
            slaveUserData.append("service mesos-slave start\n");
        }

//        slaveUserData.append(
//                "while true; do\n").append("service gridengine-exec start\n").append("sleep 60\n").append("done\n");
//        String checkifFileExists = "ssh -q -o StrictHostKeyChecking=no bibigrid-slave-1-pDj1myEySSih6vw [[ -f /tmp/slave.finished ]] && echo \"File exists\" || echo \"File does not exist\";";
//        slaveUserData.append("sleep 60 \nsudo service gridengine-exec start\n");
        slaveUserData.append("echo 'slave done' >> /vol/spool/slaves.finished \n");
        switch (cfg.getMode()) {
            case AWS_EC2:
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
    public static String masterUserData(DeviceMapper masterDeviceMapper, Configuration cfg, String privateKey) {
        StringBuilder masterUserData = new StringBuilder();

        int ephemeralamount = cfg.getMasterInstanceType().getSpec().ephemerals;
        List<String> masterNfsShares = cfg.getNfsShares();
        masterUserData.append("#!/bin/sh\n").append("sleep 5\n");

        masterUserData.append("echo '").append(privateKey).append("' > /home/ubuntu/.ssh/id_rsa\n");
        masterUserData.append("chown ubuntu:ubuntu /home/ubuntu/.ssh/id_rsa\n");
        masterUserData.append("chmod 600 /home/ubuntu/.ssh/id_rsa\n");

        /*
         * Ephemeral/RAID Preperation
         */
        String blockDeviceBase = "";
        switch (cfg.getMode()) {
            case AWS_EC2:
                blockDeviceBase = "/dev/xvd";
                break;
            case OPENSTACK:
                blockDeviceBase = "/dev/vd";
                break;
        }
        // if 1 ephemeral is available mount it as /vol/spool
        if (ephemeralamount == 1) {
            masterUserData.append("umount /mnt\n"); // 
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
            masterUserData.append("mkfs.xfs -f /dev/md0\n");
            masterUserData.append("mount -t xfs -o noatime /dev/md0 /vol/\n");

        }

        /*
         * NFS Prep of Vol
         */
        if (cfg.isNfs()) {

            masterUserData.append("ipbase=`curl http://169.254.169.254/latest/meta-data/local-ipv4 | cut -f 1-3 -d .`\n");
            masterUserData.append("echo \"/vol/spool/ $ipbase.0/24(rw,nohide,insecure,no_subtree_check,async)\" >> /etc/exports\n");
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
        masterUserData.append("service cassandra stop\n");
        if (cfg.isCassandra()) {
            masterUserData.append("mkdir -p /vol/cassandra\n");
            masterUserData.append("chown -R cassandra:cassandra /vol/cassandra\n");
            masterUserData.append("chmod -R 777 /vol/cassandra\n");
            masterUserData.append("sed -i s/##MASTER_IP##/$(curl http://instance-data/latest/meta-data/local-ipv4)/g /etc/cassandra/cassandra.yaml\n");
            masterUserData.append("sed -i s/##PRIVATE_IP##/$(curl http://instance-data/latest/meta-data/local-ipv4)/g /etc/cassandra/cassandra.yaml\n");
            masterUserData.append("service cassandra start\n");
        }

        /* 
         * Mesos Block
         */
        masterUserData.append("service mesos-master stop\n");
        masterUserData.append("service mesos-slave stop\n");
        if (cfg.isMesos()) {
            //masterUserData.append("rm /etc/mesos/zk\n"); // currently no zk supported
            masterUserData.append("mkdir -p /vol/spool/mesos\n");
            masterUserData.append("chmod -R 777 /vol/spool/mesos\n");
            masterUserData.append("echo bibigrid > /etc/mesos-master/cluster\n");
            masterUserData.append("curl http://instance-data/latest/meta-data/local-ipv4 > /etc/mesos-master/ip\n");
            masterUserData.append("echo /vol/spool/mesos > /etc/mesos-master/work_dir\n");
            masterUserData.append("service mesos-master start\n");
            if (cfg.isUseMasterAsCompute()) {
                masterUserData.append("echo /vol/spool/mesos > /etc/mesos-slave/work_dir\n");
                masterUserData.append("curl http://instance-data/latest/meta-data/local-ipv4 > /etc/mesos-master/master\n");
                masterUserData.append("echo \":5050\" >> /etc/mesos-master/master\n");
                masterUserData.append("service mesos-slave start\n");
            }
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
         * enabling nat functions of master-instance (slave inet access)
         * WARNING! 10.10.0.0 is a hardcoded SUBNET-proto...ensure generic
         * access laterly.
         */

        switch (cfg.getMode()) {
            case AWS_EC2:
                masterUserData.append("sysctl -q -w net.ipv4.ip_forward=1 net.ipv4.conf.eth0.send_redirects=0\n"
                        + "iptables -t nat -C POSTROUTING -o eth0 -s 10.10.0.0/24 -j MASQUERADE 2> /dev/null || iptables -t nat -A POSTROUTING -o eth0 -s 10.10.0.0/24 -j MASQUERADE\n");
                break;
            case OPENSTACK:
                // TESTING!!! @TODO
                masterUserData.append("echo 'http_proxy=http://proxy.cebitec.uni-bielefeld.de:3128' >> /etc/environment \n")
                        .append("echo 'https_proxy=https://proxy.cebitec.uni-bielefeld.de:3128' >> /etc/environment \n")
                        .append("echo 'ftp_proxy=http://proxy.cebitec.uni-bielefeld.de:3128' >> /etc/environment \n")
                        .append("echo 'no_proxy=localhost,127.0.0.1,169.254.169.254' >> /etc/environment \n")
                        .append("echo 'HTTP_PROXY=http://proxy.cebitec.uni-bielefeld.de:3128' >> /etc/environment \n")
                        .append("echo 'HTTPS_PROXY=https://proxy.cebitec.uni-bielefeld.de:3128' >> /etc/environment \n")
                        .append("echo 'FTP_PROXY=http://proxy.cebitec.uni-bielefeld.de:3128' >> /etc/environment \n")
                        .append("echo 'NO_PROXY=localhost,127.0.0.1,169.254.169.254' >> /etc/environment \n");
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
            case AWS_EC2:
                return new String(Base64.encodeBase64(masterUserData.toString().getBytes()));
            default:
                return masterUserData.toString();
        }
    }

    private static char ephemeral(int i) {
        return (char) (i + 98);
    }
}
