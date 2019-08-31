package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.intents.CreateClusterEnvironment;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.model.Port;
import de.unibi.cebitec.bibigrid.core.util.SubNets;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

import java.util.ArrayList;
import java.util.List;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ComputeSecurityGroupService;
import org.openstack4j.api.exceptions.ClientResponseException;
import org.openstack4j.api.networking.PortService;
import org.openstack4j.model.compute.IPProtocol;
import org.openstack4j.model.compute.SecGroupExtension;
import org.openstack4j.model.compute.builder.SecurityGroupRuleBuilder;
import org.openstack4j.model.network.AttachInterfaceType;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.IPVersionType;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.model.network.options.PortListOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prepare the cloud environment for an OpenStack cluster.
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class CreateClusterEnvironmentOpenstack extends CreateClusterEnvironment {
    private static final Logger LOG = LoggerFactory.getLogger(CreateClusterEnvironmentOpenstack.class);
    private static final String NETWORK_CIDR = "192.168.0.0/16";

    private CreateClusterOpenstack cluster;
    private SecGroupExtension sge;

    CreateClusterEnvironmentOpenstack(Client client, CreateClusterOpenstack cluster) throws ConfigurationException {
        super(client, cluster);
        this.cluster = cluster;
    }

    @Override
    public CreateClusterEnvironmentOpenstack createNetwork() {
        // complete setup is done by createSubnet
        return this;
    }

    @Override
    public CreateClusterEnvironmentOpenstack createSubnet() throws ConfigurationException {
        Router router = null;
        Subnet subnet;
        Network network = null;
        try {
            // User can specify three parameters to define the network connection for a BiBiGrid cluster
            // 1) router
            // 2) network
            // 3) subnet
            OSClient osc = cluster.getClient();
            ConfigurationOpenstack cfg = (ConfigurationOpenstack) getConfig();
            // if subnet is set just use it
            if (cfg.getSubnet() != null) {
                // check if subnet exists
                subnet = getSubnetByIdOrName(osc, cfg.getSubnet());
                if (subnet == null) {
                    throw new ConfigurationException("No subnet with id '" + cfg.getSubnet() + "' found!");
                }
                network = getNetworkByIdOrName(osc, subnet.getNetworkId());
                if (network == null) {
                    throw new ConfigurationException("No network with id '" + subnet.getNetworkId() + "' found!");
                }
                router = getRouterByNetwork(osc, network.getId(), subnet.getId()); // @ToDo
                LOG.info("Using existing subnet. (ID: {}, CIDR: {})", subnet.getId(), subnet.getCidr());
                this.subnet = new SubnetOpenstack(subnet);
                this.network = new NetworkOpenstack(network, router);
                return this;
            }
            // if network is set try to determine router
            if (cfg.getNetwork() != null) {
                // check if net exists
                network = getNetworkByIdOrName(osc, cfg.getNetwork());
                if (network == null) {
                    throw new ConfigurationException("No network with name '" + cfg.getSubnet() + "' found!");
                }
                router = getRouterByNetwork(osc, network);
                LOG.info("Using existing network (ID: {}) and router (ID: {}).", network.getId(), router.getId());
            }
            if (router == null) {
                if (cfg.getRouter() == null) {
                    throw new ConfigurationException("No router found and no router name provided!");
                } else {
                    router = getRouterByIdOrName(osc, cfg.getRouter());
                    if (router == null) {
                        throw new ConfigurationException("No router with name '" + cfg.getRouter() + "' found!");
                    }
                    LOG.info("Using existing router (ID: {}).", router.getId());
                }
                // create a new network
                network = osc.networking().network().create(Builders.network()
                        .name(NETWORK_PREFIX + cluster.getClusterId())
                        .adminStateUp(true)
                        .build());
                cfg.setNetwork(network.getName());
                LOG.info("Network (ID: {}, NAME: {}) created.", network.getId(), network.getName());
            }
            // get new free /24 subnet
            SubNets sn = new SubNets(NETWORK_CIDR, 24);
            List<String> usedCIDR = new ArrayList<>();
            for (org.openstack4j.model.network.Port p : getPortsByRouter(osc, router)) {
                Network portNetwork = getNetworkByIdOrName(osc, p.getNetworkId());
                if (portNetwork != null && portNetwork.getNeutronSubnets() != null) {
                    for (Subnet s : portNetwork.getNeutronSubnets()) {
                        usedCIDR.add(s.getCidr());
                    }
                }
            }
            String CIDR = sn.nextCidr(usedCIDR);
            if (CIDR == null) {
                throw new ConfigurationException("No free /24 network found in " + NETWORK_CIDR + " for router " + router.getName());
            }
            // now we can create a new subnet
            subnet = osc.networking().subnet().create(Builders.subnet()
                    .name(SUBNET_PREFIX + cluster.getClusterId())
                    .network(network)
                    .ipVersion(IPVersionType.V4)
                    .enableDHCP(true)
                    .cidr(CIDR)
                    .build());

            cfg.setSubnet(subnet.getName());
            LOG.info("Subnet (ID: {}, NAME: {}, CIDR: {}) created.", subnet.getId(), subnet.getName(), subnet.getCidr());

            RouterInterface routerInterface = osc.networking().router().attachInterface(router.getId(),
                    AttachInterfaceType.SUBNET, subnet.getId());

            LOG.info("Interface (ID: {}) added.", routerInterface.getId());
        } catch (ClientResponseException crs) {
            LOG.error(V, crs.getMessage(), crs);
            throw new ConfigurationException(crs.getMessage(), crs);
        }
        this.subnet = new SubnetOpenstack(subnet);
        this.network = new NetworkOpenstack(network, router);
        return this;
    }

    @Override
    public CreateClusterEnvironmentOpenstack createSecurityGroup() throws ConfigurationException {
        OSClient osc = cluster.getClient();
        // if a security group is configured then used it
        String securityGroup = ((ConfigurationOpenstack) getConfig()).getSecurityGroup();
        if (securityGroup != null) {
            sge = getSecGroupExtensionByName(osc, securityGroup);
            if (sge == null) {
                LOG.warn("Configured security group (name: {}) not found. Trying to create a new one ...", securityGroup);
            } else {
                LOG.info("Using existing Security group (name: {}).", sge.getName());
                return this;
            }
        }
        try {
            ComputeSecurityGroupService csgs = cluster.getClient().compute().securityGroups();
            sge = csgs.create(SECURITY_GROUP_PREFIX + cluster.getClusterId(),
                    "Security group for cluster: " + cluster.getClusterId());
            // allow ssh access (TCP:22) from everywhere
            csgs.createRule(getPortBuilder(sge.getId(), IPProtocol.TCP, 22, 22).cidr("0.0.0.0/0").build());
            // no restriction within the security group
            csgs.createRule(getPortBuilder(sge.getId(), IPProtocol.TCP, 1, 65535).groupId(sge.getId()).build());
            csgs.createRule(getPortBuilder(sge.getId(), IPProtocol.UDP, 1, 65535).groupId(sge.getId()).build());
            // User selected Ports.
            List<Port> ports = getConfig().getPorts();
            for (Port p : ports) {
                IPProtocol protocol = p.getType().equals(Port.Protocol.TCP) ? IPProtocol.TCP :
                        (p.getType().equals(Port.Protocol.UDP) ? IPProtocol.UDP : IPProtocol.ICMP);
                csgs.createRule(getPortBuilder(sge.getId(), protocol, p.getNumber(), p.getNumber()).cidr(p.getIpRange()).build());
            }
            LOG.info("Security group created. (name: {})", sge.getName());
        } catch (ClientResponseException e) {
            throw new ConfigurationException(e.getMessage(), e);
        }
        return this;
    }

    private SecurityGroupRuleBuilder getPortBuilder(String groupId, IPProtocol protocol, int from, int to) {
        return Builders.secGroupRule().parentGroupId(groupId).protocol(protocol).range(from, to);
    }

    SecGroupExtension getSecGroupExtension() {
        return sge;
    }

    /**
     * Determine secgroupExt by given name. Returns secgroupext object or null in the
     * case that no suitable secgroupexetension is found.
     */
    private static SecGroupExtension getSecGroupExtensionByName(OSClient osc, String name) {
        for (SecGroupExtension sge : osc.compute().securityGroups().list()) {
            if (sge.getName().equals(name)) {
                return sge;
            }
        }
        return null;
    }

    /**
     * Determine router by given router name. Returns router object or null in the case that no suitable router is found.
     */
    private static Router getRouterByIdOrName(OSClient osc, String r) {
        for (Router router : osc.networking().router().list()) {
            if (router.getName().equals(r) || router.getId().equals(r)) {
                return router;
            }
        }
        return null;
    }


    /**
     * Determine network by given network id. Returns Network object or null if no network with given id is found.
     */
    static Network getNetworkByIdOrName(OSClient osc, String n) {
        for (Network net : osc.networking().network().list()) {
            if (net.getId().equals(n) || net.getName().equals(n)) {
                return net;
            }
        }
        return null;
    }

    /**
     * Determine subnet by given subnet name. Returns subnet object or null in the case no suitable subnet is found.
     */
    private static Subnet getSubnetByIdOrName(OSClient osc, String s) {
        for (Subnet subnet : osc.networking().subnet().list()) {
            if (subnet.getName().equals(s) || subnet.getId().equals(s)) {
                return subnet;
            }
        }
        return null;
    }

    /**
     * Determine Router by given network. If more than one Router is connected to the network -> return first.
     */
    private static Router getRouterByNetwork(OSClient osc, Network network) {
        return getRouterByNetwork(osc, network.getId(), null);
    }

    /**
     * Determine Router by given network and subnet.
     */
    static Router getRouterByNetwork(OSClient osc, String networkId, String subnetId) {
        PortService ps = osc.networking().port();
        PortListOptions portListOptions = PortListOptions.create();
        portListOptions.networkId(networkId);
        // 1st check for device_owner "network:router_interface
        portListOptions.deviceOwner("network:router_interface");
        List<? extends org.openstack4j.model.network.Port> lop = ps.list(portListOptions);
        if (lop.isEmpty()) { // if no port found 2nd check for "network:ha_router_replicated_interface"
            portListOptions.deviceOwner("network:ha_router_replicated_interface");
            lop = ps.list(portListOptions);
        }
        if (lop.isEmpty()) { // if no port found 3nd check for "network:router_interface_distributed"
            portListOptions.deviceOwner("network:router_interface_distributed");
            lop = ps.list(portListOptions);
        }
        if (subnetId == null && lop.size() > 1) {
            LOG.warn("Network (ID: {}) uses more than one router, returning the first one!", networkId);
        }
        for (org.openstack4j.model.network.Port port : lop) {
            if (subnetId == null) {
                return getRouterByIdOrName(osc, port.getDeviceId()); // if no subnet is given just return first router
            } else {
                for (IP ip : port.getFixedIps()) {
                    if (ip.getSubnetId().equals(subnetId)) {
                        return getRouterByIdOrName(osc, port.getDeviceId());
                    }
                }
            }
        }
        LOG.warn("No router matches given constraints!");
        return null;
    }

    private static List<? extends org.openstack4j.model.network.Port> getPortsByRouter(OSClient osc, Router router) {
        PortListOptions portListOptions = PortListOptions.create().deviceId(router.getId());
        return osc.networking().port().list(portListOptions);
    }
}
