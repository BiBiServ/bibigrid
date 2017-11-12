package de.unibi.cebitec.bibigrid.meta.googlecloud;

import com.google.api.services.compute.model.*;
import com.google.cloud.WaitForOption;
import com.google.cloud.compute.*;
import com.google.cloud.compute.Network;
import com.google.cloud.compute.Operation;
import com.google.cloud.compute.Subnetwork;
import com.jcraft.jsch.JSchException;
import de.unibi.cebitec.bibigrid.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.meta.CreateClusterEnvironment;
import de.unibi.cebitec.bibigrid.model.Port;
import de.unibi.cebitec.bibigrid.util.KEYPAIR;
import de.unibi.cebitec.bibigrid.util.SubNets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static de.unibi.cebitec.bibigrid.meta.googlecloud.CreateClusterGoogleCloud.SECURITY_GROUP_PREFIX;
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
    private String masterIP;

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
            log.info(V, "Use VPC {}%n", vpc.getNetworkId().getNetwork());
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

        // Use the internal compute api to get more information about the vpc to be used.
        com.google.api.services.compute.Compute internalCompute =
                GoogleCloudUtils.getInternalCompute(cluster.getCompute());

        // As default we assume that the vpc is auto creating subnets. Then we can
        // only reuse the default subnets and can't create new ones!
        boolean isAutoCreate = true;
        try {
            if (internalCompute != null) {
                com.google.api.services.compute.model.Network internalVpc = internalCompute.networks().get(
                        cluster.getConfig().getGoogleProjectId(),
                        vpc.getNetworkId().getNetwork()).execute();
                if (internalVpc != null) {
                    isAutoCreate = internalVpc.getAutoCreateSubnetworks();
                    log.debug(V, "VPC auto create is {}.", isAutoCreate);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (isAutoCreate) {
            // Reuse the first (and only) subnet for the vpc and region.
            subnet = cluster.getCompute().listSubnetworks(region).iterateAll().iterator().next();
            log.debug(V, "Use {} for reused SubNet.", subnet.getIpRange());
        } else {
            // check for unused Subnet Cidr and create one
            List<String> listofUsedCidr = new ArrayList<>(); // contains all subnet.cidr which are in current vpc
            for (Subnetwork sn : cluster.getCompute().listSubnetworks(region).iterateAll()) {
                if (sn.getNetwork().getSelfLink().equals(vpc.getNetworkId().getSelfLink())) {
                    listofUsedCidr.add(sn.getIpRange());
                }
            }
            SubNets subnets = new SubNets(listofUsedCidr.size() > 0 ? listofUsedCidr.get(0) : "10.128.0.0", 24);
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
        }
        return this;
    }

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    public CreateClusterEnvironmentGoogleCloud createSecurityGroup() throws ConfigurationException {
        // create security group with full internal access / ssh from outside
        log.info("Creating security group...");

        // Master IP
        masterIP = SubNets.getFirstIP(subnet.getIpRange());
        log.debug(V, "masterIP: {}.", masterIP);

        // Since the google cloud java library currently doesn't support firewall rules,
        // we use the underlying api library.
        com.google.api.services.compute.Compute internalCompute =
                GoogleCloudUtils.getInternalCompute(cluster.getCompute());
        if (internalCompute == null) {
            log.error("Failed to create firewall rules. Unable to get internal compute");
            return this;
        }

        // Collect all firewall rules grouped by the source ip range because the number of rules
        // is limited and therefore should be combined!
        Map<String, List<Firewall.Allowed>> firewallRuleMap = new HashMap<>();
        firewallRuleMap.put("0.0.0.0/0", new ArrayList<>());
        firewallRuleMap.get("0.0.0.0/0").add(new Firewall.Allowed().setIPProtocol("tcp").setPorts(Arrays.asList("22", "3389")));
        firewallRuleMap.get("0.0.0.0/0").add(new Firewall.Allowed().setIPProtocol("icmp"));
        firewallRuleMap.put(subnet.getIpRange(), new ArrayList<>());
        firewallRuleMap.get(subnet.getIpRange()).add(new Firewall.Allowed().setIPProtocol("tcp").setPorts(Arrays.asList("0-65535")));
        firewallRuleMap.get(subnet.getIpRange()).add(new Firewall.Allowed().setIPProtocol("udp").setPorts(Arrays.asList("0-65535")));
        firewallRuleMap.get(subnet.getIpRange()).add(new Firewall.Allowed().setIPProtocol("icmp"));
        for (Port port : cluster.getConfig().getPorts()) {
            log.info(port.toString());
            if (!firewallRuleMap.containsKey(port.ipRange)) {
                firewallRuleMap.put(port.ipRange, new ArrayList<>());
            }
            List<String> portList = Collections.singletonList(String.valueOf(port.number));
            firewallRuleMap.get(port.ipRange).add(new Firewall.Allowed().setIPProtocol("tcp").setPorts(portList));
            firewallRuleMap.get(port.ipRange).add(new Firewall.Allowed().setIPProtocol("udp").setPorts(portList));
        }

        // Create the firewall rules
        try {
            int ruleIndex = 1;
            for (String ipRange : firewallRuleMap.keySet()) {
                Firewall firewall = new Firewall()
                        .setName(SECURITY_GROUP_PREFIX + "rule" + ruleIndex + "-" + cluster.getClusterId())
                        .setNetwork(vpc.getNetworkId().getSelfLink())
                        .setSourceRanges(Collections.singletonList(ipRange));
                ruleIndex++;
                // TODO: possibly add cluster instance ids to targetTags, to limit the access!
                firewall.setAllowed(firewallRuleMap.get(ipRange));
                internalCompute.firewalls().insert(cluster.getConfig().getGoogleProjectId(), firewall).execute();
            }
        } catch (Exception e) {
            log.error("Failed to create firewall rules {}", e);
        }
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

    String getMasterIP() {
        return masterIP;
    }
}