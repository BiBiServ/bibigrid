package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.services.compute.model.*;
import com.google.cloud.RetryOption;
import com.google.cloud.compute.*;
import com.google.cloud.compute.Network;
import com.google.cloud.compute.Operation;
import com.google.cloud.compute.Subnetwork;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.intents.CreateClusterEnvironment;
import de.unibi.cebitec.bibigrid.core.model.Port;
import de.unibi.cebitec.bibigrid.core.util.SubNets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.bp.Duration;

import java.io.IOException;
import java.util.*;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

/**
 * Implementation of the general CreateClusterEnvironment interface for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class CreateClusterEnvironmentGoogleCloud extends CreateClusterEnvironment {
    private static final Logger LOG = LoggerFactory.getLogger(CreateClusterEnvironmentGoogleCloud.class);

    private final CreateClusterGoogleCloud cluster;
    private Network vpc;
    private Subnetwork subnet;
    private String masterIP;
    private String subnetCidr;

    CreateClusterEnvironmentGoogleCloud(final CreateClusterGoogleCloud cluster) throws ConfigurationException {
        super();
        this.cluster = cluster;
    }

    @Override
    public CreateClusterEnvironmentGoogleCloud createVPC() throws ConfigurationException {
        // check for (default) VPC
        String vpcId = cluster.getConfig().getVpcId() == null ? "default" : cluster.getConfig().getVpcId();
        vpc = getVpcById(vpcId);
        if (vpc == null) {
            throw new ConfigurationException("No suitable vpc found ... define a default VPC for you account or set VPC_ID");
        } else {
            LOG.info(V, "Use VPC {}", vpc.getNetworkId().getNetwork());
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

    @Override
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
                    LOG.debug(V, "VPC auto create is {}.", isAutoCreate);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (isAutoCreate) {
            // Reuse the first (and only) subnet for the vpc and region.
            subnet = cluster.getCompute().listSubnetworks(region).iterateAll().iterator().next();
            LOG.debug(V, "Use {} for reused SubNet.", subnet.getIpRange());
        } else {
            // check for unused Subnet Cidr and create one
            List<String> listOfUsedCidr = new ArrayList<>(); // contains all subnet.cidr which are in current vpc
            for (Subnetwork sn : cluster.getCompute().listSubnetworks(region).iterateAll()) {
                if (sn.getNetwork().getSelfLink().equals(vpc.getNetworkId().getSelfLink())) {
                    listOfUsedCidr.add(sn.getIpRange());
                }
            }
            SubNets subnets = new SubNets(listOfUsedCidr.size() > 0 ? listOfUsedCidr.get(0) : "10.128.0.0", 24);
            subnetCidr = subnets.nextCidr(listOfUsedCidr);
            LOG.debug(V, "Use {} for generated SubNet.", subnetCidr);

            // create new subnet
            SubnetworkId subnetId = SubnetworkId.of(region, SUBNET_PREFIX + cluster.getClusterId());
            Operation createSubnetOperation = vpc.createSubnetwork(subnetId, subnetCidr);
            try {
                createSubnetOperation.waitFor(RetryOption.initialRetryDelay(Duration.ZERO));
                subnet = cluster.getCompute().getSubnetwork(subnetId);
            } catch (InterruptedException e) {
                LOG.error("Failed to create subnetwork {}", e);
            }
        }
        return this;
    }

    @Override
    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    public CreateClusterEnvironmentGoogleCloud createSecurityGroup() throws ConfigurationException {
        // create security group with full internal access / ssh from outside
        LOG.info("Creating security group...");

        // Master IP
        masterIP = SubNets.getFirstIP(subnet.getIpRange());
        LOG.debug(V, "masterIP: {}.", masterIP);

        // Since the google cloud java library currently doesn't support firewall rules,
        // we use the underlying api library.
        com.google.api.services.compute.Compute internalCompute =
                GoogleCloudUtils.getInternalCompute(cluster.getCompute());
        if (internalCompute == null) {
            LOG.error("Failed to create firewall rules. Unable to get internal compute");
            return this;
        }

        // Collect all firewall rules grouped by the source ip range because the number of rules
        // is limited and therefore should be combined!
        Map<String, List<Firewall.Allowed>> firewallRuleMap = new HashMap<>();
        firewallRuleMap.put("0.0.0.0/0", new ArrayList<>());
        firewallRuleMap.get("0.0.0.0/0").add(buildFirewallRule("tcp", "22", "3389"));
        firewallRuleMap.get("0.0.0.0/0").add(buildFirewallRule("icmp"));
        firewallRuleMap.put(subnet.getIpRange(), new ArrayList<>());
        firewallRuleMap.get(subnet.getIpRange()).add(buildFirewallRule("tcp", "0-65535"));
        firewallRuleMap.get(subnet.getIpRange()).add(buildFirewallRule("udp", "0-65535"));
        firewallRuleMap.get(subnet.getIpRange()).add(buildFirewallRule("icmp"));
        for (Port port : cluster.getConfig().getPorts()) {
            LOG.info(port.toString());
            if (!firewallRuleMap.containsKey(port.ipRange)) {
                firewallRuleMap.put(port.ipRange, new ArrayList<>());
            }
            List<String> portList = Collections.singletonList(String.valueOf(port.number));
            firewallRuleMap.get(port.ipRange).add(buildFirewallRule("tcp").setPorts(portList));
            firewallRuleMap.get(port.ipRange).add(buildFirewallRule("udp").setPorts(portList));
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
            LOG.error("Failed to create firewall rules {}", e);
        }
        return this;
    }

    private Firewall.Allowed buildFirewallRule(String protocol, String... ports) {
        Firewall.Allowed rule = new Firewall.Allowed().setIPProtocol(protocol);
        if (ports.length > 0) {
            rule.setPorts(Arrays.asList(ports));
        }
        return rule;
    }

    @Override
    public CreateClusterGoogleCloud createPlacementGroup() throws ConfigurationException {
        return cluster;
    }

    Subnetwork getSubnet() {
        return subnet;
    }

    String getMasterIP() {
        return masterIP;
    }

    String getSubnetCidr() {
        return subnetCidr;
    }
}