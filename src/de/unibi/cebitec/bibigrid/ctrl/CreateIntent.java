package de.unibi.cebitec.bibigrid.ctrl;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.*;
import com.jcraft.jsch.*;
import de.unibi.cebitec.bibigrid.StartUpOgeCluster;
import de.unibi.cebitec.bibigrid.exc.IntentNotConfiguredException;
import de.unibi.cebitec.bibigrid.model.Port;
import static de.unibi.cebitec.bibigrid.util.ImportantInfoOutputFilter.I;
import de.unibi.cebitec.bibigrid.util.*;
import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.*;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateIntent extends Intent {

    public static final Logger log = LoggerFactory.getLogger(CreateIntent.class);
    public static final String PREFIX="bibigrid-";
    public static final String SECURITY_GROUP_PREFIX = PREFIX+"sg-";
    
    public static final String MASTER_SSH_USER = "ubuntu";
    public static final String PLACEMENT_GROUP_PREFIX = PREFIX+"pg-";
    public static final String SUBNET_PREFIX = PREFIX+"subnet-";
    private AmazonEC2 ec2;
    private Vpc vpc;
    private Subnet subnet;
    private String MASTERIP;

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
        } catch (AmazonClientException | JSchException ace) {
            log.error("{}", ace);
            return false;
        }
        return true;
    }

    private boolean runInstances() throws AmazonClientException, JSchException {

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
        Tag bibigridid = new Tag().withKey("bibigrid-id").withValue(clusterId);
        log.debug("cluster id: {}", clusterId);
        
        
        // create KeyPair for cluster communication
        
        KEYPAIR keypair = new KEYPAIR();
        

        ////////////////////////////////////////////////////////////////////////
        ///// check for (default) VPC
        if (this.getConfiguration().getVpcid() == null) {
            vpc = getVPC(ec2);
        } else {
            vpc = getVPC(ec2, this.getConfiguration().getVpcid());
        }

        if (vpc == null) {
            log.error("No suitable vpc found ... define a default VPC for you account or set VPC_ID");
            System.exit(1);
        } else {
            log.info(V, "Use VPC {} ({})%n", vpc.getVpcId(), vpc.getCidrBlock());
        }

        ///////////////////////////////////////////////////////////////////////
        ///// check for unused Subnet Cidr and create one
        DescribeSubnetsRequest describesubnetsreq = new DescribeSubnetsRequest();
        DescribeSubnetsResult describesubnetres = ec2.describeSubnets(describesubnetsreq);
        List<Subnet> loSubnets = describesubnetres.getSubnets();

        List<String> listofUsedCidr = new ArrayList<>(); // contains all subnet.cidr which are in current vpc
        for (Subnet sn : loSubnets) {
            if (sn.getVpcId().equals(vpc.getVpcId())) {
                listofUsedCidr.add(sn.getCidrBlock());
            }
        }

        SubNets subnets = new SubNets(vpc.getCidrBlock(), 24);
        String SUBNETCIDR = subnets.nextCidr(listofUsedCidr);

        log.debug(V, "Use {} for generated SubNet.", SUBNETCIDR);

        // create new subnetdir      
        CreateSubnetRequest createsubnetreq = new CreateSubnetRequest(vpc.getVpcId(), SUBNETCIDR);
        createsubnetreq.withAvailabilityZone(this.getConfiguration().getAvailabilityZone());
        CreateSubnetResult createsubnetres = ec2.createSubnet(createsubnetreq);
        subnet = createsubnetres.getSubnet();

        CreateTagsRequest tagRequest = new CreateTagsRequest();
        tagRequest.withResources(subnet.getSubnetId()).withTags(bibigridid,new Tag("Name", SUBNET_PREFIX + clusterId));
        ec2.createTags(tagRequest);

        ///////////////////////////////////////////////////////////////////////
        ///// MASTERIP
        MASTERIP = SubNets.getFirstIP(subnet.getCidrBlock());

        ////////////////////////////////////////////////////////////////////////
        ///// create security group with full internal access / ssh from outside
        log.info("Creating security group...");
        CreateSecurityGroupRequest secReq = new CreateSecurityGroupRequest();
        secReq.withGroupName(SECURITY_GROUP_PREFIX + clusterId)
                .withDescription(clusterId)
                .withVpcId(vpc.getVpcId());
        CreateSecurityGroupResult secReqResult = ec2.createSecurityGroup(secReq);
        log.info(V, "security group id: {}", secReqResult.getGroupId());

        UserIdGroupPair secGroupSelf = new UserIdGroupPair().withGroupId(secReqResult.getGroupId());

        IpPermission secGroupAccessSsh = new IpPermission();
        secGroupAccessSsh.withIpProtocol("tcp").withFromPort(22).withToPort(22).withIpRanges("0.0.0.0/0");
        IpPermission secGroupSelfAccessTcp = new IpPermission();
        secGroupSelfAccessTcp.withIpProtocol("tcp").withFromPort(0).withToPort(65535).withUserIdGroupPairs(secGroupSelf);
        IpPermission secGroupSelfAccessUdp = new IpPermission();
        secGroupSelfAccessUdp.withIpProtocol("udp").withFromPort(0).withToPort(65535).withUserIdGroupPairs(secGroupSelf);
        IpPermission secGroupSelfAccessIcmp = new IpPermission();
        secGroupSelfAccessIcmp.withIpProtocol("icmp").withFromPort(-1).withToPort(-1).withUserIdGroupPairs(secGroupSelf);

        List<IpPermission> allIpPermissions = new ArrayList<>();
        allIpPermissions.add(secGroupAccessSsh);
        allIpPermissions.add(secGroupSelfAccessTcp);
        allIpPermissions.add(secGroupSelfAccessUdp);
        allIpPermissions.add(secGroupSelfAccessIcmp);
        for (Port port : this.getConfiguration().getPorts()) {
            log.info("{}:{}",port.iprange,""+port.number);
            IpPermission additionalPortTcp = new IpPermission();
            additionalPortTcp.withIpProtocol("tcp").withFromPort(port.number).withToPort(port.number).withIpRanges(port.iprange);
            allIpPermissions.add(additionalPortTcp);
            IpPermission additionalPortUdp = new IpPermission();
            additionalPortUdp.withIpProtocol("udp").withFromPort(port.number).withToPort(port.number).withIpRanges(port.iprange);
            allIpPermissions.add(additionalPortUdp);
        }

        AuthorizeSecurityGroupIngressRequest ruleChangerReq = new AuthorizeSecurityGroupIngressRequest();
        ruleChangerReq.withGroupId(secReqResult.getGroupId()).withIpPermissions(allIpPermissions);

        tagRequest = new CreateTagsRequest();
        tagRequest.withResources(secReqResult.getGroupId()).withTags(bibigridid,new Tag("Name", SECURITY_GROUP_PREFIX + clusterId));
        ec2.createTags(tagRequest);

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

        String base64MasterUserData = UserDataCreator.masterUserData(masterDeviceMapper, this.getConfiguration(),keypair.getPrivateKey());

        log.info(V,"Master UserData:\n {}",base64MasterUserData);
        //////////////////////////////////////////////////////////////////////////
        /////// run master instance, tag it and wait for boot ////////////////////
        log.info("Requesting master instance ...");

        Placement instancePlacement = new Placement(this.getConfiguration().getAvailabilityZone());

        if (InstanceInformation.getSpecs(
                this.getConfiguration().getMasterInstanceType()).clusterInstance) {
            instancePlacement.setGroupName(placementGroup);
        }

        //////////////////////////////////////////////////////////////////////////
        /////// create NetworkInterfaceSpecification for MASTER instance with FIXED internal IP and public ip
        InstanceNetworkInterfaceSpecification inis = new InstanceNetworkInterfaceSpecification();
        inis.withPrivateIpAddress(MASTERIP)
                .withGroups(secReqResult.getGroupId())
                .withAssociatePublicIpAddress(true)
                .withSubnetId(subnet.getSubnetId())
                .withDeviceIndex(0);

        RunInstancesRequest masterReq = new RunInstancesRequest();
        masterReq.withInstanceType(this.getConfiguration().getMasterInstanceType())
                .withMinCount(1).withMaxCount(1).withPlacement(instancePlacement)
                .withKeyName(this.getConfiguration().getKeypair())
                .withImageId(this.getConfiguration().getMasterImage())
                .withUserData(base64MasterUserData)
                .withBlockDeviceMappings(masterDeviceMappings)
                .withNetworkInterfaces(inis);

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
        masterNameTagRequest.withResources(masterInstance.getInstanceId()).withTags(bibigridid, new Tag().withKey("Name").withValue(PREFIX+"master-" + clusterId));

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

        String base64SlaveUserData = UserDataCreator.forSlave(masterInstance.getPrivateIpAddress(), masterInstance.getPrivateDnsName(), slaveDeviceMapper, this.getConfiguration(),keypair.getPublicKey());

        log.info(V,"Slave Userdata:\n{}",base64SlaveUserData);

        RunInstancesRequest slaveReq = new RunInstancesRequest();
        slaveReq.withInstanceType(this.getConfiguration().getSlaveInstanceType())
                .withMinCount(this.getConfiguration().getSlaveInstanceCount())
                .withMaxCount(this.getConfiguration().getSlaveInstanceCount())
                .withPlacement(instancePlacement)
                .withKeyName(this.getConfiguration().getKeypair())
                .withImageId(this.getConfiguration().getSlaveImage())
                .withUserData(base64SlaveUserData)
                .withBlockDeviceMappings(slaveBlockDeviceMappings)
                .withSubnetId(subnet.getSubnetId())
                .withSecurityGroupIds(secReqResult.getGroupId());

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
            slaveNameTagRequest.withResources(si.getInstanceId()).withTags(bibigridid, new Tag().withKey("Name").withValue(PREFIX+"slave-" + clusterId));
            ec2.createTags(slaveNameTagRequest);
        }

        sleep(5);

        JSch ssh = new JSch();
        JSch.setLogger(new JSchLogger());
        /*
         * Building Command
         */
        log.info("Now configuring ...");
        String execCommand = SshFactory.buildSshCommand(clusterId, this.getConfiguration(), masterInstance, slaveInstances);

        log.info(V, "Building SSH-Command : {}",execCommand);

        boolean uploaded = false;
        boolean configured = false;

        int ssh_attempts = 5;
        while (!configured && ssh_attempts > 0) {
            try {

                ssh.addIdentity(this.getConfiguration().getIdentityFile().toString());
                sleep(10);

                /*
                 * Create new Session to avoid packet corruption.
                 */
                Session sshSession = SshFactory.createNewSshSession(ssh, masterInstance.getPublicIpAddress(), MASTER_SSH_USER, this.getConfiguration().getIdentityFile());

                /*
                 * Start connect attempt
                 */
                sshSession.connect();

                if (!uploaded || ssh_attempts > 0) {
                    String remoteDirectory = "/home/ubuntu/.ssh";
                    String filename = "id_rsa";
                    String localFile = getConfiguration().getIdentityFile().toString();
                    log.info(V, "Uploading key");
                    ChannelSftp channelPut = (ChannelSftp) sshSession.openChannel("sftp");
                    channelPut.connect();
                    channelPut.cd(remoteDirectory);
                    channelPut.put(new FileInputStream(localFile), filename);
                    channelPut.disconnect();
                    log.info(V, "Upload done");
                    uploaded = true;
                }
                ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");

                BufferedReader stdout = new BufferedReader(new InputStreamReader(channel.getInputStream()));
                BufferedReader stderr = new BufferedReader(new InputStreamReader(channel.getErrStream()));

                channel.setCommand(execCommand);

                log.info(V, "Connecting ssh channel...");
                channel.connect();

                String lineout  = null, lineerr = null;

                while (((lineout = stdout.readLine()) != null) || ((lineerr = stderr.readLine()) != null)) {

                    if (lineout != null) {
                        if (lineout.contains("CONFIGURATION_FINISHED")) {
                            configured = true;
                        }
                        log.info(V, "SSH: {}", lineout);
                    }

                    if (lineerr != null) {
                        log.error(V, "SSH: {}", lineerr);
                    }
                    if (channel.isClosed() || configured) {
                        log.info(V, "SSH: exit-status: {}", channel.getExitStatus());
                        configured = true;
                    }

                    sleep(2);
                }
                if (configured) {
                    channel.disconnect();
                    sshSession.disconnect();
                }

            } catch (IOException | SftpException | JSchException e) {
                log.error(V, "SSH: {}", e);
                ssh_attempts--;
                sleep(2);
            }
        }
        log.info(I, "Master instance has been configured.");
        
        // Prepare Output Message // Grid Properties
        StringBuilder sb = new StringBuilder();
        Properties gp = new Properties();

        sb.append("\n============\n");
        sb.append("You might want to set these environment variables:\n");
        sb.append("\n");
        sb.append("export BIBIGRID_MASTER=");
        sb.append(masterInstance.getPublicIpAddress());
        sb.append("\n");

        gp.setProperty("BIBIGRID_MASTER", masterInstance.getPublicIpAddress());


        sb.append("\n");
        sb.append("You can log on to the master node with:\n");
        sb.append("\n");
        sb.append("ssh -i ");
        sb.append(this.getConfiguration().getIdentityFile());
        gp.setProperty("IdentityFile", this.getConfiguration().getIdentityFile().toString());
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
        gp.setProperty("clusterId", clusterId);
        if (this.getConfiguration().isAlternativeConfigFile()) {
            sb.append(" -o ");
            sb.append(this.getConfiguration().getAlternativeConfigPath());
            gp.setProperty("AlternativeConfigFile", this.getConfiguration().getAlternativeConfigPath());
        }
        sb.append("\n");

        log.info(I, sb.toString());

        // write 
        if (this.getConfiguration().getGridPropertiesFile() != null) {
            try {
                gp.store(new FileOutputStream(this.getConfiguration().getGridPropertiesFile()), "Autogenerated by BiBiGrid");
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

    /**
     * Return a VPC that currently exists in selected region. Returns either the
     * *default* vpc from all or the given vpcIds list. If only one vpcId is
     * given it is returned wether it is default or not. Return null in the case
     * no default or fitting VPC is found.
     *
     * @param ec2 - AmazonEC2Client
     * @param vpcIds - String...
     * @return
     */
    private Vpc getVPC(AmazonEC2 ec2, String... vpcIds) {
        DescribeVpcsRequest dvreq = new DescribeVpcsRequest();
        dvreq.setVpcIds(Arrays.asList(vpcIds));

        DescribeVpcsResult describeVpcsResult = ec2.describeVpcs(dvreq);
        List<Vpc> lvpcs = describeVpcsResult.getVpcs();

        if (vpcIds.length == 1 && lvpcs.size() == 1) {
            return lvpcs.get(0);
        }
        if (!lvpcs.isEmpty()) {
            for (Vpc vpc : lvpcs) {
                if (vpc.isDefault()) {
                    return vpc;
                }
            }
        }
        return null;
    }
}
