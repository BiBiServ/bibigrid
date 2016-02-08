/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.meta.aws;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsResult;
import com.amazonaws.services.ec2.model.EbsBlockDevice;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceNetworkInterfaceSpecification;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.LaunchSpecification;
import com.amazonaws.services.ec2.model.ModifyInstanceAttributeRequest;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.RequestSpotInstancesRequest;
import com.amazonaws.services.ec2.model.RequestSpotInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.SpotInstanceType;
import com.amazonaws.services.ec2.model.SpotPlacement;
import com.amazonaws.services.ec2.model.Tag;
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
import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jsteiner
 */
public class CreateClusterAWS implements CreateCluster<CreateClusterAWS, CreateClusterEnvironmentAWS> {

    public static final Logger log = LoggerFactory.getLogger(CreateClusterAWS.class);
    public static final String PREFIX = "bibigrid-";
    public static final String SECURITY_GROUP_PREFIX = PREFIX + "sg-";

    public static final String MASTER_SSH_USER = "ubuntu";
    public static final String PLACEMENT_GROUP_PREFIX = PREFIX + "pg-";
    public static final String SUBNET_PREFIX = PREFIX + "subnet-";
    private AmazonEC2 ec2;

    /* Placementgroups */
    private Placement instancePlacement;
    private SpotPlacement spotInstancePlacement;

    private String base64MasterUserData;

    private InstanceNetworkInterfaceSpecification inis;
    private List<InstanceNetworkInterfaceSpecification> masterNetworkInterfaces, slaveNetworkInterfaces;
    private List<BlockDeviceMapping> masterDeviceMappings;
    private Tag bibigridid, username;
    private String clusterId;
    private DeviceMapper slaveDeviceMapper;
    private List<BlockDeviceMapping> slaveBlockDeviceMappings;

    Instance masterInstance;
    List<Instance> slaveInstances;

    private final Configuration config;

    private CreateClusterEnvironmentAWS environment;

    public CreateClusterAWS(Configuration conf) {
        this.config = conf;
    }

    @Override
    public CreateClusterEnvironmentAWS createClusterEnvironment() {
        ////////////////////////////////////////////////////////////////////////
        ///// create client and unique cluster-id //////////////////////////////
        ec2 = new AmazonEC2Client(this.config.getCredentials());
        ec2.setEndpoint("ec2." + this.config.getRegion() + ".amazonaws.com");

        // Cluster ID is a cut down base64 encoded version of a random UUID:
        UUID clusterIdUUID = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(clusterIdUUID.getMostSignificantBits());
        bb.putLong(clusterIdUUID.getLeastSignificantBits());
        String clusterIdBase64 = Base64.encodeBase64URLSafeString(bb.array()).replace("-", "").replace("_", "");
        int len = clusterIdBase64.length() >= 15 ? 15 : clusterIdBase64.length();
        clusterId = clusterIdBase64.substring(0, len);
        bibigridid = new Tag().withKey("bibigrid-id").withValue(clusterId);
        username = new Tag().withKey("user").withValue(config.getUser());
        log.debug("cluster id: {}", clusterId);

        return environment = new CreateClusterEnvironmentAWS(this);
    }

    @Override
    public CreateClusterAWS configureClusterMasterInstance() {
        // done for master. More volume description later when master is running

        ////////////////////////////////////////////////////////////////////////
        /////////////// preparing blockdevicemappings for master////////////////
        Map<String, String> masterSnapshotToMountPointMap = this.config.getMasterMounts();
        int ephemerals = config.getMasterInstanceType().getSpec().ephemerals;
        DeviceMapper masterDeviceMapper = new DeviceMapper(masterSnapshotToMountPointMap, ephemerals);
        masterDeviceMappings = new ArrayList<>();
        // create Volumes first
        if (!this.config.getMasterMounts().isEmpty()) {
            log.info(V, "Defining master volumes");
            masterDeviceMappings = createBlockDeviceMappings(masterDeviceMapper);
        }

        List<BlockDeviceMapping> ephemeralList = new ArrayList<>();
        for (int i = 0; i < this.config.getMasterInstanceType().getSpec().ephemerals; ++i) {
            BlockDeviceMapping temp = new BlockDeviceMapping();
            String virtualName = "ephemeral" + i;
            String deviceName = "/dev/sd" + ephemeral(i);
            temp.setVirtualName(virtualName);
            temp.setDeviceName(deviceName);
            ephemeralList.add(temp);
        }

        masterDeviceMappings.addAll(ephemeralList);

        base64MasterUserData = UserDataCreator.masterUserData(masterDeviceMapper, this.config, environment.getKeypair().getPrivateKey());

        log.info(V, "Master UserData:\n {}", new String(Base64.decodeBase64(base64MasterUserData)));
        //////////////////////////////////////////////////////////////////////////
        /////// create Placementgroup ////////////////////

        if (this.config.getMasterInstanceType().getSpec().clusterInstance) {
            if (config.isUseSpotInstances()) {
                spotInstancePlacement = new SpotPlacement(config.getAvailabilityZone());
                spotInstancePlacement.setGroupName(environment.getPlacementGroup());
            } else {
                instancePlacement = new Placement(this.config.getAvailabilityZone());
                instancePlacement.setGroupName(environment.getPlacementGroup());
            }
        }

        //////////////////////////////////////////////////////////////////////////
        /////// create NetworkInterfaceSpecification for MASTER instance with FIXED internal IP and public ip
        masterNetworkInterfaces = new ArrayList<>();

        inis = new InstanceNetworkInterfaceSpecification();
        inis.withPrivateIpAddress(environment.getMASTERIP())
                .withGroups(environment.getSecReqResult().getGroupId())
                .withAssociatePublicIpAddress(true)
                .withSubnetId(environment.getSubnet().getSubnetId())
                .withDeviceIndex(0);

        masterNetworkInterfaces.add(inis); // add eth0

        slaveNetworkInterfaces = new ArrayList<>();
        inis = new InstanceNetworkInterfaceSpecification();
        inis.withGroups(environment.getSecReqResult().getGroupId())
                .withSubnetId(environment.getSubnet().getSubnetId())
                .withDeviceIndex(0);
        slaveNetworkInterfaces.add(inis);

        return this;
    }

    @Override
    public CreateClusterAWS configureClusterSlaveInstance() {
        //now defining Slave Volumes
        Map<String, String> snapShotToSlaveMounts = this.config.getSlaveMounts();
        int ephemerals = config.getSlaveInstanceType().getSpec().ephemerals;
        slaveDeviceMapper = new DeviceMapper(snapShotToSlaveMounts, ephemerals);
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

        return this;
    }

    @Override
    public boolean launchClusterInstances() {
        log.info("Requesting master instance ...");

        if (config.isUseSpotInstances()) {
            RequestSpotInstancesRequest masterReq = new RequestSpotInstancesRequest();
            masterReq.withType(SpotInstanceType.OneTime)
                    .withInstanceCount(1)
                    .withLaunchGroup("lg_" + clusterId)
                    .withSpotPrice(Double.toString(config.getBidPrice()));

            LaunchSpecification masterLaunchSpecification = new LaunchSpecification();
            masterLaunchSpecification.withInstanceType(InstanceType.fromValue(this.config.getSlaveInstanceType().getValue()))
                    .withPlacement(spotInstancePlacement)
                    .withKeyName(this.config.getKeypair())
                    .withImageId(this.config.getMasterImage())
                    .withUserData(base64MasterUserData)
                    .withBlockDeviceMappings(masterDeviceMappings)
                    .withNetworkInterfaces(masterNetworkInterfaces);

            masterReq.setLaunchSpecification(masterLaunchSpecification);

            RequestSpotInstancesResult masterReqResult = ec2.requestSpotInstances(masterReq);

            List<SpotInstanceRequest> masterReqResponses = masterReqResult.getSpotInstanceRequests();
            // collect all spotInstanceRequestIds ...
            List<String> spotInstanceRequestIds = new ArrayList<>();

            for (SpotInstanceRequest requestResponse : masterReqResponses) {

                spotInstanceRequestIds.add(requestResponse.getSpotInstanceRequestId());

            }
            // Tag spotrequest
            CreateTagsRequest ctr = new CreateTagsRequest();
            ctr.withResources(spotInstanceRequestIds);
            ctr.withTags(bibigridid, username, new Tag().withKey("Name").withValue(PREFIX + "master-" + clusterId));
            ec2.createTags(ctr);
            // Wait for spot request finished  
            log.info("Waiting for master instance (spot request) to finish booting ...");
            masterInstance = waitForInstances(waitForSpotInstances(spotInstanceRequestIds)).get(0);
        } else {

            RunInstancesRequest masterReq = new RunInstancesRequest();
            masterReq.withInstanceType(InstanceType.fromValue(this.config.getMasterInstanceType().getValue()))
                    .withMinCount(1).withMaxCount(1)
                    .withPlacement(instancePlacement)
                    .withKeyName(this.config.getKeypair())
                    .withImageId(this.config.getMasterImage())
                    .withUserData(base64MasterUserData)
                    .withBlockDeviceMappings(masterDeviceMappings)
                    .withNetworkInterfaces(masterNetworkInterfaces);

            // mounting ephemerals
            RunInstancesResult masterReqResult = ec2.runInstances(masterReq);
            String masterReservationId = masterReqResult.getReservation().getReservationId();
            masterInstance = masterReqResult.getReservation().getInstances().get(0);
            log.info("Waiting for master instance to finish booting ...");

            /////////////////////////////////////////////
            //// Waiting for master instance to run ////
            masterInstance = waitForInstances(Arrays.asList(new String[]{masterInstance.getInstanceId()})).get(0);

        }
        log.info(I, "Master instance is now running!");

        ModifyInstanceAttributeRequest ia_req = new ModifyInstanceAttributeRequest();
        ia_req.setInstanceId(masterInstance.getInstanceId());
        ia_req.setSourceDestCheck(Boolean.FALSE);
        ec2.modifyInstanceAttribute(ia_req);

        ////////////////////////////////////
        //// Tagging Master with a name ////
        CreateTagsRequest masterNameTagRequest = new CreateTagsRequest();
        masterNameTagRequest.withResources(masterInstance.getInstanceId()).withTags(bibigridid, username, new Tag().withKey("Name").withValue(PREFIX + "master-" + clusterId));

        ec2.createTags(masterNameTagRequest);

        /*
         * Waiting for Status Checks to finish
         *
         */
        log.info("Waiting for Status Checks on master ...");
        do {
            DescribeInstanceStatusRequest request
                    = new DescribeInstanceStatusRequest();
            request.setInstanceIds((Arrays.asList(new String[]{masterInstance.getInstanceId()})));

            DescribeInstanceStatusResult response
                    = ec2.describeInstanceStatus(request);

            InstanceStatus status = response.getInstanceStatuses().get(0);
            String instanceStatus = status.getInstanceStatus().getStatus();
            String systemStatus = status.getSystemStatus().getStatus();
            log.debug("Status of master instance: " + instanceStatus + "," + systemStatus);
            if (instanceStatus.equalsIgnoreCase("ok") && systemStatus.equalsIgnoreCase("ok")) {
                break;
            } else {
                log.info(V, "...");
                sleep(10);
            }
        } while (true);
        log.info(I, "Status checks successful.");
        ////////////////////////////////////////////////////////////////////////
        ///// run slave instances and supply userdata //////////////////////////

        if (config.getSlaveInstanceCount() > 0) {

            String base64SlaveUserData = UserDataCreator.forSlave(masterInstance.getPrivateIpAddress(),
                    masterInstance.getPrivateDnsName(),
                    slaveDeviceMapper,
                    this.config,
                    environment.getKeypair().getPublicKey());

            log.info(V, "Slave Userdata:\n{}", new String(Base64.decodeBase64(base64SlaveUserData)));

            if (config.isUseSpotInstances()) {
                RequestSpotInstancesRequest slaveReq = new RequestSpotInstancesRequest();
                slaveReq.withType(SpotInstanceType.OneTime)
                        .withInstanceCount(config.getSlaveInstanceCount())
                        .withLaunchGroup("lg_" + clusterId)
                        .withSpotPrice(Double.toString(config.getBidPrice()));

                LaunchSpecification slaveLaunchSpecification = new LaunchSpecification();
                slaveLaunchSpecification.withInstanceType(InstanceType.fromValue(this.config.getSlaveInstanceType().getValue()))
                        .withPlacement(spotInstancePlacement)
                        .withKeyName(this.config.getKeypair())
                        .withImageId(this.config.getSlaveImage())
                        .withUserData(base64SlaveUserData)
                        .withBlockDeviceMappings(slaveBlockDeviceMappings)
                        .withNetworkInterfaces(slaveNetworkInterfaces);

                slaveReq.setLaunchSpecification(slaveLaunchSpecification);

                RequestSpotInstancesResult slaveReqResult = ec2.requestSpotInstances(slaveReq);

                List<SpotInstanceRequest> slaveReqResponses = slaveReqResult.getSpotInstanceRequests();
                // collect all spotInstanceRequestIds ...
                List<String> spotInstanceRequestIds = new ArrayList<>();

                for (SpotInstanceRequest requestResponse : slaveReqResponses) {

                    spotInstanceRequestIds.add(requestResponse.getSpotInstanceRequestId());

                }

                // tag spot requests (slave)
                CreateTagsRequest ctr = new CreateTagsRequest();
                ctr.withResources(spotInstanceRequestIds);
                ctr.withTags(bibigridid, username, new Tag().withKey("Name").withValue(PREFIX + "slave-" + clusterId));
                ec2.createTags(ctr);
                // wait for spot request (slave) finished
                slaveInstances = waitForInstances(waitForSpotInstances(spotInstanceRequestIds));

            } else {

                RunInstancesRequest slaveReq = new RunInstancesRequest();
                slaveReq.withInstanceType(InstanceType.fromValue(this.config.getSlaveInstanceType().getValue()))
                        .withMinCount(this.config.getSlaveInstanceCount())
                        .withMaxCount(this.config.getSlaveInstanceCount())
                        .withPlacement(instancePlacement)
                        .withKeyName(this.config.getKeypair())
                        .withImageId(this.config.getSlaveImage())
                        .withUserData(base64SlaveUserData)
                        .withBlockDeviceMappings(slaveBlockDeviceMappings)
                        .withNetworkInterfaces(slaveNetworkInterfaces);

                RunInstancesResult slaveReqResult = ec2.runInstances(slaveReq);
                String slaveReservationId = slaveReqResult.getReservation().getReservationId();
                // create a list of all slave instances
                List<String> slaveInstanceListIds = new ArrayList<>();
                for (Instance i : slaveReqResult.getReservation().getInstances()) {
                    slaveInstanceListIds.add(i.getInstanceId());
                }

                log.info("Waiting for slave instance(s) to finish booting ...");
                slaveInstances = waitForInstances(slaveInstanceListIds);

            }

            /////////////////////////////////////////////
            //// Waiting for master instance to run ////
            log.info(I, "Slave instance(s) is now running!");

            ////////////////////////////////////
            //// Tagging all slaves  with a name
            for (Instance si : slaveInstances) {
                CreateTagsRequest slaveNameTagRequest = new CreateTagsRequest();
                slaveNameTagRequest.withResources(si.getInstanceId())
                        .withTags(bibigridid, username, new Tag().withKey("Name").withValue(PREFIX + "slave-" + clusterId));
                ec2.createTags(slaveNameTagRequest);
            }
        } else {
            log.info("No Slave instance(s) requested !");

        }

        //////////////////////////////////
        ////// post configure master
        configureMaster();

        ////////////////////////////////////
        //// Human friendly output
        StringBuilder sb = new StringBuilder();
        sb.append("\n You might want to set the following environment variable:\n\n");
        sb.append("export BIBIGRID_MASTER=").append(masterInstance.getPublicIpAddress()).append("\n\n");
        sb.append("You can then log on the master node with:\n\n")
                .append("ssh -i ")
                .append(config.getIdentityFile())
                .append(" ubuntu@$BIBIGRID_MASTER\n\n");
        sb.append("The cluster id of your started cluster is : ")
                .append(clusterId)
                .append("\n\n");
        sb.append("The can easily terminate the cluster at any time with :\n")
                .append("./bibigrid -t ").append(clusterId).append(" ");
        if (getConfig().isAlternativeConfigFile()) {
            sb.append("-o ").append(config.getAlternativeConfigPath()).append(" ");
        }

        sb.append("\n");

        log.info(sb.toString());

        ////////////////////////////////////
        //// Grid Properties file
        if (getConfig().getGridPropertiesFile() != null) {
            Properties gp = new Properties();
            gp.setProperty("BIBIGRID_MASTER", masterInstance.getPublicIpAddress());
            gp.setProperty("IdentityFile", getConfig().getIdentityFile().toString());
            gp.setProperty("clusterId", clusterId);
            if (getConfig().isAlternativeConfigFile()) {
                gp.setProperty("AlternativeConfigFile", config.getAlternativeConfigPath());
            }
            try {
                gp.store(new FileOutputStream(getConfig().getGridPropertiesFile()), "Autogenerated by BiBiGrid");
            } catch (IOException e) {
                log.error(I, "Exception while creating grid properties file : " + e.getMessage());
            }
        }

        return true;
    }

    private void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException ie) {
            log.error("Thread.sleep interrupted!");
        }
    }

    /**
     * Takes a list of instance IDs as Strings and monitors their system status
     *
     * @param listOfInstances Returns a list of Instances when they have been
     * started.
     * @return
     */
    private List<Instance> waitForInstances(List<String> listOfInstances) {
        do {
            if (listOfInstances.isEmpty()) {
                log.error("No instances found");
                return new ArrayList<>();
            }
            DescribeInstancesRequest instanceDescrReq = new DescribeInstancesRequest();
            instanceDescrReq.setInstanceIds(listOfInstances);
            boolean allrunning = true;
            try {
                DescribeInstancesResult instanceDescrReqResult = ec2.describeInstances(instanceDescrReq);

                String state;
                for (Reservation v : instanceDescrReqResult.getReservations()) {
                    for (Instance e : v.getInstances()) {
                        state = e.getState().getName();
                        if (!state.equals(InstanceStateName.Running.toString())) {
                            log.debug(V, "ID " + e.getInstanceId() + "in state:" + state);
                            allrunning = false;
                            break;
                        }
                    }
                }
                if (allrunning) {
                    List<Instance> returnList = new ArrayList<>();
                    for (Reservation e : instanceDescrReqResult.getReservations()) {
                        returnList.addAll(e.getInstances());
                    }
                    return returnList;
                } else {
                    log.info(V, "...");
                    sleep(10);
                }

            } catch (AmazonServiceException e) {
                log.debug("{}", e);
                sleep(3);
            }
        } while (true);
    }

    /**
     * Get a lost of spotInstance IDs as Strings and monitors their spot request
     * status
     *
     *
     * @param listOfSpotInstances
     * @return
     */
    private List<String> waitForSpotInstances(List<String> listOfSpotInstances) {
        // Create a variable that will track whether there are any
        // requests still in the open state.
        boolean anyOpen;

        List<String> fullfilled = new ArrayList<>();

        do {
            // Create the describeRequest object with all of the request ids
            // to monitor (e.g. that we started).
            DescribeSpotInstanceRequestsRequest describeRequest = new DescribeSpotInstanceRequestsRequest();
            describeRequest.setSpotInstanceRequestIds(listOfSpotInstances);

            // Initialize the anyOpen variable to false - which assumes there
            // are no requests open unless we find one that is still open.
            anyOpen = false;

            try {
                // Retrieve all of the requests we want to monitor.
                DescribeSpotInstanceRequestsResult describeResult = ec2.describeSpotInstanceRequests(describeRequest);
                List<SpotInstanceRequest> describeResponses = describeResult.getSpotInstanceRequests();

                // Look through each request and determine if they are all in
                // the active state.
                for (SpotInstanceRequest describeResponse : describeResponses) {
                    // If the state is open, it hasn't changed since we attempted
                    // to request it. There is the potential for it to transition
                    // almost immediately to closed or cancelled so we compare
                    // against open instead of active.
                    if (describeResponse.getState().equals("open")) {
                        anyOpen = true;
                        break;
                    }

                }
            } catch (AmazonServiceException e) {
                // If we have an exception, ensure we don't break out of
                // the loop. This prevents the scenario where there was
                // blip on the wire.
                anyOpen = true;
            }

            try {
                // Sleep for 30 seconds.
                log.debug(V, "Wait for spot instance request finished!");
                Thread.sleep(30 * 1000);
            } catch (Exception e) {
                // Do nothing because it woke up early.
            }
        } while (anyOpen);

        // get all instance id's
        DescribeSpotInstanceRequestsRequest describeRequest = new DescribeSpotInstanceRequestsRequest();
        describeRequest.setSpotInstanceRequestIds(listOfSpotInstances);
        DescribeSpotInstanceRequestsResult describeResult = ec2.describeSpotInstanceRequests(describeRequest);
        List<SpotInstanceRequest> describeResponses = describeResult.getSpotInstanceRequests();
        for (SpotInstanceRequest describeResponse : describeResponses) {
            log.info(V, "{} : {}", describeResponse.getInstanceId(), describeResponse.getState());
            if (describeResponse.getState().equals("active")) {
                log.info(V, "{} - {}", describeResponse.getInstanceId(), describeResponse.getInstanceId());
                fullfilled.add(describeResponse.getInstanceId());
            }
        }

        return fullfilled;
    }

    private List<BlockDeviceMapping> createBlockDeviceMappings(DeviceMapper deviceMapper) {

        List<BlockDeviceMapping> mappings = new ArrayList<>();

        Map<String, String> snapshotToMountPointMap = deviceMapper.getSnapshotIdToMountPoint();
        for (Map.Entry<String, String> snapshotIdMountPoint : snapshotToMountPointMap.entrySet()) {
            try {

                BlockDeviceMapping blockDeviceMapping = new BlockDeviceMapping();
                blockDeviceMapping.setEbs(new EbsBlockDevice().withSnapshotId(DeviceMapper.stripSnapshotId(snapshotIdMountPoint.getKey())));
                blockDeviceMapping.setDeviceName(deviceMapper.getDeviceNameForSnapshotId(snapshotIdMountPoint.getKey()));

                mappings.add(blockDeviceMapping);

            } catch (AmazonServiceException ex) {
                log.debug("{}", ex.getMessage());

            }
        }
        return mappings;
    }

    public AmazonEC2 getEc2() {
        return ec2;
    }

    public Configuration getConfig() {
        return config;
    }

    public String getBase64MasterUserData() {
        return base64MasterUserData;
    }

    public Tag getBibigridid() {
        return bibigridid;
    }

    public String getClusterId() {
        return clusterId;
    }

    private void configureMaster() {
        JSch ssh = new JSch();
        JSch.setLogger(new JSchLogger());
        /*
         * Building Command
         */
        log.info("Now configuring ...");
        String execCommand = SshFactory.buildSshCommand(clusterId, getConfig(), masterInstance, slaveInstances);

        log.info(V, "Building SSH-Command : {}", execCommand);

        boolean uploaded = false;
        boolean configured = false;

        int ssh_attempts = 25; // @TODO attempts
        while (!configured && ssh_attempts > 0) {
            try {

                ssh.addIdentity(getConfig().getIdentityFile().toString());
                log.info("Trying to connect to master ({})...", ssh_attempts);
                Thread.sleep(4000);

                /*
                 * Create new Session to avoid packet corruption.
                 */
                Session sshSession = SshFactory.createNewSshSession(ssh, masterInstance.getPublicIpAddress(), MASTER_SSH_USER, getConfig().getIdentityFile());

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

    private char ephemeral(int i) {
        return (char) (i + 98);
    }

}
