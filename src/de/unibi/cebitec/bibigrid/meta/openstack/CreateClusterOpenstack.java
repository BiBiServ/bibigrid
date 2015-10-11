/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.meta.openstack;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import static de.unibi.cebitec.bibigrid.ctrl.CreateIntent.log;
import de.unibi.cebitec.bibigrid.meta.CreateCluster;
import de.unibi.cebitec.bibigrid.model.Configuration;
import de.unibi.cebitec.bibigrid.util.DeviceMapper;
import static de.unibi.cebitec.bibigrid.util.ImportantInfoOutputFilter.I;
import de.unibi.cebitec.bibigrid.util.InstanceInformation;
import de.unibi.cebitec.bibigrid.util.JSchLogger;
import de.unibi.cebitec.bibigrid.util.KEYPAIR;
import de.unibi.cebitec.bibigrid.util.SshFactory;
import de.unibi.cebitec.bibigrid.util.UserDataCreator;
import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.apache.commons.codec.binary.Base64;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.Address;
import org.jclouds.openstack.nova.v2_0.domain.BlockDeviceMapping;
import org.jclouds.openstack.nova.v2_0.domain.Flavor;
import org.jclouds.openstack.nova.v2_0.domain.Image;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.ServerCreated;
import org.jclouds.openstack.nova.v2_0.extensions.SecurityGroupApi;
import org.jclouds.openstack.nova.v2_0.features.FlavorApi;
import org.jclouds.openstack.nova.v2_0.features.ImageApi;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.openstack.nova.v2_0.options.CreateServerOptions;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jsteiner
 */
public class CreateClusterOpenstack implements CreateCluster<CreateClusterOpenstack, CreateClusterEnvironmentOpenstack> {

    public static final Logger log = LoggerFactory.getLogger(CreateClusterOpenstack.class);

    private final NovaApi novaApi;
    private final ServerApi serverApi;
    private final SecurityGroupApi securityGroupApi;
    private final FlavorApi flavorApi;
    private final ImageApi imageApi;

    private final String os_region;

    private final String provider = "openstack-nova";

    private CreateClusterEnvironmentOpenstack environment;

    private Configuration conf;

    public static final String PREFIX = "bibigrid-";
    public static final String SECURITY_GROUP_PREFIX = PREFIX + "sg-";

    public static final String MASTER_SSH_USER = "ubuntu";
    public static final String PLACEMENT_GROUP_PREFIX = PREFIX + "pg-";
    public static final String SUBNET_PREFIX = PREFIX + "subnet-";

    private String clusterId;
    private DeviceMapper slaveDeviceMapper;

    public CreateClusterOpenstack(Configuration conf) {

        // Cluster ID is a cut down base64 encoded version of a random UUID:
        UUID clusterIdUUID = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(clusterIdUUID.getMostSignificantBits());
        bb.putLong(clusterIdUUID.getLeastSignificantBits());
        String clusterIdBase64 = Base64.encodeBase64URLSafeString(bb.array()).replace("-", "").replace("_", "");
        int len = clusterIdBase64.length() >= 15 ? 15 : clusterIdBase64.length();
        clusterId = clusterIdBase64.substring(0, len);
        log.debug("cluster id: {}", clusterId);

        this.conf = conf;
        os_region = conf.getRegion();

        Iterable<Module> modules = ImmutableSet.<Module>of(
                new SshjSshClientModule(),
                new SLF4JLoggingModule());

        novaApi = ContextBuilder.newBuilder(provider)
                .endpoint(conf.getOpenstackCredentials().getEndpoint())
                .credentials(conf.getOpenstackCredentials().getTenantName() + ":" + conf.getOpenstackCredentials().getUsername(), conf.getOpenstackCredentials().getPassword())
                .modules(modules)
                .buildApi(NovaApi.class);

        serverApi = novaApi.getServerApi(os_region);
        securityGroupApi = novaApi.getSecurityGroupApi(os_region).get();
        flavorApi = novaApi.getFlavorApi(os_region);
        imageApi = novaApi.getImageApi(os_region);

        log.info("Openstack connection established ...");
    }

    @Override
    public CreateClusterEnvironmentOpenstack createClusterEnvironment() {
        return environment = new CreateClusterEnvironmentOpenstack(this);
    }

    private Flavor masterFlavor, slaveFlavor;
    private CreateServerOptions masterOptions, slaveOptions;
    private String masterImage, slaveImage;

    @Override
    public CreateClusterOpenstack configureClusterMasterInstance() {
        List<Flavor> flavors = listFlavors();

        /**
         * DeviceMapper init.
         */
        Map<String, String> masterSnapshotToMountPointMap = this.conf.getMasterMounts();
        DeviceMapper masterDeviceMapper = new DeviceMapper(masterSnapshotToMountPointMap);

        /**
         * BlockDeviceMapping.
         */
        Set<BlockDeviceMapping> mappings = new HashSet<>();
        String[] ephemerals = {"b", "c", "d", "e"};
        for (int i = 0; i < this.conf.getMasterInstanceType().getSpec().ephemerals; ++i) {
            BlockDeviceMapping m = BlockDeviceMapping.builder()
                    .deviceName("sd" + ephemerals[i])
                    .deleteOnTermination(Boolean.TRUE)
                    .sourceType("blank")
                    .destinationType("local")
                    .bootIndex(-1)
                    .build();
            mappings.add(m);
        }
        /**
         * Options.
         */
        masterOptions = new CreateServerOptions();
        masterOptions.keyPairName(conf.getKeypair());
        masterOptions.securityGroupNames(environment.getSecurityGroup().getName());
        masterOptions.availabilityZone(conf.getAvailabilityZone());
        masterOptions.userData(UserDataCreator.masterUserData(masterDeviceMapper, conf, environment.getKeypair().getPrivateKey()).getBytes()); //fails cause of null devicemapper
//        masterOptions.blockDeviceMappings(mappings);

        masterImage = os_region + "/" + conf.getMasterImage();
        String type = conf.getMasterInstanceType().getValue();
        masterFlavor = null;
        for (Flavor f : flavors) {
            if (f.getName().equals(type)) {
                masterFlavor = f;
                break;
            }
        }
        log.info("Master configured");
        return this;
    }

    @Override
    public CreateClusterOpenstack configureClusterSlaveInstance() {
        List<Flavor> flavors = listFlavors();

        /**
         * DeviceMapper Slave.
         */
        Map<String, String> snapShotToSlaveMounts = this.conf.getSlaveMounts();
        slaveDeviceMapper = new DeviceMapper(snapShotToSlaveMounts);

        /**
         * BlockDeviceMapping.
         */
        Set<BlockDeviceMapping> mappings = new HashSet<>();
        String[] ephemerals = {"b", "c", "d", "e"};
        for (int i = 0; i < this.conf.getSlaveInstanceType().getSpec().ephemerals; ++i) {
            BlockDeviceMapping m = BlockDeviceMapping.builder()
                    .deviceName("/dev/sd" + ephemerals[i])
                    .sourceType("blank")
                    .deleteOnTermination(Boolean.TRUE)
                    .destinationType("local")
                    .build();
            mappings.add(m);
        }

        /**
         * Options.
         */
        slaveOptions = new CreateServerOptions();
        slaveOptions.keyPairName(conf.getKeypair());
        slaveOptions.securityGroupNames(environment.getSecurityGroup().getName());
        slaveOptions.availabilityZone(conf.getAvailabilityZone());
//        slaveOptions.blockDeviceMappings(mappings);

        slaveImage = os_region + "/" + conf.getSlaveImage();
        String type = conf.getSlaveInstanceType().getValue();
        slaveFlavor = null;

        for (Flavor f : flavors) {
            if (f.getName().equals(type)) {
                slaveFlavor = f;
                break;
            }
        }
        log.info("Slave(s) configured");
        return this;
    }

    private String masterIP;
    private List<String> slaveIPs;

    private String masterDNS;
    private List<String> slaveDns = new ArrayList<>();

    @Override
    public boolean launchClusterInstances() {
        try {
            ServerCreated createdMaster = serverApi.create("bibigrid-master-" + clusterId, masterImage, masterFlavor.getId(), masterOptions);
            log.info("Master (ID: {}) successfully started", createdMaster.getId());

            masterIP = getPublicIpFromServer(createdMaster.getId(), "Master");
            masterDNS = "bibigrid-master-" + clusterId + ".novalocal";

            slaveOptions.userData(UserDataCreator.forSlave(masterIP,
                    masterDNS,
                    slaveDeviceMapper,
                    conf,
                    environment.getKeypair().getPublicKey()).getBytes()
            );

            slaveIPs = new ArrayList<>();

            for (int i = 0; i < conf.getSlaveInstanceCount(); i++) {
                ServerCreated createdSlave = serverApi.create("bibigrid-slave-" + (i + 1) + "-" + clusterId, slaveImage, slaveFlavor.getId(), slaveOptions);
                slaveIPs.add(getPublicIpFromServer(createdSlave.getId(), "Slave_" + (i + 1))); //store all slave ips
                slaveDns.add("bibigrid-slave-" + (i + 1) + "-" + clusterId + ".novalocal");
                log.info("Slave_{} (ID: {}) successfully started", i + 1, createdSlave.getId());
            }
            log.info("Cluster (ID: {}) successfully created!", clusterId);

            sshTestAndExecute();

            ////////////////////////////////////
            //// Human friendly output
            StringBuilder sb = new StringBuilder();
            sb.append("\n You might want to set the following environment variable:\n\n");
            sb.append("export BIBIGRID_MASTER=").append(masterIP).append("\n\n");
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

            log.info(sb.toString());
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
        return true;
    }

    private String getPublicIpFromServer(String serverID, String instance) {
        Server server = serverApi.get(serverID);
        if (server != null) {
            String addr = "";
            log.info("Waiting for {} network configuration ...", instance);
            do {
                for (Address a : server.getAddresses().get("demo-net")) {
                    addr = a.getAddr();
                }
                if (addr.isEmpty()) {
                    try {
                        Thread.sleep(2500);
                    } catch (InterruptedException ex) {
                        java.util.logging.Logger.getLogger(CreateClusterOpenstack.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    server = serverApi.get(serverID);
                }

            } while (addr.isEmpty());
            return addr;
        } else {
            return null;
        }
    }

    private void sshTestAndExecute() {
        JSch ssh = new JSch();
        JSch.setLogger(new JSchLogger());
        /*
         * Building Command
         */
        log.info("Now configuring ...");
        String execCommand;
        if (slaveDns.isEmpty()) {
            execCommand = SshFactory.buildSshCommandOpenstack(clusterId, this.getConfiguration(), masterDNS);
        } else {
            execCommand = SshFactory.buildSshCommandOpenstack(clusterId, this.getConfiguration(), masterDNS, slaveDns);
        }

        log.info(V, "Building SSH-Command : {}", execCommand);

        boolean uploaded = false;
        boolean configured = false;

        int ssh_attempts = 25; // @TODO attempts
        while (!configured && ssh_attempts > 0) {
            try {

                ssh.addIdentity(this.getConfiguration().getIdentityFile().toString());
                log.info("Trying to connect to master ({})...", ssh_attempts);
                Thread.sleep(4000);

                /*
                 * Create new Session to avoid packet corruption.
                 */
                Session sshSession = SshFactory.createNewSshSession(ssh, masterIP, MASTER_SSH_USER, this.getConfiguration().getIdentityFile());

                /*
                 * Start connect attempt
                 */
                sshSession.connect();
                log.info("Connected to master!");

//                if (!uploaded || ssh_attempts > 0) {
//                    String remoteDirectory = "/home/ubuntu/.ssh";
//                    String filename = "id_rsa";
//                    String localFile = getConfiguration().getIdentityFile().toString();
//                    log.info(V, "Uploading key");
//                    ChannelSftp channelPut = (ChannelSftp) sshSession.openChannel("sftp");
//                    channelPut.connect();
//                    channelPut.cd(remoteDirectory);
//                    channelPut.put(new FileInputStream(localFile), filename);
//                    channelPut.disconnect();
//                    log.info(V, "Upload done");
//                    uploaded = true;
//                }
                ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");

                BufferedReader stdout = new BufferedReader(new InputStreamReader(channel.getInputStream()));
                BufferedReader stderr = new BufferedReader(new InputStreamReader(channel.getErrStream()));

                channel.setCommand(execCommand);

                log.info(V, "Connecting ssh channel...");
                channel.connect();

                String lineout = null, lineerr = null;

                while (((lineout = stdout.readLine()) != null) || ((lineerr = stderr.readLine()) != null)) {

                    if (lineout != null) {
                        if (lineout.contains("CONFIGURATION_FINISHED")) {
                            configured = true;
                        }
                        log.info(V, "SSH: {}", lineout);
                    }

//                    if (lineerr != null) {
                    if (lineerr != null && !configured) {
                        log.error(V, "SSH: {}", lineerr);
                    }
//                    if (channel.isClosed() || configured) {
                    if (channel.isClosed() && configured) {
                        log.info(V, "SSH: exit-status: {}", channel.getExitStatus());
                        configured = true;
                    }

                    Thread.sleep(2000);
                }
                if (configured) {
                    channel.disconnect();
                    sshSession.disconnect();
                }

            } catch (IOException | JSchException e) {
                ssh_attempts--;
                if (ssh_attempts == 0) {
                    log.error(V, "SSH: {}", e);
                }

//                try {
//                    Thread.sleep(2000);
//                } catch (InterruptedException ex) {
//                    log.error("Interrupted ...");
//                }
            } catch (InterruptedException ex) {
                log.error("Interrupted ...");
            }
        }
        log.info(I, "Master instance has been configured.");
    }

    private List<Server> listServers(String region) {
        List<Server> ret = new ArrayList<>();

        for (Server server : serverApi.listInDetail().concat()) {
            ret.add(server);
        }
        return ret;
    }

    private List<Flavor> listFlavors() {
        List<Flavor> ret = new ArrayList<>();
        for (Flavor r : flavorApi.listInDetail().concat()) {
            ret.add(r);
        }
        return ret;
    }

    private List<Image> listImages() {
        List<Image> ret = new ArrayList<>();
        for (Image m : imageApi.listInDetail().concat()) {
            ret.add(m);
        }
        return ret;
    }

    public NovaApi getNovaApi() {
        return novaApi;
    }

    public ServerApi getServerApi() {
        return serverApi;
    }

    public SecurityGroupApi getSecurityGroupApi() {
        return securityGroupApi;
    }

    public FlavorApi getFlavorApi() {
        return flavorApi;
    }

    public ImageApi getImageApi() {
        return imageApi;
    }

    public String getClusterId() {
        return clusterId;
    }

    public Configuration getConfiguration() {
        return this.conf;
    }

}
