package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.intents.CreateCluster;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.Instance;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.NotYetSupportedException;
import de.unibi.cebitec.bibigrid.core.util.*;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ServerGroupService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.*;
import org.openstack4j.model.compute.builder.ServerCreateBuilder;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.model.network.Router;
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

    private final OSClient os;

    CreateClusterOpenstack(final ProviderModule providerModule, Client client, final Configuration config) {
        super(providerModule, client, config);
        os = ((ClientOpenstack) client).getInternal();
    }

    @Override
    protected List<Configuration.MountPoint> resolveMountSources(List<Configuration.MountPoint> mountPoints) {
        List<Configuration.MountPoint> result = new ArrayList<>();
        for (Configuration.MountPoint mountPoint : mountPoints) {
            // check if master mount is a volume
            Volume volume = getVolumeByNameOrId(mountPoint.getSource());
            // could also be a snapshot
            if (volume == null) {
                VolumeSnapshot snapshot = getSnapshotByNameOrId(mountPoint.getSource());
                if (snapshot != null) {
                    volume = createVolumeFromSnapshot(snapshot, mountPoint.getSource() + "-" + clusterId);
                    LOG.info(V, "Create volume '{}' from snapshot '{}'.", volume.getName(), mountPoint.getSource());
                }
            }
            // Volume exists or created from snapshot
            if (volume != null) {
                LOG.info(V, "Add volume '{}' to master volume mount map.", volume.getName());
                Configuration.MountPoint idTargetMount = new Configuration.MountPoint();
                idTargetMount.setSource(volume.getId());
                idTargetMount.setTarget(mountPoint.getTarget());
                result.add(idTargetMount);
            } else {
                LOG.warn("Volume/Snapshot with name/id '{}' not found!", mountPoint.getSource());
            }
        }
        return result;
    }

    @Override
    protected InstanceOpenstack launchClusterMasterInstance(String masterNameTag) {
        InstanceOpenstack master = null;
        try {
            final Map<String, String> metadata = new HashMap<>();
            metadata.put(Instance.TAG_NAME, masterNameTag);
            metadata.put(Instance.TAG_BIBIGRID_ID, clusterId);
            metadata.put(Instance.TAG_USER, config.getUser());
            InstanceTypeOpenstack masterSpec = (InstanceTypeOpenstack) config.getMasterInstance().getProviderType();
            ServerCreateBuilder scb = null;
            scb = Builders.server()
                    .name(masterNameTag)
                    .flavor(masterSpec.getFlavor().getId())
                    // .image(config.getMasterInstance().getImage())
                    .image((client.getImageByIdOrName(config.getMasterInstance().getImage())).getId())
                    .keypairName(config.getKeypair())
                    .addSecurityGroup(((CreateClusterEnvironmentOpenstack) environment).getSecGroupExtension().getId())
                    .availabilityZone(config.getAvailabilityZone())
                    .userData(ShellScriptCreator.getUserData(config, environment.getKeypair(), true))
                    .addMetadata(metadata)
                    .configDrive(masterSpec.getConfigDrive() != 0)
                    .networks(Arrays.asList(environment.getNetwork().getId()));
            if (config.getServerGroup() != null) {
                scb.addSchedulerHint("group", config.getServerGroup());
            }

            ServerCreate sc = scb.build();
            // Boot the server async
            Server server = os.compute().servers().boot(sc);

            // check if anything goes wrong
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
            master = new InstanceOpenstack(config.getMasterInstance(), server);
            master.setPrivateIp(waitForAddress(master.getId(), environment.getNetwork().getName()).getAddr());

            master.updateNeutronHostname();
            // get and assign floating ip to master
            if (!assignPublicIpToMaster(master)) {
                return null;
            }
            LOG.info("Master (ID: {}) network configuration finished.", master.getId());

            // wait for master available
            do {
                checkForServerAndUpdateInstance(master.getId(), master);
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
                                LOG.info(V, "Waiting for volume '{}' to be available.", v.getId());
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
                            LOG.error("Attaching volume '{}' to master failed.", mountPoint.getSource());
                        } else {
                            LOG.info(V, "Volume '{}' attached to Master.", va.getId());
                        }
                    }
                }
                LOG.info("{} Volume(s) attached to Master.", masterDeviceMapper.getSnapshotIdToMountPoint().size());
            }
        } catch (NotYetSupportedException e) {
            // Should never occur
        }
        return master;
    }

    private boolean assignPublicIpToMaster(InstanceOpenstack master) {
        // If we don't use a public ip for the master instance, just return.
        if (!config.isUseMasterWithPublicIp()) {
            return true;
        }
        ActionResponse ar = null;
        boolean assigned = false;
        List<String> blacklist = new ArrayList<>();
        while (ar == null || !assigned) {
            // get next free floatingIP
            NetFloatingIP floatingIp = getFloatingIP(blacklist);
            // if null there is no free floating ip available
            if (floatingIp == null) {
                LOG.error("No unused FloatingIP available! Abort!");
                return false;
            }
            // put ip on blacklist
            blacklist.add(floatingIp.getFloatingIpAddress());
            // try to assign floating ip to server
            ar = os.compute().floatingIps().addFloatingIP(master.getInternal(), floatingIp.getFloatingIpAddress());
            // in case of success try  update master object
            if (ar.isSuccess()) {
                sleep(1, false);
                Server tmp = os.compute().servers().get(master.getId());
                if (tmp != null) {
                    assigned = checkForFloatingIp(tmp, floatingIp.getFloatingIpAddress());
                    if (assigned) {
                        master.setPublicIp(floatingIp.getFloatingIpAddress());
                        LOG.info("FloatingIP '{}' has been assigned to the master (ID: {}).", master.getPublicIp(), master.getId());
                    } else {
                        LOG.warn("FloatingIP '{}' assignment failed! Trying a different IP ...", floatingIp.getFloatingIpAddress());
                    }
                }
            } else {
                LOG.warn("FloatingIP '{}' assignment failed: '{}'. Trying a different IP ...", floatingIp.getFloatingIpAddress(), ar.getFault());
            }
        }
        return true;
    }

    @Override
    protected List<Instance> launchClusterWorkerInstances(
            int batchIndex, Configuration.WorkerInstanceConfiguration instanceConfiguration, String workerNameTag) {
        Map<String, InstanceOpenstack> workers = new HashMap<>();
        try {
            final Map<String, String> metadata = new HashMap<>();
            metadata.put(Instance.TAG_NAME, workerNameTag);
            metadata.put(Instance.TAG_BIBIGRID_ID, clusterId);
            metadata.put(Instance.TAG_USER, config.getUser());
            InstanceTypeOpenstack workerSpec = (InstanceTypeOpenstack) instanceConfiguration.getProviderType();
            for (int i = 0; i < instanceConfiguration.getCount(); i++) {
                ServerCreateBuilder scb = null;
                    scb = Builders.server()
                            .name(buildWorkerInstanceName(batchIndex, i))
                            .flavor(workerSpec.getFlavor().getId())
                            //.image(instanceConfiguration.getImage())
                            .image((client.getImageByIdOrName(instanceConfiguration.getImage())).getId())
                            .keypairName(config.getKeypair())
                            .addSecurityGroup(((CreateClusterEnvironmentOpenstack) environment).getSecGroupExtension().getId())
                            .availabilityZone(config.getAvailabilityZone())
                            .userData(ShellScriptCreator.getUserData(config, environment.getKeypair(), true))
                            .addMetadata(metadata)
                            .configDrive(instanceConfiguration.getProviderType().getConfigDrive() != 0)
                            .networks(Arrays.asList(environment.getNetwork().getId()));
                if (config.getServerGroup() != null) {
                    scb.addSchedulerHint("group", config.getServerGroup());
                }
                ServerCreate sc  = scb.build();
                Server server = os.compute().servers().boot(sc);
                InstanceOpenstack instance = new InstanceOpenstack(instanceConfiguration, server);
                workers.put(server.getId(), instance);
                LOG.info(V, "Instance request for '{}'.", sc.getName());
            }
            LOG.info("Waiting for worker instances to be ready ...");
            int active = 0;
            List<String> ignoreList = new ArrayList<>();
            while (workers.size() > active + ignoreList.size()) {
                // wait for some seconds to not overload REST API
                sleep(2);
                // get fresh server object for given server id
                for (InstanceOpenstack worker : workers.values()) {
                    //ignore if instance is already active ...
                    if (!(worker.isActive() || worker.hasError())) {
                        // check server status
                        checkForServerAndUpdateInstance(worker.getId(), worker);
                        if (worker.isActive()) {
                            active++;
                            LOG.info("[{}/{}] Instance '{}' is active!", active, workers.size(), worker.getHostname());
                        } else if (worker.hasError()) {
                            LOG.warn("Ignoring worker instance '{}'.", worker.getHostname());
                            ignoreList.add(worker.getId());
                        }
                    }
                }
            }
            // remove ignored instances from worker map
            for (String id : ignoreList) {
                workers.remove(id);
            }
            LOG.info(V, "Waiting for worker network configuration completion ...");
            // wait for worker network finished ... update server instance list
            for (InstanceOpenstack worker : workers.values()) {
                worker.setPrivateIp(waitForAddress(worker.getId(), environment.getNetwork().getName()).getAddr());
                worker.updateNeutronHostname();
            }
            // TODO
            // Mount a volume to worker instance
            // - create (count of worker instances) snapshots
            // - mount each snapshot as volume to an instance
        } catch (NotYetSupportedException e) {
            // Should never occur
        }
        return new ArrayList<>(workers.values());
    }

    private NetFloatingIP getFloatingIP(List<String> blacklist) {
        // get list of all available floating IP's, and search for free ones ...
        List<? extends NetFloatingIP> floatingIps = os.networking().floatingip().list();
        Router router = ((NetworkOpenstack) environment.getNetwork()).getRouter();
        for (NetFloatingIP floatingIp : floatingIps) {
            if (floatingIp.getPortId() == null
                    // check if floating ip fits to router network id
                    && floatingIp.getFloatingNetworkId().equals(router.getExternalGatewayInfo().getNetworkId())
                    // check if tenant id fits routers tenant id
                    && floatingIp.getTenantId().equals(router.getTenantId())
                    && !blacklist.contains(floatingIp.getFloatingIpAddress())) {
                //found an unused floating ip and return it
                return floatingIp;
            }
        }
        // try to allocate a new floating from network pool
        try {
            return os.networking().floatingip().create(Builders.netFloatingIP()
                    .floatingNetworkId(router.getExternalGatewayInfo().getNetworkId())
                    .build());
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return null;
    }

    OSClient getClient() {
        return os;
    }

    /**
     * Wait until the server has a private ip address.
     * This blocks and polls the server object every second.
     */
    private Address waitForAddress(String serverId, String networkName) {
        List<? extends Address> addressList;
        Server server;
        do {
            sleep(2, false);
            // refresh server object - ugly
            server = os.compute().servers().get(serverId);
            addressList = server.getAddresses().getAddresses().get(networkName);
            if (addressList == null) {
                LOG.info(V,"Waiting for address ...");
            }

        } while (addressList == null || addressList.isEmpty());
        LOG.info(V, "address: {}", addressList);
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
    private Volume createVolumeFromSnapshot(VolumeSnapshot snapshot, String name) {
        return os.blockStorage().volumes().create(Builders.volume()
                .name(name)
                .snapshot(snapshot.getId())
                .description("created from snapshot " + snapshot.getId() + " by BiBiGrid")
                .build());
    }

    /**
     * Check for Server status and update instance with id, hostname and active
     * state. Returns false in the case of an error, true otherwise.
     */
    private void checkForServerAndUpdateInstance(String id, InstanceOpenstack instance) {
        Server server = os.compute().servers().get(id);
        instance.setServer(server);
        // check for status available
        if (server.getStatus() != null) {
            switch (server.getStatus()) {
                case ACTIVE:
                    instance.setActive(true);
                    break;
                case ERROR:
                    // check and print error anything goes wrong,
                    instance.setError(true);
                    Fault fault = server.getFault();
                    if (fault == null) {
                        LOG.error("Launch of '{}' failed with an unknown error!", server.getName());
                    } else {
                        LOG.error("Launch of '{}' failed with error code '{}'. Message: '{}'", server.getName(), fault.getCode(), fault.getMessage());
                    }
                    break;
                default:
                    // other non critical state ... just wait
                    break;
            }
        } else {
            LOG.warn(V, "Status of instance '{}' is not available (== null).", server.getId());
        }
    }


//    /**
//     * Search for  server group by name or by id and return the id or null if server group not found.
//     *
//     * @param v - name or id of server group
//     *
//     * @return id of the found server group
//     */
//    private String getServerGroupByNameOrId(String v){
//        ServerGroupService sgs = os.compute().serverGroups();
//        List<? extends ServerGroup> sgl = sgs.list();
//        for (ServerGroup sg : sgl) {
//            if (sg.getId() == v || sg.getName() == v) {
//                return sg.getId();
//            }
//        }
//        return null;
//    }
}
