package de.unibi.cebitec.bibigrid.core.model;

import org.yaml.snakeyaml.Yaml;

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
    private final Configuration config;
    private final String blockdevicebase;
    private String subnetCidr;
    private String masterIp;
    private String masterHostname;
    private final List<String> slaveIps;
    private final List<String> slaveHostnames;

    public AnsibleConfig(Configuration config, String blockdevicebase) {
        this.config = config;
        this.blockdevicebase = blockdevicebase;
        slaveIps = new ArrayList<>();
        slaveHostnames = new ArrayList<>();
    }

    public void setSubnetCidr(String subnetCidr) {
        this.subnetCidr = subnetCidr;
    }

    public void setMasterIpHostname(String masterIp, String masterHostname) {
        this.masterIp = masterIp;
        this.masterHostname = masterHostname;
    }

    public void addSlaveIpHostname(String ip, String hostname) {
        slaveIps.add(ip);
        slaveHostnames.add(hostname);
    }

    @Override
    public String toString() {
        Yaml yaml = new Yaml();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("default_user", config.getUser());
        map.put("ssh_user", config.getSshUser());
        map.put("master", getMasterMap());
        map.put("slaves", getSlavesMap());
        map.put("CIDR", subnetCidr);
        if (config.isNfs()) {
            map.put("nfs_mounts", getNfsSharesMap());
        }
        addBooleanOption(map, "enable_nfs", config.isNfs());
        addBooleanOption(map, "enable_gridengine", config.isOge());
        addBooleanOption(map, "use_master_as_compute", config.isUseMasterAsCompute());
        addBooleanOption(map, "enable_mesos", config.isMesos());
        addBooleanOption(map, "enable_cloud9", config.isCloud9());
        return yaml.dumpAsMap(map);
    }

    private void addBooleanOption(Map<String, Object> map, String option, boolean value) {
        map.put(option, value ? "yes" : "no");
    }

    private Map<String, Object> getMasterMap() {
        Map<String, Object> masterMap = new LinkedHashMap<>();
        masterMap.put("ip", masterIp);
        masterMap.put("hostname", masterHostname);
        masterMap.put("cores", config.getMasterInstance().getProviderType().getCpuCores());
        masterMap.put("ephemerals", getEphemeralDevices(config.getMasterInstance().getProviderType().getEphemerals()));
        return masterMap;
    }

    private List<Map<String, Object>> getSlavesMap() {
        List<Map<String, Object>> slavesMap = new ArrayList<>();
        List<Configuration.SlaveInstanceConfiguration> slaveInstanceConfigurations = config.getExpandedSlaveInstances();
        for (int i = 0; i < slaveIps.size(); i++) {
            Map<String, Object> slaveMap = new LinkedHashMap<>();
            slaveMap.put("ip", slaveIps.get(i));
            slaveMap.put("hostname", slaveHostnames.get(i));
            slaveMap.put("cores", slaveInstanceConfigurations.get(i).getProviderType().getCpuCores());
            slaveMap.put("ephemerals", getEphemeralDevices(slaveInstanceConfigurations.get(i).getProviderType().getEphemerals()));
            slavesMap.add(slaveMap);
        }
        return slavesMap;
    }

    private List<String> getEphemeralDevices(int count){
        List<String> ephemerals = new ArrayList<>();
        for (int c = 98; c < 98+count; c++) {
            ephemerals.add(blockdevicebase+(char)c);
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
