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
import com.amazonaws.services.ec2.model.EbsBlockDevice;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceNetworkInterfaceSpecification;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.ModifyInstanceAttributeRequest;
import com.amazonaws.services.ec2.model.Placement;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import de.unibi.cebitec.bibigrid.meta.CreateCluster;
import de.unibi.cebitec.bibigrid.model.Configuration;
import de.unibi.cebitec.bibigrid.util.DeviceMapper;
import static de.unibi.cebitec.bibigrid.util.ImportantInfoOutputFilter.I;
import de.unibi.cebitec.bibigrid.util.InstanceInformation;
import de.unibi.cebitec.bibigrid.util.UserDataCreator;
import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;
import java.io.FileOutputStream;
import java.io.IOException;
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

    private Placement instancePlacement;
    private String base64MasterUserData;

    private InstanceNetworkInterfaceSpecification inis;
    private List<InstanceNetworkInterfaceSpecification> networkInterfaces;
    private List<BlockDeviceMapping> masterDeviceMappings;
    private Tag bibigridid, username;
    private String clusterId;
    private DeviceMapper slaveDeviceMapper;
    private List<BlockDeviceMapping> slaveBlockDeviceMappings;

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
        //now defining Slave Volumes
        Map<String, String> snapShotToSlaveMounts = this.config.getSlaveMounts();
        slaveDeviceMapper = new DeviceMapper(snapShotToSlaveMounts);
        slaveBlockDeviceMappings = new ArrayList<>();
        // Create a list of slaves first. Associate with slave instance-ids later
        if (!snapShotToSlaveMounts.isEmpty()) {
            log.info(V, "Defining slave volumes");

            slaveBlockDeviceMappings = createBlockDeviceMappings(slaveDeviceMapper);
        }
        ////////////////////////////////////////////////////////////////////////
        /////////////// preparing blockdevicemappings for master////////////////

        Map<String, String> masterSnapshotToMountPointMap = this.config.getMasterMounts();
        DeviceMapper masterDeviceMapper = new DeviceMapper(masterSnapshotToMountPointMap);
        masterDeviceMappings = new ArrayList<>();
        // create Volumes first
        if (!this.config.getMasterMounts().isEmpty()) {
            log.info(V, "Defining master volumes");
            masterDeviceMappings = createBlockDeviceMappings(masterDeviceMapper);
        }

        String[] ephemerals = {"b", "c", "d", "e"};
        List<BlockDeviceMapping> ephemeralList = new ArrayList<>();
        for (int i = 0; i < this.config.getMasterInstanceType().getSpec().ephemerals; ++i) {
            BlockDeviceMapping temp = new BlockDeviceMapping();
            String virtualName = "ephemeral" + i;
            String deviceName = "/dev/sd" + ephemerals[i];
            temp.setVirtualName(virtualName);
            temp.setDeviceName(deviceName);

            ephemeralList.add(temp);
        }

        masterDeviceMappings.addAll(ephemeralList);

        base64MasterUserData = UserDataCreator.masterUserData(masterDeviceMapper, this.config, environment.getKeypair().getPrivateKey());

        log.info(V, "Master UserData:\n {}", base64MasterUserData);
        //////////////////////////////////////////////////////////////////////////
        /////// run master instance, tag it and wait for boot ////////////////////
        log.info("Requesting master instance ...");

        instancePlacement = new Placement(this.config.getAvailabilityZone());

        if (this.config.getMasterInstanceType().getSpec().clusterInstance) {
            instancePlacement.setGroupName(environment.getPlacementGroup());
        }

        //////////////////////////////////////////////////////////////////////////
        /////// create NetworkInterfaceSpecification for MASTER instance with FIXED internal IP and public ip
        networkInterfaces = new ArrayList<>();

        inis = new InstanceNetworkInterfaceSpecification();
        inis.withPrivateIpAddress(environment.getMASTERIP())
                .withGroups(environment.getSecReqResult().getGroupId())
                .withAssociatePublicIpAddress(true)
                .withSubnetId(environment.getSubnet().getSubnetId())
                .withDeviceIndex(0);

        networkInterfaces.add(inis); // add eth0

        return this;
    }

    @Override
    public CreateClusterAWS configureClusterSlaveInstance() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean launchClusterInstances() {
        RunInstancesRequest masterReq = new RunInstancesRequest();
        masterReq.withInstanceType(InstanceType.fromValue(this.config.getMasterInstanceType().getValue()))
                .withMinCount(1).withMaxCount(1).withPlacement(instancePlacement)
                .withKeyName(this.config.getKeypair())
                .withImageId(this.config.getMasterImage())
                .withUserData(base64MasterUserData)
                .withBlockDeviceMappings(masterDeviceMappings)
                .withNetworkInterfaces(networkInterfaces);

        // mounting ephemerals
        RunInstancesResult masterReqResult = ec2.runInstances(masterReq);
        String masterReservationId = masterReqResult.getReservation().getReservationId();
        Instance masterInstance = masterReqResult.getReservation().getInstances().get(0);
        log.info("Waiting for master instance to finish booting ...");

        /////////////////////////////////////////////
        //// Waiting for master instance to run ////
        masterInstance = waitForInstances(Arrays.asList(new String[]{masterInstance.getInstanceId()})).get(0);
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

            log.info(V, "Slave Userdata:\n{}", base64SlaveUserData);

            RunInstancesRequest slaveReq = new RunInstancesRequest();
            slaveReq.withInstanceType(InstanceType.fromValue(this.config.getSlaveInstanceType().getValue()))
                    .withMinCount(this.config.getSlaveInstanceCount())
                    .withMaxCount(this.config.getSlaveInstanceCount())
                    .withPlacement(instancePlacement)
                    .withKeyName(this.config.getKeypair())
                    .withImageId(this.config.getSlaveImage())
                    .withUserData(base64SlaveUserData)
                    .withBlockDeviceMappings(slaveBlockDeviceMappings)
                    .withSubnetId(environment.getSubnet().getSubnetId())
                    .withSecurityGroupIds(environment.getSecReqResult().getGroupId());

            RunInstancesResult slaveReqResult = ec2.runInstances(slaveReq);
            String slaveReservationId = slaveReqResult.getReservation().getReservationId();
            // create a list of all slave instances
            List<String> slaveInstanceListIds = new ArrayList<>();
            for (Instance i : slaveReqResult.getReservation().getInstances()) {
                slaveInstanceListIds.add(i.getInstanceId());
            }
            log.info("Waiting for slave instance(s) to finish booting ...");
            List<Instance> slaveInstances = waitForInstances(slaveInstanceListIds);

            /////////////////////////////////////////////
            //// Waiting for master instance to run ////
            log.info(I, "Slave instance(s) is now running!");

            ////////////////////////////////////
            //// Tagging all slaves  with a name
            for (Instance si : slaveInstances) {
                CreateTagsRequest slaveNameTagRequest = new CreateTagsRequest();
                slaveNameTagRequest.withResources(si.getInstanceId())
                        .withTags(bibigridid, new Tag().withKey("Name").withValue(PREFIX + "slave-" + clusterId));
                ec2.createTags(slaveNameTagRequest);
            }
        } else {
            log.info("No Slave instance(s) requested !");

        }

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

}
