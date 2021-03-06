package de.unibi.cebitec.bibigrid.core.util;

import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceMapper {
    // vdb ... vdz
    private static final int MAX_DEVICES = 25;

    private final ProviderModule providerModule;
    // snap-0a12b34c -> /my/dir/
    private final List<Configuration.MountPoint> snapshotToMountPoint;
    // snap-0a12b34c -> /dev/sdf
    private final Map<String, String> snapshotToDeviceName;
    // /my/dir/ -> /dev/xvdf
    private final Map<String, String> mountPointToRealDeviceName;

    private int usedDevices;

    public DeviceMapper(ProviderModule providerModule, List<Configuration.MountPoint> snapshotIdToMountPoint,
                        int usedDevices) throws IllegalArgumentException {
        this.providerModule = providerModule;

        // calculate the number of avail devices after removing all used ephemerals
        this.usedDevices = usedDevices;

        if (snapshotIdToMountPoint.size() > (MAX_DEVICES - this.usedDevices)) {
            throw new IllegalArgumentException("Too many volumes in map. Not enough device drivers left!");
        }
        this.snapshotToMountPoint = snapshotIdToMountPoint;
        this.snapshotToDeviceName = new HashMap<>();
        this.mountPointToRealDeviceName = new HashMap<>();
        for (Configuration.MountPoint mapping : this.snapshotToMountPoint) {
            char letter = nextAvailableDeviceLetter();
            this.snapshotToDeviceName.put(mapping.getSource(), createDeviceName(letter));
            StringBuilder realDeviceName = new StringBuilder().append(createRealDeviceName(letter));
            int partitionNumber = getPartitionNumber(mapping.getSource());
            if (partitionNumber > 0) {
                realDeviceName.append(partitionNumber);
            }
            this.mountPointToRealDeviceName.put(mapping.getTarget(), realDeviceName.toString());
        }
    }

    public List<Configuration.MountPoint> getSnapshotIdToMountPoint() {
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
        return getBlockDeviceBase(providerModule) + letter;
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
    public static String getBlockDeviceBase(final ProviderModule providerModule) {
        return providerModule != null ? providerModule.getBlockDeviceBase() : null;
    }

    /**
     * Return BlockDeviceBase in dependence of used cluster mode
     */
    public String getBlockDeviceBase() {
        return getBlockDeviceBase(providerModule);
    }
}
