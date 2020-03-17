package de.unibi.cebitec.bibigrid.core.model;

import de.unibi.cebitec.bibigrid.core.model.Configuration.AnsibleRoles;
import de.unibi.cebitec.bibigrid.core.util.AnsibleResources;
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
 * TODO Could / Should probably be static, AnsibleResources as well
 */
public final class AnsibleConfig {
    private static final int BLOCK_DEVICE_START = 98;

    private final Configuration config;
    private final String blockDeviceBase;
    private final String subnetCidr;
    private final Instance masterInstance;
    private final List<Instance> workerInstances;
    private List<Configuration.MountPoint> masterMounts;

    public AnsibleConfig(Configuration config, String blockDeviceBase, String subnetCidr, Instance masterInstance,
                         List<Instance> workerInstances) {
        this.config = config;
        this.blockDeviceBase = blockDeviceBase;
        this.subnetCidr = subnetCidr;
        this.masterInstance = masterInstance;
        this.workerInstances = new ArrayList<>(workerInstances);
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

    /**
     * Generates site.yml automatically including custom ansible roles.
     *
     * @param stream write file to remote
     * @param customMasterRoles master ansible roles names and variable file names
     * @param customWorkerRoles worker ansible roles names and variable file names
     */
    public void writeSiteFile(OutputStream stream,
                              Map<String, String> customMasterRoles,
                              Map<String, String> customWorkerRoles) {
//        String COMMON_FILE = AnsibleResources.COMMON_YML;
        String LOGIN_FILE = AnsibleResources.LOGIN_YML;
        String INSTANCES_FILE = AnsibleResources.INSTANCES_YML;
        String CONFIG_FILE = AnsibleResources.CONFIG_YML;

        String DEFAULT_IP_FILE = AnsibleResources.VARS_PATH + "{{ ansible_default_ipv4.address }}.yml";
        // master configuration
        Map<String, Object> master = new LinkedHashMap<>();
        master.put("hosts", "master");
        master.put("become", "yes");
        List<String> vars_files = new ArrayList<>();
//        vars_files.add(COMMON_FILE);
        vars_files.add(LOGIN_FILE);
        vars_files.add(INSTANCES_FILE);
        vars_files.add(CONFIG_FILE);
        for (String vars_file : customMasterRoles.values()) {
            if (!vars_file.equals("")) {
                vars_files.add(vars_file);
            }
        }
        master.put("vars_files", vars_files);
        List<String> roles = new ArrayList<>();
        roles.add("common");
        roles.add("master");
        roles.addAll(customMasterRoles.keySet());
        master.put("roles", roles);
        // worker configuration
        Map<String, Object> workers = new LinkedHashMap<>();
        workers.put("hosts", "workers");
        workers.put("become", "yes");
        vars_files = new ArrayList<>();
//        vars_files.add(COMMON_FILE);
        vars_files.add(LOGIN_FILE);
        vars_files.add(INSTANCES_FILE);
        vars_files.add(CONFIG_FILE);
        vars_files.add(DEFAULT_IP_FILE);
        for (String vars_file : customWorkerRoles.values()) {
            if (!vars_file.equals("")) {
                vars_files.add(vars_file);
            }
        }
        workers.put("vars_files", vars_files);
        roles = new ArrayList<>();
        roles.add("common");
        roles.add("worker");
        roles.addAll(customWorkerRoles.keySet());
        workers.put("roles", roles);
        writeToOutputStream(stream, Arrays.asList(master, workers));
    }

    /**
     * Writes file for each ansible role to integrate environment variables.
     *
     * @param stream write file to remote
     */
    public void writeAnsibleVarsFile(OutputStream stream, Map<String, Object> vars) {
        if (vars != null && !vars.isEmpty()) {
            writeToOutputStream(stream, vars);
        }
    }

    /**
     * Generates roles/requirements.yml automatically including roles to install via ansible-galaxy.
     *
     * @param stream write file to remote
     */
    public void writeRequirementsFile(OutputStream stream) {
        List<Map<String, Object>> galaxy_roles = new ArrayList<>();
        List<Map<String, Object>> git_roles = new ArrayList<>();
        List<Map<String, Object>> url_roles = new ArrayList<>();

        for (Configuration.AnsibleGalaxyRoles galaxyRole : config.getAnsibleGalaxyRoles()) {
            Map<String, Object> role = new LinkedHashMap<>();
            role.put("name", galaxyRole.getName());
            if (galaxyRole.getGalaxy() != null) {
                role.put("src", galaxyRole.getGalaxy());
                galaxy_roles.add(role);
            } else if (galaxyRole.getGit() != null) {
                role.put("src", galaxyRole.getGit());
                git_roles.add(role);
            } else if (galaxyRole.getUrl() != null) {
                role.put("src", galaxyRole.getUrl());
                url_roles.add(role);
            }
        }
        List<Map<String, Object>> roles = new ArrayList<>();
        roles.addAll(galaxy_roles);
        roles.addAll(git_roles);
        roles.addAll(url_roles);
        writeToOutputStream(stream, roles);
    }

    /**
     * Write specified instance to stream (in YAML format)
     */
    public void writeSpecificInstanceFile(Instance instance, OutputStream stream) {
        writeToOutputStream(stream, getInstanceMap(instance, true));
    }

    /**
     * Generates cluster_login.yml, cluster_instances.yml and cluster_configuration.yml.
     * Writes files into ~/playbook/vars/ folder
     * @param login_stream sftp channel to write login file to remote
     * @param instances_stream sftp channel to write instances file to remote
     * @param config_stream sftp channel to write configuration file to remote
     */
    public static void writeCommonFiles(OutputStream login_stream,
                                OutputStream instances_stream,
                                OutputStream config_stream) {
        writeLoginFile(login_stream);
        writeInstancesFile(instances_stream, workerInstances);
        writeConfigFile(config_stream);
    }

    /**
     * Writes cluster_login.yml with essential user data.
     * @param stream write into cluster_login.yml
     */
    private void writeLoginFile(OutputStream stream) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("mode", config.getMode());
        map.put("default_user", config.getUser());
        map.put("ssh_user", config.getSshUser());
        map.put("munge_key",config.getMungeKey());
        writeToOutputStream(stream, map);
    }

    /**
     * Writes cluster_instances.yml with instances information.
     * @param stream write into cluster_instances.yml
     */
    public static void writeInstancesFile(OutputStream stream, Instance master, List<Instance> workers) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("master", getMasterMap(master));
        map.put("workers", getWorkerMap(workers));
        writeToOutputStream(stream, map);
    }

    /**
     * Writes cluster_config.yml with cluster configuration.
     * @param stream write into cluster_configuration.yml
     */
    private void writeConfigFile(OutputStream stream) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("CIDR", subnetCidr);
        map.put("local_fs", config.getLocalFS().name().toLowerCase(Locale.US));
        addBooleanOption(map, "enable_nfs", config.isNfs());
        addBooleanOption(map, "local_dns_lookup", config.isLocalDNSLookup());
        addBooleanOption(map, "enable_gridengine", config.isOge());
        addBooleanOption(map, "enable_slurm",config.isSlurm());
        addBooleanOption(map, "use_master_as_compute", config.isUseMasterAsCompute());
        addBooleanOption(map, "enable_ganglia",config.isGanglia());
        addBooleanOption(map, "enable_zabbix", config.isZabbix());
        addBooleanOption(map, "enable_ide", config.isIDE());
        if (config.isNfs()) {
            map.put("nfs_mounts", getNfsSharesMap());
            map.put("ext_nfs_mounts", getExtNfsSharesMap());
        }
        if (config.isIDE()) {
            map.put("ideConf", getIdeConf());
        }
        if (config.isZabbix()) {
            map.put("zabbix", getZabbixConf());
        }
        if (config.isOge()) {
            map.put("oge", getOgeConf());
        }
        if (config.hasCustomAnsibleRoles()) {
            map.put("ansible_roles", getAnsibleRoles());
        }
        if (config.hasCustomAnsibleGalaxyRoles()) {
            map.put("ansible_galaxy_roles", getAnsibleGalaxyRoles());
        }
        writeToOutputStream(stream, map);
    }

    /**
     * Uses stream to write map on remote.
     *
     * @param stream OutputStream to remote instance
     * @param map (yml) file content
     */
    private static void writeToOutputStream(OutputStream stream, Object map) {
        try (OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
                if (map instanceof Map) {
                    writer.write(new Yaml().dumpAsMap(map));
                } else {
                    writer.write(new Yaml().dumpAs(map, Tag.SEQ, DumperOptions.FlowStyle.BLOCK));
                }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addBooleanOption(Map<String, Object> map, String option, boolean value) {
        map.put(option, value ? "yes" : "no");
    }

    private static Map<String, Object> getMasterMap(Instance masterInstance) {
        Map<String, Object> masterMap = new LinkedHashMap<>();
        masterMap.put("ip", masterInstance.getPrivateIp());
        masterMap.put("hostname", masterInstance.getHostname());
        InstanceType providerType = masterInstance.getConfiguration().getProviderType();
        masterMap.put("cores", providerType.getCpuCores());
        masterMap.put("memory", providerType.getMaxRam());
        masterMap.put("ephemerals", getEphemeralDevices(providerType.getEphemerals()));
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

    /**
     * Provides instance map for all worker instances.
     * @param workers list of workers
     * @return list of maps for each worker instance
     */
    private static List<Map<String, Object>> getWorkerMap(List<Instance> workers) {
        List<Map<String, Object>> workerList = new ArrayList<>();
        for (Instance worker : workers) {
            workerList.add(getInstanceMap(worker, true));
        }
        return workerList;
    }

    /**
     * Creates map of instance configuration.
     *
     * @param instance current remote instance
     * @param full degree of detail TODO can be removed
     * @return map of instance configuration
     */
    private static Map<String, Object> getInstanceMap(Instance instance, boolean full) {
        Map<String, Object> instanceMap = new LinkedHashMap<>();
        instanceMap.put("ip", instance.getPrivateIp());
        instanceMap.put("cores", instance.getConfiguration().getProviderType().getCpuCores());
        instanceMap.put("memory", instance.getConfiguration().getProviderType().getMaxRam());
        if (full) {
            instanceMap.put("hostname", instance.getHostname());
            instanceMap.put("ephemerals", getEphemeralDevices(instance.getConfiguration().getProviderType().getEphemerals()));
        }
        return instanceMap;
    }

    private Map<String, Object> getZabbixConf() {
        Configuration.ZabbixConf zc = config.getZabbixConf();
        Map<String, Object> zabbixConf = new LinkedHashMap<>();
        zabbixConf.put("db",zc.getDb());
        zabbixConf.put("db_user",zc.getDb_user());
        zabbixConf.put("db_password",zc.getDb_password());
        zabbixConf.put("timezone",zc.getTimezone());
        zabbixConf.put("server_name",zc.getServer_name());
        zabbixConf.put("admin_password",zc.getAdmin_password());
        return zabbixConf;
    }

    private Map<String, String> getOgeConf() {
        Map<String, String> ogeConf = new HashMap<>();
        Properties oc = config.getOgeConf();
        for (final String name : oc.stringPropertyNames()) {
            ogeConf.put(name, oc.getProperty(name));
        }
        return ogeConf;
    }

    private Map<String, Object> getIdeConf() {
        Configuration.IdeConf ic = config.getIdeConf();
        Map<String, Object> ideConf = new LinkedHashMap<>();
        ideConf.put("ide", ic.isIde());
        ideConf.put("workspace",ic.getWorkspace());
        ideConf.put("port_start", ic.getPort_start());
        ideConf.put("port_end", ic.getPort_end());
        return ideConf;
    }

    /**
     * Puts parameter values of every given role into Map list.
     * @return list of roles with single parameters
     */
    private List<Map<String, Object>> getAnsibleRoles() {
        List<AnsibleRoles> roles = config.getAnsibleRoles();
        List<Map<String, Object>> ansibleRoles = new ArrayList<>();
        for (AnsibleRoles role : roles) {
            Map<String, Object> roleConf = new LinkedHashMap<>();
            if (role.getName() != null && !role.getName().equals("")) roleConf.put("name", role.getName());
            roleConf.put("file", role.getFile());
            roleConf.put("hosts", role.getHosts());
            if (role.getVars() != null && !role.getVars().isEmpty()) roleConf.put("vars", role.getVars());
            if (role.getVarsFile() != null) roleConf.put("vars_file", role.getVarsFile());
            ansibleRoles.add(roleConf);
        }
        return ansibleRoles;
    }

    /**
     * Puts parameter values of every given ansible-galaxy role into Map list.
     * @return list of roles with single parameters
     */
    private List<Map<String, Object>> getAnsibleGalaxyRoles() {
        List<Configuration.AnsibleGalaxyRoles> roles = config.getAnsibleGalaxyRoles();
        List<Map<String, Object>> ansibleGalaxyRoles = new ArrayList<>();
        for (Configuration.AnsibleGalaxyRoles role : roles) {
            Map<String, Object> roleConf = new LinkedHashMap<>();
            roleConf.put("name", role.getName());
            roleConf.put("hosts", role.getHosts());
            if (role.getGalaxy() != null) roleConf.put("galaxy", role.getGalaxy());
            if (role.getGit() != null) roleConf.put("git", role.getGit());
            if (role.getUrl() != null) roleConf.put("url", role.getUrl());
            if (role.getVars() != null && !role.getVars().isEmpty()) roleConf.put("vars", role.getVars());
            if (role.getVarsFile() != null) roleConf.put("vars_file", role.getVarsFile());
            ansibleGalaxyRoles.add(roleConf);
        }
        return ansibleGalaxyRoles;
    }

    private static List<String> getEphemeralDevices(int count) {
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
}
