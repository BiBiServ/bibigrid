package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.intents.TerminateIntent;
import de.unibi.cebitec.bibigrid.core.model.Cluster;
import de.unibi.cebitec.bibigrid.core.model.Instance;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
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
public class TerminateIntentOpenstack extends TerminateIntent {
    private static final Logger LOG = LoggerFactory.getLogger(TerminateIntentOpenstack.class);
    private final OSClient os;

    TerminateIntentOpenstack(ProviderModule providerModule, ConfigurationOpenstack config) {
        super(providerModule, config);
        os = OpenStackUtils.buildOSClient(config);
    }

    @Override
    protected boolean terminateCluster(Cluster cluster) {
        // master
        if (cluster.getMasterInstance() != null) {
            ActionResponse response = os.compute().servers().delete(cluster.getMasterInstance().getId());
            if (!response.isSuccess()) {
                LOG.error("Failed to delete instance '{}'. {}", cluster.getMasterInstance().getName(), response.getFault());
                return false;
            }
        }
        // slaves
        for (Instance slave : cluster.getSlaveInstances()) {
            if (slave != null) {
                ActionResponse response = os.compute().servers().delete(slave.getId());
                if (!response.isSuccess()) {
                    LOG.error("Failed to delete instance '{}'. {}", slave.getName(), response.getFault());
                    return false;
                }
            }
        }
        // security groups
        if (cluster.getSecurityGroup() != null) {
            while (true) {
                sleep(1, false);
                ActionResponse ar = os.compute().securityGroups().delete(cluster.getSecurityGroup());
                if (ar.isSuccess()) {
                    break;
                }
                LOG.warn("{} Try again in a second...", ar.getFault());
            }
            LOG.info("Security group '{}' deleted.", cluster.getSecurityGroup());
        }
        // subnet
        if (cluster.getSubnet() != null) {
            Subnet subnet = getSubnetById(os, cluster.getSubnet());
            if (subnet == null) {
                return false;
            }
            Network net = CreateClusterEnvironmentOpenstack.getNetworkById(os, subnet.getNetworkId());
            if (net == null) {
                return false;
            }
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
                    LOG.info("Subnet '{}' deleted!", subnet.getId());
                } else {
                    LOG.warn("Can't remove subnet '{}'. {}", subnet.getId(), ar.getFault());
                }
            } catch (ClientResponseException e) {
                LOG.warn(e.getMessage());
            }
        }
        // network
        if (cluster.getNetwork() != null) {
            // delete network
            ActionResponse ar = os.networking().network().delete(cluster.getNetwork());
            if (ar.isSuccess()) {
                LOG.info("Network '{}' deleted!", cluster.getNetwork());
            } else {
                LOG.warn("Can't remove network '{}'. {}", cluster.getNetwork(), ar.getFault());
            }
        }
        return true;
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
        LOG.warn("No Port matches givens constraints.");
        return null;
    }
}
