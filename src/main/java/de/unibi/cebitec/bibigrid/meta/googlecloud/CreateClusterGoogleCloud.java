package de.unibi.cebitec.bibigrid.meta.googlecloud;

import com.google.cloud.compute.*;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import de.unibi.cebitec.bibigrid.meta.CreateCluster;
import de.unibi.cebitec.bibigrid.model.Configuration;
import de.unibi.cebitec.bibigrid.util.DeviceMapper;
import de.unibi.cebitec.bibigrid.util.JSchLogger;
import de.unibi.cebitec.bibigrid.util.SshFactory;
import de.unibi.cebitec.bibigrid.util.UserDataCreator;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
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
    static final String SUBNET_PREFIX = PREFIX + "subnet-";
    static final String SECURITY_GROUP_PREFIX = PREFIX + "sg-";
    private static final String MASTER_SSH_USER = "ubuntu";
    private final Configuration conf;

    private Compute compute;
    private Instance masterInstance;
    private List<Instance> slaveInstances;
    private String base64MasterUserData;
    private List<NetworkInterface> masterNetworkInterfaces, slaveNetworkInterfaces;
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
        // All resource ids must be lower case!
        clusterId = clusterIdBase64.substring(0, len).toLowerCase(Locale.US);
        bibigridid = "bibigrid-id-" + clusterId;
        username = "user--" + conf.getUser();
        log.debug("cluster id: {}", clusterId);

        return environment = new CreateClusterEnvironmentGoogleCloud(this);
    }

    public CreateClusterGoogleCloud configureClusterMasterInstance() {
        // done for master. More volume description later when master is running

        // preparing blockdevicemappings for master
        Map<String, String> masterSnapshotToMountPointMap = conf.getMasterMounts();
        int ephemerals = conf.getMasterInstanceType().getSpec().ephemerals;
        DeviceMapper masterDeviceMapper = new DeviceMapper(conf.getMode(), masterSnapshotToMountPointMap, ephemerals);

        base64MasterUserData = UserDataCreator.masterUserData(masterDeviceMapper, conf, environment.getKeypair());

        log.info(V, "Master UserData:\n {}", new String(Base64.decodeBase64(base64MasterUserData)));

        // create NetworkInterfaceSpecification for MASTER instance with FIXED internal IP and public ip
        masterNetworkInterfaces = new ArrayList<>();

        NetworkInterface.Builder inis = NetworkInterface.newBuilder(environment.getSubnet().getNetwork());
        inis.setSubnetwork(environment.getSubnet().getSubnetworkId())
                .setAccessConfigurations(NetworkInterface.AccessConfig.newBuilder()
                        .setType(NetworkInterface.AccessConfig.Type.ONE_TO_ONE_NAT)
                        .setName("external-nat")
                        .build());
        // Currently only accessible through reflection. Should be public according to docs...
        try {
            Method m = NetworkInterface.Builder.class.getDeclaredMethod("setNetworkIp", String.class);
            m.setAccessible(true);
            m.invoke(inis, environment.getMasterIP());
        } catch (Exception e) {
            e.printStackTrace();
        }

        masterNetworkInterfaces.add(inis.build()); // add eth0

        slaveNetworkInterfaces = new ArrayList<>();
        inis = NetworkInterface.newBuilder(environment.getSubnet().getNetwork());
        inis.setSubnetwork(environment.getSubnet().getSubnetworkId());
        // TODO: conf.isPublicSlaveIps() => private-ip-google-accesss?
        slaveNetworkInterfaces.add(inis.build());

        return this;
    }

    public CreateClusterGoogleCloud configureClusterSlaveInstance() {
        //now defining Slave Volumes
        Map<String, String> snapShotToSlaveMounts = conf.getSlaveMounts();
        int ephemerals = conf.getSlaveInstanceType().getSpec().ephemerals;
        slaveDeviceMapper = new DeviceMapper(conf.getMode(), snapShotToSlaveMounts, ephemerals);
        return this;
    }

    public boolean launchClusterInstances() {
        log.info("Requesting master instance ...");

        String zone = conf.getAvailabilityZone();
        String masterInstanceName = PREFIX + "master-" + clusterId;
        InstanceInfo.Builder masterBuilder = GoogleCloudUtils.getInstanceBuilder(zone, masterInstanceName,
                conf.getMasterInstanceType().getValue())
                .setNetworkInterfaces(masterNetworkInterfaces)
                .setTags(Tags.of(bibigridid, username, "name--" + masterInstanceName))
                .setMetadata(Metadata.newBuilder().add("startup-script", base64MasterUserData).build());
        GoogleCloudUtils.attachDisks(compute, masterBuilder, conf.getMasterImage(), zone, conf.getMasterMounts());
        GoogleCloudUtils.setInstanceSchedulingOptions(masterBuilder, conf.isUseSpotInstances());

        // Waiting for master instance to run
        log.info("Waiting for master instance to finish booting ...");
        Operation createMasterOperation = compute.create(masterBuilder.build());
        masterInstance = waitForInstances(Collections.singletonList(InstanceId.of(zone, masterInstanceName)),
                Collections.singletonList(createMasterOperation)).get(0);
        log.info(I, "Master instance is now running!");

        waitForMasterStatusCheck(Collections.singletonList(masterInstance));

        String masterPrivateIp = GoogleCloudUtils.getInstancePrivateIp(masterInstance);
        String masterPublicIp = GoogleCloudUtils.getInstancePublicIp(masterInstance);
        String masterDnsName = GoogleCloudUtils.getInstanceFQDN(masterInstance);

        // run slave instances and supply userdata
        if (conf.getSlaveInstanceCount() > 0) {
            String base64SlaveUserData = UserDataCreator.forSlave(masterPrivateIp, masterDnsName, slaveDeviceMapper,
                    conf, environment.getKeypair());
            // Google doesn't use base64 encoded startup scripts. Just use plain text
            base64SlaveUserData = new String(Base64.decodeBase64(base64SlaveUserData));
            log.info(V, "Slave Userdata:\n{}", base64SlaveUserData);

            List<InstanceId> slaveInstanceIds = new ArrayList<>();
            List<Operation> slaveInstanceOperations = new ArrayList<>();
            for (int i = 0; i < conf.getSlaveInstanceCount(); i++) {
                String slaveInstanceName = PREFIX + "slave-" + clusterId;
                String slaveInstanceId = PREFIX + "slave" + i + "-" + clusterId;
                InstanceInfo.Builder slaveBuilder = GoogleCloudUtils.getInstanceBuilder(zone, slaveInstanceId,
                        conf.getSlaveInstanceType().getValue())
                        .setNetworkInterfaces(slaveNetworkInterfaces)
                        .setTags(Tags.of(bibigridid, username, "name--" + slaveInstanceName))
                        .setMetadata(Metadata.newBuilder().add("startup-script", base64SlaveUserData).build());
                GoogleCloudUtils.attachDisks(compute, slaveBuilder, conf.getSlaveImage(), zone, conf.getSlaveMounts());
                GoogleCloudUtils.setInstanceSchedulingOptions(masterBuilder, conf.isUseSpotInstances());

                Operation createSlaveOperation = compute.create(slaveBuilder.build());
                slaveInstanceIds.add(InstanceId.of(zone, slaveInstanceId));
                slaveInstanceOperations.add(createSlaveOperation);
            }
            log.info("Waiting for slave instance(s) to finish booting ...");
            slaveInstances = waitForInstances(slaveInstanceIds, slaveInstanceOperations);
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
            gp.setProperty("IdentityFile", conf.getIdentityFile().toString());
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

    private void waitForMasterStatusCheck(List<Instance> instances) {
        log.info("Waiting for Status Checks on master ...");
        for (Instance instance : instances) {
            do {
                InstanceInfo.Status status = instance.getStatus();
                log.debug("Status of master instance: " + status);
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

    private List<Instance> waitForInstances(List<InstanceId> instanceIds, List<Operation> operations) {
        if (instanceIds.isEmpty() || operations.isEmpty() || instanceIds.size() != operations.size()) {
            log.error("No instances found");
            return new ArrayList<>();
        }

        List<Instance> returnList = new ArrayList<>();
        for (int i = 0; i < instanceIds.size(); i++) {
            Operation operation = operations.get(i);
            while (!operation.isDone()) {
                log.info(V, "...");
                sleep(1);
            }
            operation = operation.reload();
            if (operation.getErrors() == null) {
                returnList.add(compute.getInstance(instanceIds.get(i)));
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
        JSch ssh = new JSch();
        JSch.setLogger(new JSchLogger());

        // Building Command
        log.info("Now configuring ...");
        String execCommand = SshFactory.buildSshCommandGoogleCloud(clusterId, getConfig(), masterInstance, slaveInstances);
        log.info(V, "Building SSH-Command : {}", execCommand);
        boolean configured = false;
        int ssh_attempts = 25; // TODO attempts
        while (!configured && ssh_attempts > 0) {
            try {
                ssh.addIdentity(getConfig().getIdentityFile().toString());
                log.info("Trying to connect to master ({})...", ssh_attempts);
                Thread.sleep(4000);

                // Create new Session to avoid packet corruption.
                Session sshSession = SshFactory.createNewSshSession(ssh,
                        masterInstance.getNetworkInterfaces().get(0)
                                .getAccessConfigurations().get(0)
                                .getNatIp(),
                        MASTER_SSH_USER, getConfig().getIdentityFile());

                // Start connect attempt
                //noinspection ConstantConditions
                sshSession.connect();
                log.info("Connected to master!");

                ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");
                BufferedReader stdout = new BufferedReader(new InputStreamReader(channel.getInputStream()));
                BufferedReader stderr = new BufferedReader(new InputStreamReader(channel.getErrStream()));
                channel.setCommand(execCommand);

                log.info(V, "Connecting ssh channel...");
                channel.connect();

                String lineout, lineerr = null;
                while (((lineout = stdout.readLine()) != null) || ((lineerr = stderr.readLine()) != null)) {
                    if (lineout != null) {
                        if (lineout.contains("CONFIGURATION_FINISHED")) {
                            configured = true;
                        }
                        log.info(V, "SSH: {}", lineout);
                    }
                    if (lineerr != null && !configured) {
                        log.error(V, "SSH: {}", lineerr);
                    }
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
            } catch (InterruptedException ex) {
                log.error("Interrupted ...");
            }
        }
        log.info(I, "Master instance has been configured.");
    }

    Configuration getConfig() {
        return conf;
    }

    public Compute getCompute() {
        return compute;
    }

    String getClusterId() {
        return clusterId;
    }
}