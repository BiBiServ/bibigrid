/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.util;

import java.util.List;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author alueckne
 */
public class UserDataCreator {

    public static String forSlave(String masterIp, String masterDns, DeviceMapper slaveDeviceMapper, List<String> slaveNfsMounts, int ephemerals) {
        String slaveUserData = "#!/bin/sh\n"
                + "sleep 5\n"
                + "sudo mkdir -p /vol/spool/\n"
                + "sudo mkdir -p /vol/scratch/\n"
                + "echo " + masterIp + " > /var/lib/gridengine/default/common/act_qmaster\n"
                + "echo " + masterIp + " " + masterDns + " >> /etc/hosts\n"
                + "pid=`ps acx | grep sge_execd | cut -c1-6`\n"
                + "if [ -n $pid ]; then\n"
                + "        kill $pid;\n"
                + "fi;\n"
                + "sleep 1\n"
                + "while test $(ps acx | grep sge_execd | wc -l) -eq 0; do\n"
                + "        service gridengine-exec start\n"
                + "        sleep 35\n"
                + "done\n"
                + "sed -i s/MASTER_IP/" + masterIp + "/g /etc/ganglia/gmond.conf\n"
                + "service ganglia-monitor restart \n";


        if (ephemerals == 1) {
            slaveUserData += "sudo umount /mnt\n"; // 
            slaveUserData += "sudo mount /dev/xvdb /vol/scratch\n";
        } else if (ephemerals >= 2) {
            // if 2 or more ephemerals are available use 2 as a RAID system
            slaveUserData += "sudo umount /mnt\n";

            switch (ephemerals) {
                case 2: {
                    slaveUserData += "yes | mdadm --create /dev/md0 --level=0 -c256 --raid-devices=2 /dev/xvdb /dev/xvdc\n";
                    break;
                }
                case 4: {
                    slaveUserData += "yes | mdadm --create /dev/md0 --level=0 -c256 --raid-devices=4 /dev/xvdb /dev/xvdc /dev/xvdd /dev/xvde\n";
                    break;
                }
            }

            slaveUserData += "echo 'DEVICE /dev/xvdb /dev/xvdc' > /etc/mdadm.conf\n";
            slaveUserData += "mdadm --detail --scan >> /etc/mdadm.conf\n";

            slaveUserData += "blockdev --setra 65536 /dev/md0\n";
            slaveUserData += "mkfs.xfs -f /dev/md0\n";
            slaveUserData += "mount -t xfs -o noatime /dev/md0 /vol/scratch\n";


        }
        slaveUserData += "chown ubuntu:ubuntu /vol/ \n";
        slaveUserData += "mount -t nfs4 -o proto=tcp,port=2049 " + masterIp + ":/vol/spool /vol/spool\n";

        for (String e : slaveDeviceMapper.getSnapshotIdToMountPoint().keySet()) {
            slaveUserData += "mkdir -p " + slaveDeviceMapper.getSnapshotIdToMountPoint().get(e) + "\n";
            slaveUserData += "mount " + slaveDeviceMapper.getRealDeviceNameforMountPoint(slaveDeviceMapper.getSnapshotIdToMountPoint().get(e)) + " " + slaveDeviceMapper.getSnapshotIdToMountPoint().get(e) + "\n";
        }
        if (!slaveNfsMounts.isEmpty()) {
            for (String share : slaveNfsMounts) {
                slaveUserData += "sudo mkdir -p " + share + "\n";
                slaveUserData += "mount -t nfs4 -o proto=tcp,port=2049 " + masterIp + ":" + share + " " + share + "\n";
            }
        }
        String base64 = new String(Base64.encodeBase64(slaveUserData.getBytes()));
        return base64;
    }

    public static String masterUserData(int ephemeralamount, List<String> masterNfsShares, DeviceMapper masterDeviceMapper) {
        String masterUserData = "#!/bin/sh\n"
                + "sleep 5\n";

        // if 1 ephemeral is available mount it as /vol/spool
        if (ephemeralamount == 1) {
            masterUserData += "sudo umount /mnt\n"; // 
            masterUserData += "sudo mount /dev/xvdb /vol/\n";
        } else if (ephemeralamount >= 2) {
            // if 2 or more ephemerals are available use 2 as a RAID system
            masterUserData += "sudo umount /mnt\n";
            switch (ephemeralamount) {
                case 2: {
                    masterUserData += "yes | mdadm --create /dev/md0 --level=0 -c256 --raid-devices=2 /dev/xvdb /dev/xvdc\n";
                    break;
                }
                case 4: {
                    masterUserData += "yes | mdadm --create /dev/md0 --level=0 -c256 --raid-devices=4 /dev/xvdb /dev/xvdc /dev/xvdd /dev/xvde\n";
                    break;
                }
            }
            masterUserData += "echo 'DEVICE /dev/xvdb /dev/xvdc' > /etc/mdadm.conf\n";
            masterUserData += "mdadm --detail --scan >> /etc/mdadm.conf\n";

            masterUserData += "blockdev --setra 65536 /dev/md0\n";
            masterUserData += "mkfs.xfs -f /dev/md0\n";
            masterUserData += "mount -t xfs -o noatime /dev/md0 /vol/\n";

        }

        masterUserData += "sudo mkdir -p " + "/vol/spool/" + "\n";
        masterUserData += "sudo chmod 777 " + "/vol/spool/" + "\n";
        masterUserData += "echo \"echo '" + "/vol/spool/" + " 10.0.0.0/8(rw,nohide,insecure,no_subtree_check,async)'>> /etc/exports\" | sudo sh\n";

        masterUserData += "sudo mkdir -p " + "/vol/scratch/" + "\n";
        masterUserData += "chown ubuntu:ubuntu /vol/ \n";
        masterUserData += "chown ubuntu:ubuntu /vol/scratch \n";
        for (String e : masterDeviceMapper.getSnapshotIdToMountPoint().keySet()) {
            masterUserData += "mkdir -p " + masterDeviceMapper.getSnapshotIdToMountPoint().get(e) + "\n";
            masterUserData += "mount " + masterDeviceMapper.getRealDeviceNameforMountPoint(masterDeviceMapper.getSnapshotIdToMountPoint().get(e)) + " " + masterDeviceMapper.getSnapshotIdToMountPoint().get(e) + "\n";
        }
        for (String mastershare : masterNfsShares) {
            masterUserData += "sudo mkdir -p " + mastershare + "\n";
            masterUserData += "sudo chmod 777 " + mastershare + "\n";
            masterUserData += "echo \"echo '" + mastershare + " 10.0.0.0/8(rw,nohide,insecure,no_subtree_check,async)'>> /etc/exports\" | sudo sh\n";
        }
        masterUserData += "sudo /etc/init.d/nfs-kernel-server restart\n";
        String base64 = new String(Base64.encodeBase64(masterUserData.getBytes()));
        return base64;
    }
}
