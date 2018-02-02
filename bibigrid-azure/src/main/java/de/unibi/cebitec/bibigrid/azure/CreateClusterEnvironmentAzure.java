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
    private static final String SECURITY_GROUP_PREFIX = PREFIX + "sg-";
    static final String RESOURCE_GROUP_PREFIX = PREFIX + "rg-";

    private final CreateClusterAzure cluster;
    private Network vpc;
    private Subnet subnet;
    private NetworkSecurityGroup securityGroup;
    private String masterIP;
    private String subnetCidr;
    private ResourceGroup resourceGroup;

    CreateClusterEnvironmentAzure(final CreateClusterAzure cluster) throws ConfigurationException {
        super();
        this.cluster = cluster;
        resourceGroup = cluster.getCompute().resourceGroups()
                .define(RESOURCE_GROUP_PREFIX + cluster.getClusterId())
                .withRegion(cluster.getConfig().getRegion())
                .create();
    }

    @Override
    public CreateClusterEnvironmentAzure createVPC() throws ConfigurationException {
        // check for VPC
        String vpcId = cluster.getConfig().getVpcId();
        if (vpcId == null) {
            throw new ConfigurationException("vpc id is not defined");
        }
        for (Network vpc : cluster.getCompute().networks().list()) {
            if (vpc.name().equals(vpcId)) {
                this.vpc = vpc;
                break;
            }
        }
        if (vpc == null) {
            throw new ConfigurationException("No suitable vpc found ... define a valid vpc id");
        } else {
            LOG.info(V, "Use VPC {}", vpc.name());
        }
        return this;
    }

    @Override
    public CreateClusterEnvironmentAzure createSubnet() {
        String subnetName = cluster.getConfig().getSubnetName();
        if (subnetName != null && subnetName.length() > 0) {
            for (Subnet sn : vpc.subnets().values()) {
                if (sn.name().equalsIgnoreCase(subnetName)) {
                    subnet = sn;
                    break;
                }
            }
            securityGroup = subnet.getNetworkSecurityGroup(); // TODO: security group can be null
            subnetCidr = subnet.addressPrefix();
        } else {
            securityGroup = cluster.getCompute().networkSecurityGroups()
                    .define(SECURITY_GROUP_PREFIX + cluster.getClusterId())
                    .withRegion(cluster.getConfig().getRegion())
                    .withExistingResourceGroup(resourceGroup)
                    .create();
            // check for unused Subnet Cidr and create one
            List<String> listOfUsedCidr = new ArrayList<>(); // contains all subnet.cidr which are in current vpc
            for (Subnet sn : vpc.subnets().values()) {
                listOfUsedCidr.add(sn.addressPrefix());
            }
            SubNets subnets = new SubNets(vpc.addressSpaces().size() > 0 ? vpc.addressSpaces().get(0) : "10.128.0.0", 24);
            subnetCidr = subnets.nextCidr(listOfUsedCidr); // TODO: not generating correct next cidr according to azure
            LOG.debug(V, "Use {} for generated subnet.", subnetCidr);
            // create new subnet
            try {
                subnetName = SUBNET_PREFIX + cluster.getClusterId();
                vpc = vpc.update()
                        .defineSubnet(subnetName)
                        .withAddressPrefix(subnetCidr)
                        .withExistingNetworkSecurityGroup(securityGroup)
                        .attach()
                        .apply();
                subnet = vpc.subnets().get(subnetName);
            } catch (Exception e) {
                LOG.error("Failed to create subnet {}", e);
            }
        }
        return this;
    }

    @Override
    public CreateClusterEnvironmentAzure createSecurityGroup() {
        // create security group with full internal access / ssh from outside
        LOG.info("Creating security group...");

        // Master IP
        masterIP = SubNets.getFirstIP(subnet.addressPrefix());
        LOG.debug(V, "masterIP: {}.", masterIP);

        // Create the firewall rules
        try {
            NetworkSecurityGroup.Update update = securityGroup.update();
            int ruleIndex = 1;
            update = addSecurityRule(update, ruleIndex++, "0.0.0.0/0", SecurityRuleProtocol.TCP, 22, 22);
            update = addSecurityRule(update, ruleIndex++, subnet.addressPrefix(), SecurityRuleProtocol.TCP, 0, 65535);
            update = addSecurityRule(update, ruleIndex++, subnet.addressPrefix(), SecurityRuleProtocol.UDP, 0, 65535);
            update = addSecurityRule(update, ruleIndex++, subnet.addressPrefix(), new SecurityRuleProtocol("icmp"), -1, -1);
            for (Port port : cluster.getConfig().getPorts()) {
                LOG.info(port.toString());
                update = addSecurityRule(update, ruleIndex++, port.ipRange, SecurityRuleProtocol.TCP, port.number, port.number);
                update = addSecurityRule(update, ruleIndex++, port.ipRange, SecurityRuleProtocol.UDP, port.number, port.number);
            }
            securityGroup = update.apply();
        } catch (Exception e) {
            LOG.error("Failed to create firewall rules {}", e);
        }
        return this;
    }

    private NetworkSecurityGroup.Update addSecurityRule(NetworkSecurityGroup.Update update, int nameIndex,
                                                        String address, SecurityRuleProtocol protocol, int portFrom,
                                                        int portTo) {
        String name = SECURITY_GROUP_PREFIX + "rule" + nameIndex + "-" + cluster.getClusterId();
        NetworkSecurityRule.UpdateDefinitionStages.WithSourcePort<NetworkSecurityGroup.Update> withSourcePort =
                update.defineRule(name).allowInbound().fromAddress(address);
        NetworkSecurityRule.UpdateDefinitionStages.WithDestinationAddress<NetworkSecurityGroup.Update> withDestinationAddress =
                portFrom == -1 && portTo == -1 ? withSourcePort.fromAnyPort() :
                        portFrom == portTo ? withSourcePort.fromPort(portFrom) : withSourcePort.fromPortRange(portFrom, portTo);
        NetworkSecurityRule.UpdateDefinitionStages.WithDestinationPort<NetworkSecurityGroup.Update> withDestinationPort =
                withDestinationAddress.toAddress(address);
        NetworkSecurityRule.UpdateDefinitionStages.WithProtocol<NetworkSecurityGroup.Update> withProtocol =
                portFrom == -1 && portTo == -1 ? withDestinationPort.toAnyPort() :
                        portFrom == portTo ? withDestinationPort.toPort(portFrom) : withDestinationPort.toPortRange(portFrom, portTo);
        return withProtocol.withProtocol(protocol).attach();
    }

    @Override
    public CreateClusterAzure createPlacementGroup() {
        return cluster;
    }

    String getMasterIP() {
        return masterIP;
    }

    String getSubnetCidr() {
        return subnetCidr;
    }

    public ResourceGroup getResourceGroup() {
        return resourceGroup;
    }

    public Network getVpc() {
        return vpc;
    }

    public Subnet getSubnet() {
        return subnet;
    }
}