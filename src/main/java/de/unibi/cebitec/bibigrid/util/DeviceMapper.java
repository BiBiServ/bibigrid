package de.unibi.cebitec.bibigrid.util;

import de.unibi.cebitec.bibigrid.model.Configuration;

import java.util.HashMap;
import java.util.Map;

public class DeviceMapper {
    // vdb ... vdz
    private static final int MAX_DEVICES = 25;

    private final Configuration.MODE mode;
    // snap-0a12b34c -> /my/dir/
    private final Map<String, String> snapshotToMountPoint;
    // snap-0a12b34c -> /dev/sdf
    private final Map<String, String> snapshotToDeviceName;
    // /my/dir/ -> /dev/xvdf
    private final Map<String, String> mountPointToRealDeviceName;

    private int usedDevices = 0;

    public DeviceMapper(Configuration.MODE mode, Map<String, String> snapshotIdToMountPoint, int usedDevices)
            throws IllegalArgumentException {
        this.mode = mode;

        // calculate the number of avail devices after removing all used ephemerals
        this.usedDevices = usedDevices;

        if (snapshotIdToMountPoint.size() > (MAX_DEVICES - this.usedDevices)) {
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

    public String getRealDeviceNameForMountPoint(String mountPoint) {
        return this.mountPointToRealDeviceName.get(mountPoint);
    }

    private char nextAvailableDeviceLetter() {
        char nextLetter = (char) (usedDevices + 98); // b
        usedDevices++;
        return nextLetter;
    }

    private String createDeviceName(char letter) {
        return "/dev/sd" + letter;
    }

    private String createRealDeviceName(char letter) {
        return getBlockDeviceBase(mode) + letter;
    }

    private int getPartitionNumber(String rawSnapshotId) {
        // rawSnapshotId is e.g. 'snap-0a12b34c:1' where 1 is the partition number
        if (rawSnapshotId.contains(":")) {
            try {
                String[] idParts = rawSnapshotId.split(":");
                return Integer.parseInt(idParts[1]);
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                throw new IllegalArgumentException("The partition number for snapshotId '" + rawSnapshotId + "' is invalid!");
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
        return rawSnapshotId != null ? rawSnapshotId.split(":")[0] : null;
    }

    /**
     * Return BlockDeviceBase in dependence of used cluster mode
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
