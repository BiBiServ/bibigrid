/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.meta.openstack;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import de.unibi.cebitec.bibigrid.meta.CreateCluster;
import de.unibi.cebitec.bibigrid.model.Configuration;
import de.unibi.cebitec.bibigrid.util.DeviceMapper;
import de.unibi.cebitec.bibigrid.util.InstanceInformation;
import de.unibi.cebitec.bibigrid.util.KEYPAIR;
import de.unibi.cebitec.bibigrid.util.UserDataCreator;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.codec.binary.Base64;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.BlockDeviceMapping;
import org.jclouds.openstack.nova.v2_0.domain.Flavor;
import org.jclouds.openstack.nova.v2_0.domain.Image;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.domain.ServerCreated;
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

    private final NovaApi novaClient;

    private final String os_region = "regionOne";

    private final String provider = "openstack-nova";

    private CreateClusterEnvironmentOpenstack environment;

    private Configuration conf;

    public static final String PREFIX = "bibigrid-";
    public static final String SECURITY_GROUP_PREFIX = PREFIX + "sg-";

    public static final String MASTER_SSH_USER = "ubuntu";
    public static final String PLACEMENT_GROUP_PREFIX = PREFIX + "pg-";
    public static final String SUBNET_PREFIX = PREFIX + "subnet-";

    private String base64MasterUserData;

    private String clusterId;
    private DeviceMapper slaveDeviceMapper;

    private KEYPAIR keypair;

    public CreateClusterOpenstack(Configuration conf) {

        // Cluster ID is a cut down base64 encoded version of a random UUID:
        UUID clusterIdUUID = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(clusterIdUUID.getMostSignificantBits());
        bb.putLong(clusterIdUUID.getLeastSignificantBits());
        String clusterIdBase64 = Base64.encodeBase64URLSafeString(bb.array()).replace("-", "").replace("_", "");
        int len = clusterIdBase64.length() >= 15 ? 15 : clusterIdBase64.length();
        clusterId = clusterIdBase64.substring(0, len);
//        bibigridid = new Tag().withKey("bibigrid-id").withValue(clusterId);
//        username = new Tag().withKey("user").withValue(config.getUser());
        log.debug("cluster id: {}", clusterId);

        this.conf = conf;
        Iterable<Module> modules = ImmutableSet.<Module>of(
                new SshjSshClientModule(),
                new SLF4JLoggingModule());

//        context = ContextBuilder.newBuilder(provider)
//                .endpoint(this.endPoint)
//                .credentials(username, password)
//                .modules(modules)
//                .buildView(ComputeServiceContext.class);
//        context.getComputeService().templateOptions().
        novaClient = ContextBuilder.newBuilder(provider)
                .endpoint(conf.getOpenstackCredentials().getEndpoint())
                .credentials(conf.getOpenstackCredentials().getTenantName() + ":" + conf.getOpenstackCredentials().getUsername(), conf.getOpenstackCredentials().getPassword())
                .modules(modules)
                .buildApi(NovaApi.class);
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
        ServerApi s = novaClient.getServerApi(os_region);
        List<Image> images = listImages();
        List<Flavor> flavors = listFlavors();
        /**
         * BlockDeviceMapping.
         */
        Set<BlockDeviceMapping> mappings = new HashSet<>();
        String[] ephemerals = {"b", "c", "d", "e"};
        for (int i = 0; i < InstanceInformation.getSpecs(this.conf.getMasterInstanceType()).ephemerals; ++i) {
            BlockDeviceMapping m = BlockDeviceMapping.builder()
                    .deviceName("/dev/sd" + ephemerals[i])
                    .bootIndex(i)
                    .uuid("ephemeral" + i)
                    .build();
            mappings.add(m);
        }
        /**
         * Options.
         */
        masterOptions = new CreateServerOptions();
        masterOptions.keyPairName(conf.getKeypair());
        masterOptions.securityGroupNames("default");
        masterOptions.availabilityZone(conf.getAvailabilityZone());
        masterOptions.userData(UserDataCreator.masterUserData(null, conf, environment.getKeypair().getPrivateKey()).getBytes()); //fails cause of null devicemapper
        masterOptions.blockDeviceMappings(mappings);

        masterImage = os_region + "/" + conf.getMasterImage();
        String type = conf.getMasterInstanceType().toString();
        masterFlavor = null;
        for (Flavor f : flavors) {
            if (f.getName().equals(type)) {
                masterFlavor = f;
            }
        }
        return this;
    }

    @Override
    public CreateClusterOpenstack configureClusterSlaveInstance() {
        ServerApi s = novaClient.getServerApi(os_region);
        List<Image> images = listImages();
        List<Flavor> flavors = listFlavors();

        /**
         * BlockDeviceMapping.
         */
        Set<BlockDeviceMapping> mappings = new HashSet<>();
        String[] ephemerals = {"b", "c", "d", "e"};
        for (int i = 0; i < InstanceInformation.getSpecs(this.conf.getSlaveInstanceType()).ephemerals; ++i) {
            BlockDeviceMapping m = BlockDeviceMapping.builder()
                    .deviceName("/dev/sd" + ephemerals[i])
                    .bootIndex(i)
                    .uuid("ephemeral" + i)
                    .build();
            mappings.add(m);
        }

        /**
         * Options.
         */
        slaveOptions = new CreateServerOptions();
        slaveOptions.keyPairName(conf.getKeypair());
        slaveOptions.securityGroupNames("default");
        slaveOptions.availabilityZone(conf.getAvailabilityZone());
        slaveOptions.userData(UserDataCreator.masterUserData(null, conf, environment.getKeypair().getPrivateKey()).getBytes()); //fails cause of null devicemapper
        slaveOptions.blockDeviceMappings(mappings);

        slaveImage = os_region + "/" + conf.getSlaveImage();
        String type = conf.getSlaveInstanceType().toString();
        slaveFlavor = null;

        for (Flavor f : flavors) {
            if (f.getName().equals(type)) {
                slaveFlavor = f;
            }
        }
        return this;
    }

    @Override
    public boolean launchClusterInstances() {
        try {
            ServerApi s = novaClient.getServerApi(os_region);
            ServerCreated createdMaster = s.create("bibigrid_master_" + clusterId, masterImage, masterFlavor.getId(), masterOptions);
            log.info("Master (ID: {}) successfully started", createdMaster.getId());
            for (int i = 0; i < conf.getSlaveInstanceCount(); i++) {
                ServerCreated createdSlave = s.create("bibigrid_slave_" + (i + 1) + "_" + clusterId, slaveImage, slaveFlavor.getId(), slaveOptions);
                log.info("Slave_{} (ID: {}) successfully started", i + 1, createdSlave.getId());
            }
            log.info("Cluster (ID: {}) successfully created!", clusterId);

            ////////////////////////////////////
            //// Human friendly output
            StringBuilder sb = new StringBuilder();
            sb.append("\n You might want to set the following environment variable:\n\n");
            sb.append("export BIBIGRID_MASTER=").append(getPublicIpFromServer(createdMaster.getId())).append("\n\n");
            sb.append("You can then log on the master node with:\n\n")
                    .append("[TODO] ssh")
                    //                    .append(conf.getIdentityFile())
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
            return false;
        }
        return true;
    }

    private String getPublicIpFromServer(String serverID) {
        ServerApi serverApi = novaClient.getServerApi(os_region);

        for (Server server : serverApi.listInDetail().concat()) {
            if (server.getId().equals(serverID)) {
                return server.getAccessIPv4();
            }
        }
        return "";
    }

    private List<Server> listServers() {
        List<Server> ret = new ArrayList<>();
        Set<String> regions = novaClient.getConfiguredRegions();
        for (String region : regions) {
            ServerApi serverApi = novaClient.getServerApi(region);

            for (Server server : serverApi.listInDetail().concat()) {
                ret.add(server);
            }
        }
        return ret;
    }

    private List<Flavor> listFlavors() {
        List<Flavor> ret = new ArrayList<>();
        FlavorApi f = novaClient.getFlavorApi(os_region); // hardcoded
        for (Flavor r : f.listInDetail().concat()) {
            ret.add(r);
        }
        return ret;
    }

    private List<Image> listImages() {
        List<Image> ret = new ArrayList<>();
        ImageApi imageApi = novaClient.getImageApi(os_region);
        for (Image m : imageApi.listInDetail().concat()) {
            ret.add(m);
        }
        return ret;
    }

}
