/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.util;

import de.unibi.cebitec.bibigrid.model.Configuration;
import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;
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
 * @author jkrueger(at)cebitec.uni-bielefeld.de,
 * alueckne(at)cebitec.uni-bielefeld.de
 */
public class UserDataCreator {

  public static final Logger LOG = LoggerFactory.getLogger(UserDataCreator.class);
  private static final String SSH = "ssh -o CheckHostIP=no -o StrictHostKeyChecking=no ";
  private static final String SCP = "scp -o CheckHostIP=no -o StrictHostKeyChecking=no ";

  /**
   * Creates slaveUserData content.
   *
   *
   * @param masterIp
   * @param masterDns
   * @param slaveDeviceMapper
   * @param cfg
   * @param keypair
   *
   * @return
   */
  public static String forSlave(String masterIp, String masterDns, DeviceMapper slaveDeviceMapper, Configuration cfg, KEYPAIR keypair) {
    StringBuilder slaveUserData = new StringBuilder();

    slaveUserData.append("#!/bin/bash\n");
    slaveUserData.append("exec > /var/log/userdata.log\n")
            .append("exec 2>&1\n");
    /* append additional service check fct */
    shellFct(slaveUserData);

    /* Save currentIP as env var */
    slaveUserData.append("IP=$(curl http://169.254.169.254/latest/meta-data/local-ipv4)\n");

    slaveUserData.append("echo '").append(keypair.getPrivateKey()).append("' > /home/ubuntu/.ssh/id_rsa\n");
    slaveUserData.append("chown ubuntu:ubuntu /home/ubuntu/.ssh/id_rsa\n");
    slaveUserData.append("chmod 600 /home/ubuntu/.ssh/id_rsa\n");
    slaveUserData.append("echo '").append(keypair.getPublicKey()).append("' >> /home/ubuntu/.ssh/authorized_keys\n");

    slaveUserData.append("mkdir -p /vol/spool/log\n");
    slaveUserData.append("mkdir -p /vol/scratch/\n");

    /*
         * GridEngine Block
     */
    if (cfg.isOge()) {
      slaveUserData.append("echo ").append(masterDns).append(" > /var/lib/gridengine/default/common/act_qmaster\n");
      // test for sge_master available
      slaveUserData.append("ch_s ").append(masterIp).append(" 6444\n");
      // start sge_exed 
      slaveUserData.append("ch_p sge_execd 10 \"service gridengine-exec start\"\n");
      slaveUserData.append("log 'sge_execd started'\n");
    }

    /*
         * Ganglia service monitor
     */
    slaveUserData.append("sed -i s/MASTER_IP/").append(masterIp).append("/g /etc/ganglia/gmond.conf\n");
    slaveUserData.append("service ganglia-monitor start \n");
    slaveUserData.append("log 'ganglia configured and started'\n");

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

    if (ephemeralamount == 1) {
      slaveUserData.append("umount /mnt\n");
      switch (cfg.getLocalFS()) {
        case EXT2: {
          slaveUserData.append("mkfs.ext2 ").append(blockDeviceBase).append("b\n");
          break;
        }
        case EXT3: {
          slaveUserData.append("mkfs.ext3 ").append(blockDeviceBase).append("b\n");
          break;
        }
        case EXT4: {
          slaveUserData.append("mkfs.ext4 ").append(blockDeviceBase).append("b\n");
          break;
        }
        default: {
          slaveUserData.append("mkfs.xfs -f /").append(blockDeviceBase).append("b\n");
        }
      }
      slaveUserData.append("mount ").append(blockDeviceBase).append("b /vol/scratch\n");
      slaveUserData.append("log 'ephemeral configured'\n");

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
        case EXT2: {
          slaveUserData.append("mkfs.ext2 /dev/md0\n");
          slaveUserData.append("mount -t ext2 -o noatime /dev/md0 /vol/scratch\n");
          break;
        }
        case EXT3: {
          slaveUserData.append("mkfs.ext3 /dev/md0\n");
          slaveUserData.append("mount -t ext3 -o noatime /dev/md0 /vol/scratch\n");
          break;
        }
        case EXT4: {
          slaveUserData.append("mkfs.ext4 /dev/md0\n");
          slaveUserData.append("mount -t ext4 -o noatime /dev/md0 /vol/scratch\n");
          break;
        }
        default: {
          slaveUserData.append("mkfs.xfs -f /dev/md0\n");
          slaveUserData.append("mount -t xfs -o noatime /dev/md0 /vol/scratch\n");
        }
      }
      slaveUserData.append("log 'ephemerals configured'\n");
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
      slaveUserData.append("log 'Cassandra pre-configured'\n");

    }

    /*
         * NFS//Mount Block
     */
    slaveUserData.append("chown ubuntu:ubuntu /vol/ \n");
    if (cfg.isNfs()) {
      // wait for NFS server is ready and available
      slaveUserData.append("ch_s ").append(masterIp).append(" 2049\n");

      slaveUserData.append(
              "mount -t nfs4 -o proto=tcp,port=2049 ").append(masterIp).append(":/vol/spool /vol/spool\n");
      slaveUserData.append(
              "mount -t nfs4 -o proto=tcp,port=2049 ").append(masterIp).append(":/opt/ /opt/\n");

      for (String e
              : slaveDeviceMapper.getSnapshotIdToMountPoint()
              .keySet()) {
        slaveUserData.append("mkdir -p ")
                .append(slaveDeviceMapper.getSnapshotIdToMountPoint().get(e))
                .append("\n");
        slaveUserData.append("mount ")
                .append(slaveDeviceMapper.getRealDeviceNameforMountPoint(slaveDeviceMapper.getSnapshotIdToMountPoint().get(e)))
                .append(" ")
                .append(slaveDeviceMapper.getSnapshotIdToMountPoint().get(e))
                .append("\n");
      }

      // add additional master nfs shares
      if (!cfg.getNfsShares().isEmpty()) {
        for (String share : cfg.getNfsShares()) {
          slaveUserData.append("mkdir -p ").append(share).append("\n");
          slaveUserData.append("mount -t nfs4 -o proto=tcp,port=2049 ").append(masterIp).append(":").append(share).append(" ").append(share).append("\n");
        }
      }

      // add external nfs shares
      if (!cfg.getExtNfsShares().isEmpty()) {
        for (String server : cfg.getExtNfsShares().keySet()) {
          String share = cfg.getExtNfsShares().get(server);
          slaveUserData.append("mkdir -p ").append(share).append("\n");
          slaveUserData.append("mount -t nfs4 -o proto=tcp,port=2049 ").append(server).append(":").append(share).append(" ").append(share).append("\n");
        }
      }

      slaveUserData.append("log \"nfs configured\"\n");

      /*
         * HDFS Block
       */
      if (cfg.isHdfs()) {
        slaveUserData.append("mkdir -p /vol/scratch/hadoop/dn\n");
        slaveUserData.append("chown -R hadoop:hadoop /vol/scratch/hadoop\n");
        slaveUserData.append("chmod -R 777 /vol/scratch/hadoop\n");
        slaveUserData.append("log \"hdfs configured and started\"\n");
      }

    }
    /**
     * route all traffic to master-instance (inet access) if slaves not
     * configured with public ip address
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

      // configure zk
      slaveUserData.append("echo zk://").append(masterIp).append(":2181/mesos > /etc/mesos/zk\n");
      //configure mesos-slave
      slaveUserData.append("echo /vol/spool/mesos > /etc/mesos-slave/work_dir\n");
      slaveUserData.append("echo mesos,docker > /etc/mesos-slave/containerizers\n");
      slaveUserData.append("echo false > /etc/mesos-slave/switch_user\n");
      // start mesos-slave
      slaveUserData.append("service mesos-slave start\n");
      slaveUserData.append("log \"mesos slave configured and started\"\n");
    }

    /*
         * Early Execute Script for Slave
     */
    if (cfg.getEarlySlaveShellScriptFile() != null) {
      try {

        String base64 = new String(Base64.encodeBase64(Files.readAllBytes(cfg.getEarlySlaveShellScriptFile())));

        if (base64.length() > 10000) {
          LOG.info("Early shell script file too large  (base64 encoded size exceeds 10000 chars)");
        } else {
          slaveUserData.append("echo ").append(base64).append(" | base64 --decode  | bash - 2>&1 > /var/log/earlyshellscript.log \n");
          slaveUserData.append("log \"earlyshellscript executed\"\n");
        }

      } catch (IOException e) {
        LOG.info("Early shell script could not be read.");
      }
    }

    // copy userdata.log using scp to master : must be done as ubuntu user since the ubuntu user has the
    slaveUserData.append("ch_s ${IP} 22\n");
    slaveUserData.append("sudo -u ubuntu ").append(SCP).append("/var/log/userdata.log ").append(masterIp).append(":/var/log/bibigrid/${IP}\n");
    slaveUserData.append("exit 0\n");

    return new String(Base64.encodeBase64(slaveUserData.toString().getBytes()));
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
    masterUserData.append("#!/bin/bash\n");
    masterUserData.append("exec > /var/log/userdata.log\n")
            .append("exec 2>&1\n");
    masterUserData.append("echo 'MasterUserData executed!'\n");

    /* append additional shell fct */
    shellFct(masterUserData);

    
    masterUserData.append("IP=$(curl http://169.254.169.254/latest/meta-data/local-ipv4)\n");
    masterUserData.append("echo '").append(keypair.getPrivateKey()).append("' > /home/ubuntu/.ssh/id_rsa\n");
    masterUserData.append("chown ubuntu:ubuntu /home/ubuntu/.ssh/id_rsa\n");
    masterUserData.append("chmod 600 /home/ubuntu/.ssh/id_rsa\n");
    masterUserData.append("echo '").append(keypair.getPublicKey()).append("' >> /home/ubuntu/.ssh/authorized_keys\n");

    /*
         * Ephemeral/RAID Preperation
     */
    String blockDeviceBase = DeviceMapper.getBlockDeviceBase(cfg.getMode());
    // if 1 ephemeral is available mount it as /vol/spool
    if (ephemeralamount == 1) {
      masterUserData.append("umount /mnt\n"); // 
      switch (cfg.getLocalFS()) {
        case EXT2: {
          masterUserData.append("mkfs.ext2 ").append(blockDeviceBase).append("b\n");
          break;
        }
        case EXT3: {
          masterUserData.append("mkfs.ext3 ").append(blockDeviceBase).append("b\n");
          break;
        }
        case EXT4: {
          masterUserData.append("mkfs.ext4 ").append(blockDeviceBase).append("b\n");
          break;
        }
        default: {
          masterUserData.append("mkfs.xfs -f /").append(blockDeviceBase).append("b\n");
        }
      }
      masterUserData.append("mount ").append(blockDeviceBase).append("b /vol/\n");
      masterUserData.append("log \"ephemeral configured\"\n");
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
        case EXT2: {
          masterUserData.append("mkfs.ext2 -f /dev/md0\n");
          masterUserData.append("mount -t ext2 -o noatime /dev/md0 /vol/\n");
          break;
        }
        case EXT3: {
          masterUserData.append("mkfs.ext3 -f /dev/md0\n");
          masterUserData.append("mount -t ext3 -o noatime /dev/md0 /vol/\n");
          break;
        }
        case EXT4: {
          masterUserData.append("mkfs.ext4 -f /dev/md0\n");
          masterUserData.append("mount -t ext4 -o noatime /dev/md0 /vol/\n");
          break;
        }
        default: {
          masterUserData.append("mkfs.xfs -f /dev/md0\n");
          masterUserData.append("mount -t xfs -o noatime /dev/md0 /vol/\n");
        }
      }
      masterUserData.append("log \"ephemerals configured\"\n");
    }

    /*
         * create spool, scratch, log
     */
    masterUserData.append("mkdir -p /vol/spool/log\n");
    masterUserData.append("mkdir -p /vol/scratch/\n");
    masterUserData.append("chown -R ubuntu:ubuntu /vol/\n");
    masterUserData.append("chmod -R 777 /vol/\n");
    
    /* create bibigrid specific log within /var/spool */
    masterUserData.append("mkdir -p /var/log/bibigrid\n");
    masterUserData.append("chown root:ubuntu /var/log/bibigrid\n");
    masterUserData.append("chmod 775 /var/log/bibigrid\n");

    /*
         * Cassandra Bloock
     */
    if (cfg.isCassandra()) {
      masterUserData.append("mkdir -p /vol/scratch/cassandra\n");
      masterUserData.append("chown -R cassandra:cassandra /vol/scratch/cassandra\n");
      masterUserData.append("chmod -R 777 /vol/scratch/cassandra\n");
      masterUserData.append("log \"cassandra pre-configured\"\n");
    }

    /*
         * HDFS Block
     */
    if (cfg.isHdfs()) {
      masterUserData.append("mkdir -p /vol/scratch/hadoop/nn\n");
      masterUserData.append("mkdir -p /vol/scratch/hadoop/dn\n");
      masterUserData.append("chown -R hadoop:hadoop /vol/scratch/hadoop\n");
      masterUserData.append("chmod -R 777 /vol/scratch/hadoop\n");
      masterUserData.append("log \"hdfs configured - nn/dn started\"\n");
    }

    /*
         * OGE Block
     */
    if (cfg.isOge()) {
      switch (cfg.getMode()) {
        case AWS:
          masterUserData.append("curl -sS http://169.254.169.254/latest/meta-data/public-hostname > /var/lib/gridengine/default/common/act_qmaster\n");
          break;
        case OPENSTACK:
          masterUserData.append("echo $(hostname) > /var/lib/gridengine/default/common/act_qmaster\n");
          break;
      }

      masterUserData.append("chown sgeadmin:sgeadmin /var/lib/gridengine/default/common/act_qmaster\n");
      masterUserData.append("service gridengine-master start\n");
      masterUserData.append("log \"gridengine-master configured and started\"\n");
    }


    /* 
         * Mesos Block
     */
    if (cfg.isMesos()) {
      // start zookeeper
      masterUserData.append("service zookeeper start\n");
      // configure mesos master
      masterUserData.append("mkdir -p /vol/spool/mesos\n");
      masterUserData.append("chmod -R 777 /vol/spool/mesos\n");
      masterUserData.append("echo bibigrid > /etc/mesos-master/cluster\n");
      masterUserData.append("echo ${IP} > /etc/mesos-master/ip\n");
      masterUserData.append("echo /vol/spool/mesos > /etc/mesos-master/work_dir\n");
      masterUserData.append("echo zk://${IP}:2181/mesos > /etc/mesos/zk\n");
      masterUserData.append("service mesos-master start\n");
      masterUserData.append("log \"mesos-master configured and started\"\n");
      if (cfg.isUseMasterAsCompute()) {
        //configure mesos-slave
        masterUserData.append("echo /vol/spool/mesos > /etc/mesos-slave/work_dir\n");
        masterUserData.append("echo mesos,docker > /etc/mesos-slave/containerizers\n");
        masterUserData.append("echo false > /etc/mesos-slave/switch_user\n");
        masterUserData.append("service mesos-slave start\n");
        masterUserData.append("log \"mesos-slave configured and started\"\n");
      }
    }

    /* Block Devices */
    if (masterDeviceMapper != null) {
      for (String e : masterDeviceMapper.getSnapshotIdToMountPoint().keySet()) {
        String device = masterDeviceMapper.getRealDeviceNameforMountPoint(masterDeviceMapper.getSnapshotIdToMountPoint().get(e));
        String mountpoint = masterDeviceMapper.getSnapshotIdToMountPoint().get(e);
        masterUserData.append("umount ").append(device).append("\n");
        masterUserData.append("mkdir -p ").append(mountpoint).append("\n");
        masterUserData.append("mount ").append(device).append(" ").append(mountpoint).append("\n");
      }
    }

    /*
         * NFS//Mounts Block
     */
    if (cfg.isNfs()) {

      masterUserData.append("IPBASE=`echo ${IP} | cut -f 1-3 -d .`\n");
      // export spool dir
      masterUserData.append("echo \"/vol/spool/ ${IPBASE}.0/24(rw,nohide,insecure,no_subtree_check,async)\" >> /etc/exports\n");
      // export opt dir
      masterUserData.append("echo \"/opt/ ${IPBASE}.0/24(rw,nohide,insecure,no_subtree_check,async)\" >> /etc/exports\n");

      for (String mastershare : masterNfsShares) {
        masterUserData.append("mkdir -p ").append(mastershare).append("\n");
        masterUserData.append("chmod 777 ").append(mastershare).append("\n");
        masterUserData.append("echo \"").append(mastershare).append(" ${IPBASE}.0/24(rw,nohide,insecure,no_subtree_check,async)\">> /etc/exports\n");
      }
      masterUserData.append("service nfs-kernel-server restart\n");
      masterUserData.append("log \"NFS Server configured and restarted\"\n");

      // add external nfs shares
      if (!cfg.getExtNfsShares().isEmpty()) {
        for (String server : cfg.getExtNfsShares().keySet()) {
          String share = cfg.getExtNfsShares().get(server);
          masterUserData.append("mkdir -p ").append(share).append("\n");
          masterUserData.append("mount -t nfs4 -o proto=tcp,port=2049 ").append(server).append(":").append(share).append(" ").append(share).append("\n");
        }
        masterUserData.append("log \"External NFS shares mounted.\"\n");
      }

    }
    /**
     * Enabling nat functions of master-instance (slave inet access) if slaves
     * configured without public ip address WARNING! 10.10.0.0 is a hardcoded
     * SUBNET-proto...ensure generic access later.
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
    if (cfg.getEarlyMasterShellScriptFile() != null) {
      try {

        String base64 = new String(Base64.encodeBase64(Files.readAllBytes(cfg.getEarlyMasterShellScriptFile())));

        if (base64.length() > 10000) {
          LOG.info("Early shell script file too large  (base64 encoded size exceeds 10000 chars)");
        } else {
          masterUserData.append("echo ").append(base64).append(" | base64 --decode  | bash - 2>&1 > /var/log/earlyshellscript.log \n");
          masterUserData.append("log \"earlyshellscript executed\"\n");
        }

      } catch (IOException e) {
        LOG.info("Early shell script could not be read.");
      }
    }
    masterUserData.append("log \"userdata.finished\"\n");
    masterUserData.append("cp /var/log/userdata.log /var/log/bibigrid/${IP}\n");

    masterUserData.append("exit 0\n");

    LOG.info(V, "Master userdata:\n{}", masterUserData.toString());
    return new String(Base64.encodeBase64(masterUserData.toString().getBytes()));
  }

  private static char ephemeral(int i) {
    return (char) (i + 98);
  }

  public static void shellFct(StringBuilder sb) {
    sb.append("function log { date +\"%x %R:%S - ${1}\";}\n");
    sb.append("function ch_s {\n")
            .append("\t/bin/nc ${1} ${2} </dev/null 2>/dev/null\n")
            .append("\twhile test $? -eq 1; do\n")
            .append("\t\tlog \"wait for service at ${1}:${2}\"\n")
            .append("\t\tsleep 2\n")
            .append("\t\t/bin/nc ${1} ${2} </dev/null 2>/dev/null\n")
            .append("\tdone\n")
            .append("}\n");
    sb.append("function ch_f {\n"
            + "\twhile [ ! -f ${1} ]; do\n"
            + "\t\tlog \"wait for file ${1}\"\n"
            + "\t\tsleep 2\n"
            + "\tdone\n"
            + "}\n");
            
    
    sb.append("function ch_p {\n")
            .append("\twhile [ $(ps -e | grep ${1} | wc -l) != '1' ]; do\n")
            .append("\t\t${3}\n")
            .append("\t\tlog \"wait for process ${1}\"\n")
            .append("\t\tsleep ${2}\n")
            .append("\t\tlog \"$(ps -e | grep ${1})\"\n")
            .append("\tdone;\n")
            .append("}\n");
    

  }
}
