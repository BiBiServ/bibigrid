package de.unibi.cebitec.bibigrid.util;

import de.unibi.cebitec.bibigrid.model.Configuration;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class DeviceMapperTest {
    private static final String SNAPSHOT_ID1 = "snap-0a12b34c";
    private static final String SNAPSHOT_ID2 = "snap-0b584cd2";
    private static final String SNAPSHOT_ID3 = "snap-3a0c00ba";
    private static final String MOUNT_POINT1 = "/vol/test1";
    private static final String MOUNT_POINT2 = "/vol/test2";
    private static final String MOUNT_POINT3 = "/vol/test3";
    private static final Map<String, String> SNAPSHOT_ID_TO_MOUNT_POINT = new HashMap<String, String>() {
        {
            put(SNAPSHOT_ID1, MOUNT_POINT1);
            put(SNAPSHOT_ID2, MOUNT_POINT2);
            put(SNAPSHOT_ID3, MOUNT_POINT3);
        }
    };

    @Test
    public void getSnapshotIdToMountPoint() throws Exception {
        for (Configuration.MODE mode : Configuration.MODE.values()) {
            DeviceMapper mapper = new DeviceMapper(mode, SNAPSHOT_ID_TO_MOUNT_POINT, 0);
            assertEquals(SNAPSHOT_ID_TO_MOUNT_POINT, mapper.getSnapshotIdToMountPoint());
        }
    }

    @Test
    public void getDeviceNameForSnapshotId() throws Exception {
        for (Configuration.MODE mode : Configuration.MODE.values()) {
            DeviceMapper mapper = new DeviceMapper(mode, SNAPSHOT_ID_TO_MOUNT_POINT, 0);
            assertEquals("/dev/sdb", mapper.getDeviceNameForSnapshotId(SNAPSHOT_ID1));
            assertEquals("/dev/sdc", mapper.getDeviceNameForSnapshotId(SNAPSHOT_ID2));
            assertEquals("/dev/sdd", mapper.getDeviceNameForSnapshotId(SNAPSHOT_ID3));
        }
    }

    @Test
    public void getDeviceNameForSnapshotIdWithUsedDevices() throws Exception {
        for (Configuration.MODE mode : Configuration.MODE.values()) {
            DeviceMapper mapper = new DeviceMapper(mode, SNAPSHOT_ID_TO_MOUNT_POINT, 5);
            assertEquals("/dev/sdg", mapper.getDeviceNameForSnapshotId(SNAPSHOT_ID1));
            assertEquals("/dev/sdh", mapper.getDeviceNameForSnapshotId(SNAPSHOT_ID2));
            assertEquals("/dev/sdi", mapper.getDeviceNameForSnapshotId(SNAPSHOT_ID3));
        }
    }

    @Test
    public void getRealDeviceNameForMountPoint() throws Exception {
        DeviceMapper mapper = new DeviceMapper(Configuration.MODE.AWS, SNAPSHOT_ID_TO_MOUNT_POINT, 0);
        assertEquals("/dev/xvdb", mapper.getRealDeviceNameForMountPoint(MOUNT_POINT1));
        assertEquals("/dev/xvdc", mapper.getRealDeviceNameForMountPoint(MOUNT_POINT2));
        assertEquals("/dev/xvdd", mapper.getRealDeviceNameForMountPoint(MOUNT_POINT3));
        mapper = new DeviceMapper(Configuration.MODE.OPENSTACK, SNAPSHOT_ID_TO_MOUNT_POINT, 0);
        assertEquals("/dev/vdb", mapper.getRealDeviceNameForMountPoint(MOUNT_POINT1));
        assertEquals("/dev/vdc", mapper.getRealDeviceNameForMountPoint(MOUNT_POINT2));
        assertEquals("/dev/vdd", mapper.getRealDeviceNameForMountPoint(MOUNT_POINT3));
        mapper = new DeviceMapper(Configuration.MODE.GOOGLECLOUD, SNAPSHOT_ID_TO_MOUNT_POINT, 0);
        assertEquals("/dev/sdb", mapper.getRealDeviceNameForMountPoint(MOUNT_POINT1));
        assertEquals("/dev/sdc", mapper.getRealDeviceNameForMountPoint(MOUNT_POINT2));
        assertEquals("/dev/sdd", mapper.getRealDeviceNameForMountPoint(MOUNT_POINT3));
    }

    @Test
    public void getRealDeviceNameForMountPointWithUsedDevices() throws Exception {
        DeviceMapper mapper = new DeviceMapper(Configuration.MODE.AWS, SNAPSHOT_ID_TO_MOUNT_POINT, 4);
        assertEquals("/dev/xvdf", mapper.getRealDeviceNameForMountPoint(MOUNT_POINT1));
        assertEquals("/dev/xvdg", mapper.getRealDeviceNameForMountPoint(MOUNT_POINT2));
        assertEquals("/dev/xvdh", mapper.getRealDeviceNameForMountPoint(MOUNT_POINT3));
        mapper = new DeviceMapper(Configuration.MODE.OPENSTACK, SNAPSHOT_ID_TO_MOUNT_POINT, 4);
        assertEquals("/dev/vdf", mapper.getRealDeviceNameForMountPoint(MOUNT_POINT1));
        assertEquals("/dev/vdg", mapper.getRealDeviceNameForMountPoint(MOUNT_POINT2));
        assertEquals("/dev/vdh", mapper.getRealDeviceNameForMountPoint(MOUNT_POINT3));
        mapper = new DeviceMapper(Configuration.MODE.GOOGLECLOUD, SNAPSHOT_ID_TO_MOUNT_POINT, 4);
        assertEquals("/dev/sdf", mapper.getRealDeviceNameForMountPoint(MOUNT_POINT1));
        assertEquals("/dev/sdg", mapper.getRealDeviceNameForMountPoint(MOUNT_POINT2));
        assertEquals("/dev/sdh", mapper.getRealDeviceNameForMountPoint(MOUNT_POINT3));
    }

    @Test
    public void stripSnapshotId() throws Exception {
        assertEquals("snap-0a12b34c", DeviceMapper.stripSnapshotId("snap-0a12b34c"));
        assertEquals("snap-0a12b34c", DeviceMapper.stripSnapshotId("snap-0a12b34c:1"));
        assertEquals("", DeviceMapper.stripSnapshotId(""));
        assertNull(DeviceMapper.stripSnapshotId(null));
    }

    @Test
    public void getBlockDeviceBase() throws Exception {
        assertEquals("/dev/xvd", DeviceMapper.getBlockDeviceBase(Configuration.MODE.AWS));
        assertEquals("/dev/sd", DeviceMapper.getBlockDeviceBase(Configuration.MODE.GOOGLECLOUD));
        assertEquals("/dev/vd", DeviceMapper.getBlockDeviceBase(Configuration.MODE.OPENSTACK));
    }
}