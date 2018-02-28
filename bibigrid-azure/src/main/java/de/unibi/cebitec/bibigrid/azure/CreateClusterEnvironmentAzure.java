package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.management.network.*;
import com.microsoft.azure.management.resources.ResourceGroup;
import de.unibi.cebitec.bibigrid.core.model.Port;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.intents.CreateClusterEnvironment;
import de.unibi.cebitec.bibigrid.core.util.SubNets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static de.unibi.cebitec.bibigrid.core.intents.CreateCluster.PREFIX;
import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

/**
 * Implementation of the general CreateClusterEnvironment interface for an Azure based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class CreateClusterEnvironmentAzure extends CreateClusterEnvironment {
    private static final Logger LOG = LoggerFactory.getLogger(CreateClusterEnvironmentAzure.class);
    static final String RESOURCE_GROUP_PREFIX = PREFIX + "rg-";

    private final CreateClusterAzure cluster;
    private NetworkSecurityGroup securityGroup;
    private String masterIP;
    private ResourceGroup resourceGroup;

    CreateClusterEnvironmentAzure(final CreateClusterAzure cluster) throws ConfigurationException {
        super(cluster);
        this.cluster = cluster;
        resourceGroup = cluster.getCompute().resourceGroups()
                .define(RESOURCE_GROUP_PREFIX + cluster.getClusterId())
                .withRegion(getConfig().getRegion())
                .create();
    }

    @Override
    protected NetworkAzure getNetworkOrDefault(String networkId) {
        if (networkId != null) {
            for (Network network : cluster.getCompute().networks().list()) {
                if (network.name().equals(networkId)) {
                    return new NetworkAzure(network);
                }
            }
        }
        return null;
    }

    @Override
    public CreateClusterEnvironmentAzure createSubnet() {
        Network network = ((NetworkAzure) this.network).getInternal();
        String subnetName = getConfig().getSubnet();
        if (subnetName != null && subnetName.length() > 0) {
            for (Subnet sn : network.subnets().values()) {
                if (sn.name().equalsIgnoreCase(subnetName)) {
                    subnet = new SubnetAzure(sn);
                    break;
                }
            }
            securityGroup = ((SubnetAzure) subnet).getInternal().getNetworkSecurityGroup(); // TODO: security group can be null
        } else {
            securityGroup = cluster.getCompute().networkSecurityGroups()
                    .define(SECURITY_GROUP_PREFIX + cluster.getClusterId())
                    .withRegion(getConfig().getRegion())
                    .withExistingResourceGroup(resourceGroup)
                    .create();
            // check for unused Subnet Cidr and create one
            List<String> listOfUsedCidr = new ArrayList<>(); // contains all subnet.cidr which are in current network
            for (Subnet sn : network.subnets().values()) {
                listOfUsedCidr.add(sn.addressPrefix());
            }
            SubNets subnets = new SubNets(this.network.getCidr() != null ? this.network.getCidr() : "10.128.0.0", 24);
            String subnetCidr = subnets.nextCidr(listOfUsedCidr); // TODO: not generating correct next cidr according to azure
            LOG.debug(V, "Use {} for generated subnet.", subnetCidr);
            // create new subnet
            try {
                subnetName = SUBNET_PREFIX + cluster.getClusterId();
                network = network.update()
                        .defineSubnet(subnetName)
                        .withAddressPrefix(subnetCidr)
                        .withExistingNetworkSecurityGroup(securityGroup)
                        .attach()
                        .apply();
                this.network = new NetworkAzure(network);
                subnet = new SubnetAzure(network.subnets().get(subnetName));
            } catch (Exception e) {
                LOG.error("Failed to create subnet. {}", e);
            }
        }
        return this;
    }

    @Override
    public CreateClusterEnvironmentAzure createSecurityGroup() {
        // create security group with full internal access / ssh from outside
        LOG.info("Creating security group...");

        // Master IP
        masterIP = SubNets.getFirstIP(subnet.getCidr());
        LOG.debug(V, "masterIP: {}.", masterIP);

        // Create the firewall rules
        try {
            NetworkSecurityGroup.Update update = securityGroup.update();
            int ruleIndex = 1;
            update = addSecurityRule(update, ruleIndex++, "0.0.0.0/0", SecurityRuleProtocol.TCP, 22);
            update = addSecurityRule(update, ruleIndex++, subnet.getCidr(), SecurityRuleProtocol.TCP, 0, 65535);
            update = addSecurityRule(update, ruleIndex++, subnet.getCidr(), SecurityRuleProtocol.UDP, 0, 65535);
            update = addSecurityRule(update, ruleIndex++, subnet.getCidr(), new SecurityRuleProtocol("icmp"));
            for (Port port : getConfig().getPorts()) {
                LOG.info(port.toString());
                update = addSecurityRule(update, ruleIndex++, port.ipRange, SecurityRuleProtocol.TCP, port.number);
                update = addSecurityRule(update, ruleIndex++, port.ipRange, SecurityRuleProtocol.UDP, port.number);
            }
            securityGroup = update.apply();
        } catch (Exception e) {
            LOG.error("Failed to create firewall rules. {}", e);
        }
        return this;
    }

    @SuppressWarnings("SameParameterValue")
    private NetworkSecurityGroup.Update addSecurityRule(NetworkSecurityGroup.Update update, int nameIndex,
                                                        String address, SecurityRuleProtocol protocol, int portFrom,
                                                        int portTo) {
        String name = SECURITY_GROUP_PREFIX + "rule" + nameIndex + "-" + cluster.getClusterId();
        return update.defineRule(name)
                .allowInbound()
                .fromAddress(address)
                .fromPortRange(portFrom, portTo)
                .toAddress(address)
                .toPortRange(portFrom, portTo)
                .withProtocol(protocol)
                .attach();
    }

    private NetworkSecurityGroup.Update addSecurityRule(NetworkSecurityGroup.Update update, int nameIndex,
                                                        String address, SecurityRuleProtocol protocol, int port) {
        String name = SECURITY_GROUP_PREFIX + "rule" + nameIndex + "-" + cluster.getClusterId();
        return update.defineRule(name)
                .allowInbound()
                .fromAddress(address)
                .fromPort(port)
                .toAddress(address)
                .toPort(port)
                .withProtocol(protocol)
                .attach();
    }

    private NetworkSecurityGroup.Update addSecurityRule(NetworkSecurityGroup.Update update, int nameIndex,
                                                        String address, SecurityRuleProtocol protocol) {
        String name = SECURITY_GROUP_PREFIX + "rule" + nameIndex + "-" + cluster.getClusterId();
        return update.defineRule(name)
                .allowInbound()
                .fromAddress(address)
                .fromAnyPort()
                .toAddress(address)
                .toAnyPort()
                .withProtocol(protocol)
                .attach();
    }

    String getMasterIP() {
        return masterIP;
    }

    public ResourceGroup getResourceGroup() {
        return resourceGroup;
    }
}