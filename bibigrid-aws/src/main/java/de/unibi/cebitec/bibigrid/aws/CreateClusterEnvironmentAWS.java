package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.services.ec2.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.intents.CreateClusterEnvironment;

import static de.unibi.cebitec.bibigrid.aws.CreateClusterAWS.PREFIX;

import de.unibi.cebitec.bibigrid.core.model.Port;
import de.unibi.cebitec.bibigrid.core.util.SubNets;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public class CreateClusterEnvironmentAWS extends CreateClusterEnvironment {
    private static final Logger LOG = LoggerFactory.getLogger(CreateClusterEnvironmentAWS.class);
    private static final String SECURITY_GROUP_PREFIX = PREFIX + "sg-";
    private static final String PLACEMENT_GROUP_PREFIX = PREFIX + "pg-";

    private Vpc vpc;
    private Subnet subnet;
    private String placementGroup;
    private final CreateClusterAWS cluster;
    private String masterIp;
    private CreateSecurityGroupResult secReqResult;

    CreateClusterEnvironmentAWS(CreateClusterAWS cluster) throws ConfigurationException {
        super();
        this.cluster = cluster;
    }

    @Override
    public CreateClusterEnvironmentAWS createVPC() throws ConfigurationException {
        // check for (default) VPC
        vpc = cluster.getConfig().getVpcId() == null ? getVPC() : getVPC(cluster.getConfig().getVpcId());
        if (vpc == null) {
            throw new ConfigurationException("No suitable vpc found. Define a default VPC for you account or set VPC_ID");
        } else {
            LOG.info(V, "Use VPC {} ({})", vpc.getVpcId(), vpc.getCidrBlock());
        }
        return this;
    }

    @Override
    public CreateClusterEnvironmentAWS createSubnet() {
        // check for unused Subnet CIDR and create one
        DescribeSubnetsRequest describeSubnetsRequest = new DescribeSubnetsRequest();
        DescribeSubnetsResult describeSubnetsResult = cluster.getEc2().describeSubnets(describeSubnetsRequest);
        // contains all subnet.cidr which are in current vpc
        List<String> listOfUsedCidr = describeSubnetsResult.getSubnets().stream()
                .filter(s -> s.getVpcId().equals(vpc.getVpcId()))
                .map(Subnet::getCidrBlock)
                .collect(Collectors.toList());
        SubNets subnets = new SubNets(vpc.getCidrBlock(), 24);
        String subnetCidr = subnets.nextCidr(listOfUsedCidr);
        LOG.debug(V, "Use {} for generated SubNet.", subnetCidr);
        // create new subnet
        CreateSubnetRequest createSubnetRequest = new CreateSubnetRequest(vpc.getVpcId(), subnetCidr);
        createSubnetRequest.withAvailabilityZone(cluster.getConfig().getAvailabilityZone());
        CreateSubnetResult createSubnetResult = cluster.getEc2().createSubnet(createSubnetRequest);
        subnet = createSubnetResult.getSubnet();
        return this;
    }

    @Override
    public CreateClusterEnvironmentAWS createSecurityGroup() {
        CreateTagsRequest tagRequest = new CreateTagsRequest();
        tagRequest.withResources(subnet.getSubnetId())
                .withTags(cluster.getBibigridId(), new Tag("Name", SUBNET_PREFIX + cluster.getClusterId()));
        cluster.getEc2().createTags(tagRequest);
        // master IP
        masterIp = SubNets.getFirstIP(subnet.getCidrBlock());
        // create security group with full internal access / ssh from outside
        LOG.info("Creating security group...");
        CreateSecurityGroupRequest secReq = new CreateSecurityGroupRequest();
        secReq.withGroupName(SECURITY_GROUP_PREFIX + cluster.getClusterId())
                .withDescription(cluster.getClusterId())
                .withVpcId(vpc.getVpcId());
        secReqResult = cluster.getEc2().createSecurityGroup(secReq);

        LOG.info(V, "security group id: {}", secReqResult.getGroupId());

        UserIdGroupPair secGroupSelf = new UserIdGroupPair().withGroupId(secReqResult.getGroupId());
        List<IpPermission> allIpPermissions = new ArrayList<>();
        allIpPermissions.add(buildIpPermission("tcp", 22, 22).withIpRanges("0.0.0.0/0"));
        allIpPermissions.add(buildIpPermission("tcp", 0, 65535).withUserIdGroupPairs(secGroupSelf));
        allIpPermissions.add(buildIpPermission("udp", 0, 65535).withUserIdGroupPairs(secGroupSelf));
        allIpPermissions.add(buildIpPermission("icmp", -1, -1).withUserIdGroupPairs(secGroupSelf));
        for (Port port : cluster.getConfig().getPorts()) {
            LOG.info(port.toString());
            allIpPermissions.add(buildIpPermission("tcp", port.number, port.number).withIpRanges(port.ipRange));
            allIpPermissions.add(buildIpPermission("udp", port.number, port.number).withIpRanges(port.ipRange));
        }

        AuthorizeSecurityGroupIngressRequest ruleChangerReq = new AuthorizeSecurityGroupIngressRequest();
        ruleChangerReq.withGroupId(secReqResult.getGroupId()).withIpPermissions(allIpPermissions);

        tagRequest = new CreateTagsRequest();
        tagRequest.withResources(secReqResult.getGroupId())
                .withTags(cluster.getBibigridId(), new Tag("Name", SECURITY_GROUP_PREFIX + cluster.getClusterId()));
        cluster.getEc2().createTags(tagRequest);
        cluster.getEc2().authorizeSecurityGroupIngress(ruleChangerReq);
        return this;
    }

    private IpPermission buildIpPermission(String protocol, int fromPort, int toPort) {
        return new IpPermission().withIpProtocol(protocol).withFromPort(fromPort).withToPort(toPort);
    }

    @Override
    public CreateClusterAWS createPlacementGroup() {
        // if both instance-types fulfill the cluster specifications, create a placementGroup.
        if (cluster.getConfig().getMasterInstanceType().getSpec().isClusterInstance()
                && cluster.getConfig().getSlaveInstanceType().getSpec().isClusterInstance()) {
            placementGroup = (PLACEMENT_GROUP_PREFIX + cluster.getClusterId());
            LOG.info("Creating placement group...");
            cluster.getEc2().createPlacementGroup(new CreatePlacementGroupRequest(placementGroup, PlacementStrategy.Cluster));
        } else {
            LOG.info(V, "Placement Group not available for selected Instances-types ...");
            return cluster;
        }
        return cluster;
    }

    /**
     * Return a VPC that currently exists in selected region. Returns either the *default* vpc from all or the given
     * vpcIds list. If only one vpcId is given it is returned whether it is default or not. Return null in the case
     * no default or fitting VPC is found.
     */
    private Vpc getVPC(String... vpcIds) {
        DescribeVpcsRequest describeVpcsRequest = new DescribeVpcsRequest();
        describeVpcsRequest.setVpcIds(Arrays.asList(vpcIds));
        DescribeVpcsResult describeVpcsResult = cluster.getEc2().describeVpcs(describeVpcsRequest);
        List<Vpc> vpcs = describeVpcsResult.getVpcs();
        if (vpcIds.length == 1 && vpcs.size() == 1) {
            return vpcs.get(0);
        }
        if (!vpcs.isEmpty()) {
            for (Vpc vpc : vpcs) {
                if (vpc.isDefault()) {
                    return vpc;
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

    CreateSecurityGroupResult getSecReqResult() {
        return secReqResult;
    }
}
