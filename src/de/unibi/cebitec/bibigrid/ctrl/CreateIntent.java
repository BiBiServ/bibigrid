package de.unibi.cebitec.bibigrid.ctrl;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.InstanceMonitoring;
import com.amazonaws.services.autoscaling.model.*;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.*;
import com.jcraft.jsch.*;
import de.unibi.cebitec.bibigrid.StartUpOgeCluster;
import de.unibi.cebitec.bibigrid.exc.IntentNotConfiguredException;
import de.unibi.cebitec.bibigrid.model.CurrentClusters;
import static de.unibi.cebitec.bibigrid.util.ImportantInfoOutputFilter.I;
import de.unibi.cebitec.bibigrid.util.*;
import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.*;
import org.apache.commons.codec.binary.Base64;
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
        return Arrays.asList(new String[]{"m", "M", "s", "S", "n", "u", "k", "i", "e", "a", "z", "g", "r", "b"});
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
                cleanupIntent.execute();
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
        ec2.setEndpoint("ec2." + this.getConfiguration().getRegion() + ".amazonaws.com");

        // Cluster ID is a cut down base64 encoded version of a random UUID:
        UUID clusterIdUUID = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(clusterIdUUID.getMostSignificantBits());
        bb.putLong(clusterIdUUID.getLeastSignificantBits());
        String clusterIdBase64 = Base64.encodeBase64URLSafeString(bb.array()).replace("-", "").replace("_", "");
        int len = clusterIdBase64.length() >= 15 ? 15 : clusterIdBase64.length();
        String clusterId = clusterIdBase64.substring(0, len);
        log.debug("cluster id: {}", clusterId);

        ////////////////////////////////////////////////////////////////////////
        ///// create security group with full internal access / ssh from outside
        log.info("Creating security group...");
        CreateSecurityGroupRequest secReq = new CreateSecurityGroupRequest();
        secReq.withGroupName(SECURITY_GROUP_PREFIX + clusterId).
                withDescription(clusterId);
        CreateSecurityGroupResult secReqResult = ec2.createSecurityGroup(secReq);
        log.info(V,"security group id: {}", secReqResult.getGroupId());

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

        String placementGroup = PLACEMENT_GROUP_PREFIX + clusterId;
        if (InstanceInformation.getSpecs(this.getConfiguration().getMasterInstanceType()).clusterInstance && InstanceInformation.getSpecs(this.getConfiguration().getSlaveInstanceType()).clusterInstance) {
        ec2.createPlacementGroup(new CreatePlacementGroupRequest(placementGroup, PlacementStrategy.Cluster));
        log.info("Creating placement group...");
        }
        // done for master. More volume description later when master is running
        //now defining Slave Volumes
        Map<String, String> snapShotToSlaveMounts = this.getConfiguration().getSlaveMounts();
        DeviceMapper slaveDeviceMapper = new DeviceMapper(snapShotToSlaveMounts);
        List<BlockDeviceMapping> slaveBlockDeviceMappings = new ArrayList<>();
        // Create a list of slaves first. Associate with slave instance-ids later
        if (!snapShotToSlaveMounts.isEmpty()) {
            log.info(V, "Defining slave volumes");

            slaveBlockDeviceMappings = createBlockDeviceMappings(slaveDeviceMapper);
        }
        ////////////////////////////////////////////////////////////////////////
        /////////////// preparing blockdevicemappings for master////////////////

        Map<String, String> masterSnapshotToMountPointMap = this.getConfiguration().getMasterMounts();
        DeviceMapper masterDeviceMapper = new DeviceMapper(masterSnapshotToMountPointMap);
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

        String base64MasterUserData = UserDataCreator.masterUserData(masterDeviceMapper, this.getConfiguration());
        //////////////////////////////////////////////////////////////////////////
        /////// run master instance, tag it and wait for boot ////////////////////
        log.info("Requesting master instance ...");

        Placement instancePlacement = new Placement(this.getConfiguration().getAvailabilityZone());

        if (InstanceInformation.getSpecs(
                this.getConfiguration().getMasterInstanceType()).clusterInstance) {
            instancePlacement.setGroupName(placementGroup);
        }

        RunInstancesRequest masterReq = new RunInstancesRequest();
        masterReq.withInstanceType(this.getConfiguration().getMasterInstanceType())
                .withMinCount(1).withMaxCount(1).withPlacement(instancePlacement)
                .withSecurityGroupIds(secReqResult.getGroupId())
                .withKeyName(this.getConfiguration().getKeypair())
                .withImageId(this.getConfiguration().getMasterImage())
                .withUserData(base64MasterUserData)
                .withBlockDeviceMappings(masterDeviceMappings);

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
        masterNameTagRequest.withResources(masterInstance.getInstanceId()).withTags(new Tag().withKey("bibigrid-id").withValue(clusterId), new Tag().withKey("Name").withValue("master-" + clusterId));

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

        String base64SlaveUserData = UserDataCreator.forSlave(masterInstance.getPrivateIpAddress(), masterInstance.getPrivateDnsName(), slaveDeviceMapper, this.getConfiguration());

        List<com.amazonaws.services.autoscaling.model.BlockDeviceMapping> slaveAutoScaleBlockDeviceMappings = new ArrayList<>();
        // need to parse EC2 BlockdeviceMapping to AS BlockDeviceMapping
        for (com.amazonaws.services.ec2.model.BlockDeviceMapping bdm : slaveBlockDeviceMappings) {
            slaveAutoScaleBlockDeviceMappings.add(new com.amazonaws.services.autoscaling.model.BlockDeviceMapping().withDeviceName(bdm.getDeviceName()).withEbs(new Ebs().withSnapshotId(bdm.getEbs().getSnapshotId())));
        }

        // setting up Ephemerals for Slaves
        List<com.amazonaws.services.autoscaling.model.BlockDeviceMapping> ephemeralSlaveList = new ArrayList<>();
        for (int i = 0; i < InstanceInformation.getSpecs(this.getConfiguration().getSlaveInstanceType()).ephemerals; ++i) {
            com.amazonaws.services.autoscaling.model.BlockDeviceMapping temp = new com.amazonaws.services.autoscaling.model.BlockDeviceMapping();
            String virtualName = "ephemeral" + i;
            String deviceName = "/dev/sd" + ephemerals[i];
            temp.setVirtualName(virtualName);
            temp.setDeviceName(deviceName);
            ephemeralSlaveList.add(temp);
        }
        slaveAutoScaleBlockDeviceMappings.addAll(ephemeralSlaveList);

        AmazonAutoScaling as = new AmazonAutoScalingClient(this.getConfiguration().getCredentials());
        as.setEndpoint("autoscaling." + this.getConfiguration().getRegion() + ".amazonaws.com");

        CreateLaunchConfigurationRequest launchMainConfig = new CreateLaunchConfigurationRequest()
                .withImageId(this.getConfiguration().getSlaveImage())
                .withInstanceType(this.getConfiguration().getSlaveInstanceType().toString())
                .withLaunchConfigurationName(clusterId + "-config")
                .withBlockDeviceMappings(slaveAutoScaleBlockDeviceMappings)
                .withSecurityGroups(secReqResult.getGroupId())
                .withKeyName(this.getConfiguration().getKeypair())
                .withUserData(base64SlaveUserData)
                .withInstanceMonitoring(new InstanceMonitoring().withEnabled(false));

        as.createLaunchConfiguration(launchMainConfig);

        /*
         * if (this.getConfiguration().getType()==GridType.OnlySpot) {
         * launchMainConfig =
         * launchMainConfig.withSpotPrice(this.getConfiguration().getBidPrice()+"");
         * } * if (this.getConfiguration().getType()==GridType.Hybrid) {
         * launchSpotConfig =
         * launchSpotConfig.withImageId(this.getConfiguration().getSlaveImage())
         * .withInstanceType(this.getConfiguration().getSlaveInstanceType().toString())
         * .withLaunchConfigurationName(clusterId+"-onSpot")
         * .withBlockDeviceMappings(slaveAutoScaleBlockDeviceMappings)
         * .withSecurityGroups(secReqResult.getGroupId())
         * .withKeyName(this.getConfiguration().getKeypair())
         * .withUserData(base64SlaveUserData) .withInstanceMonitoring(new
         * InstanceMonitoring().withEnabled(false))
         * .withSpotPrice(this.getConfiguration().getBidPrice()+"");
         *
         * as.createLaunchConfiguration(launchSpotConfig); }
         */
        while (true) {
            DescribeLaunchConfigurationsResult describeLaunchResult = as.describeLaunchConfigurations(new DescribeLaunchConfigurationsRequest().withLaunchConfigurationNames(launchMainConfig.getLaunchConfigurationName()));
            boolean found = false;
            for (LaunchConfiguration e : describeLaunchResult.getLaunchConfigurations()) {
                if (e.getLaunchConfigurationName().equals(clusterId + "-config")) {
                    log.info(I, "Launch Configuration successfully created.");
                    found = true;
                    break;
                }
            }
            if (found) {
                break;
            } else {
                sleep(5);
            }
        }

        com.amazonaws.services.autoscaling.model.Tag autoScalingTag = new com.amazonaws.services.autoscaling.model.Tag();

        autoScalingTag.setKey("bibigrid");
        autoScalingTag.setValue(clusterId + "-slave");
        CreateAutoScalingGroupRequest myGroup = new CreateAutoScalingGroupRequest()
                .withAutoScalingGroupName("as_group-" + clusterId)
                .withLaunchConfigurationName(launchMainConfig.getLaunchConfigurationName())
                .withAvailabilityZones(Arrays.asList(this.getConfiguration()
                                .getAvailabilityZone()))
                .withDesiredCapacity(this.getConfiguration().getSlaveInstanceStartAmount())
                .withTags(autoScalingTag)
                .withTerminationPolicies("NewestInstance", "ClosestToNextInstanceHour");

        if (this.getConfiguration().isAutoscaling()) {
            myGroup.setMaxSize(this.getConfiguration().getSlaveInstanceMaximum());
            myGroup.setMinSize(this.getConfiguration().getSlaveInstanceMinimum());
        } else {
            myGroup.setMaxSize(this.getConfiguration().getSlaveInstanceStartAmount());
            myGroup.setMinSize(this.getConfiguration().getSlaveInstanceStartAmount());
        }

        if (InstanceInformation.getSpecs(
                this.getConfiguration().getSlaveInstanceType()).clusterInstance) {
            myGroup.setPlacementGroup(placementGroup);
        }
        as.createAutoScalingGroup(myGroup);
        List<com.amazonaws.services.autoscaling.model.Instance> slaveAsInstances = new ArrayList<>();
        while (true) {
            DescribeAutoScalingGroupsResult autoScalingResult = as.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(myGroup.getAutoScalingGroupName()));
            boolean asGroupFound = false;
            if (!autoScalingResult.getAutoScalingGroups().isEmpty()) {
                slaveAsInstances.clear();
                for (AutoScalingGroup e : autoScalingResult.getAutoScalingGroups()) {
                    if (e.getAutoScalingGroupName().equals("as_group-" + clusterId) && !e.getInstances().isEmpty() ) {
                        if (e.getInstances().size()== this.getConfiguration().getSlaveInstanceStartAmount()) {
                        slaveAsInstances.addAll(e.getInstances());
                        asGroupFound = true;
                        break;
                        } else {
                            log.debug(V,"There are still instances missing inside the autoscaling group.");
                            sleep(5);
                        }
                    }
                }
                if (asGroupFound) {
                    log.info(I, "AutoScaling Group has been successfully created.");
                    break;
                } else {
                    sleep(5);
                }
            } else {
                sleep(5);
            }
        }
        List<String> slaveInstancesIds = new ArrayList<>();
        for (com.amazonaws.services.autoscaling.model.Instance asInstance : slaveAsInstances) {
            slaveInstancesIds.add(asInstance.getInstanceId());
        }
        List<Instance> slaveInstances = new ArrayList<>();
        if (!this.getConfiguration().isAutoscaling()) {
            log.info("Waiting for slaves...");
            slaveInstances = waitForInstances(slaveInstancesIds);
            log.info(I, "Slaves successfully started.");
        }

        /*
         * if (this.getConfiguration().getType() == GridType.Hybrid) {
         * UpdateAutoScalingGroupRequest changeAutoScalingGroupRequest = new
         * UpdateAutoScalingGroupRequest()
         * .withLaunchConfigurationName(launchSpotConfig.getLaunchConfigurationName())
         * .withAutoScalingGroupName(myGroup.getAutoScalingGroupName())
         * .withMaxSize(this.getConfiguration().getSlaveInstanceMaximum())
         * .withMinSize(this.getConfiguration().getSlaveInstanceMinimum())
         * .withAvailabilityZones(Arrays.asList(this.getConfiguration().getAvailabilityZone()))
         * .withTerminationPolicies("NewestInstance","ClosestToNextInstanceHour");;
         *
         * as.updateAutoScalingGroup(changeAutoScalingGroupRequest); } sleep(5);
         */
        
        CurrentClusters.addCluster(masterReservationId, myGroup.getAutoScalingGroupName(), clusterId, this.getConfiguration().getSlaveInstanceMaximum(), false, "empty");
        PutScalingPolicyRequest addPolicyRequest = new PutScalingPolicyRequest().withAdjustmentType("ChangeInCapacity").withCooldown(300).withScalingAdjustment(1).withAutoScalingGroupName(myGroup.getAutoScalingGroupName()).withPolicyName(myGroup.getAutoScalingGroupName() + "-add");

        as.putScalingPolicy(addPolicyRequest);

        sleep(5);

        JSch ssh = new JSch();
        JSch.setLogger(new JSchLogger());
        /*
         * Building Command
         */
        log.info("Now configuring ...");
        String execCommand;
        if (this.getConfiguration().isAutoscaling()) {
            execCommand = SshFactory.buildSshCommand(clusterId, this.getConfiguration(), masterInstance);
        } else {
            execCommand = SshFactory.buildSshCommand(clusterId, this.getConfiguration(), masterInstance, slaveInstances);
            log.info(V, "Building SSH-Command");
        }
        boolean uploaded = false;
        boolean configured = false;
        while (!configured) {
            try {

                ssh.addIdentity(this.getConfiguration().getIdentityFile().toString());
                sleep(10);

                /*
                 * Create new Session to avoid packet corruption.
                 */
                Session sshSession = SshFactory.createNewSshSession(ssh, masterInstance.getPublicDnsName(), MASTER_SSH_USER, this.getConfiguration().getIdentityFile());

                /*
                 * Start connect attempt
                 */
                sshSession.connect();

                if (!uploaded && this.getConfiguration().isAutoscaling()) {
                    String remoteDirectory = "/home/ubuntu/.monitor";
                    String filename = "monitor.jar";
                    String localFile = "/monitor.jar";
                    log.info(V, "Uploading monitor.");
                    ChannelSftp channelPut = (ChannelSftp) sshSession.openChannel("sftp");
                    channelPut.connect();
                    channelPut.cd(remoteDirectory);
                    channelPut.put(getClass().getResourceAsStream(localFile), filename);
                    channelPut.disconnect();
                    log.info(V, "Upload done");
                    uploaded = true;
                }
                ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");

                InputStream in = channel.getInputStream();

                channel.setCommand(execCommand);

                log.info(V, "Connecting ssh channel...");
                channel.connect();

                byte[] tmp = new byte[1024];
                while (true) {
                    while (in.available() > 0) {
                        int i = in.read(tmp, 0, 1024);
                        if (i <= 0) {
                            break;
                        }
                        String returnString = new String(tmp, 0, i);
                        if (returnString.contains("Adding instances")) {
                            configured = true;
                            break;
                        }
                        log.info(V, "SSH: {}", returnString);
                    }
                    if (channel.isClosed() || configured) {

                        log.info(V,"SSH: exit-status: {}", channel.getExitStatus());
                        configured = true;
                        break;
                    }

                    sleep(2);
                }
                if (configured) {
                    channel.disconnect();
                    sshSession.disconnect();
                }

            } catch (IOException | SftpException | JSchException e) {
                log.error(V, "SSH: {}", e);
                sleep(2);
            }
        }
         log.info(I, "Master instance has been configured.");
        log.info("Launch Summary:\nSecurity Group: {} \nMaster Reservation: {} \nPlacement Group: {} \nAutoScaling Group Name: {} \nLaunch Configuration ID: {} \nClusterId: {}\n",
                secReqResult.getGroupId(), masterReservationId, myGroup.getPlacementGroup()==null ? "NONE": myGroup.getPlacementGroup(), myGroup.getAutoScalingGroupName(), myGroup.getLaunchConfigurationName(), clusterId);

        // Prepare Output Message
        StringBuilder sb = new StringBuilder();

        sb.append("\n============\n");
        sb.append("You might want to set these environment variables:\n");
        sb.append("\n");
        sb.append("export BIBIGRID_MASTER=");
        sb.append(masterInstance.getPublicDnsName());
        sb.append("\n");
        int i = 1;
        for (Instance e : slaveInstances) { // prepare environment variables
            sb.append("export BIBIGRID_SLAVE");
            sb.append(i);
            sb.append("=");
            sb.append(e.getPublicDnsName());
            sb.append("\n");
            ++i;
        }
        sb.append("\n");
        sb.append("You can log on to the master node with:\n");
        sb.append("\n");
        sb.append("ssh -i ");
        sb.append(this.getConfiguration().getIdentityFile());
        sb.append(" ubuntu@$BIBIGRID_MASTER\n");
        sb.append("\n");
        sb.append("The Ganglia Web Interface is available at:\n");
        sb.append("http://");
        sb.append(masterInstance.getPublicDnsName());
        sb.append("/ganglia\n");
        sb.append("\n==========\n");
        sb.append("You can terminate the cluster at any time with:\n");
        sb.append("./bibigrid -t ");
        sb.append(clusterId);
        if (this.getConfiguration().isAlternativeConfigFile()) {
            sb.append(" -o ");
            sb.append(this.getConfiguration().getAlternativeConfigPath());
        }
        sb.append("\n");
        
        
        log.info(I, sb.toString());

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
                for (Instance e : instanceDescrReqResult.getReservations().get(0).getInstances()) {
                    state = e.getState().getName();
                    if (!state.equals(InstanceStateName.Running.toString())) {
                        log.debug(V, "ID " + e.getInstanceId() + "in state:" + state);
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
                blockDeviceMapping.setEbs(new EbsBlockDevice().withSnapshotId(DeviceMapper.stripSnapshotId(snapshotIdMountPoint.getKey())));
                blockDeviceMapping.setDeviceName(deviceMapper.getDeviceNameForSnapshotId(snapshotIdMountPoint.getKey()));

                mappings.add(blockDeviceMapping);

            } catch (AmazonServiceException ex) {
                log.debug("{}", ex.getMessage());

            }
        }
        return mappings;
    }
}
