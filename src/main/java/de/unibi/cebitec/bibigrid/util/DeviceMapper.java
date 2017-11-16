package de.unibi.cebitec.bibigrid.util;

import de.unibi.cebitec.bibigrid.Provider;
import de.unibi.cebitec.bibigrid.model.ProviderModule;

import java.util.HashMap;
import java.util.Map;

public class DeviceMapper {
    // vdb ... vdz
    private static final int MAX_DEVICES = 25;

    private final String mode;
    // snap-0a12b34c -> /my/dir/
    private final Map<String, String> snapshotToMountPoint;
    // snap-0a12b34c -> /dev/sdf
    private final Map<String, String> snapshotToDeviceName;
    // /my/dir/ -> /dev/xvdf
    private final Map<String, String> mountPointToRealDeviceName;

    private int usedDevices = 0;

    public DeviceMapper(String mode, Map<String, String> snapshotIdToMountPoint, int usedDevices)
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

    private String createDeviceName(final char letter) {
        return "/dev/sd" + letter;
    }

    private String createRealDeviceName(final char letter) {
        return getBlockDeviceBase(mode) + letter;
    }

    private int getPartitionNumber(final String rawSnapshotId) {
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
    public static String stripSnapshotId(final String rawSnapshotId) {
        return rawSnapshotId != null ? rawSnapshotId.split(":")[0] : null;
    }

    /**
     * Return BlockDeviceBase in dependence of used cluster mode
     */
    public static String getBlockDeviceBase(final String mode) {
        ProviderModule providerModule = Provider.getInstance().getProviderModule(mode);
        return providerModule != null ? providerModule.getBlockDeviceBase() : null;
    }
}
