package de.unibi.cebitec.bibigrid.meta.googlecloud;

import com.google.cloud.compute.*;
import de.unibi.cebitec.bibigrid.meta.CreateCluster;
import de.unibi.cebitec.bibigrid.model.Configuration;
import de.unibi.cebitec.bibigrid.util.DeviceMapper;
import de.unibi.cebitec.bibigrid.util.UserDataCreator;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import static de.unibi.cebitec.bibigrid.util.ImportantInfoOutputFilter.I;
import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;

/**
 * Implementation of the general CreateCluster interface for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class CreateClusterGoogleCloud implements CreateCluster<CreateClusterGoogleCloud, CreateClusterEnvironmentGoogleCloud> {
    private static final Logger log = LoggerFactory.getLogger(CreateClusterGoogleCloud.class);
    private static final String PREFIX = "bibigrid-";
    public static final String SUBNET_PREFIX = PREFIX + "subnet-";
    private final Configuration conf;

    private Compute compute;
    private Instance masterInstance;
    private List<Instance> slaveInstances;
    private CreateClusterEnvironmentGoogleCloud environment;
    private String bibigridid, username;
    private String clusterId;
    private DeviceMapper slaveDeviceMapper;

    public CreateClusterGoogleCloud(final Configuration conf) {
        this.conf = conf;
    }

    public CreateClusterEnvironmentGoogleCloud createClusterEnvironment() {
        compute = GoogleCloudUtils.getComputeService(conf);

        // Cluster ID is a cut down base64 encoded version of a random UUID:
        UUID clusterIdUUID = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(clusterIdUUID.getMostSignificantBits());
        bb.putLong(clusterIdUUID.getLeastSignificantBits());
        String clusterIdBase64 = Base64.encodeBase64URLSafeString(bb.array()).replace("-", "").replace("_", "");
        int len = clusterIdBase64.length() >= 15 ? 15 : clusterIdBase64.length();
        clusterId = clusterIdBase64.substring(0, len);
        bibigridid = "bibigrid-id:" + clusterId;
        username = "user:" + conf.getUser();
        log.debug("cluster id: {}", clusterId);

        return environment = new CreateClusterEnvironmentGoogleCloud(this);
    }

    public CreateClusterGoogleCloud configureClusterMasterInstance() {
        // TODO: stub
        return this;
    }

    public CreateClusterGoogleCloud configureClusterSlaveInstance() {
        //now defining Slave Volumes
        Map<String, String> snapShotToSlaveMounts = conf.getSlaveMounts();
        int ephemerals = conf.getSlaveInstanceType().getSpec().ephemerals;
        slaveDeviceMapper = new DeviceMapper(conf.getMode(),snapShotToSlaveMounts, ephemerals);
        /* TODO
        slaveBlockDeviceMappings = new ArrayList<>();
        // configure volumes first ...
        if (!snapShotToSlaveMounts.isEmpty()) {
            log.info(V, "configure slave volumes");
            slaveBlockDeviceMappings = createBlockDeviceMappings(slaveDeviceMapper);
        }
        // configure ephemeral devices
        List<BlockDeviceMapping> ephemeralList = new ArrayList<>();
        if (ephemerals > 0) {
            for (int i = 0; i < ephemerals; ++i) {
                BlockDeviceMapping temp = new BlockDeviceMapping();
                String virtualName = "ephemeral" + i;
                String deviceName = "/dev/sd" + ephemeral(i);
                temp.setVirtualName(virtualName);
                temp.setDeviceName(deviceName);
                ephemeralList.add(temp);
            }
        }

        slaveBlockDeviceMappings.addAll(ephemeralList);
        */
        return this;
    }

    public boolean launchClusterInstances() {
        log.info("Requesting master instance ...");

        String zone = conf.getAvailabilityZone();
        String masterInstanceName = PREFIX + "master-" + clusterId;
        InstanceInfo.Builder masterBuilder = GoogleCloudUtils.getInstanceBuilder(conf.getMasterImage(),
                zone, masterInstanceName, conf.getMasterInstanceType().getValue())
                //.setNetworkInterfaces() // TODO
                .setTags(Tags.of(bibigridid, username, "Name:" + masterInstanceName))
                .setMetadata(Metadata.newBuilder().add("startup-script", base64MasterUserData).build());
        // TODO .withBlockDeviceMappings(masterDeviceMappings)
        // TODO .withKeyName(conf.getKeypair())
        GoogleCloudUtils.setInstanceSchedulingOptions(masterBuilder, conf.isUseSpotInstances());

        // Waiting for master instance to run
        log.info("Waiting for master instance to finish booting ...");
        Operation createMasterOperation = compute.create(masterBuilder.build());
        masterInstance = waitForInstances(Collections.singletonList(createMasterOperation)).get(0);
        log.info(I, "Master instance is now running!");

        waitForStatusCheck("master", Collections.singletonList(masterInstance));

        String masterPrivateIp = GoogleCloudUtils.getInstancePrivateIp(masterInstance);
        String masterPublicIp = GoogleCloudUtils.getInstancePublicIp(masterInstance);
        String masterDnsName = GoogleCloudUtils.getInstanceFQDN(conf.getGoogleProjectId(), masterInstanceName);

        // run slave instances and supply userdata
        if (conf.getSlaveInstanceCount() > 0) {
            String base64SlaveUserData = UserDataCreator.forSlave(masterPrivateIp, masterDnsName, slaveDeviceMapper,
                    conf, environment.getKeypair());
            // Google doesn't use base64 encoded startup scripts. Just use plain text
            base64SlaveUserData = new String(Base64.decodeBase64(base64SlaveUserData));
            log.info(V, "Slave Userdata:\n{}", base64SlaveUserData);

            List<Operation> slaveInstanceOperations = new ArrayList<>();
            for (int i = 0; i < conf.getSlaveInstanceCount(); i++) {
                String slaveInstanceName = PREFIX + "slave-" + clusterId;
                String slaveInstanceId = PREFIX + "slave" + i + "-" + clusterId;
                InstanceInfo.Builder slaveBuilder = GoogleCloudUtils.getInstanceBuilder(conf.getSlaveImage(),
                        zone, slaveInstanceId, conf.getSlaveInstanceType().getValue())
                        //.setNetworkInterfaces() // TODO
                        .setTags(Tags.of(bibigridid, username, "Name:" + slaveInstanceName))
                        .setMetadata(Metadata.newBuilder().add("startup-script", base64SlaveUserData).build());
                // TODO .withBlockDeviceMappings(slaveBlockDeviceMappings)
                // TODO .withKeyName(this.config.getKeypair())
                GoogleCloudUtils.setInstanceSchedulingOptions(masterBuilder, conf.isUseSpotInstances());

                Operation createSlaveOperation = compute.create(slaveBuilder.build());
                slaveInstanceOperations.add(createSlaveOperation);
            }
            log.info("Waiting for slave instance(s) to finish booting ...");
            slaveInstances = waitForInstances(slaveInstanceOperations);
            log.info(I, "Slave instance(s) is now running!");
        } else {
            log.info("No Slave instance(s) requested!");
        }

        // post configure master
        configureMaster();

        // Human friendly output
        StringBuilder sb = new StringBuilder();
        sb.append("\n You might want to set the following environment variable:\n\n");
        sb.append("export BIBIGRID_MASTER=").append(masterPublicIp).append("\n\n");
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

        // Grid Properties file
        if (conf.getGridPropertiesFile() != null) {
            Properties gp = new Properties();
            gp.setProperty("BIBIGRID_MASTER", masterPublicIp);
            gp.setProperty("IdentityFile", conf.getIdentityFile().toString()); // TODO: used?
            gp.setProperty("clusterId", clusterId);
            if (conf.isAlternativeConfigFile()) {
                gp.setProperty("AlternativeConfigFile", conf.getAlternativeConfigPath());
            }
            try {
                gp.store(new FileOutputStream(conf.getGridPropertiesFile()), "Autogenerated by BiBiGrid");
            } catch (IOException e) {
                log.error(I, "Exception while creating grid properties file : " + e.getMessage());
            }
        }

        return true;
    }

    private void waitForStatusCheck(String type, List<Instance> instances) {
        log.info("Waiting for Status Checks on {} ...", type);
        for (Instance instance : instances) {
            do {
                InstanceInfo.Status status = instance.getStatus();
                log.debug("Status of {} instance: " + status, type);
                if (status == InstanceInfo.Status.RUNNING) {
                    break;
                } else {
                    log.info(V, "...");
                    sleep(10);
                }
            } while (true);
        }
        log.info(I, "Status checks successful.");
    }

    private List<Instance> waitForInstances(List<Operation> operations) {
        if (operations.isEmpty()) {
            log.error("No instances found");
            return new ArrayList<>();
        }

        List<Instance> returnList = new ArrayList<>();
        for (Operation operation : operations) {
            while (!operation.isDone()) {
                log.info(V, "...");
                sleep(1);
            }
            operation = operation.reload();
            if (operation.getErrors() == null) {
                returnList.add(compute.getInstance(InstanceId.of(conf.getAvailabilityZone(), operation.getTargetId())));
            } else {
                log.error("Creation of instance failed: {}", operation.getErrors());
                break;
            }
        }

        return returnList;
    }

    private void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException ie) {
            log.error("Thread.sleep interrupted!");
        }
    }

    private void configureMaster() {
        // TODO: stub
    }

    public Configuration getConfig() {
        return conf;
    }

    public Compute getCompute() {
        return compute;
    }

    String getClusterId() {
        return clusterId;
    }
}