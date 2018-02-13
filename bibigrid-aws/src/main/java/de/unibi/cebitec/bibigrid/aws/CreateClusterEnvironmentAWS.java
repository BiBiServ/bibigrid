package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.services.ec2.model.*;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.intents.CreateClusterEnvironment;

import static de.unibi.cebitec.bibigrid.aws.CreateClusterAWS.PREFIX;

import de.unibi.cebitec.bibigrid.core.model.Port;
import de.unibi.cebitec.bibigrid.core.util.SubNets;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public class CreateClusterEnvironmentAWS extends CreateClusterEnvironment {
    private static final Logger LOG = LoggerFactory.getLogger(CreateClusterEnvironmentAWS.class);
    private static final String PLACEMENT_GROUP_PREFIX = PREFIX + "pg-";

    private Vpc network;
    private Subnet subnet;
    private String placementGroup;
    private final CreateClusterAWS cluster;
    private String masterIp;
    private String securityGroup;

    CreateClusterEnvironmentAWS(CreateClusterAWS cluster) throws ConfigurationException {
        super();
        this.cluster = cluster;
    }

    @Override
    public CreateClusterEnvironmentAWS createNetwork() throws ConfigurationException {
        // check for (default) network
        network = getNetworkOrDefault(cluster.getConfig().getNetwork());
        if (network == null) {
            throw new ConfigurationException("No suitable network found. Define a default VPC for you account or a valid network id.");
        } else {
            LOG.info(V, "Use network '{}' ({}).", network.getVpcId(), network.getCidrBlock());
        }
        return this;
    }

    @Override
    public CreateClusterEnvironmentAWS createSubnet() {
        // check for unused Subnet CIDR and create one
        DescribeSubnetsRequest describeSubnetsRequest = new DescribeSubnetsRequest();
        DescribeSubnetsResult describeSubnetsResult = cluster.getEc2().describeSubnets(describeSubnetsRequest);
        // contains all subnet.cidr which are in current network
        List<String> listOfUsedCidr = describeSubnetsResult.getSubnets().stream()
                .filter(s -> s.getVpcId().equals(network.getVpcId()))
                .map(Subnet::getCidrBlock)
                .collect(Collectors.toList());
        SubNets subnets = new SubNets(network.getCidrBlock(), 24);
        String subnetCidr = subnets.nextCidr(listOfUsedCidr);
        LOG.debug(V, "Use {} for generated subnet.", subnetCidr);
        // create new subnet
        CreateSubnetRequest createSubnetRequest = new CreateSubnetRequest(network.getVpcId(), subnetCidr);
        createSubnetRequest.withAvailabilityZone(cluster.getConfig().getAvailabilityZone());
        CreateSubnetResult createSubnetResult = cluster.getEc2().createSubnet(createSubnetRequest);
        subnet = createSubnetResult.getSubnet();
        return this;
    }

    @Override
    public CreateClusterEnvironmentAWS createSecurityGroup() {
        CreateTagsRequest tagRequest = new CreateTagsRequest();
        tagRequest.withResources(subnet.getSubnetId())
                .withTags(cluster.getBibigridId(), new Tag(de.unibi.cebitec.bibigrid.core.model.Instance.TAG_NAME, SUBNET_PREFIX + cluster.getClusterId()));
        cluster.getEc2().createTags(tagRequest);
        // master IP
        masterIp = SubNets.getFirstIP(subnet.getCidrBlock());
        // create security group with full internal access / ssh from outside
        LOG.info("Creating security group...");
        CreateSecurityGroupRequest secReq = new CreateSecurityGroupRequest();
        secReq.withGroupName(SECURITY_GROUP_PREFIX + cluster.getClusterId())
                .withDescription(cluster.getClusterId())
                .withVpcId(network.getVpcId());
        securityGroup = cluster.getEc2().createSecurityGroup(secReq).getGroupId();

        LOG.info(V, "security group id: {}", securityGroup);

        UserIdGroupPair secGroupSelf = new UserIdGroupPair().withGroupId(securityGroup);
        List<IpPermission> allIpPermissions = new ArrayList<>();
        allIpPermissions.add(buildIpPermission("tcp", 22, 22).withIpv4Ranges(new IpRange().withCidrIp("0.0.0.0/0")));
        allIpPermissions.add(buildIpPermission("tcp", 0, 65535).withUserIdGroupPairs(secGroupSelf));
        allIpPermissions.add(buildIpPermission("udp", 0, 65535).withUserIdGroupPairs(secGroupSelf));
        allIpPermissions.add(buildIpPermission("icmp", -1, -1).withUserIdGroupPairs(secGroupSelf));
        for (Port port : cluster.getConfig().getPorts()) {
            LOG.info(port.toString());
            allIpPermissions.add(buildIpPermission("tcp", port.number, port.number)
                    .withIpv4Ranges(new IpRange().withCidrIp(port.ipRange)));
            allIpPermissions.add(buildIpPermission("udp", port.number, port.number)
                    .withIpv4Ranges(new IpRange().withCidrIp(port.ipRange)));
        }

        AuthorizeSecurityGroupIngressRequest ruleChangerReq = new AuthorizeSecurityGroupIngressRequest();
        ruleChangerReq.withGroupId(securityGroup).withIpPermissions(allIpPermissions);

        tagRequest = new CreateTagsRequest();
        tagRequest.withResources(securityGroup)
                .withTags(cluster.getBibigridId(), new Tag(de.unibi.cebitec.bibigrid.core.model.Instance.TAG_NAME, SECURITY_GROUP_PREFIX + cluster.getClusterId()));
        cluster.getEc2().createTags(tagRequest);
        cluster.getEc2().authorizeSecurityGroupIngress(ruleChangerReq);
        return this;
    }

    private IpPermission buildIpPermission(String protocol, int fromPort, int toPort) {
        return new IpPermission().withIpProtocol(protocol).withFromPort(fromPort).withToPort(toPort);
    }

    @Override
    public CreateClusterAWS createPlacementGroup() {
        // if all instance-types fulfill the cluster specifications, create a placementGroup.
        boolean allTypesClusterInstances =
                cluster.getConfig().getMasterInstance().getProviderType().isClusterInstance();
        for (Configuration.InstanceConfiguration instanceConfiguration : cluster.getConfig().getSlaveInstances()) {
            allTypesClusterInstances = allTypesClusterInstances &&
                    instanceConfiguration.getProviderType().isClusterInstance();
        }
        if (allTypesClusterInstances) {
            placementGroup = (PLACEMENT_GROUP_PREFIX + cluster.getClusterId());
            LOG.info("Creating placement group...");
            cluster.getEc2().createPlacementGroup(new CreatePlacementGroupRequest(placementGroup, PlacementStrategy.Cluster));
        } else {
            LOG.info(V, "Placement Group not available for selected Instances-types...");
            return cluster;
        }
        return cluster;
    }

    /**
     * Return a network that currently exists in selected region. Returns either the *default* network from all or
     * the given networkId. If a networkId is given it is returned whether it is default or not. Return null in the
     * case no default or fitting network is found.
     */
    private Vpc getNetworkOrDefault(String networkId) {
        DescribeVpcsRequest describeVpcsRequest = new DescribeVpcsRequest();
        List<String> networkIds = new ArrayList<>();
        if (networkId != null) {
            networkIds.add(networkId);
        }
        describeVpcsRequest.setVpcIds(networkIds);
        DescribeVpcsResult describeVpcsResult = cluster.getEc2().describeVpcs(describeVpcsRequest);
        List<Vpc> networks = describeVpcsResult.getVpcs();
        if (networkId != null && networks.size() == 1) {
            return networks.get(0);
        }
        if (!networks.isEmpty()) {
            for (Vpc network : networks) {
                if (network.isDefault()) {
                    return network;
                }
            }
        }
        return null;
    }

    Subnet getSubnet() {
        return subnet;
    }

    String getPlacementGroup() {
        return placementGroup;
    }

    String getMasterIp() {
        return masterIp;
    }

    String getSecurityGroup() {
        return securityGroup;
    }
}
