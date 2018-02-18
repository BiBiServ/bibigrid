package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.services.compute.model.Firewall;
import com.google.api.services.compute.model.Network;
import com.google.api.services.compute.model.Operation;
import com.google.api.services.compute.model.Subnetwork;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.intents.CreateClusterEnvironment;
import de.unibi.cebitec.bibigrid.core.model.Port;
import de.unibi.cebitec.bibigrid.core.util.SubNets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private Network network;
    private Subnetwork subnet;
    private String masterIP;
    private String subnetCidr;

    CreateClusterEnvironmentGoogleCloud(final CreateClusterGoogleCloud cluster) throws ConfigurationException {
        super();
        this.cluster = cluster;
    }

    @Override
    public CreateClusterEnvironmentGoogleCloud createNetwork() throws ConfigurationException {
        // check for (default) network
        String networkId = cluster.getConfig().getNetwork() == null ? "default" : cluster.getConfig().getNetwork();
        try {
            String projectId = ((ConfigurationGoogleCloud) cluster.getConfig()).getGoogleProjectId();
            for (Network network : cluster.getCompute().networks().list(projectId).execute().getItems()) {
                if (network.getName().equals(networkId)) {
                    this.network = network;
                    break;
                }
            }
        } catch (IOException ignored) {
        }
        if (network == null) {
            throw new ConfigurationException("No suitable network found! Define a default network for you account or a valid network id.");
        }
        LOG.info(V, "Use Network '{}'.", network.getName());
        return this;
    }

    @Override
    public CreateClusterEnvironmentGoogleCloud createSubnet() throws ConfigurationException {
        ConfigurationGoogleCloud config = (ConfigurationGoogleCloud) cluster.getConfig();
        // As default we assume that the network is auto creating subnets. Then we can only reuse the default
        // subnets and can't create new ones!
        if (network.getAutoCreateSubnetworks()) {
            reuseAutoCreateSubnet(config);
        } else {
            createNewSubnet(config);
        }
        return this;
    }

    private void reuseAutoCreateSubnet(ConfigurationGoogleCloud config) throws ConfigurationException {
        String region = config.getRegion();
        String projectId = config.getGoogleProjectId();
        // Reuse the first (and only) subnet for the network and region.
        List<Subnetwork> subnets = GoogleCloudUtils.listSubnetworks(cluster.getCompute(), projectId, region);
        for (Subnetwork subnet : subnets) {
            if (subnet.getNetwork().equals(network.getName())) {
                this.subnet = subnet;
                break;
            }
        }
        if (subnet == null) {
            throw new ConfigurationException(
                    String.format("No suitable subnet found with region '%s' in auto creating network '%s'!",
                            region, network.getName()));
        }
        subnetCidr = subnet.getIpCidrRange();
        LOG.debug(V, "Use '{}' for reused subnet.", subnetCidr);
    }

    private void createNewSubnet(ConfigurationGoogleCloud config) {
        String region = config.getRegion();
        String projectId = config.getGoogleProjectId();
        // check for unused Subnet Cidr and create one
        List<String> listOfUsedCidr = new ArrayList<>(); // contains all subnet.cidr which are in current network
        for (Subnetwork sn : GoogleCloudUtils.listSubnetworks(cluster.getCompute(), projectId, region)) {
            if (sn.getNetwork().equals(network.getSelfLink())) {
                listOfUsedCidr.add(sn.getIpCidrRange());
            }
        }
        SubNets subnets = new SubNets(listOfUsedCidr.size() > 0 ? listOfUsedCidr.get(0) : "10.128.0.0", 24);
        subnetCidr = subnets.nextCidr(listOfUsedCidr);
        LOG.debug(V, "Use '{}' for generated subnet.", subnetCidr);
        // create new subnet
        try {
            Subnetwork subnetwork = new Subnetwork().setIpCidrRange(subnetCidr).setRegion(region)
                    .setName(SUBNET_PREFIX + cluster.getClusterId());
            Operation createSubnetOperation = cluster.getCompute().subnetworks()
                    .insert(projectId, region, subnetwork).execute();
            GoogleCloudUtils.waitForOperation(cluster.getCompute(), config, createSubnetOperation);
            subnet = cluster.getCompute().subnetworks().get(projectId, region, subnetwork.getName()).execute();
        } catch (Exception e) {
            LOG.error("Failed to create subnet. {}", e);
        }
    }

    @Override
    public CreateClusterEnvironmentGoogleCloud createSecurityGroup() {
        // create security group with full internal access / ssh from outside
        LOG.info("Creating security group...");

        // Master IP
        masterIP = SubNets.getFirstIP(subnet.getIpCidrRange());
        LOG.debug(V, "masterIP: {}.", masterIP);

        // Collect all firewall rules grouped by the source ip range because the number of rules
        // is limited and therefore should be combined!
        Map<String, List<Firewall.Allowed>> firewallRuleMap = new HashMap<>();
        firewallRuleMap.put("0.0.0.0/0", new ArrayList<>());
        firewallRuleMap.get("0.0.0.0/0").add(buildFirewallRule("tcp", "22"));
        firewallRuleMap.put(subnet.getIpCidrRange(), new ArrayList<>());
        firewallRuleMap.get(subnet.getIpCidrRange()).add(buildFirewallRule("tcp", "0-65535"));
        firewallRuleMap.get(subnet.getIpCidrRange()).add(buildFirewallRule("udp", "0-65535"));
        firewallRuleMap.get(subnet.getIpCidrRange()).add(buildFirewallRule("icmp"));
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
            for (Map.Entry<String, List<Firewall.Allowed>> rule : firewallRuleMap.entrySet()) {
                Firewall firewall = new Firewall()
                        .setName(SECURITY_GROUP_PREFIX + "rule" + ruleIndex + "-" + cluster.getClusterId())
                        .setNetwork(network.getSelfLink())
                        .setSourceRanges(Collections.singletonList(rule.getKey()));
                ruleIndex++;
                // TODO: possibly add cluster instance ids to targetTags, to limit the access!
                firewall.setAllowed(rule.getValue());
                cluster.getCompute().firewalls().insert(
                        ((ConfigurationGoogleCloud) cluster.getConfig()).getGoogleProjectId(),
                        firewall).execute();
            }
        } catch (Exception e) {
            LOG.error("Failed to create firewall rules. {}", e);
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
    public CreateClusterGoogleCloud createPlacementGroup() {
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