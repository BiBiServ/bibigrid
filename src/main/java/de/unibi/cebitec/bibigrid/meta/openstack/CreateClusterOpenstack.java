package de.unibi.cebitec.bibigrid.meta.openstack;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import de.unibi.cebitec.bibigrid.meta.CreateCluster;
import de.unibi.cebitec.bibigrid.model.Configuration;
import de.unibi.cebitec.bibigrid.util.DeviceMapper;
import static de.unibi.cebitec.bibigrid.util.ImportantInfoOutputFilter.I;
import de.unibi.cebitec.bibigrid.util.JSchLogger;
import de.unibi.cebitec.bibigrid.util.SshFactory;
import de.unibi.cebitec.bibigrid.util.UserDataCreator;
import de.unibi.cebitec.bibigrid.util.VerboseOutputFilter;
import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.codec.binary.Base64;
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
import org.openstack4j.model.compute.Image;
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
 *
 * @author Jan Krueger - jkrueger (at)cebitec.uni-bielfeld.de 1st version by
 * Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public class CreateClusterOpenstack extends OpenStackIntent implements CreateCluster<CreateClusterOpenstack, CreateClusterEnvironmentOpenstack> {

    public static final Logger LOG = LoggerFactory.getLogger(CreateClusterOpenstack.class);

    private CreateClusterEnvironmentOpenstack environment;

    public static final String PREFIX = "bibigrid-";
    public static final String SECURITY_GROUP_PREFIX = PREFIX + "sg-";

    public static final String MASTER_SSH_USER = "ubuntu";
    public static final String PLACEMENT_GROUP_PREFIX = PREFIX + "pg-";
    public static final String SUBNET_PREFIX = PREFIX + "subnet-";

    /*
     * Cluster ID
     */
    private final String clusterId;

    /*
     * MetaData
     */
    private final Map<String, String> metadata = new HashMap<>();

    public CreateClusterOpenstack(Configuration conf) {
        super(conf);
        // MetaData
        metadata.put("user", conf.getUser());

        // Cluster ID is a cut down base64 encoded version of a random UUID:
        UUID clusterIdUUID = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(clusterIdUUID.getMostSignificantBits());
        bb.putLong(clusterIdUUID.getLeastSignificantBits());
        String clusterIdBase64 = Base64.encodeBase64URLSafeString(bb.array()).replace("-", "").replace("_", "");
        int len = clusterIdBase64.length() >= 15 ? 15 : clusterIdBase64.length();
        clusterId = clusterIdBase64.substring(0, len);
        conf.setClusterId(clusterId);

        LOG.debug("cluster id: {}", clusterId);

        LOG.info("Openstack connection established ...");
    }

    @Override
    public CreateClusterEnvironmentOpenstack createClusterEnvironment() {
        return environment = new CreateClusterEnvironmentOpenstack(this);
    }

    private Flavor masterFlavor, slaveFlavor;
    //private CreateServerOptions masterOptions, slaveOptions;  -> os.compute()
    private String masterImage, slaveImage;
    private DeviceMapper masterDeviceMapper, slaveDeviceMapper;
    private Set<BlockDeviceMappingCreate> masterMappings, slaveMappings;

    @Override
    public CreateClusterOpenstack configureClusterMasterInstance() {
        List<Flavor> flavors = listFlavors();

        /**
         * DeviceMapper init.
         *
         */
        Map<String, String> masterVolumetoMountPointMap = new HashMap<>();
        for (String snapshot : conf.getMasterMounts().keySet()) {
            // 1st check if Snapshot exist
            VolumeSnapshot vss = getSnapshotbyName(snapshot);
            if (vss == null) {
                LOG.warn("Snapshot with name {} not found!", snapshot);
            } else {
                // 2nd create new  Volume from Snapshot
                Volume v = createVolumefromSnapshot(vss, snapshot + "_" + clusterId);
                masterVolumetoMountPointMap.put(v.getId(), conf.getMasterMounts().get(snapshot));
                LOG.info(V, "Create volume ({}) from snapshot ({}).", v.getName(), snapshot);
            }
        }
        masterDeviceMapper = new DeviceMapper(conf.getMode(), masterVolumetoMountPointMap, conf.getMasterInstanceType().getSpec().ephemerals);

        /**
         * BlockDeviceMapping.
         *
         */
        masterMappings = new HashSet<>();
        String[] ephemerals = {"b", "c", "d", "e"};
        for (int i = 0; i < this.conf.getMasterInstanceType().getSpec().ephemerals; ++i) {
            BlockDeviceMappingCreate bdmc = Builders.blockDeviceMapping()
                    .deviceName("sd" + ephemerals[i])
                    .deleteOnTermination(true)
                    .sourceType(BDMSourceType.BLANK)
                    .destinationType(BDMDestType.LOCAL)
                    .bootIndex(-1)
                    .build();
            masterMappings.add(bdmc);
        }

        masterImage = conf.getRegion() + "/" + conf.getMasterImage();

        String type = conf.getMasterInstanceType().getValue();
        masterFlavor = null;
        for (Flavor f : flavors) {
            if (f.getName().equals(type)) {
                masterFlavor = f;
                break;
            }
        }
        LOG.info("Master configured");
        return this;
    }

    @Override
    public CreateClusterOpenstack configureClusterSlaveInstance() {
        List<Flavor> flavors = listFlavors();

        /**
         * DeviceMapper Slave.
         *
         * @ToDo
         */
        Map<String, String> snapShotToSlaveMounts = this.conf.getSlaveMounts();
        slaveDeviceMapper = new DeviceMapper(conf.getMode(), snapShotToSlaveMounts, conf.getMasterInstanceType().getSpec().ephemerals);

        /**
         * BlockDeviceMapping.
         *
         * @ToDo
         */
        slaveMappings = new HashSet<>();
        String[] ephemerals = {"b", "c", "d", "e"};
        for (int i = 0; i < this.conf.getSlaveInstanceType().getSpec().ephemerals; ++i) {
            BlockDeviceMappingCreate bdmc = Builders.blockDeviceMapping()
                    .deviceName("sd" + ephemerals[i])
                    .deleteOnTermination(true)
                    .sourceType(BDMSourceType.BLANK)
                    .destinationType(BDMDestType.LOCAL)
                    .bootIndex(-1)
                    .build();
            slaveMappings.add(bdmc);

        }

        /**
         * Options.
         */
        slaveImage = conf.getRegion() + "/" + conf.getSlaveImage();
        String type = conf.getSlaveInstanceType().getValue();
        slaveFlavor = null;

        for (Flavor f : flavors) {
            if (f.getName().equals(type)) {
                slaveFlavor = f;
                break;
            }
        }
        LOG.info("Slave(s) configured");
        return this;
    }

    private final Instance master = new Instance();
    private final Map<String, Instance> slaves = new HashMap<>();

    @Override
    public boolean launchClusterInstances() {

        try {
            ServerCreate sc = Builders.server()
                    .name("bibigrid-master-" + clusterId)
                    .flavor(masterFlavor.getId())
                    .image(masterImage)
                    .keypairName(conf.getKeypair())
                    .addSecurityGroup(environment.getSecGroupExtension().getName())
                    .availabilityZone(conf.getAvailabilityZone())
                    .userData(UserDataCreator.masterUserData(masterDeviceMapper, conf, environment.getKeypair()))
                    .addMetadata(metadata)
                    .networks(Arrays.asList(environment.getNetwork().getId()))
                    .build();
            Server server = os.compute().servers().bootAndWaitActive(sc, 60000);

            // check if anything goes wrong,
            Fault fault = server.getFault();
            if (fault != null) {
                // some more debug information in verboose mode
                LOG.info(V, "{},{}", fault.getCode(), fault.getMessage());
                // print error message and abort launch
                if (fault.getCode() == 500) {
                    LOG.error("Launch master :: {}", fault.getMessage());
                    return false;
                }
            }

            LOG.info("Master (ID: {}) started", server.getId());

            master.setId(server.getId());
            master.setIp(waitForAddress(server.getId(), environment.getNetwork().getName()).getAddr());

            master.setHostname("bibigrid-master-" + clusterId);
            master.updateNeutronHostname();

            LOG.info("Master (ID: {}) network configuration finished", master.getId());

            // get and assign floating ip to master
            ActionResponse ar = null;
            boolean assigned = false;

            List<String> blacklist = new ArrayList<>();
            while (ar == null || !assigned) {
                // get next free floatingIP
                NetFloatingIP floatingip = getFloatingIP(blacklist);
                // if null  there is no free floating ip available
                if (floatingip == null) {
                    LOG.error("No unused FloatingIP available! Abbort!");
                    return false;
                }
                // put ip on blacklist
                blacklist.add(floatingip.getFloatingIpAddress());
                // try to assign floating ip to server
                ar = os.compute().floatingIps().addFloatingIP(server, floatingip.getFloatingIpAddress());
                // in case of success try  update master object
                if (ar.isSuccess()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // do nothing
                    }
                    Server tmp = os.compute().servers().get(server.getId());
                    if (tmp != null) {
                        assigned = checkforFloatingIp(tmp, floatingip.getFloatingIpAddress());
                        if (assigned) {
                            master.setPublicIp(floatingip.getFloatingIpAddress());
                            LOG.info("FloatingIP {} assigned to Master(ID: {}) ", master.getPublicIp(), master.getId());
                        } else {
                            LOG.warn("FloatingIP {} assignment failed ! Try another one ...", floatingip.getFloatingIpAddress());
                        }
                    }
                } else {
                    LOG.warn("FloatingIP {} assignment failed with fault : {}! Try another one ...", floatingip.getFloatingIpAddress(), ar.getFault());
                }
            }

            // attach Volumes
            for (String volumeid : masterDeviceMapper.getSnapshotIdToMountPoint().keySet()) {
                //check if volume is availabe

                Volume v = os.blockStorage().volumes().get(volumeid);

                boolean waiting = true;
                while (waiting) {
                    switch (v.getStatus()) {
                        case AVAILABLE:
                            waiting = false;
                            break;
                        case CREATING: {
                            try {
                                Thread.sleep(5000);
                                LOG.info(V, "Wait for Volume ({}) available", v.getId());
                            } catch (InterruptedException e) {
                                // do nothing
                            }
                            v = os.blockStorage().volumes().get(volumeid);
                            break;
                        }
                        default:
                            waiting = false;
                            LOG.error("Volume not available  (Status : {})",v.getStatus());
                    }
                }

                if (v.getStatus().equals(Status.AVAILABLE)) {
                    VolumeAttachment va = os.compute().servers().attachVolume(server.getId(), volumeid, masterDeviceMapper.getDeviceNameForSnapshotId(volumeid));
                    if (va == null) {
                        LOG.error("Attaching volume {} to master failed ...", volumeid);
                    } else {
                        LOG.info(V, "Volume {}  attached to Master.", va.getId());
                    }
                }
            }

            //boot slave instances ...
            List<String> slaveIDs = new ArrayList<>(); // temporary list
            for (int i = 0; i < conf.getSlaveInstanceCount(); i++) {
                // ServerCreated createdSlave = serverApi.create("bibigrid-slave-" + (i + 1) + "-" + clusterId, slaveImage, slaveFlavor.getId(), slaveOptions);
                sc = Builders.server()
                        .name("bibigrid-slave-" + (i + 1) + "-" + clusterId)
                        .flavor(slaveFlavor.getId())
                        .image(slaveImage)
                        .keypairName(conf.getKeypair())
                        .addSecurityGroup(environment.getSecGroupExtension().getId())
                        .availabilityZone(conf.getAvailabilityZone())
                        .userData(UserDataCreator.forSlave(master.getIp(),
                                master.getNeutronHostname(),
                                slaveDeviceMapper,
                                conf,
                                environment.getKeypair()))
                        .networks(Arrays.asList(environment.getNetwork().getId()))
                        .build();

                slaveIDs.add(os.compute().servers().boot(sc).getId());
                LOG.info(V, "Instance request for {}  ", sc.getName());
            }

            LOG.info("Waiting for slave instances ready ...");

            // check     
            while (slaves.size() != slaveIDs.size()) {
                // wait for some seconds to not overload REST API
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    // do nothing
                }
                // get fresh server object for given server id
                for (String id : slaveIDs) {
                    //ignore if instance is already active ...
                    if (!slaves.containsKey(id)) {
                        Server si = os.compute().servers().get(id);

                        // check for status available ...
                        if (si.getStatus() != null) {

                            switch (si.getStatus()) {
                                case ACTIVE:
                                    Instance slave = new Instance();
                                    slave.setId(si.getId());
                                    slave.setHostname(si.getName());
                                    slaves.put(id, slave);
                                    LOG.info("Instance {} is active !", si.getName());
                                    break;
                                case ERROR:
                                    // check if anything goes wrong,
                                    fault = si.getFault();
                                    if (fault != null) {
                                        LOG.error("Launch {} failed without message!", si.getName());
                                    } else {
                                        LOG.error("Launch {} failed with Code {} :: {}", si.getName(), fault.getCode(), fault.getMessage());

                                    }
                                    return false;

                                default:
                                    // other not critical state
                                    break;
                            }
                        } else {
                            LOG.warn(V, "Status  of instance {} not available (== null)", si.getId());
                        }
                    }
                }
            }
            LOG.info(V, "Wait for slave network configuration finished ...");
            // wait for slave network finished ... update server instance list            
            for (Instance i : slaves.values()) {
                i.setIp(waitForAddress(i.getId(), environment.getNetwork().getName()).getAddr());
                i.updateNeutronHostname();
            }
            LOG.info("Cluster (ID: {}) successfully created!", clusterId);

            sshTestAndExecute();

            ////////////////////////////////////
            //// Human friendly output
            StringBuilder sb = new StringBuilder();
            sb.append("\n You might want to set the following environment variable:\n\n");
            sb.append("export BIBIGRID_MASTER=").append(master.getPublicIp()).append("\n\n");
            sb.append("You can then log on the master node with:\n\n")
                    .append("ssh -i ")
                    .append(conf.getIdentityFile())
                    .append(" ubuntu@$BIBIGRID_MASTER\n\n");
            sb.append("The cluster id of your started cluster is : ")
                    .append(clusterId)
                    .append("\n\n");
            sb.append("The can easily terminate the cluster at any time with :\n")
                    .append("./bibigrid -t ").append(clusterId).append(" ");
            if (conf.isAlternativeConfigFile()) {
                sb.append("-o ").append(conf.getAlternativeConfigPath()).append(" ");
            }

            sb.append("\n");

            LOG.info(sb.toString());
        } catch (Exception e) {
            // print stacktrace only verbose mode, otherwise the message returned by OS is fine
            if (VerboseOutputFilter.SHOW_VERBOSE) {
                LOG.error(e.getMessage(), e);
            } else {
                LOG.error(e.getMessage());
            }
            return false;
        }
        return true;
    }

    /**
     * Return first network address object of given instance (by its ID and
     * network name). Block until address is available.
     *
     * @param instanceID - ID of given instances
     * @param network - Network name
     *
     * @return Address
     */
    private Address getLocalIp(String instanceID, String network) {
        Address addr = null;
        do {
            Server instance = os.compute().servers().get(instanceID);
            if (instance != null && instance.getAddresses() != null && instance.getAddresses().getAddresses(network) != null && instance.getAddresses().getAddresses(network).size() > 0) {
                addr = instance.getAddresses().getAddresses(network).iterator().next();
            } else {
                try {
                    Thread.sleep(2500);
                    System.out.print(".");
                } catch (InterruptedException ex) {
                    LOG.error("Can't sleep!");
                }
            }
        } while (addr == null);
        return addr;
    }

    private void sshTestAndExecute() {
        JSch ssh = new JSch();
        JSch.setLogger(new JSchLogger());
        /*
         * Building Command
         */
        LOG.info("Now configuring ...");
        String execCommand = SshFactory.buildSshCommandOpenstack(clusterId, this.getConfiguration(), master, slaves.values());
        LOG.info(V, "Building SSH-Command : {}", execCommand);

        boolean uploaded = false;
        boolean configured = false;

        int ssh_attempts = 50; // @TODO attempts
        while (!configured && ssh_attempts > 0) {
            try {

                ssh.addIdentity(this.getConfiguration().getIdentityFile().toString());
                LOG.info("Trying to connect to master ({})...", ssh_attempts);
                Thread.sleep(5000);

                /*
                 * Create new Session to avoid packet corruption.
                 */
                Session sshSession = SshFactory.createNewSshSession(ssh, master.getPublicIp(), MASTER_SSH_USER, getConfiguration().getIdentityFile());

                /*
                 * Start connect attempt
                 */
                sshSession.connect();
                LOG.info("Connected to master!");
                ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");

                BufferedReader stdout = new BufferedReader(new InputStreamReader(channel.getInputStream()));
                BufferedReader stderr = new BufferedReader(new InputStreamReader(channel.getErrStream()));

                channel.setCommand(execCommand);

                LOG.info(V, "Connecting ssh channel...");
                channel.connect();

                String lineout = null, lineerr = null;

                while (!configured) {

                    if (stdout.ready()) {
                        lineout = stdout.readLine();
                        if (lineout.equals("CONFIGURATION_FINISHED")) {
                            configured = true;
                        }
                        LOG.info(V, "SSH: {}", lineout);
                    }

                    if (stderr.ready()) {
                        lineerr = stderr.readLine();
                        LOG.error("SSH: {}", lineerr);
                    }

                    Thread.sleep(500);
                }
                if (channel.isClosed()) {
                    LOG.info(V, "SSH: exit-status: {}", channel.getExitStatus());
                }
                channel.disconnect();
                sshSession.disconnect();

            } catch (IOException | JSchException e) {
                ssh_attempts--;
                if (ssh_attempts == 0) {
                    LOG.error(V, "SSH: {}", e.getMessage());
                }

            } catch (InterruptedException ex) {
                LOG.warn("Interrupted ...");
            }
        }

        if (configured) {
            LOG.info(I, "Master instance has been configured.");
        } else {
            LOG.error("Master instance configuration failed!");
        }

    }

    private List<Server> listServers(String region) {
        List<Server> ret = new ArrayList<>();

        for (Server server : os.compute().servers().list()) {
            ret.add(server);
        }
        return ret;
    }

    private List<Flavor> listFlavors() {
        List<Flavor> ret = new ArrayList<>();
        for (Flavor r : os.compute().flavors().list()) {
            ret.add(r);
        }
        return ret;
    }

    private List<Image> listImages() {
        List<Image> ret = new ArrayList<>();
        for (Image m : os.compute().images().list()) {
            ret.add(m);
        }
        return ret;
    }

    /**
     * Returns an available (not used) FloatingIP. Returns null in the case that
     * no unused FloatingIP is available and no new FloatingIP could be
     * allocated.
     *
     * @return
     */
    private NetFloatingIP getFloatingIP() {
        return getFloatingIP(new ArrayList<>());
    }

    private NetFloatingIP getFloatingIP(List<String> blacklist) {
        // get list of all available floating IP's, and search for free ones ...
        List<? extends NetFloatingIP> l = os.networking().floatingip().list();

        for (int i = 0; i < l.size(); i++) {
            NetFloatingIP fip = l.get(i);

            if (fip.getPortId() == null
                    // check if floatingip fits to router network id
                    && fip.getFloatingNetworkId().equals(environment.getRouter().getExternalGatewayInfo().getNetworkId())
                    // check if tentant id fits routers tenant id
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

    public String getClusterId() {
        return clusterId;
    }

    public Configuration getConfiguration() {
        return this.conf;
    }

    public class Instance {

        private String id, ip, publicIp;

        private String hostname, neutronHostname;

        public Instance() {
        }

        ;

       
       public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getPublicIp() {
            return publicIp;
        }

        public void setPublicIp(String publicIp) {
            this.publicIp = publicIp;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getNeutronHostname() {
            return neutronHostname;
        }

        public void setNeutronHostname(String neutronHostname) {
            this.neutronHostname = neutronHostname;
        }

        public void updateNeutronHostname() {
            if (ip != null && ip.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
                String[] t = ip.split("\\.");
                neutronHostname = "host-" + String.join("-", t);
            } else {
                LOG.warn("ip must be a valid IPv4 address string.");
            }
        }

    }

    public OSClient getOs() {
        return os;
    }

    private Address waitForAddress(String serverid, String networkname) {
        // ################################################################
        // following block is a ugly hack to refresh the server object
        // ####################################################### -> Start
        Addresses addresses;
        List<? extends Address> addrlist;
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
            server = os.compute().servers().get(serverid);
            addresses = server.getAddresses();
            map = addresses.getAddresses();
            addrlist = map.get(networkname);
            LOG.info(V, "addrlist {}", addrlist);
        } while (addrlist == null || addrlist.isEmpty());
        // <- End ##########################################################
        return addrlist.get(0);
    }

    /**
     * Check if a given floating ip is set for a given server instance.
     *
     * @param serv - Server instance
     * @param floatingip - floatingIp to be checked
     * @return
     */
    private boolean checkforFloatingIp(Server serv, String floatingip) {
        Map<String, List<? extends Address>> madr = serv.getAddresses().getAddresses();
        // map should contain only one  network
        if (madr.keySet().size() == 1) {
            for (Address address : madr.get((String) (madr.keySet().toArray()[0]))) {
                if (address.getType().equals("floating") && address.getAddr().equals(floatingip)) {
                    return true;
                }
            }

        } else {
            LOG.warn("No or more than one network associated with instance {}", serv.getId());
        }
        return false;

    }

    /**
     * Return a Snapshot by its name or null if no snapshot is found ...
     *
     * @param name
     * @return
     */
    private VolumeSnapshot getSnapshotbyName(String name) {
        List<? extends VolumeSnapshot> allsnapshots = os.blockStorage().snapshots().list();
        for (VolumeSnapshot vss : allsnapshots) {
            if (vss.getName().equals(name)) {

                return vss;
            }
        }
        // nothing found
        return null;
    }

    /**
     * Return a new volume from a Snapshot
     *
     * @param vss
     * @param name of newly created volume
     * @return
     */
    private Volume createVolumefromSnapshot(VolumeSnapshot vss, String name) {
        return os.blockStorage().volumes().create(Builders.volume()
                .name(name)
                .snapshot(vss.getId())
                .description("created from SnapShot " + vss.getId() + " by BiBiGrid")
                .build());
    }

}
