package de.unibi.cebitec.bibigrid.util;


import de.unibi.cebitec.bibigrid.model.Configuration;
import java.util.HashMap;
import java.util.Map;

public class DeviceMapper {


    private Configuration.MODE MODE;
    
    private static final int MAX_DEVICES = 25; // vdb ... vdz
       
    //private int AVAIL_DEVICES;
    private int USED_DEVICES = 0;
    
    // snap-0a12b34c -> /my/dir/
    private Map<String, String> snapshotToMountPoint;

    // snap-0a12b34c -> /dev/sdf
    private Map<String, String> snapshotToDeviceName;

    // /my/dir/ -> /dev/xvdf
    private Map<String, String> mountPointToRealDeviceName;

    public DeviceMapper(Configuration.MODE mode, Map<String, String> snapshotIdToMountPoint, int used_devices) throws IllegalArgumentException {
        // set mode
        MODE = mode;
        
        // calculate the number of avail devices after  removing all used ephemerals
       USED_DEVICES = used_devices;

        if (snapshotIdToMountPoint.size() > (MAX_DEVICES-USED_DEVICES)) {
            throw new IllegalArgumentException("Too many volumes in map. Not enough device drivers left!");
        }
        this.snapshotToMountPoint = snapshotIdToMountPoint;
        this.snapshotToDeviceName = new HashMap<>();
        this.mountPointToRealDeviceName = new HashMap<>();
        for (Map.Entry<String, String> mapping : this.snapshotToMountPoint.entrySet()) {
            char letter = nextAvailableDeviceLetter();
            this.snapshotToDeviceName.put(mapping.getKey(), createDeviceName(letter));
            StringBuilder realDeviceName = new StringBuilder().append(createRealDeviceName(letter));
            int partitionNumber = getPartitionNumber(mapping.getKey());
            if (partitionNumber > 0) {
                realDeviceName.append(partitionNumber);
            }
            this.mountPointToRealDeviceName.put(mapping.getValue(), realDeviceName.toString());
        }

    }

    public Map<String, String> getSnapshotIdToMountPoint() {
        return snapshotToMountPoint;
    }

    public String getDeviceNameForSnapshotId(String snapshotId) {
        return this.snapshotToDeviceName.get(snapshotId);
    }

    public String getRealDeviceNameforMountPoint(String mountPoint) {
        return this.mountPointToRealDeviceName.get(mountPoint);
    }

    
  
    
    private char nextAvailableDeviceLetter() {
        char nextLetter = (char)(USED_DEVICES+98);
        USED_DEVICES ++;
        return nextLetter;
    }

    private String createDeviceName(char letter) {
        return new StringBuilder("/dev/sd").append(letter).toString();
    }

    private String createRealDeviceName(char letter) {
        return new StringBuilder(getBlockDeviceBase()).append(letter).toString();
    }

    private int getPartitionNumber(String rawSnapshotId) {
        // rawSnapshotId is e.g. 'snap-0a12b34c:1' where 1 is the partition number
        if (rawSnapshotId.contains(":")) {
            try {
                String[] idParts = rawSnapshotId.split(":");
                return Integer.parseInt(idParts[1]);
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                throw new IllegalArgumentException(
                        new StringBuilder().append("The partition number for snapshotId '")
                        .append(rawSnapshotId).append("' is invalid!").toString());
            }
        } else {
            return -1;
        }
    }

    /**
     * Remove any partition information from the given snapshot ID.
     *
     * @param rawSnapshotId The raw snapshot id (e.g. snap-0a12b34c:1)
     * @return A snapshot id without partition information. (e.g. snap-0a12b34c)
     */
    public static String stripSnapshotId(String rawSnapshotId) {
        if (rawSnapshotId.contains(":")) {
            String[] idParts = rawSnapshotId.split(":");
            return idParts[0];
        }
        return rawSnapshotId;
    }
    
    private String getBlockDeviceBase(){
        return getBlockDeviceBase(MODE);
    }
    
    /**
     * Return BlockDeviceBase in dependence of used cluster mode
     * 
     * @param mode
     * @return 
     */
    public static String getBlockDeviceBase(Configuration.MODE mode) {
    
        switch (mode) {
            case AWS:
                return "/dev/xvd";
               
            case OPENSTACK:
                return "/dev/vd";

            case GOOGLECLOUD:
                return "/dev/sd";
        }
        return null;
    }

}
