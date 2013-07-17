package de.unibi.cebitec.bibigrid.ctrl;

import de.unibi.cebitec.bibigrid.util.DeviceMapper;
import de.unibi.cebitec.bibigrid.util.UserDataCreator;
import de.unibi.cebitec.bibigrid.util.SshFactory;
import de.unibi.cebitec.bibigrid.util.InstanceInformation;
import de.unibi.cebitec.bibigrid.util.JSchLogger;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import de.unibi.cebitec.bibigrid.StartUpOgeCluster;
import de.unibi.cebitec.bibigrid.exc.IntentNotConfiguredException;
import de.unibi.cebitec.bibigrid.model.CurrentClusters;
import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;
import static de.unibi.cebitec.bibigrid.util.ImportantInfoOutputFilter.I;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateIntent extends Intent {

    public static final Logger log = LoggerFactory.getLogger(CreateIntent.class);
    public static final String SECURITY_GROUP_PREFIX = "bibigrid-";
    public static final String MASTER_SSH_USER = "ubuntu";
    public static final String PLACEMENT_GROUP_PREFIX = "bibigrid-pg-";
    private AmazonEC2 ec2;

    @Override
    public String getCmdLineOption() {
        return "c";
    }

    @Override
    public List<String> getRequiredOptions() {
        return Arrays.asList(new String[]{"m", "M", "s", "S", "n", "k", "i", "e", "a", "z"});
    }

    @Override
    public boolean execute() throws IntentNotConfiguredException {
        if (getConfiguration() == null) {
            throw new IntentNotConfiguredException();
        }
        try {
            if (!runInstances()) {
                log.error(StartUpOgeCluster.ABORT_WITH_INSTANCES_RUNNING);
                Intent cleanupIntent = new TerminateIntent();
                cleanupIntent.setConfiguration(getConfiguration());
//                cleanupIntent.execute();
                return false;
            }
        } catch (AmazonClientException ace) {
            log.error("{}", ace);
            return false;
        }
        return true;
    }

    private boolean runInstances() throws AmazonClientException {

        ////////////////////////////////////////////////////////////////////////
        ///// create client and unique cluster-id //////////////////////////////

        ec2 = new AmazonEC2Client(this.getConfiguration().getCredentials());
        ec2.setEndpoint(this.getConfiguration().getEndpoint());
        String clusterId = new StringBuffer(Long.toHexString(Calendar.getInstance().getTimeInMillis())).reverse().toString();
        log.debug("cluster id: {}", clusterId);


        ////////////////////////////////////////////////////////////////////////
        ///// create security group with full internal access / ssh from outside

        log.info(V, "Creating security group...");
        CreateSecurityGroupRequest secReq = new CreateSecurityGroupRequest();
        secReq.withGroupName(SECURITY_GROUP_PREFIX + clusterId).
                withDescription(clusterId);
        CreateSecurityGroupResult secReqResult = ec2.createSecurityGroup(secReq);
        log.debug("security group id: {}", secReqResult.getGroupId());

        UserIdGroupPair secGroupSelf = new UserIdGroupPair().withGroupId(secReqResult.getGroupId());

        IpPermission secGroupAccessSsh = new IpPermission();
        secGroupAccessSsh.withIpProtocol("tcp").withFromPort(22).withToPort(22).withIpRanges("0.0.0.0/0");
        IpPermission secGroupSelfAccessTcp = new IpPermission();
        secGroupSelfAccessTcp.withIpProtocol("tcp").withFromPort(0).withToPort(65535).withUserIdGroupPairs(secGroupSelf);
        IpPermission secGroupSelfAccessUdp = new IpPermission();
        secGroupSelfAccessUdp.withIpProtocol("udp").withFromPort(0).withToPort(65535).withUserIdGroupPairs(secGroupSelf);
        IpPermission secGroupSelfAccessIcmp = new IpPermission();
        secGroupSelfAccessIcmp.withIpProtocol("icmp").withFromPort(-1).withUserIdGroupPairs(secGroupSelf);


        List<IpPermission> allIpPermissions = new ArrayList<>();
        allIpPermissions.add(secGroupAccessSsh);
        allIpPermissions.add(secGroupSelfAccessTcp);
        allIpPermissions.add(secGroupSelfAccessUdp);
        allIpPermissions.add(secGroupSelfAccessIcmp);
        for (int port : this.getConfiguration().getPorts()) {
            IpPermission additionalPortTcp = new IpPermission();
            additionalPortTcp.withIpProtocol("tcp").withFromPort(port).withToPort(port).withIpRanges("0.0.0.0/0");
            allIpPermissions.add(additionalPortTcp);
            IpPermission additionalPortUdp = new IpPermission();
            additionalPortUdp.withIpProtocol("udp").withFromPort(port).withToPort(port).withIpRanges("0.0.0.0/0");
            allIpPermissions.add(additionalPortUdp);
        }

        AuthorizeSecurityGroupIngressRequest ruleChangerReq = new AuthorizeSecurityGroupIngressRequest();
        ruleChangerReq.withGroupId(secReqResult.getGroupId()).withIpPermissions(allIpPermissions);
        ec2.authorizeSecurityGroupIngress(ruleChangerReq);
        ////////////////////////////////////////////////////////////////////////
        /////////// Create the volume for the master from snapshot /////////////
        String placementGroup = PLACEMENT_GROUP_PREFIX + clusterId;

        ec2.createPlacementGroup(new CreatePlacementGroupRequest(placementGroup, PlacementStrategy.Cluster));

        DeviceMapper masterDeviceMapper = new DeviceMapper(this.getConfiguration().getMasterMounts());

        List<BlockDeviceMapping> masterDeviceMappings = new ArrayList<>();
        // create Volumes first
        if (!this.getConfiguration().getMasterMounts().isEmpty()) {
            log.info(V, "Defining master volumes");
            masterDeviceMappings = createBlockDeviceMappings(masterDeviceMapper);
        }

        String[] ephemerals = {"b", "c", "d", "e"};
        List<BlockDeviceMapping> ephemeralList = new ArrayList<>();
        for (int i = 0; i < InstanceInformation.getSpecs(this.getConfiguration().getMasterInstanceType()).ephemerals; ++i) {
            BlockDeviceMapping temp = new BlockDeviceMapping();
            String virtualName = "ephemeral" + i;
            String deviceName = "/dev/sd" + ephemerals[i];
            temp.setVirtualName(virtualName);
            temp.setDeviceName(deviceName);

            ephemeralList.add(temp);
        }

        masterDeviceMappings.addAll(ephemeralList);
        // done for master. More volume description later when master is running
        //now creating Slave Volumes


        Map<String, String> snapShotToSlaveMounts = this.getConfiguration().getSlaveMounts();
        DeviceMapper slaveDeviceMapper = new DeviceMapper(snapShotToSlaveMounts);
        List<BlockDeviceMapping> slaveBlockDeviceMappings = new ArrayList<>();

        List<BlockDeviceMapping> slaveEphemeralList = new ArrayList<>();

        for (int i = 0; i < InstanceInformation.getSpecs(this.getConfiguration().getSlaveInstanceType()).ephemerals; ++i) {
            BlockDeviceMapping temp = new BlockDeviceMapping();
            String virtualName = "ephemeral" + i;
            String deviceName = "/dev/sd" + ephemerals[i];
            temp.setVirtualName(virtualName);
            temp.setDeviceName(deviceName);

            slaveEphemeralList.add(temp);
        }
        // Create a list of slaves first. Associate with slave instance-ids later
        if (!snapShotToSlaveMounts.isEmpty()) {
            log.info(V, "Defining slave volumes");

            slaveBlockDeviceMappings = createBlockDeviceMappings(slaveDeviceMapper);


        }
        slaveBlockDeviceMappings.addAll(slaveEphemeralList);
        ////////////////////////////////////////////////////////////////////////
        ///// run master instance, tag it and wait for boot ////////////////////

        String base64MasterUserData = UserDataCreator.masterUserData(InstanceInformation.getSpecs(this.getConfiguration().
                getMasterInstanceType()).ephemerals, this.getConfiguration().getNfsShares(), masterDeviceMapper,this.getConfiguration());
        Placement instancePlacement = new Placement(this.getConfiguration().getAvailabilityZone());

        if (InstanceInformation.getSpecs(this.getConfiguration().getMasterInstanceType()).clusterInstance) {
            
            if (this.getConfiguration().getMasterInstanceType().equals(this.getConfiguration().getSlaveInstanceType())) {
                instancePlacement.setGroupName(placementGroup);
                log.info("Cluster will be launched in placement group " + placementGroup);
            } 

        }
        log.info("Requesting master instance ...");
        RunInstancesRequest masterReq = new RunInstancesRequest();
        masterReq.withInstanceType(this.getConfiguration().getMasterInstanceType())
                .withMinCount(1)
                .withMaxCount(1)
                .withPlacement(instancePlacement)
                .withSecurityGroupIds(secReqResult.getGroupId())
                .withKeyName(this.getConfiguration().getKeypair())
                .withImageId(this.getConfiguration().getMasterImage())
                .withUserData(base64MasterUserData).withBlockDeviceMappings(masterDeviceMappings);

        // mounting ephemerals


        RunInstancesResult masterReqResult = ec2.runInstances(masterReq);
        String masterReservationId = masterReqResult.getReservation().getReservationId();
        Instance masterInstance = masterReqResult.getReservation().getInstances().get(0);


        log.info("Waiting for master instance to finish booting ...");

        /////////////////////////////////////////////
        //// Waiting for master instance to run ////

        masterInstance = waitForInstances(Arrays.asList(new String[]{masterInstance.getInstanceId()})).get(0);
        log.info(I, "Master instance is now running!");

        ////////////////////////////////////
        //// Tagging Master with a name ////

        CreateTagsRequest masterNameTagRequest = new CreateTagsRequest();
        masterNameTagRequest.withResources(masterInstance.getInstanceId()).withTags(new Tag().withKey("Name").withValue("master-" + clusterId));


        ec2.createTags(masterNameTagRequest);

        /*
         * Waiting for Status Checks to finish
         *
         */
        log.info("Waiting for Status Checks on master ...");
        do {
            DescribeInstanceStatusRequest request =
                    new DescribeInstanceStatusRequest();
            request.setInstanceIds((Arrays.asList(new String[]{masterInstance.getInstanceId()})));

            DescribeInstanceStatusResult response =
                    ec2.describeInstanceStatus(request);

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
        log.info(I, "Status checks done");
        ////////////////////////////////////////////////////////////////////////
        ///// run slave instances and supply userdata //////////////////////////

        String base64SlaveUserData = UserDataCreator.forSlave(masterInstance.getPrivateIpAddress(), masterInstance.getPrivateDnsName(), slaveDeviceMapper,
                this.getConfiguration().getNfsShares(), InstanceInformation.getSpecs(this.getConfiguration().
                getSlaveInstanceType()).ephemerals);

        RunInstancesRequest slaveReq = new RunInstancesRequest();
        slaveReq.withInstanceType(this.getConfiguration().getSlaveInstanceType())
                .withMinCount(this.getConfiguration().getSlaveInstanceCount())
                .withMaxCount(this.getConfiguration().getSlaveInstanceCount())
                .withPlacement(instancePlacement).withSecurityGroupIds(secReqResult.getGroupId()).withKeyName(this.getConfiguration().getKeypair()).withUserData(base64SlaveUserData).withImageId(this.getConfiguration().getSlaveImage()).withBlockDeviceMappings(slaveBlockDeviceMappings);

        String slaveReservationId = "";
        List<Instance> slaveInstances = new ArrayList<>();
        if (this.getConfiguration().getSlaveInstanceCount() > 0) {
            RunInstancesResult slaveReqResult = ec2.runInstances(slaveReq);
            slaveReservationId = slaveReqResult.getReservation().getReservationId();
            slaveInstances = slaveReqResult.getReservation().getInstances();


            log.info("Waiting for slave instances to finish booting ...");

            ///////////////////////////////////////////////////////////////////////////
            ///////////////// Waiting for Slave instances to run //////////////////////

            List<String> slaveIds = new ArrayList<>();
            for (Instance e : slaveInstances) {
                slaveIds.add(e.getInstanceId());
            }

            slaveInstances = waitForInstances(slaveIds);
        }
        log.info(I, "All slave instances are running now!");



        log.debug("master reservation: {}   slave reservation: {}   clusterId: {}    slaveCount: {}",
                masterReservationId, slaveReservationId, clusterId, this.getConfiguration().getSlaveInstanceCount());

        CurrentClusters.addCluster(masterReservationId, slaveReservationId, clusterId, this.getConfiguration().getSlaveInstanceCount());


        JSch ssh = new JSch();
        JSch.setLogger(new JSchLogger());

        /*
         * Building Command
         */
        log.info("Now configuring ...");
        String execCommand = SshFactory.buildSshCommand(masterInstance, slaveInstances, this.getConfiguration());
        boolean configured = false;
        while (!configured) {
            try {

                ssh.addIdentity(this.getConfiguration().getIdentityFile().toString());
                sleep(10);
                log.info(V, "Registering slaves at master instance...");

                /*
                 * Create new Session to avoid packet corruption.
                 */
                Session sshSession = SshFactory.createNewSshSession(ssh, masterInstance.getPublicDnsName(), MASTER_SSH_USER, this.getConfiguration().getIdentityFile());




                /*
                 * Start connect attempt
                 */
                sshSession.connect(5000);



                ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");

                //channel.setOutputStream(System.out);
                // channel.setErrStream(System.out);

                InputStream in = channel.getInputStream();



                channel.setCommand(execCommand);

                log.debug("Connecting ssh channel...");
                channel.connect(5000);
                int attempts = 0;
                byte[] tmp = new byte[1024];
                while (true) {
                    while (in.available() > 0) {
                        int i = in.read(tmp, 0, 1024);
                        if (i <= 0) {
                            break;
                        }
//                        log.info("SSH: {}", new String(tmp, 0, i));
                    }
                    if (channel.isClosed()) {

                        log.info("SSH: exit-status: {}", channel.getExitStatus());
                        configured = true;
                        break;
                    }
                    if (in.available() <= 0 && !channel.isClosed()) {
                        log.info(V, "...");
                        attempts++;
                        if (attempts > 3) {
                            in.close();
                            log.debug("Too many attempts. Retrying...");
                            channel.disconnect();
                            sshSession.disconnect();
                            break;
                        }
                    }
                    sleep(2);
                }
                if (attempts <= 3 || configured) {
                    channel.disconnect();
                    sshSession.disconnect();
                }


            } catch (IOException | JSchException e) {
                log.warn("SSH: {} ... retrying", e.getMessage());
            }
        }
        log.info(I, "Master instance has been configured.");
        log.info("Access master at:  {}", masterInstance.getPublicDnsName());

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
            DescribeInstancesRequest instanceDescrReq = new DescribeInstancesRequest();
            instanceDescrReq.setInstanceIds(listOfInstances);
            boolean allrunning = true;
            try {
                DescribeInstancesResult instanceDescrReqResult = ec2.describeInstances(instanceDescrReq);

                String state;
                for (Instance e : instanceDescrReqResult.getReservations().get(0).getInstances()) {
                    state = e.getState().getName();
                    if (!state.equals(InstanceStateName.Running.toString())) {
                        allrunning = false;
                        break;
                    }
                }

                if (allrunning) {
                    return instanceDescrReqResult.getReservations().get(0).getInstances();
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
                blockDeviceMapping.setEbs(new EbsBlockDevice().withSnapshotId(snapshotIdMountPoint.getKey()));
                blockDeviceMapping.setDeviceName(deviceMapper.getDeviceNameForSnapshotId(snapshotIdMountPoint.getKey()));

                mappings.add(blockDeviceMapping);


            } catch (AmazonServiceException ex) {
                log.debug("{}", ex.getMessage());

            }
        }
        return mappings;
    }
}
