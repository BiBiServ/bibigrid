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
    private String subnetCidr;
    private String masterIp;
    private String masterHostname;
    private final List<String> slaveIps;
    private final List<String> slaveHostnames;

    public AnsibleConfig(Configuration config) {
        this.config = config;
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
        map.put("master", getMasterMap());
        map.put("slaves", getSlavesMap());
        map.put("CIDR", subnetCidr);
        map.put("nfs_mounts", getNfsSharesMap());
        addBooleanOption(map, "enable_nfs", config.isNfs());
        addBooleanOption(map, "enable_gridengine", config.isOge());
        addBooleanOption(map, "use_master_as_compute", config.isUseMasterAsCompute());
        addBooleanOption(map, "enable_mesos", config.isMesos());
        return yaml.dumpAsMap(map);
    }

    private void addBooleanOption(Map<String, Object> map, String option, boolean value) {
        map.put(option, value ? "yes" : "no");
    }

    private Map<String, Object> getMasterMap() {
        Map<String, Object> masterMap = new LinkedHashMap<>();
        masterMap.put("ip", masterIp);
        masterMap.put("hostname", masterHostname);
        masterMap.put("cores", config.getMasterInstanceType().getSpec().getInstanceCores());
        return masterMap;
    }

    private List<Map<String, Object>> getSlavesMap() {
        List<Map<String, Object>> slavesMap = new ArrayList<>();
        for (int i = 0; i < slaveIps.size(); i++) {
            Map<String, Object> slaveMap = new LinkedHashMap<>();
            slaveMap.put("ip", slaveIps.get(i));
            slaveMap.put("hostname", slaveHostnames.get(i));
            slaveMap.put("cores", config.getSlaveInstanceType().getSpec().getInstanceCores());
            slavesMap.add(slaveMap);
        }
        return slavesMap;
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