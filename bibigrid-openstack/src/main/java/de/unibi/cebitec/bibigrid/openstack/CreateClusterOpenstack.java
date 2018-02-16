package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.intents.CreateCluster;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.Instance;
import de.unibi.cebitec.bibigrid.core.model.InstanceType;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.util.*;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.Addresses;
import org.openstack4j.model.compute.BDMDestType;
import org.openstack4j.model.compute.BDMSourceType;
import org.openstack4j.model.compute.BlockDeviceMappingCreate;
import org.openstack4j.model.compute.Fault;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.compute.VolumeAttachment;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.Volume.Status;
import org.openstack4j.model.storage.block.VolumeSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Openstack specific implementation of CreateCluster interface.
 *
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public class CreateClusterOpenstack extends CreateCluster {
    private static final Logger LOG = LoggerFactory.getLogger(CreateClusterOpenstack.class);
    private static final int CONFIG_DRIVE = 1;

    private final OSClient os;
    private CreateClusterEnvironmentOpenstack environment;
    private final Map<String, String> metadata = new HashMap<>();
    private DeviceMapper masterDeviceMapper, slaveDeviceMapper;
    private Set<BlockDeviceMappingCreate> masterMappings, slaveMappings;

    CreateClusterOpenstack(final ConfigurationOpenstack config, final ProviderModule providerModule) {
        super(config, providerModule);
        os = OpenStackUtils.buildOSClient(config);
    }

    @Override
    public CreateClusterEnvironmentOpenstack createClusterEnvironment() throws ConfigurationException {
        metadata.put(Instance.TAG_USER, config.getUser());
        LOG.info("Openstack connection established ...");
        return environment = new CreateClusterEnvironmentOpenstack(this);
    }

    @Override
    public CreateClusterOpenstack configureClusterMasterInstance() {
        // DeviceMapper init.
        List<Configuration.MountPoint> masterVolumeToMountPointMap = new ArrayList<>();
        for (Configuration.MountPoint mountPoint : config.getMasterMounts()) {
            // check if master mount is a volume
            Volume v = getVolumeByNameOrId(mountPoint.getSource());
            // could also be a snapshot
            if (v == null) {
                VolumeSnapshot vss = getSnapshotByNameOrId(mountPoint.getSource());
                if (vss != null) {
                    v = createVolumeFromSnapshot(vss, mountPoint.getSource() + "_" + clusterId);
                    LOG.info(V, "Create volume ({}) from snapshot ({}).", v.getName(), mountPoint.getSource());
                }
            }
            if (v != null) { // Volume exists or created from snapshot
                LOG.info(V, "Add volume ({}) to MasterVolumeMountMap", v.getName());
                Configuration.MountPoint idTargetMount = new Configuration.MountPoint();
                idTargetMount.setSource(v.getId());
                idTargetMount.setTarget(mountPoint.getTarget());
                masterVolumeToMountPointMap.add(idTargetMount);

            } else {
                LOG.warn("Volume/Snapshot with name/id {} not found!", mountPoint.getSource());
            }
        }

        InstanceType masterSpec = config.getMasterInstance().getProviderType();
        masterDeviceMapper = new DeviceMapper(providerModule, masterVolumeToMountPointMap,
                CONFIG_DRIVE + masterSpec.getEphemerals() + masterSpec.getSwap());

        // BlockDeviceMapping.
        masterMappings = new HashSet<>();
        String[] ephemerals = {"b", "c", "d", "e"};
        for (int i = 0; i < masterSpec.getEphemerals(); ++i) {
            BlockDeviceMappingCreate blockDeviceMappingCreate = Builders.blockDeviceMapping()
                    .deviceName("sd" + ephemerals[i])
                    .deleteOnTermination(true)
                    .sourceType(BDMSourceType.BLANK)
                    .destinationType(BDMDestType.LOCAL)
                    .bootIndex(-1)
                    .build();
            masterMappings.add(blockDeviceMappingCreate);
        }
        LOG.info("Master configured");
        return this;
    }

    private Flavor getFlavorByName(String name) {
        for (Flavor flavor : os.compute().flavors().list()) {
            if (flavor.getName().equals(name)) {
                return flavor;
            }
        }
        return null;
    }

    @Override
    public CreateClusterOpenstack configureClusterSlaveInstance() {
        // DeviceMapper Slave. @ToDo
        List<Configuration.MountPoint> snapShotToSlaveMounts = config.getSlaveMounts();

        // TODO: why was this always master instance spec?
        InstanceType masterSpec = config.getMasterInstance().getProviderType();
        slaveDeviceMapper = new DeviceMapper(providerModule, snapShotToSlaveMounts,
                CONFIG_DRIVE + masterSpec.getEphemerals() + masterSpec.getSwap());

        // BlockDeviceMapping. @ToDo
        slaveMappings = new HashSet<>();
        /* TODO
        String[] ephemerals = {"b", "c", "d", "e"};
        for (int i = 0; i < config.getSlaveInstanceType().getSpec().getEphemerals(); ++i) {
            BlockDeviceMappingCreate blockDeviceMappingCreate = Builders.blockDeviceMapping()
                    .deviceName("sd" + ephemerals[i])
                    .deleteOnTermination(true)
                    .sourceType(BDMSourceType.BLANK)
                    .destinationType(BDMDestType.LOCAL)
                    .bootIndex(-1)
                    .build();
            slaveMappings.add(blockDeviceMappingCreate);
        }
        */
        LOG.info("Slave(s) configured");
        return this;
    }

    @Override
    protected InstanceOpenstack launchClusterMasterInstance(String masterNameTag) {
        ServerCreate sc = Builders.server()
                .name(masterNameTag)
                .flavor(getFlavorByName(config.getMasterInstance().getProviderType().getValue()).getId())
                .image(config.getMasterInstance().getImage())
                .keypairName(config.getKeypair())
                .addSecurityGroup(environment.getSecGroupExtension().getId())
                .availabilityZone(config.getAvailabilityZone())
                .userData(ShellScriptCreator.getUserData(config, environment.getKeypair(), true, true))
                .addMetadata(metadata)
                .configDrive(CONFIG_DRIVE != 0)
                .networks(Arrays.asList(environment.getNetwork().getId()))
                .build();
        Server server = os.compute().servers().boot(sc); // boot and return immediately

        // check if anything goes wrong,
        Fault fault = server.getFault();
        if (fault != null) {
            // some more debug information in verbose mode
            LOG.info(V, "{},{}", fault.getCode(), fault.getMessage());
            // print error message and abort launch
            if (fault.getCode() == 500) {
                LOG.error("Launch master :: {}", fault.getMessage());
                return null;
            }
        }

        // Network configuration
        LOG.info("Master (ID: {}) started", server.getId());
        InstanceOpenstack master = new InstanceOpenstack(config.getMasterInstance(), server);
        master.setPrivateIp(waitForAddress(server.getId(), environment.getNetwork().getName()).getAddr());

        master.updateNeutronHostname();

        // get and assign floating ip to master
        ActionResponse ar = null;
        boolean assigned = false;

        List<String> blacklist = new ArrayList<>();
        while (ar == null || !assigned) {
            // get next free floatingIP
            NetFloatingIP floatingIp = getFloatingIP(blacklist);
            // if null  there is no free floating ip available
            if (floatingIp == null) {
                LOG.error("No unused FloatingIP available! Abort!");
                return null;
            }
            // put ip on blacklist
            blacklist.add(floatingIp.getFloatingIpAddress());
            // try to assign floating ip to server
            ar = os.compute().floatingIps().addFloatingIP(server, floatingIp.getFloatingIpAddress());
            // in case of success try  update master object
            if (ar.isSuccess()) {
                sleep(1, false);
                Server tmp = os.compute().servers().get(server.getId());
                if (tmp != null) {
                    assigned = checkForFloatingIp(tmp, floatingIp.getFloatingIpAddress());
                    if (assigned) {
                        master.setPublicIp(floatingIp.getFloatingIpAddress());
                        LOG.info("FloatingIP {} assigned to Master(ID: {}) ", master.getPublicIp(), master.getId());
                    } else {
                        LOG.warn("FloatingIP {} assignment failed ! Try another one ...", floatingIp.getFloatingIpAddress());
                    }
                }
            } else {
                LOG.warn("FloatingIP {} assignment failed with fault : {}! Try another one ...", floatingIp.getFloatingIpAddress(), ar.getFault());
            }
        }
        LOG.info("Master (ID: {}) network configuration finished", master.getId());

        // wait for master available
        do {
            checkForServerAndUpdateInstance(server.getId(), master);
            if (!master.isActive()) { // if not yet active wait ....
                sleep(2);
            } else if (master.hasError()) {
                // if the master fails we can do nothing and must shutdown everything
                return null;
            }
        } while (!master.isActive());

        // attach Volumes
        if (!masterDeviceMapper.getSnapshotIdToMountPoint().isEmpty()) {
            for (Configuration.MountPoint mountPoint : masterDeviceMapper.getSnapshotIdToMountPoint()) {
                //check if volume is available
                Volume v = os.blockStorage().volumes().get(mountPoint.getSource());
                boolean waiting = true;
                while (waiting) {
                    switch (v.getStatus()) {
                        case AVAILABLE:
                            waiting = false;
                            break;
                        case CREATING: {
                            sleep(5);
                            LOG.info(V, "Wait for Volume ({}) available", v.getId());
                            v = os.blockStorage().volumes().get(mountPoint.getSource());
                            break;
                        }
                        default:
                            waiting = false;
                            LOG.error("Volume not available (Status : {})", v.getStatus());
                    }
                }

                if (v.getStatus().equals(Status.AVAILABLE)) {
                    // @ToDo: Test if a volume can be attached to a non active server instance ...
                    VolumeAttachment va = os.compute().servers().attachVolume(server.getId(), mountPoint.getSource(),
                            masterDeviceMapper.getDeviceNameForSnapshotId(mountPoint.getSource()));
                    if (va == null) {
                        LOG.error("Attaching volume {} to master failed ...", mountPoint.getSource());
                    } else {
                        LOG.info(V, "Volume {} attached to Master.", va.getId());
                    }
                }
            }
            LOG.info("{} Volume(s) attached to Master.", masterDeviceMapper.getSnapshotIdToMountPoint().size());
        }
        return master;
    }

    @Override
    protected List<Instance> launchClusterSlaveInstances(
            int batchIndex, Configuration.SlaveInstanceConfiguration instanceConfiguration, String slaveNameTag) {
        Map<String, InstanceOpenstack> slaves = new HashMap<>();
        for (int i = 0; i < instanceConfiguration.getCount(); i++) {
            ServerCreate sc = Builders.server()
                    .name(buildSlaveInstanceName(batchIndex, i))
                    .flavor(getFlavorByName(instanceConfiguration.getProviderType().getValue()).getId())
                    .image(instanceConfiguration.getImage())
                    .keypairName(config.getKeypair())
                    .addSecurityGroup(environment.getSecGroupExtension().getId())
                    .availabilityZone(config.getAvailabilityZone())
                    .userData(ShellScriptCreator.getUserData(config, environment.getKeypair(), true, false))
                    .configDrive(CONFIG_DRIVE != 0)
                    .networks(Arrays.asList(environment.getNetwork().getId()))
                    .build();
            Server tmp = os.compute().servers().boot(sc);
            InstanceOpenstack tmp_instance = new InstanceOpenstack(instanceConfiguration, tmp);
            slaves.put(tmp.getId(), tmp_instance);
            LOG.info(V, "Instance request for {}  ", sc.getName());
        }

        LOG.info("Waiting for slave instances ready ...");
        int active = 0;
        List<String> ignoreList = new ArrayList<>();
        while (slaves.size() > active + ignoreList.size()) {
            // wait for some seconds to not overload REST API
            sleep(2);
            // get fresh server object for given server id
            for (InstanceOpenstack slave : slaves.values()) {
                //ignore if instance is already active ...
                if (!(slave.isActive() || slave.hasError())) {
                    // check server status
                    checkForServerAndUpdateInstance(slave.getId(), slave);
                    if (slave.isActive()) {
                        active++;
                        LOG.info("[{}/{}] Instance {} is active !", active, slaves.size(), slave.getHostname());
                    } else if (slave.hasError()) {
                        LOG.warn("Ignore slave instance '{}' ", slave.getHostname());
                        ignoreList.add(slave.getHostname());
                    }
                }
            }
        }

        // remove ignored instances from slave map
        for (String name : ignoreList) {
            slaves.remove(name);
        }

        LOG.info(V, "Wait for slave network configuration finished ...");
        // wait for slave network finished ... update server instance list
        for (InstanceOpenstack slave : slaves.values()) {
            slave.setPrivateIp(waitForAddress(slave.getId(), environment.getNetwork().getName()).getAddr());
            slave.updateNeutronHostname();
        }
        // TODO
        // Mount a volume to slave instance
        // - create (count of slave instances) snapshots
        // - mount each snapshot as volume to an instance
        return new ArrayList<>(slaves.values());
    }

    @Override
    protected String getSubnetCidr() {
        return environment.getSubnet().getCidr();
    }

    private NetFloatingIP getFloatingIP(List<String> blacklist) {
        // get list of all available floating IP's, and search for free ones ...
        List<? extends NetFloatingIP> l = os.networking().floatingip().list();

        for (NetFloatingIP fip : l) {
            if (fip.getPortId() == null
                    // check if floating ip fits to router network id
                    && fip.getFloatingNetworkId().equals(environment.getRouter().getExternalGatewayInfo().getNetworkId())
                    // check if tenant id fits routers tenant id
                    && fip.getTenantId().equals(environment.getRouter().getTenantId())
                    && !blacklist.contains(fip.getFloatingIpAddress())) {
                //found an unused floating ip and return it
                return fip;
            }
        }
        // try to allocate a new floating from network pool
        try {
            return os.networking().floatingip().create(Builders.netFloatingIP()
                    .floatingNetworkId(environment.getRouter().getExternalGatewayInfo().getNetworkId())
                    .build());

        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return null;
    }

    OSClient getClient() {
        return os;
    }

    private Address waitForAddress(String serverId, String networkName) {
        // ################################################################
        // following block is a ugly hack to refresh the server object
        // ####################################################### -> Start
        Addresses addresses;
        List<? extends Address> addressList;
        Map<String, List<? extends Address>> map;
        Server server;
        do {
            try {
                // wait a second
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                // do nothing
            }
            // refresh server object - ugly
            server = os.compute().servers().get(serverId);
            addresses = server.getAddresses();
            map = addresses.getAddresses();
            addressList = map.get(networkName);
            LOG.info(V, "addressList {}", addressList);
        } while (addressList == null || addressList.isEmpty());
        // <- End ##########################################################
        return addressList.get(0);
    }

    /**
     * Check if a given floating ip is set for a given server instance.
     *
     * @param server     - Server instance
     * @param floatingIp - floatingIp to be checked
     */
    private boolean checkForFloatingIp(Server server, String floatingIp) {
        Map<String, List<? extends Address>> addressMap = server.getAddresses().getAddresses();
        // map should contain only one network
        if (addressMap.size() == 1) {
            for (Address address : addressMap.values().iterator().next()) {
                if (address.getType().equals("floating") && address.getAddr().equals(floatingIp)) {
                    return true;
                }
            }
        } else {
            LOG.warn("No or more than one network associated with instance {}", server.getId());
        }
        return false;
    }

    /**
     * Return a Snapshot by its name/id or null if no snapshot is found
     *
     * @param nameOrId - name or id of snapshot
     */
    private VolumeSnapshot getSnapshotByNameOrId(String nameOrId) {
        List<? extends VolumeSnapshot> allSnapshots = os.blockStorage().snapshots().list();
        for (VolumeSnapshot vss : allSnapshots) {
            if (vss.getName() != null && vss.getName().equals(nameOrId) || vss.getId().equals(nameOrId)) {
                return vss;
            }
        }
        return null;
    }

    /**
     * Return a Volume by its name/id or null if no volume is found
     *
     * @param nameOrId - name or id of volume
     */
    private Volume getVolumeByNameOrId(String nameOrId) {
        List<? extends Volume> allVolumes = os.blockStorage().volumes().list();
        for (Volume v : allVolumes) {
            if (v.getName() != null && v.getName().equals(nameOrId) || v.getId().equals(nameOrId)) {
                return v;
            }
        }
        return null;
    }

    /**
     * Return a new volume from a Snapshot
     *
     * @param name of newly created volume
     */
    private Volume createVolumeFromSnapshot(VolumeSnapshot vss, String name) {
        return os.blockStorage().volumes().create(Builders.volume()
                .name(name)
                .snapshot(vss.getId())
                .description("created from SnapShot " + vss.getId() + " by BiBiGrid")
                .build());
    }

    /**
     * Check for Server status and update instance with id, hostname and active
     * state. Returns false in the case of an error, true otherwise.
     */
    private void checkForServerAndUpdateInstance(String id, InstanceOpenstack instance) {
        Server si = os.compute().servers().get(id);
        instance.setServer(si);
        // check for status available
        if (si.getStatus() != null) {
            switch (si.getStatus()) {
                case ACTIVE:
                    instance.setActive(true);
                    break;
                case ERROR:
                    // check and print error anything goes wrong,
                    instance.setError(true);
                    Fault fault = si.getFault();
                    if (fault == null) {
                        LOG.error("Launch {} failed without message!", si.getName());
                    } else {
                        LOG.error("Launch {} failed with Code {} :: {}", si.getName(), fault.getCode(), fault.getMessage());
                    }
                    break;
                default:
                    // other non critical state ... just wait
                    break;
            }
        } else {
            LOG.warn(V, "Status  of instance {} not available (== null)", si.getId());
        }
    }
}
