package de.unibi.cebitec.bibigrid.meta.openstack;

import de.unibi.cebitec.bibigrid.meta.TerminateIntent;
import de.unibi.cebitec.bibigrid.model.Cluster;
import de.unibi.cebitec.bibigrid.model.Configuration;
import org.openstack4j.api.exceptions.ClientResponseException;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.Subnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements TerminateIntent for Openstack.
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class TerminateIntentOpenstack extends OpenStackIntent implements TerminateIntent {
    public static final Logger LOG = LoggerFactory.getLogger(TerminateIntentOpenstack.class);

    TerminateIntentOpenstack(Configuration conf) {
        super(conf);
    }

    @Override
    public boolean terminate() {
        final Cluster cluster = new ListIntentOpenstack(conf).getList().get(conf.getClusterId());
        if (cluster != null) {
            LOG.info("Terminating cluster with ID: {}", conf.getClusterId());
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
            // sub-network
            if (cluster.getSubnet() != null) {
                // get sub-network
                Subnet subnet = CreateClusterEnvironmentOpenstack.getSubnetworkById(os, cluster.getSubnet());
                // get network
                Network net = CreateClusterEnvironmentOpenstack.getNetworkById(os, subnet.getNetworkId());
                // get router
                Router router = CreateClusterEnvironmentOpenstack.getRouterbyNetwork(os, net, subnet);

                if (router == null) {
                    return false;
                }
                // get  port which handled connects router with network/subnet
                Port port = CreateClusterEnvironmentOpenstack.getPortbyRouterAndNetworkAndSubnet(os, router, net, subnet);

                // detach interface  from router 
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
            LOG.info("Cluster (ID: {}) successfully terminated", conf.getClusterId().trim());
            return true;
        }
        LOG.warn("No cluster with id {} found.", conf.getClusterId());
        return false;
    }
}
