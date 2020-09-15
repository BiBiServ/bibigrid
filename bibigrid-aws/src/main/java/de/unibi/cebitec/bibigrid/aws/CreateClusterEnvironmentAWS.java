package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.intents.CreateClusterEnvironment;

import static de.unibi.cebitec.bibigrid.aws.CreateClusterAWS.PREFIX;

import de.unibi.cebitec.bibigrid.core.model.Port;
import de.unibi.cebitec.bibigrid.core.model.exceptions.NotImplementedException;
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

    private final CreateClusterAWS cluster;
    private final AmazonEC2 ec2;
    private String placementGroup;
    private String securityGroup;

    CreateClusterEnvironmentAWS(Client client, CreateClusterAWS cluster) throws ConfigurationException {
        super(client, cluster);
        this.cluster = cluster;
        ec2 = ((ClientAWS) client).getInternal();
    }

    @Override
    public CreateClusterEnvironmentAWS createSubnet() {
        // check for unused Subnet CIDR and create one
        DescribeSubnetsRequest describeSubnetsRequest = new DescribeSubnetsRequest();
        DescribeSubnetsResult describeSubnetsResult = ec2.describeSubnets(describeSubnetsRequest);
        // contains all subnet.cidr which are in current network
        List<String> listOfUsedCidr = describeSubnetsResult.getSubnets().stream()
                .filter(s -> s.getVpcId().equals(network.getId()))
                .map(Subnet::getCidrBlock)
                .collect(Collectors.toList());
        SubNets subnets = new SubNets(network.getCidr(), 24);
        String subnetCidr = subnets.nextCidr(listOfUsedCidr);
        LOG.debug(V, "Using '{}' for generated subnet.", subnetCidr);
        // create new subnet
        CreateSubnetRequest createSubnetRequest = new CreateSubnetRequest(network.getId(), subnetCidr);
        createSubnetRequest.withAvailabilityZone(getConfig().getAvailabilityZone());
        CreateSubnetResult createSubnetResult = ec2.createSubnet(createSubnetRequest);
        subnet = new SubnetAWS(createSubnetResult.getSubnet());
        return this;
    }

    @Override
    public CreateClusterEnvironmentAWS createSecurityGroup() {
        CreateTagsRequest tagRequest = new CreateTagsRequest();
        tagRequest.withResources(subnet.getId())
                .withTags(cluster.getBibigridId(), new Tag(de.unibi.cebitec.bibigrid.core.model.Instance.TAG_NAME, SUBNET_PREFIX + cluster.getClusterId()));
        ec2.createTags(tagRequest);
        // create security group with full internal access / ssh from outside
        LOG.info("Creating security group ...");
        CreateSecurityGroupRequest secReq = new CreateSecurityGroupRequest();
        secReq.withGroupName(SECURITY_GROUP_PREFIX + cluster.getClusterId())
                .withDescription(cluster.getClusterId())
                .withVpcId(network.getId());
        securityGroup = ec2.createSecurityGroup(secReq).getGroupId();

        LOG.info(V, "security group id: {}", securityGroup);

        UserIdGroupPair secGroupSelf = new UserIdGroupPair().withGroupId(securityGroup);
        List<IpPermission> allIpPermissions = new ArrayList<>();
        allIpPermissions.add(buildIpPermission("tcp", 22, 22).withIpv4Ranges(new IpRange().withCidrIp("0.0.0.0/0")));
        allIpPermissions.add(buildIpPermission("tcp", 1, 65535).withUserIdGroupPairs(secGroupSelf));
        allIpPermissions.add(buildIpPermission("udp", 1, 65535).withUserIdGroupPairs(secGroupSelf));
        allIpPermissions.add(buildIpPermission("icmp", -1, -1).withUserIdGroupPairs(secGroupSelf));
        for (Port port : getConfig().getPorts()) {
            LOG.info(port.toString());
            allIpPermissions.add(buildIpPermission("tcp", port.getNumber(), port.getNumber())
                    .withIpv4Ranges(new IpRange().withCidrIp(port.getIpRange())));
            allIpPermissions.add(buildIpPermission("udp", port.getNumber(), port.getNumber())
                    .withIpv4Ranges(new IpRange().withCidrIp(port.getIpRange())));
        }

        AuthorizeSecurityGroupIngressRequest ruleChangerReq = new AuthorizeSecurityGroupIngressRequest();
        ruleChangerReq.withGroupId(securityGroup).withIpPermissions(allIpPermissions);

        tagRequest = new CreateTagsRequest();
        tagRequest.withResources(securityGroup)
                .withTags(cluster.getBibigridId(), new Tag(de.unibi.cebitec.bibigrid.core.model.Instance.TAG_NAME, SECURITY_GROUP_PREFIX + cluster.getClusterId()));
        ec2.createTags(tagRequest);
        ec2.authorizeSecurityGroupIngress(ruleChangerReq);
        return this;
    }

    @Override
    public CreateClusterEnvironment createKeyPair() throws ConfigurationException {
//        Configuration.ClusterKeyPair ckp = getConfig().getClusterKeyPair();
////        CreateKeyPairRequest keyPairRequest = new CreateKeyPairRequest();
////        keyPairRequest.setKeyName(ckp.getName());
////        keyPairRequest.
        throw new NotImplementedException();
    }

    private IpPermission buildIpPermission(String protocol, int fromPort, int toPort) {
        return new IpPermission().withIpProtocol(protocol).withFromPort(fromPort).withToPort(toPort);
    }

    @Override
    public CreateClusterEnvironmentAWS createPlacementGroup() {
        // if all instance-types fulfill the cluster specifications, create a placementGroup.
        boolean allTypesClusterInstances = getConfig().getMasterInstance().getProviderType().isClusterInstance();
        for (Configuration.InstanceConfiguration instanceConfiguration : getConfig().getWorkerInstances()) {
            allTypesClusterInstances = allTypesClusterInstances &&
                    instanceConfiguration.getProviderType().isClusterInstance();
        }
        if (allTypesClusterInstances) {
            placementGroup = (PLACEMENT_GROUP_PREFIX + cluster.getClusterId());
            LOG.info("Creating placement group ...");
            ec2.createPlacementGroup(new CreatePlacementGroupRequest(placementGroup, PlacementStrategy.Cluster));
        } else {
            LOG.info(V, "Placement groups are not available for the specified instance type ...");
        }
        return this;
    }

    String getPlacementGroup() {
        return placementGroup;
    }

    String getSecurityGroup() {
        return securityGroup;
    }
}
