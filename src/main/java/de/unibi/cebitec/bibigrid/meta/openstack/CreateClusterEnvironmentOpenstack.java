/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.meta.openstack;

import com.jcraft.jsch.JSchException;
import de.unibi.cebitec.bibigrid.meta.CreateClusterEnvironment;
import de.unibi.cebitec.bibigrid.model.Configuration;
import de.unibi.cebitec.bibigrid.exception.ConfigurationException;
import de.unibi.cebitec.bibigrid.model.Port;
import de.unibi.cebitec.bibigrid.util.KEYPAIR;
import de.unibi.cebitec.bibigrid.util.SubNets;
import static de.unibi.cebitec.bibigrid.util.VerboseOutputFilter.V;
import java.util.ArrayList;
import java.util.List;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ComputeSecurityGroupService;
import org.openstack4j.api.exceptions.ClientResponseException;
import org.openstack4j.api.networking.PortService;
import org.openstack4j.model.compute.IPProtocol;
import org.openstack4j.model.compute.SecGroupExtension;
import org.openstack4j.model.network.AttachInterfaceType;
import org.openstack4j.model.network.IP;
import org.openstack4j.model.network.IPVersionType;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.model.network.options.PortListOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prepare the cloud environment for an OpenStack cluster.
 *
 *
 * @author Johannes Steiner, Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class CreateClusterEnvironmentOpenstack
        implements CreateClusterEnvironment<CreateClusterEnvironmentOpenstack, CreateClusterOpenstack> {

    private CreateClusterOpenstack cluster;

    private SecurityGroup sg;
    private SecGroupExtension sge;

    private Router router = null;
    private Network net = null;
    private Subnet subnet = null;

    private KEYPAIR keypair;

    public static final String ROUTERPREFIX = "router-";
    public static final String NETWORKPREFIX = "net-";
    public static final String SUBNETWORKPREFIX = "subnet-";
    public static final String PORTPREFIX = "port-";
    public static final String NETWORKCIDR = "192.168.0.0/16";

    public static final Logger LOG = LoggerFactory.getLogger(CreateClusterEnvironmentOpenstack.class);

    public CreateClusterEnvironmentOpenstack(CreateClusterOpenstack cluster) {
        this.cluster = cluster;   
        try {
            keypair = new KEYPAIR();
        } catch (JSchException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    @Override
    public CreateClusterEnvironmentOpenstack createVPC() {
        // complete setup is done by createSubNet
        return this;
    }

    @Override
    public CreateClusterEnvironmentOpenstack createSubnet() throws ConfigurationException {
        try {
            /*
        User can specify three parameters to define the network connection for a
        BiBiGrid cluster
        
        1) router
        2) network
        3) subnetwork
        
        
             */
            OSClient osc = cluster.getOs();
            Configuration cfg = cluster.getConfiguration();
            String clusterid = cluster.getClusterId();

            // if subnet is set just use it
            if (cfg.getSubnetname() != null) {
                // check if subnet exists
                subnet = getSubnetworkByName(osc, cfg.getSubnetname());
                if (subnet == null) {
                    throw new ConfigurationException("No Subnet with name '" + cfg.getSubnetname() + "' found!");
                }
                net = getNetworkById(osc, subnet.getNetworkId());
                router = getRouterbyNetwork(osc, net);
                LOG.info("Use existing subnet (ID: {}, CIDR: {}).", subnet.getId(), subnet.getCidr());
                return this;
            }

            // if network is set try to determine router
            if (cfg.getNetworkname() != null) {
                // check if net exists
                net = getNetworkByName(osc, cfg.getNetworkname());
                if (net == null) {
                    throw new ConfigurationException("No Net with name '" + cfg.getSubnetname() + "' found!");
                }
                router = getRouterbyNetwork(osc, net);
                LOG.info("Use existing net (ID: {}) and router (ID: {}).", net.getId(), router.getId());

            }

            if (router == null) {
                // create a new one
                if (cfg.getRoutername() == null) {
                    router = osc.networking().router().create(Builders.router()
                            .name(ROUTERPREFIX + clusterid)
                            .externalGateway(cfg.getGatewayname())
                            .build());
                    LOG.info("Router (ID: {}) created.", router.getId());
                    // otherwise use existing one
                } else {
                    router = getRouterByName(osc, cfg.getRoutername());
                    if (router == null) {
                        throw new ConfigurationException("No Router with name '" + cfg.getRoutername() + "' found!");
                    }
                    LOG.info("Use existing router (ID: {}).", router.getId());
                }
                // create a new network
                net = osc.networking().network().create(Builders.network()
                        .name(NETWORKPREFIX + cluster.getClusterId())
                        .adminStateUp(true)
                        .build());
                cfg.setNetworkname(net.getName());
                LOG.info("Network (ID: {}, NAME: {}) created.", net.getId(), net.getName());

            }
            // get new free /24 subnetwork
            SubNets sn = new SubNets(NETWORKCIDR, 24);

            List<String> usedCIDR = new ArrayList<>();

            for (org.openstack4j.model.network.Port p : getPortsbyRouter(osc, router)) {
                Network net_tmp = getNetworkById(osc, p.getNetworkId());
                if (net_tmp.getNeutronSubnets() != null) {
                    for (Subnet s : net_tmp.getNeutronSubnets()) {
                        usedCIDR.add(s.getCidr());
                    }
                }

            }

            String CIDR = sn.nextCidr(usedCIDR);

            if (CIDR == null) {
                throw new ConfigurationException("No free /24 network found in " + NETWORKCIDR + " for router " + router.getName());
            }

            // now we can create a new subnet
            subnet = osc.networking().subnet().create(Builders.subnet()
                    .name(SUBNETWORKPREFIX + cluster.getClusterId())
                    .network(net)
                    .ipVersion(IPVersionType.V4)    
                    .enableDHCP(true)
                    .cidr(CIDR)
                    .build());

            cfg.setSubnetname(subnet.getName());
            LOG.info("Subnet (ID: {}, NAME: {}, CIDR: {}) created.", subnet.getId(), subnet.getName(), subnet.getCidr());

            RouterInterface iface = osc.networking().router().attachInterface(router.getId(), AttachInterfaceType.SUBNET, subnet.getId());

            LOG.info("Interface (ID: {}) added .", iface.getId());

        } catch (ClientResponseException crs) {
            LOG.error(V, crs.getMessage(), crs);
            throw new ConfigurationException(crs.getMessage(), crs);
        }

        return this;
    }

    @Override
    public CreateClusterEnvironmentOpenstack createSecurityGroup() throws ConfigurationException {
        try {
            ComputeSecurityGroupService csgs = cluster.getOs().compute().securityGroups();
            sge = csgs.create("sg-" + cluster.getClusterId(), "Security Group for cluster: " + cluster.getClusterId());

            csgs.createRule(Builders.secGroupRule()
                    .parentGroupId(sge.getId())
                    .protocol(IPProtocol.TCP)
                    .cidr("0.0.0.0/0")
                    .range(22, 22)
                    .build());

            csgs.createRule(Builders.secGroupRule()
                    .parentGroupId(sge.getId())
                    .protocol(IPProtocol.TCP)
                    .groupId(sge.getId())
                    .range(1, 65535)
                    .build());

            /**
             * User selected Ports.
             */
            List<Port> ports = cluster.getConfiguration().getPorts();
            for (Port p : ports) {

                csgs.createRule(Builders.secGroupRule()
                        .parentGroupId(sge.getId())
                        .protocol(IPProtocol.TCP)
                        .cidr(p.iprange)
                        .range(p.number, p.number)
                        .build());
            }
            LOG.info("SecurityGroup (ID: {}) created.", sge.getName());
        } catch (ClientResponseException e) {
            throw new ConfigurationException(e.getMessage(), e);
        }
        return this;
    }

    @Override
    public CreateClusterOpenstack createPlacementGroup() {
        // Currently not supported by OpenStack
        //LOG.info("PlacementGroup creation not implemented yet");
        return cluster;
    }

    public KEYPAIR getKeypair() {
        return this.keypair;
    }

    public SecurityGroup getSecurityGroup() {
        return sg;
    }

    public SecGroupExtension getSecGroupExtension() {
        return sge;
    }

    public Router getRouter() {
        return router;
    }

    public Network getNetwork() {
        return net;
    }

    public Subnet getSubnet() {
        return subnet;
    }

    /**
     * Determine router by given router name. Returns router object or null in
     * the case that no suitable router is found.
     *
     * @param osc OSClient
     * @param routername
     * @return
     */
    public static Router getRouterByName(OSClient osc, String routername) {
        for (Router router : osc.networking().router().list()) {
            if (router.getName().equals(routername)) {
                return router;
            }
        }
        return null;
    }

    /**
     * Determine router by given router id. Returns router object or null in the
     * case that no suitable router is found
     *
     * @param osc
     * @param routerid
     * @return
     */
    public static Router getRouterById(OSClient osc, String routerid) {
        for (Router router : osc.networking().router().list()) {
            if (router.getId().equals(routerid)) {
                return router;
            }
        }
        return null;
    }

    /**
     * Determine network by given network name. Returns Network object or null
     * if no network with given name is found.
     *
     *
     * @param osc OSClient
     * @param networkname
     * @return
     */
    public static Network getNetworkByName(OSClient osc, String networkname) {
        for (Network net : osc.networking().network().list()) {
            if (net.getName().equals(networkname)) {
                return net;
            }
        }
        return null;
    }

    /**
     * Determine network by given network id. Returns Network object or null if
     * no network with given id is found.
     *
     * @param osc
     * @param networkid
     * @return
     */
    public static Network getNetworkById(OSClient osc, String networkid) {
        for (Network net : osc.networking().network().list()) {
            if (net.getId().equals(networkid)) {
                return net;
            }
        }
        return null;
    }

    /**
     * Determine subnetwork by given subnetwork name. Returns Subnetwork object
     * or null in the case no suitable subnetwork is found.
     *
     * @param osc OSClient
     * @param subnetworkname
     * @return
     */
    public static Subnet getSubnetworkByName(OSClient osc, String subnetworkname) {
        for (Subnet subnet : osc.networking().subnet().list()) {
            if (subnet.getName().equals(subnetworkname)) {
                return subnet;
            }
        }
        return null;
    }

    /**
     * Determine subnetwork by given subnetwork id. Returns Subnetwork object or
     * null in the case no suitable subnetwork is found.
     *
     * @param osc
     * @param subnetid
     * @return
     */
    public static Subnet getSubnetworkById(OSClient osc, String subnetid) {
        for (Subnet subnet : osc.networking().subnet().list()) {
            if (subnet.getId().equals(subnetid)) {
                return subnet;
            }
        }
        return null;
    }

    /**
     * Deteremine Router by given network. If more than one Router is connected
     * to the network -> return first
     *
     * @param osc
     * @param network
     * @return
     */
    public static Router getRouterbyNetwork(OSClient osc, Network network) {
        return getRouterbyNetwork(osc, network, null);
    }

    /**
     * Deteremine Router by given network and subnetwork. Return router tjaty
     *
     * @param osc
     * @param network
     * @param subnet
     * @return
     */
    public static Router getRouterbyNetwork(OSClient osc, Network network, Subnet subnet) {
        PortService ps = osc.networking().port();
        PortListOptions plopt = PortListOptions.create();
        plopt.networkId(network.getId());

        plopt.deviceOwner("network:router_interface");

        if (subnet == null && ps.list(plopt).size() > 1) {
            LOG.warn("Network (ID: {}) uses more than one router, return the 1st one !", network.getId());
        }
        for (org.openstack4j.model.network.Port port : ps.list(plopt)) {
            if (subnet == null) {
                return getRouterById(osc, port.getDeviceId()); // if no subnet is given just return first router
            } else {
                for (IP ip : port.getFixedIps()) {
                    if (ip.getSubnetId().equals(subnet.getId())) {
                        return getRouterById(osc, port.getDeviceId());
                    }
                }
            }
        }
        LOG.warn("No router matches given constraints ...");
        return null;
    }

    public static List<? extends org.openstack4j.model.network.Port> getPortsbyRouter(OSClient osc, Router router) {
        PortService ps = osc.networking().port();
        PortListOptions plopt = PortListOptions.create();
        plopt.deviceId(router.getId());
        return ps.list(plopt);
    }

    public static org.openstack4j.model.network.Port getPortbyRouterAndNetworkAndSubnet(OSClient osc, Router router, Network net,Subnet subnet) {
        PortService ps = osc.networking().port();
        PortListOptions plopt = PortListOptions.create();
        plopt.deviceId(router.getId());
        plopt.networkId(net.getId());
        
        for (org.openstack4j.model.network.Port port : ps.list(plopt)) {
            for (IP ip : port.getFixedIps()) { 
                if (ip.getSubnetId().equals(subnet.getId())) {
                        return port;
                    } 
            }
        }
        LOG.warn("No Port matches givens constraints ...");
        return null;
    }

}
