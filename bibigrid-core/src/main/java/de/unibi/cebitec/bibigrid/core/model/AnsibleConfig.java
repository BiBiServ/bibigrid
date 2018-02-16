package de.unibi.cebitec.bibigrid.core.model;

import de.unibi.cebitec.bibigrid.core.util.AnsibleResources;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Wrapper for the {@link Configuration} class with extra fields.
 * <p/>
 * The {@link #toString() toString} method outputs the configuration in yaml format
 * ready to use for ansible.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public final class AnsibleConfig {
    private static final int BLOCK_DEVICE_START = 98;

    private final Configuration config;
    private final String blockDeviceBase;
    private final String subnetCidr;
    private final Instance masterInstance;
    private final List<Instance> slaveInstances;

    public AnsibleConfig(Configuration config, String blockDeviceBase, String subnetCidr, Instance masterInstance,
                         List<Instance> slaveInstances) {
        this.config = config;
        this.blockDeviceBase = blockDeviceBase;
        this.subnetCidr = subnetCidr;
        this.masterInstance = masterInstance;
        this.slaveInstances = new ArrayList<>(slaveInstances);
    }

    public String[] getSlaveFilenames() {
        String[] filenames = new String[slaveInstances.size()];
        for (int i = 0; i < slaveInstances.size(); i++) {
            filenames[i] = AnsibleResources.CONFIG_ROOT_PATH + "slave-" + (i + 1) + ".yml";
        }
        return filenames;
    }

    public void writeSlaveFile(int i, OutputStream stream) {
        Map<String, Object> map = new LinkedHashMap<>();
        Instance slaveInstance = slaveInstances.get(i);
        map.put("ip", slaveInstance.getPrivateIp());
        map.put("hostname", slaveInstance.getHostname());
        map.put("cores", slaveInstance.getConfiguration().getProviderType().getCpuCores());
        map.put("ephemerals", getEphemeralDevices(slaveInstance.getConfiguration().getProviderType().getEphemerals()));
        writeToOutputStream(stream, map);
    }

    public void writeCommonFile(OutputStream stream) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("default_user", config.getUser());
        map.put("ssh_user", config.getSshUser());
        map.put("master", getMasterMap());
        map.put("slaves", Arrays.asList(getSlaveFilenames()));
        map.put("CIDR", subnetCidr);
        if (config.isNfs()) {
            map.put("nfs_mounts", getNfsSharesMap());
        }
        addBooleanOption(map, "enable_nfs", config.isNfs());
        addBooleanOption(map, "enable_gridengine", config.isOge());
        addBooleanOption(map, "use_master_as_compute", config.isUseMasterAsCompute());
        addBooleanOption(map, "enable_mesos", config.isMesos());
        addBooleanOption(map, "enable_cloud9", config.isCloud9());
        writeToOutputStream(stream, map);
    }

    private void writeToOutputStream(OutputStream stream, Map<String, Object> map) {
        try {
            try (OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
                writer.write(new Yaml().dumpAsMap(map));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addBooleanOption(Map<String, Object> map, String option, boolean value) {
        map.put(option, value ? "yes" : "no");
    }

    private Map<String, Object> getMasterMap() {
        Map<String, Object> masterMap = new LinkedHashMap<>();
        masterMap.put("ip", masterInstance.getPrivateIp());
        masterMap.put("hostname", masterInstance.getHostname());
        masterMap.put("cores", config.getMasterInstance().getProviderType().getCpuCores());
        masterMap.put("ephemerals", getEphemeralDevices(config.getMasterInstance().getProviderType().getEphemerals()));
        return masterMap;
    }

    private List<String> getEphemeralDevices(int count) {
        List<String> ephemerals = new ArrayList<>();
        for (int c = BLOCK_DEVICE_START; c < BLOCK_DEVICE_START + count; c++) {
            ephemerals.add(blockDeviceBase + (char) c);
        }
        return ephemerals;
    }

    private List<Map<String, String>> getNfsSharesMap() {
        List<Map<String, String>> nfsSharesMap = new ArrayList<>();
        for (String nfsShare : config.getNfsShares()) {
            Map<String, String> shareMap = new LinkedHashMap<>();
            shareMap.put("src", nfsShare);
            shareMap.put("dst", nfsShare);
            nfsSharesMap.add(shareMap);
        }
        return nfsSharesMap;
    }
}
