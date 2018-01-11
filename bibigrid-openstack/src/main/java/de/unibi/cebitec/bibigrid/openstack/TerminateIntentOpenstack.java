package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.intents.TerminateIntent;
import de.unibi.cebitec.bibigrid.core.model.Cluster;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.ClientResponseException;
import org.openstack4j.api.networking.PortService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.*;
import org.openstack4j.model.network.options.PortListOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements TerminateIntent for Openstack.
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class TerminateIntentOpenstack implements TerminateIntent {
    private static final Logger LOG = LoggerFactory.getLogger(TerminateIntentOpenstack.class);
    private final ConfigurationOpenstack config;
    private final OSClient os;

    TerminateIntentOpenstack(ConfigurationOpenstack config) {
        this.config = config;
        os = OpenStackUtils.buildOSClient(config);
    }

    @Override
    public boolean terminate() {
        final Cluster cluster = new ListIntentOpenstack(config).getList().get(config.getClusterId());
        if (cluster != null) {
            LOG.info("Terminating cluster with ID: {}", config.getClusterId());
            // master
            if (cluster.getMasterInstance() != null) {
                os.compute().servers().delete(cluster.getMasterInstance());
            }
            // slaves
            for (String slave : cluster.getSlaveInstances()) {
                if (slave != null) {
                    os.compute().servers().delete(slave);
                }
            }
            // security groups
            if (cluster.getSecurityGroup() != null) {
                while (true) {
                    try {
                        Thread.sleep(1000);
                        ActionResponse ar = os.compute().securityGroups().delete(cluster.getSecurityGroup());
                        if (ar.isSuccess()) {
                            break;
                        }
                        LOG.warn("{} Try again in a second ...", ar.getFault());
                    } catch (InterruptedException ex) {
                        // do nothing
                    }
                }
                LOG.info("SecurityGroup ({}) deleted.", cluster.getSecurityGroup());
            }
            // subnet
            if (cluster.getSubnet() != null) {
                Subnet subnet = getSubnetById(os, cluster.getSubnet());
                if (subnet == null) {
                    return false;
                }
                Network net = CreateClusterEnvironmentOpenstack.getNetworkById(os, subnet.getNetworkId());
                Router router = CreateClusterEnvironmentOpenstack.getRouterByNetwork(os, net, subnet);
                if (router == null) {
                    return false;
                }
                // get port which handled connects router with network/subnet
                Port port = getPortByRouterAndNetworkAndSubnet(os, router, net, subnet);
                if (port == null) {
                    return false;
                }
                // detach interface from router
                try {
                    os.networking().router().detachInterface(router.getId(), subnet.getId(), port.getId());
                    // delete subnet
                    ActionResponse ar = os.networking().subnet().delete(subnet.getId());
                    if (ar.isSuccess()) {
                        LOG.info("Subnet (ID:{}) deleted!", subnet.getId());
                    } else {
                        LOG.warn("Can't remove subnet (ID:{}) : {}", subnet.getId(), ar.getFault());
                    }
                } catch (ClientResponseException e) {
                    LOG.warn(e.getMessage());
                }
            }
            // network
            if (cluster.getNet() != null) {
                // delete network
                ActionResponse ar = os.networking().network().delete(cluster.getNet());
                if (ar.isSuccess()) {
                    LOG.info("Network (ID:{}) deleted!", cluster.getNet());
                } else {
                    LOG.warn("Can't remove network (ID:{}) : {}", cluster.getNet(), ar.getFault());
                }
            }
            // router
            if (cluster.getRouter() != null) {
                ActionResponse ar = os.networking().router().delete(cluster.getRouter());
                if (ar.isSuccess()) {
                    LOG.info("Router (ID:{}) deleted!", cluster.getRouter());
                } else {
                    LOG.warn("Can't remove router (ID:{}) : {}", cluster.getRouter(), ar.getFault());
                }
            }
            LOG.info("Cluster (ID: {}) successfully terminated", config.getClusterId().trim());
            return true;
        }
        LOG.warn("No cluster with id {} found.", config.getClusterId());
        return false;
    }

    /**
     * Determine subnet by given subnet id. Returns subnet object or null in the case no suitable subnet is found.
     */
    private static Subnet getSubnetById(OSClient osc, String subnetId) {
        for (Subnet subnet : osc.networking().subnet().list()) {
            if (subnet.getId().equals(subnetId)) {
                return subnet;
            }
        }
        return null;
    }

    private static org.openstack4j.model.network.Port getPortByRouterAndNetworkAndSubnet(OSClient osc, Router router,
                                                                                         Network net, Subnet subnet) {
        PortService ps = osc.networking().port();
        PortListOptions portListOptions = PortListOptions.create();
        portListOptions.deviceId(router.getId());
        portListOptions.networkId(net.getId());
        for (org.openstack4j.model.network.Port port : ps.list(portListOptions)) {
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