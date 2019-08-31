package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceType;
import de.unibi.cebitec.bibigrid.core.intents.CreateCluster;
import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.util.*;

import static de.unibi.cebitec.bibigrid.core.util.ImportantInfoOutputFilter.I;
import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the general CreateClusterAWS interface for an AWS based cluster.
 * First implementation was done by Johannes within a student project.
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class CreateClusterAWS extends CreateCluster {
    private static final Logger LOG = LoggerFactory.getLogger(CreateClusterAWS.class);
    private final AmazonEC2 ec2;

    // Placement groups
    private Placement instancePlacement;
    private SpotPlacement spotInstancePlacement;

    private String base64MasterUserData;

    private InstanceNetworkInterfaceSpecification masterNetworkInterface;
    private InstanceNetworkInterfaceSpecification workerNetworkInterface;
    private List<BlockDeviceMapping> masterDeviceMappings;
    private Tag bibigridId, username;
    private List<List<BlockDeviceMapping>> workerBlockDeviceMappings;

    private final ConfigurationAWS config;

    CreateClusterAWS(final ProviderModule providerModule, Client client, final ConfigurationAWS config) {
        super(providerModule, client, config);
        this.config = config;
        ec2 = ((ClientAWS) client).getInternal();
        bibigridId = new Tag().withKey(de.unibi.cebitec.bibigrid.core.model.Instance.TAG_BIBIGRID_ID).withValue(clusterId);
        username = new Tag().withKey(de.unibi.cebitec.bibigrid.core.model.Instance.TAG_USER).withValue(config.getUser());
    }

    @Override
    public CreateCluster configureClusterMasterInstance() {
        super.configureClusterMasterInstance();
        buildMasterDeviceMappings();
        base64MasterUserData = ShellScriptCreator.getUserData(config, environment.getKeypair(), true);
        buildMasterPlacementGroup();
        buildMasterNetworkInterface();
        return this;
    }

    private void buildMasterDeviceMappings() {
        // preparing block device mappings for master
        masterDeviceMappings = new ArrayList<>();
        // create Volumes first
        if (!config.getMasterMounts().isEmpty()) {
            LOG.info(V, "Defining master volumes");
            masterDeviceMappings = createBlockDeviceMappings(masterDeviceMapper);
        }
        masterDeviceMappings.addAll(buildEphemeralList(config.getMasterInstance().getProviderType().getEphemerals()));
    }

    private List<BlockDeviceMapping> buildEphemeralList(int ephemerals) {
        List<BlockDeviceMapping> ephemeralList = new ArrayList<>();
        for (int i = 0; i < ephemerals; i++) {
            BlockDeviceMapping temp = new BlockDeviceMapping();
            temp.setVirtualName("ephemeral" + i);
            temp.setDeviceName("/dev/sd" + ephemeral(i));
            ephemeralList.add(temp);
        }
        return ephemeralList;
    }

    private char ephemeral(int i) {
        return (char) (i + 98);
    }

    private void buildMasterPlacementGroup() {
        CreateClusterEnvironmentAWS environment = (CreateClusterEnvironmentAWS) this.environment;
        if (config.getMasterInstance().getProviderType().isClusterInstance()) {
            if (config.isUseSpotInstances()) {
                spotInstancePlacement = new SpotPlacement(config.getAvailabilityZone());
                spotInstancePlacement.setGroupName(environment.getPlacementGroup());
            } else {
                instancePlacement = new Placement(config.getAvailabilityZone());
                instancePlacement.setGroupName(environment.getPlacementGroup());
            }
        }
    }

    private void buildMasterNetworkInterface() {
        // create NetworkInterfaceSpecification for master with public ip
        masterNetworkInterface = new InstanceNetworkInterfaceSpecification()
                .withGroups(((CreateClusterEnvironmentAWS) environment).getSecurityGroup())
                .withAssociatePublicIpAddress(true)
                .withSubnetId(environment.getSubnet().getId())
                .withDeviceIndex(0);
    }

    @Override
    public CreateClusterAWS configureClusterWorkerInstance() {
        buildClientsNetworkInterface();
        buildClientsDeviceMappings();
        return this;
    }

    private void buildClientsNetworkInterface() {
        workerNetworkInterface = new InstanceNetworkInterfaceSpecification()
                .withGroups(((CreateClusterEnvironmentAWS) environment).getSecurityGroup())
                .withSubnetId(environment.getSubnet().getId())
                .withAssociatePublicIpAddress(config.isPublicWorkerIps())
                .withDeviceIndex(0);
    }

    private void buildClientsDeviceMappings() {
        // now defining Worker Volumes
        workerBlockDeviceMappings = new ArrayList<>();
        for (Configuration.InstanceConfiguration instanceConfiguration : config.getWorkerInstances()) {
            de.unibi.cebitec.bibigrid.core.model.InstanceType workerSpec = instanceConfiguration.getProviderType();
            workerBlockDeviceMappings.add(buildEphemeralList(workerSpec.getEphemerals()));
        }
    }

    @Override
    protected List<Configuration.MountPoint> resolveMountSources(List<Configuration.MountPoint> mountPoints) {
        // TODO: possibly check if snapshot or volume like openstack
        return mountPoints;
    }

    @Override
    protected InstanceAWS launchClusterMasterInstance(String masterName) {
        LOG.info("Requesting master instance ...");
        Instance masterInstance;
        Tag masterNameTag = new Tag().withKey(de.unibi.cebitec.bibigrid.core.model.Instance.TAG_NAME).withValue(masterName);
        if (config.isUseSpotInstances()) {
            RequestSpotInstancesRequest masterReq = new RequestSpotInstancesRequest()
                    .withType(SpotInstanceType.OneTime)
                    .withInstanceCount(1)
                    .withLaunchGroup("lg_" + clusterId)
                    .withSpotPrice(Double.toString(config.getBidPriceMaster()));

            LaunchSpecification masterLaunchSpecification = new LaunchSpecification()
                    .withInstanceType(InstanceType.fromValue(config.getMasterInstance().getProviderType().getValue()))
                    .withPlacement(spotInstancePlacement)
                    .withKeyName(config.getKeypair())
                    .withImageId(config.getMasterInstance().getImage())
                    .withUserData(base64MasterUserData)
                    .withBlockDeviceMappings(masterDeviceMappings)
                    .withNetworkInterfaces(masterNetworkInterface);
            masterReq.setLaunchSpecification(masterLaunchSpecification);

            RequestSpotInstancesResult masterReqResult = ec2.requestSpotInstances(masterReq);

            List<SpotInstanceRequest> masterReqResponses = masterReqResult.getSpotInstanceRequests();
            // collect all spotInstanceRequestIds ...
            List<String> spotInstanceRequestIds = new ArrayList<>();
            for (SpotInstanceRequest requestResponse : masterReqResponses) {
                spotInstanceRequestIds.add(requestResponse.getSpotInstanceRequestId());
            }
            // Tag spot request
            ec2.createTags(new CreateTagsRequest()
                    .withResources(spotInstanceRequestIds)
                    .withTags(bibigridId, username, masterNameTag));
            // Wait for spot request finished
            LOG.info("Waiting for master instance (spot request) to finish booting ...");
            masterInstance = waitForInstances(waitForSpotInstances(spotInstanceRequestIds)).get(0);
        } else {
            RunInstancesRequest masterReq = new RunInstancesRequest()
                    .withInstanceType(InstanceType.fromValue(config.getMasterInstance().getProviderType().getValue()))
                    .withMinCount(1).withMaxCount(1)
                    .withPlacement(instancePlacement)
                    .withKeyName(config.getKeypair())
                    .withImageId(config.getMasterInstance().getImage())
                    .withUserData(base64MasterUserData)
                    .withBlockDeviceMappings(masterDeviceMappings)
                    .withNetworkInterfaces(masterNetworkInterface);
            // mounting ephemerals
            RunInstancesResult runInstancesResult = ec2.runInstances(masterReq);
            runInstancesResult.getReservation().getReservationId();
            masterInstance = runInstancesResult.getReservation().getInstances().get(0);
            LOG.info("Waiting for master instance to finish booting ...");
            // Waiting for master instance to run
            masterInstance = waitForInstances(Collections.singletonList(masterInstance.getInstanceId())).get(0);
        }
        LOG.info(I, "Master instance is now running!");

        ModifyInstanceAttributeRequest instanceAttributeRequest = new ModifyInstanceAttributeRequest();
        instanceAttributeRequest.setInstanceId(masterInstance.getInstanceId());
        instanceAttributeRequest.setSourceDestCheck(Boolean.FALSE);
        ec2.modifyInstanceAttribute(instanceAttributeRequest);
        // Tagging Master with a name
        ec2.createTags(new CreateTagsRequest()
                .withResources(masterInstance.getInstanceId())
                .withTags(bibigridId, username, masterNameTag));
        // Waiting for Status Checks to finish
        LOG.info("Waiting for status checks on master ...");
        do {
            DescribeInstanceStatusRequest request = new DescribeInstanceStatusRequest();
            request.setInstanceIds(Collections.singletonList(masterInstance.getInstanceId()));
            DescribeInstanceStatusResult response = ec2.describeInstanceStatus(request);
            InstanceStatus status = response.getInstanceStatuses().get(0);
            String instanceStatus = status.getInstanceStatus().getStatus();
            String systemStatus = status.getSystemStatus().getStatus();
            LOG.debug("Status of master instance: " + instanceStatus + "," + systemStatus);
            if (instanceStatus.equalsIgnoreCase("ok") && systemStatus.equalsIgnoreCase("ok")) {
                break;
            }
            LOG.info(V, "...");
            sleep(10);
        } while (true);
        LOG.info(I, "Status checks successful.");
        return new InstanceAWS(config.getMasterInstance(), masterInstance);
    }

    @Override
    protected List<de.unibi.cebitec.bibigrid.core.model.Instance> launchClusterWorkerInstances(
            int batchIndex, Configuration.WorkerInstanceConfiguration instanceConfiguration, String workerName) {
        // run worker instances and supply userdata
        List<Instance> workerInstances;
        Tag workerNameTag = new Tag().withKey(de.unibi.cebitec.bibigrid.core.model.Instance.TAG_NAME).withValue(workerName);
        String base64WorkerUserData = ShellScriptCreator.getUserData(config, environment.getKeypair(), true);
        if (config.isUseSpotInstances()) {
            RequestSpotInstancesRequest workerReq = new RequestSpotInstancesRequest()
                    .withType(SpotInstanceType.OneTime)
                    .withInstanceCount(instanceConfiguration.getCount())
                    .withLaunchGroup("lg_" + clusterId)
                    .withSpotPrice(Double.toString(config.getBidPrice()));

            LaunchSpecification workerLaunchSpecification = new LaunchSpecification()
                    .withInstanceType(InstanceType.fromValue(instanceConfiguration.getProviderType().getValue()))
                    .withPlacement(spotInstancePlacement)
                    .withKeyName(config.getKeypair())
                    .withImageId(instanceConfiguration.getImage())
                    .withUserData(base64WorkerUserData)
                    .withBlockDeviceMappings(workerBlockDeviceMappings.get(batchIndex))
                    .withNetworkInterfaces(workerNetworkInterface);

            workerReq.setLaunchSpecification(workerLaunchSpecification);
            RequestSpotInstancesResult workerReqResult = ec2.requestSpotInstances(workerReq);
            List<SpotInstanceRequest> workerReqResponses = workerReqResult.getSpotInstanceRequests();
            // collect all spotInstanceRequestIds ...
            List<String> spotInstanceRequestIds = new ArrayList<>();
            for (SpotInstanceRequest requestResponse : workerReqResponses) {
                spotInstanceRequestIds.add(requestResponse.getSpotInstanceRequestId());
            }
            sleep(1);
            LOG.info(V, "tag spot request instances");
            // tag spot requests (worker)
            CreateTagsRequest ctr = new CreateTagsRequest()
                    .withResources(spotInstanceRequestIds)
                    .withTags(bibigridId, username, workerNameTag);
            // Setting tags for spot requests can cause an amazon service exception, if the spot request
            // returns an id, but the id isn't registered in spot request registry yet.
            int counter = 0;
            boolean finished = false;
            while (!finished) {
                try {
                    ec2.createTags(ctr);
                    finished = true;
                } catch (AmazonServiceException ase) {
                    if (counter < 5) {
                        LOG.warn("{} ... trying again in 10 seconds.", ase.getMessage());
                        sleep(10);
                        counter++;
                    } else {
                        throw ase;
                    }
                }
            }
            LOG.info("Waiting for worker instance(s) (spot request) to finish booting ...");
            // wait for spot request (worker) finished
            workerInstances = waitForInstances(waitForSpotInstances(spotInstanceRequestIds));
        } else {
            RunInstancesRequest workerReq = new RunInstancesRequest()
                    .withInstanceType(InstanceType.fromValue(instanceConfiguration.getProviderType().getValue()))
                    .withMinCount(instanceConfiguration.getCount())
                    .withMaxCount(instanceConfiguration.getCount())
                    .withPlacement(instancePlacement)
                    .withKeyName(config.getKeypair())
                    .withImageId(instanceConfiguration.getImage())
                    .withUserData(base64WorkerUserData)
                    .withBlockDeviceMappings(workerBlockDeviceMappings.get(batchIndex))
                    .withNetworkInterfaces(workerNetworkInterface);

            RunInstancesResult runInstancesResult = ec2.runInstances(workerReq);
            runInstancesResult.getReservation().getReservationId();
            // create a list of all worker instances
            List<String> workerInstanceListIds = new ArrayList<>();
            for (Instance i : runInstancesResult.getReservation().getInstances()) {
                workerInstanceListIds.add(i.getInstanceId());
            }
            LOG.info("Waiting for worker instance(s) to finish booting ...");
            workerInstances = waitForInstances(workerInstanceListIds);
        }
        // Waiting for master instance to run
        LOG.info(I, "Worker instance(s) is now running!");
        // Tagging all workers with a name
        for (Instance si : workerInstances) {
            ec2.createTags(new CreateTagsRequest()
                    .withResources(si.getInstanceId())
                    .withTags(bibigridId, username, workerNameTag));
        }
        return workerInstances.stream().map(i -> new InstanceAWS(instanceConfiguration, i)).collect(Collectors.toList());
    }

    /**
     * Takes a list of instance IDs as Strings and monitors their system status
     *
     * @param listOfInstances Returns a list of Instances when they have been started.
     */
    private List<Instance> waitForInstances(List<String> listOfInstances) {
        if (listOfInstances.isEmpty()) {
            LOG.error("No instances found");
            return new ArrayList<>();
        }
        do {
            DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
            describeInstancesRequest.setInstanceIds(listOfInstances);
            boolean allRunning = true;
            try {
                DescribeInstancesResult describeInstancesResult = ec2.describeInstances(describeInstancesRequest);
                // Collect all instances from all reservations and check their state
                List<Instance> reservationInstances = describeInstancesResult.getReservations().stream()
                        .map(Reservation::getInstances)
                        .flatMap(List::stream)
                        .collect(Collectors.toList());
                for (Instance e : reservationInstances) {
                    String state = e.getState().getName();
                    if (!state.equals(InstanceStateName.Running.toString())) {
                        LOG.debug(V, "ID " + e.getInstanceId() + " in state:" + state);
                        allRunning = false;
                        break;
                    }
                }
                if (allRunning) {
                    return reservationInstances;
                }
                LOG.info(V, "...");
                sleep(10);
            } catch (AmazonServiceException e) {
                LOG.debug("{}", e);
                sleep(3);
            }
        } while (true);
    }

    /**
     * Get a lost of spotInstance IDs as Strings and monitors their spot request status
     */
    private List<String> waitForSpotInstances(List<String> listOfSpotInstances) {
        // Create a variable that will track whether there are any requests still in the open state.
        boolean anyOpen;
        List<String> fulfilled = new ArrayList<>();
        do {
            // Create the describeRequest object with all of the request ids to monitor (e.g. that we started).
            DescribeSpotInstanceRequestsRequest describeRequest = new DescribeSpotInstanceRequestsRequest();
            describeRequest.setSpotInstanceRequestIds(listOfSpotInstances);
            // Initialize the anyOpen variable to false - which assumes there are no requests open unless we find
            // one that is still open.
            anyOpen = false;
            try {
                // Retrieve all of the requests we want to monitor.
                DescribeSpotInstanceRequestsResult describeResult = ec2.describeSpotInstanceRequests(describeRequest);
                List<SpotInstanceRequest> describeResponses = describeResult.getSpotInstanceRequests();
                // Look through each request and determine if they are all in the active state.
                for (SpotInstanceRequest describeResponse : describeResponses) {
                    // If the state is open, it hasn't changed since we attempted to request it. There is the
                    // potential for it to transition almost immediately to closed or cancelled so we compare
                    // against open instead of active.
                    if (describeResponse.getState().equals("open")) {
                        anyOpen = true;
                        break;
                    }
                }
            } catch (AmazonServiceException e) {
                // If we have an exception, ensure we don't break out of the loop. This prevents the scenario where
                // there was blip on the wire.
                anyOpen = true;
            }
            // Sleep for 30 seconds.
            LOG.debug(V, "Waiting for spot instance request completion ...");
            sleep(30);
        } while (anyOpen);

        // get all instance id's
        DescribeSpotInstanceRequestsRequest describeRequest = new DescribeSpotInstanceRequestsRequest();
        describeRequest.setSpotInstanceRequestIds(listOfSpotInstances);
        DescribeSpotInstanceRequestsResult describeResult = ec2.describeSpotInstanceRequests(describeRequest);
        List<SpotInstanceRequest> describeResponses = describeResult.getSpotInstanceRequests();
        for (SpotInstanceRequest describeResponse : describeResponses) {
            LOG.info(V, "{} : {}", describeResponse.getInstanceId(), describeResponse.getState());
            if (describeResponse.getState().equals("active")) {
                LOG.info(V, "{} - {}", describeResponse.getInstanceId(), describeResponse.getInstanceId());
                fulfilled.add(describeResponse.getInstanceId());
            }
        }
        return fulfilled;
    }

    private List<BlockDeviceMapping> createBlockDeviceMappings(DeviceMapper deviceMapper) {
        List<BlockDeviceMapping> mappings = new ArrayList<>();
        List<Configuration.MountPoint> snapshotToMountPointMap = deviceMapper.getSnapshotIdToMountPoint();
        for (Configuration.MountPoint mountPoint : snapshotToMountPointMap) {
            try {
                BlockDeviceMapping blockDeviceMapping = new BlockDeviceMapping();
                blockDeviceMapping.setEbs(new EbsBlockDevice().withSnapshotId(DeviceMapper.stripSnapshotId(mountPoint.getSource())));
                blockDeviceMapping.setDeviceName(deviceMapper.getDeviceNameForSnapshotId(mountPoint.getSource()));
                mappings.add(blockDeviceMapping);
            } catch (AmazonServiceException ex) {
                LOG.debug("{}", ex.getMessage());
            }
        }
        return mappings;
    }

    Tag getBibigridId() {
        return bibigridId;
    }
}
