package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreatePlacementGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.CreateSubnetRequest;
import com.amazonaws.services.ec2.model.CreateSubnetResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsResult;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.PlacementStrategy;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.UserIdGroupPair;
import com.amazonaws.services.ec2.model.Vpc;
import com.jcraft.jsch.JSchException;
import de.unibi.cebitec.bibigrid.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.intents.CreateClusterEnvironment;

import static de.unibi.cebitec.bibigrid.aws.CreateClusterAWS.PLACEMENT_GROUP_PREFIX;
import static de.unibi.cebitec.bibigrid.aws.CreateClusterAWS.SECURITY_GROUP_PREFIX;
import static de.unibi.cebitec.bibigrid.aws.CreateClusterAWS.SUBNET_PREFIX;

import de.unibi.cebitec.bibigrid.model.Port;
import de.unibi.cebitec.bibigrid.util.KEYPAIR;
import de.unibi.cebitec.bibigrid.util.SubNets;

import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public class CreateClusterEnvironmentAWS implements CreateClusterEnvironment {
    public static final Logger LOG = LoggerFactory.getLogger(CreateClusterEnvironmentAWS.class);

    private KEYPAIR keypair;
    private Vpc vpc;
    private Subnet subnet;
    private String placementGroup;
    private final CreateClusterAWS cluster;
    private String masterIp;
    private CreateSecurityGroupResult secReqResult;

    CreateClusterEnvironmentAWS(CreateClusterAWS cluster) {
        this.cluster = cluster;
    }

    @Override
    public CreateClusterEnvironmentAWS createVPC() throws ConfigurationException {
        try {
            // create KeyPair for cluster communication
            keypair = new KEYPAIR();
        } catch (JSchException ex) {
            throw new ConfigurationException(ex.getMessage());
        }
        // check for (default) VPC
        vpc = cluster.getConfig().getVpcId() == null ? getVPC() : getVPC(cluster.getConfig().getVpcId());
        if (vpc == null) {
            throw new ConfigurationException("No suitable vpc found. Define a default VPC for you account or set VPC_ID");
        } else {
            LOG.info(V, "Use VPC {} ({})%n", vpc.getVpcId(), vpc.getCidrBlock());
        }
        return this;
    }

    @Override
    public CreateClusterEnvironmentAWS createSubnet() {
        // check for unused Subnet CIDR and create one
        DescribeSubnetsRequest describesubnetsreq = new DescribeSubnetsRequest();
        DescribeSubnetsResult describesubnetres = cluster.getEc2().describeSubnets(describesubnetsreq);
        List<Subnet> loSubnets = describesubnetres.getSubnets();

        List<String> listofUsedCidr = new ArrayList<>(); // contains all subnet.cidr which are in current vpc
        for (Subnet sn : loSubnets) {
            if (sn.getVpcId().equals(vpc.getVpcId())) {
                listofUsedCidr.add(sn.getCidrBlock());
            }
        }

        SubNets subnets = new SubNets(vpc.getCidrBlock(), 24);
        String subnetCidr = subnets.nextCidr(listofUsedCidr);

        LOG.debug(V, "Use {} for generated SubNet.", subnetCidr);

        // create new subnetdir      
        CreateSubnetRequest createsubnetreq = new CreateSubnetRequest(vpc.getVpcId(), subnetCidr);
        createsubnetreq.withAvailabilityZone(cluster.getConfig().getAvailabilityZone());
        CreateSubnetResult createsubnetres = cluster.getEc2().createSubnet(createsubnetreq);
        subnet = createsubnetres.getSubnet();

        return this;
    }

    @Override
    public CreateClusterEnvironmentAWS createSecurityGroup() {
        CreateTagsRequest tagRequest = new CreateTagsRequest();
        tagRequest.withResources(subnet.getSubnetId())
                .withTags(cluster.getBibigridid(), new Tag("Name", SUBNET_PREFIX + cluster.getClusterId()));
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
                .withTags(cluster.getBibigridid(), new Tag("Name", SECURITY_GROUP_PREFIX + cluster.getClusterId()));
        cluster.getEc2().createTags(tagRequest);
        cluster.getEc2().authorizeSecurityGroupIngress(ruleChangerReq);
        return this;
    }

    private IpPermission buildIpPermission(String protocol, int fromPort, int toPort) {
        return new IpPermission().withIpProtocol(protocol).withFromPort(fromPort).withToPort(toPort);
    }

    @Override
    public CreateClusterAWS createPlacementGroup() {

        // if both instance-types fulfill the cluster specifications, create a 
        // placementGroup.
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
     * vpcIds list. If only one vpcId is given it is returned wether it is default or not. Return null in the case
     * no default or fitting VPC is found.
     */
    private Vpc getVPC(String... vpcIds) {
        DescribeVpcsRequest dvreq = new DescribeVpcsRequest();
        dvreq.setVpcIds(Arrays.asList(vpcIds));

        DescribeVpcsResult describeVpcsResult = cluster.getEc2().describeVpcs(dvreq);
        List<Vpc> lvpcs = describeVpcsResult.getVpcs();

        if (vpcIds.length == 1 && lvpcs.size() == 1) {
            return lvpcs.get(0);
        }
        if (!lvpcs.isEmpty()) {
            for (Vpc vpc_d : lvpcs) {
                if (vpc_d.isDefault()) {
                    return vpc_d;
                }
            }
        }
        return null;
    }

    public KEYPAIR getKeypair() {
        return keypair;
    }

    public void setKeypair(KEYPAIR keypair) {
        this.keypair = keypair;
    }

    public Subnet getSubnet() {
        return subnet;
    }

    public void setSubnet(Subnet subnet) {
        this.subnet = subnet;
    }

    public String getPlacementGroup() {
        return placementGroup;
    }

    public void setPlacementGroup(String placementGroup) {
        this.placementGroup = placementGroup;
    }

    public String getMasterIp() {
        return masterIp;
    }

    public CreateSecurityGroupResult getSecReqResult() {
        return secReqResult;
    }
}
