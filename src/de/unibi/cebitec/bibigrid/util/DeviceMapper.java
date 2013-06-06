package de.unibi.cebitec.bibigrid.util;

import java.util.HashMap;
import java.util.Map;

public class DeviceMapper {

    private static final String[] POSSIBLE_DEVICE_LETTERS = {"f", "g", "h", "i",
        "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};
    private int usedLetters = 0;
    private Map<String, String> snapshotToMountPoint;
    private Map<String, String> snapshotToDeviceName;
    private Map<String, String> mountPointToRealDeviceName;
    public DeviceMapper(Map<String, String> snapshotIdToMountPoint) throws IllegalArgumentException {
        if (snapshotIdToMountPoint.size() > POSSIBLE_DEVICE_LETTERS.length) {
            throw new IllegalArgumentException("Too many volumes in map. Not enough device drivers left!");
        }
        this.snapshotToMountPoint = snapshotIdToMountPoint;
        this.snapshotToDeviceName = new HashMap<>();
        this.mountPointToRealDeviceName = new HashMap<>();
        for (Map.Entry<String, String> mapping : this.snapshotToMountPoint.entrySet()) {
            String letter = nextAvailableDeviceLetter();
            this.snapshotToDeviceName.put(mapping.getKey(), createDeviceName(letter));
            this.mountPointToRealDeviceName.put(mapping.getValue(), createRealDeviceName(letter));
        }
        
    }

    public Map<String,String> getSnapshotIdToMountPoint() {
        return snapshotToMountPoint;
    }
    public String getDeviceNameForSnapshotId(String snapshotId) {
        return this.snapshotToDeviceName.get(snapshotId);
    }

    public String getRealDeviceNameforMountPoint(String mountPoint) {
        return this.mountPointToRealDeviceName.get(mountPoint);
    }

    private String nextAvailableDeviceLetter() {
        String nextLetter = POSSIBLE_DEVICE_LETTERS[usedLetters];
        this.usedLetters++;
        return nextLetter;
    }

    private String createDeviceName(String letter) {
        return new StringBuilder("/dev/sd").append(letter).toString();
    }

    private String createRealDeviceName(String letter) {
        return new StringBuilder("/dev/xvd").append(letter).toString();
    }
  
   
}
