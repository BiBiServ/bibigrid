package de.unibi.cebitec.bibigrid.core.model;

import de.unibi.cebitec.bibigrid.core.util.DeviceMapper;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Wrapper for the {@link Configuration} class with extra fields.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 *
 */
public final class AnsibleConfig {
    private static final int BLOCK_DEVICE_START = 98;

    private final Configuration config;
    private final String blockDeviceBase;
    private final String subnetCidr;
    private final Instance masterInstance;
    private final List<Instance> slaveInstances;
    private List<Configuration.MountPoint> masterMounts;

    public AnsibleConfig(Configuration config, String blockDeviceBase, String subnetCidr, Instance masterInstance,
                         List<Instance> slaveInstances) {
        this.config = config;
        this.blockDeviceBase = blockDeviceBase;
        this.subnetCidr = subnetCidr;
        this.masterInstance = masterInstance;
        this.slaveInstances = new ArrayList<>(slaveInstances);
    }

    public void setMasterMounts(DeviceMapper masterDeviceMapper) {
        List<Configuration.MountPoint> masterMountMap = masterDeviceMapper.getSnapshotIdToMountPoint();
        if (masterMountMap != null && masterMountMap.size() > 0) {
            masterMounts = new ArrayList<>();
            for (Configuration.MountPoint mountPoint : masterMountMap) {
                Configuration.MountPoint localMountPoint = new Configuration.MountPoint();
                localMountPoint.setSource(masterDeviceMapper.getRealDeviceNameForMountPoint(mountPoint.getTarget()));
                localMountPoint.setTarget(mountPoint.getTarget());
                masterMounts.add(localMountPoint);
            }
        }
    }

    public void writeSiteFile(OutputStream stream) {
        Map<String, Object> master = new LinkedHashMap<>();
        master.put("hosts", "master");
        master.put("become", "yes");
        master.put("vars_files", Arrays.asList("vars/common.yml"));
        List<String> roles = new ArrayList<>();
        roles.add("common");
        roles.add("master");
        for (int i = 0; i < config.getMasterAnsibleRoles().size(); i++) {
            roles.add(getCustomRoleName("master", i));
        }
        master.put("roles", roles);
        Map<String, Object> slaves = new LinkedHashMap<>();
        slaves.put("hosts", "slaves");
        slaves.put("become", "yes");
        slaves.put("vars_files", Arrays.asList("vars/common.yml", "vars/{{ ansible_default_ipv4.address }}.yml"));
        roles = new ArrayList<>();
        roles.add("common");
        roles.add("slave");
        for (int i = 0; i < config.getSlaveAnsibleRoles().size(); i++) {
            roles.add(getCustomRoleName("slaves", i));
        }
        slaves.put("roles", roles);
        writeToOutputStream(stream, Arrays.asList(master, slaves));
    }

    /**
     * Generates a unique role name with the provided hosts type and index.
     *
     * @param hosts master or slaves.
     * @param i     Index of the custom role folder.
     */
    public String getCustomRoleName(String hosts, int i) {
        return hosts + "-user-role" + i;
    }

    /**
     * Write specified instance to stream (in YAML format)
     */
    public void writeInstanceFile(Instance instance, OutputStream stream) {
        writeToOutputStream(stream, getInstanceMap(instance, true));
    }

    public void writeCommonFile(OutputStream stream) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("mode", config.getMode());
        map.put("default_user", config.getUser());
        map.put("ssh_user", config.getSshUser());
        map.put("munge_key",config.getMungeKey());
        map.put("master", getMasterMap());
        map.put("slaves", getSlavesMap());
        map.put("CIDR", subnetCidr);
        if (config.isNfs()) {
            map.put("nfs_mounts", getNfsSharesMap());
            map.put("ext_nfs_mounts", getExtNfsSharesMap());
        }
        map.put("local_fs", config.getLocalFS().name().toLowerCase(Locale.US));
        addBooleanOption(map, "enable_nfs", config.isNfs());
        addBooleanOption(map, "enable_gridengine", config.isOge());
        addBooleanOption(map, "enable_slurm",config.isSlurm());
        addBooleanOption(map, "use_master_as_compute", config.isUseMasterAsCompute());
        addBooleanOption(map, "enable_cloud9", config.isCloud9());
        /* not provided, 02/19
        addBooleanOption(map, "enable_mesos", config.isMesos());
        addBooleanOption(map, "enable_cassandra", config.isCassandra());
        addBooleanOption(map, "enable_hdfs", config.isHdfs());
        addBooleanOption(map, "enable_spark", config.isSpark());
         */
        addBooleanOption(map,"enable_ganglia",config.isGanglia());
        addBooleanOption(map, "enable_zabbix", config.isZabbix());
        map.put("zabbix",getZabbixConfigurationMap());
        writeToOutputStream(stream, map);
    }

    private void writeToOutputStream(OutputStream stream, Object map) {
        try {
            try (OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
                if (map instanceof Map) {
                    writer.write(new Yaml().dumpAsMap(map));
                } else {
                    writer.write(new Yaml().dumpAs(map, Tag.SEQ, DumperOptions.FlowStyle.BLOCK));
                }
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
        if (masterMounts != null && masterMounts.size() > 0) {
            List<Map<String, String>> masterMountsMap = new ArrayList<>();
            for (Configuration.MountPoint masterMount : masterMounts) {
                Map<String, String> mountMap = new LinkedHashMap<>();
                mountMap.put("src", masterMount.getSource());
                mountMap.put("dst", masterMount.getTarget());
                masterMountsMap.add(mountMap);
            }
            masterMap.put("disks", masterMountsMap);
        }
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

    private List<Map<String, String>> getExtNfsSharesMap() {
        List<Map<String, String>> nfsSharesMap = new ArrayList<>();
        for (Configuration.MountPoint extNfsShare : config.getExtNfsShares()) {
            Map<String, String> shareMap = new LinkedHashMap<>();
            shareMap.put("src", extNfsShare.getSource());
            shareMap.put("dst", extNfsShare.getTarget());
            nfsSharesMap.add(shareMap);
        }
        return nfsSharesMap;
    }

    private Map<String, Object> getInstanceMap(Instance instance, boolean full) {
        Map<String, Object> masterMap = new LinkedHashMap<>();
        masterMap.put("ip", instance.getPrivateIp());
        masterMap.put("cores", instance.getConfiguration().getProviderType().getCpuCores());
        if (full) {
            masterMap.put("hostname", instance.getHostname());
            masterMap.put("ephemerals", getEphemeralDevices(instance.getConfiguration().getProviderType().getEphemerals()));
        }
        return masterMap;
    }

    private List<Map<String, Object>> getSlavesMap() {
        List<Map<String, Object>> l = new ArrayList<>();
        for (Instance slave : slaveInstances) {
            l.add(getInstanceMap(slave, true));
        }
        return l;
    }

    private Map<String,Object> getZabbixConfigurationMap() {
        Map<String, Object> zabbixConf = new LinkedHashMap<>();
        Map<String, Object> zabbixDockerConf = new LinkedHashMap<>();
        //Docker config
        zabbixDockerConf.put("subnet",config.getZabbix_conf().getDocker().getSubnet());
        zabbixDockerConf.put("zdb",config.getZabbix_conf().getDocker().getZdb());
        zabbixDockerConf.put("zserver",config.getZabbix_conf().getDocker().getZserver());
        zabbixDockerConf.put("zweb",config.getZabbix_conf().getDocker().getZweb());
        zabbixConf.put("docker",zabbixDockerConf);
        zabbixConf.put("db_root_password", config.getZabbix_conf().getDb_root_password());
        zabbixConf.put("db_zabbix_password",config.getZabbix_conf().getDb_zabbix_password());
        zabbixConf.put("login_user",config.getZabbix_conf().getLogin_user());
        zabbixConf.put("login_password",config.getZabbix_conf().getLogin_password());
        zabbixConf.put("metadata",config.getZabbix_conf().getMetadata());
        return zabbixConf;
    }
}
