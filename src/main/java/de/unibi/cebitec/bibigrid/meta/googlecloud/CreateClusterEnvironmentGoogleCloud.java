package de.unibi.cebitec.bibigrid.meta.googlecloud;

import com.google.cloud.WaitForOption;
import com.google.cloud.compute.*;
import com.jcraft.jsch.JSchException;
import de.unibi.cebitec.bibigrid.exception.ConfigurationException;
import de.unibi.cebitec.bibigrid.meta.CreateClusterEnvironment;
import de.unibi.cebitec.bibigrid.util.KEYPAIR;
import de.unibi.cebitec.bibigrid.util.SubNets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static de.unibi.cebitec.bibigrid.meta.googlecloud.CreateClusterGoogleCloud.SUBNET_PREFIX;
import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;

/**
 * Implementation of the general CreateClusterEnvironment interface for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class CreateClusterEnvironmentGoogleCloud implements CreateClusterEnvironment<CreateClusterEnvironmentGoogleCloud, CreateClusterGoogleCloud> {
    public static final Logger log = LoggerFactory.getLogger(CreateClusterEnvironmentGoogleCloud.class);

    private final CreateClusterGoogleCloud cluster;
    private KEYPAIR keypair;
    private Network vpc;
    private Subnetwork subnet;

    CreateClusterEnvironmentGoogleCloud(final CreateClusterGoogleCloud cluster) {
        this.cluster = cluster;
    }

    public CreateClusterEnvironmentGoogleCloud createVPC() throws ConfigurationException {
        try {
            // create KeyPair for cluster communication
            keypair = new KEYPAIR();
        } catch (JSchException ex) {
            throw new ConfigurationException(ex.getMessage());
        }

        // check for (default) VPC
        String vpcId = cluster.getConfig().getVpcid() == null ? "default" : cluster.getConfig().getVpcid();
        vpc = getVpcById(vpcId);
        if (vpc == null) {
            throw new ConfigurationException("No suitable vpc found ... define a default VPC for you account or set VPC_ID");
        } else {
            log.info(V, "Use VPC {} ({})%n", vpc.getNetworkId().getNetwork(), vpc.getCidrBlock());
        }
        return this;
    }

    private Network getVpcById(String vpcId) {
        for (Network vpc : cluster.getCompute().listNetworks().iterateAll()) {
            if (vpc.getNetworkId().getNetwork().equals(vpcId))
                return vpc;
        }
        return null;
    }

    public CreateClusterEnvironmentGoogleCloud createSubnet() throws ConfigurationException {
        String region = cluster.getConfig().getRegion();

        // check for unused Subnet Cidr and create one
        List<String> listofUsedCidr = new ArrayList<>(); // contains all subnet.cidr which are in current vpc
        for (Subnetwork sn : cluster.getCompute().listSubnetworks(region).iterateAll()) {
            if (sn.getNetwork().getSelfLink().equals(vpc.getNetworkId().getSelfLink())) {
                listofUsedCidr.add(sn.getIpRange());
            }
        }
        SubNets subnets = new SubNets(vpc.getCidrBlock(), 24);
        String subnetCidr = subnets.nextCidr(listofUsedCidr);
        log.debug(V, "Use {} for generated SubNet.", subnetCidr);

        // create new subnet
        SubnetworkId subnetId = SubnetworkId.of(region, SUBNET_PREFIX + cluster.getClusterId());
        Operation createSubnetOperation = vpc.createSubnetwork(subnetId, subnetCidr);
        try {
            createSubnetOperation.waitFor(WaitForOption.checkEvery(1, TimeUnit.SECONDS));
            subnet = cluster.getCompute().getSubnetwork(subnetId);
        } catch (InterruptedException | TimeoutException e) {
            log.error("Failed to create subnetwork {}", e);
        }
        return this;
    }

    public CreateClusterEnvironmentGoogleCloud createSecurityGroup() throws ConfigurationException {
        // create security group with full internal access / ssh from outside
        log.info("Creating security group...");

        /* TODO
        // MASTERIP
        MASTERIP = SubNets.getFirstIP(subnet.getCidrBlock());

        UserIdGroupPair secGroupSelf = new UserIdGroupPair().withGroupId(secReqResult.getGroupId());

        tcp     22  22      0.0.0.0/0
        tcp     0   65535               secGroupSelf
        udp     0   65535               secGroupSelf
        icmp    -1   -1                 secGroupSelf

        for (Port port : cluster.getConfig().getPorts()) {
            tcp     port.number     port.number     port.iprange
            udp     port.number     port.number     port.iprange
        }

        AuthorizeSecurityGroupIngressRequest ruleChangerReq = new AuthorizeSecurityGroupIngressRequest();
        ruleChangerReq.withGroupId(secReqResult.getGroupId()).withIpPermissions(allIpPermissions);
        */
        return this;
    }

    public CreateClusterGoogleCloud createPlacementGroup() throws ConfigurationException {
        return cluster;
    }

    public KEYPAIR getKeypair() {
        return keypair;
    }

    public Subnetwork getSubnet() {
        return subnet;
    }
}