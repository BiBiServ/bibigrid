package de.unibi.cebitec.bibigrid.meta.openstack;

import de.unibi.cebitec.bibigrid.meta.TerminateIntent;
import de.unibi.cebitec.bibigrid.model.Cluster;
import de.unibi.cebitec.bibigrid.model.Configuration;
import java.util.List;
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
 * @author Johannes Steiner (1st version), Jan Krueger
 * (jkrueger@cebitec.uni-bielefeld.de)
 */
public class TerminateIntentOpenstack extends OpenStackIntent implements TerminateIntent {

    public static final Logger LOG = LoggerFactory.getLogger(TerminateIntentOpenstack.class);

    private final String os_region;
    private final Cluster cluster;

    public TerminateIntentOpenstack(Configuration conf) {
        super(conf);
        os_region = conf.getRegion();
        cluster = new ListIntentOpenstack(conf).getList().get(conf.getClusterId());
    }

    @Override
    public boolean terminate() {
        if (cluster != null) {

            LOG.info("Terminating cluster with ID: {}", conf.getClusterId());
            // master
            if (cluster.getMasterinstance() != null) {
                os.compute().servers().delete(cluster.getMasterinstance());
            }
            // slaves
            for (String slave : cluster.getSlaveinstances()) {
                if (slave != null) {
                    os.compute().servers().delete(slave);
                }
            }
            // security groups
            if (cluster.getSecuritygroup() != null) {
                while (true) {
                    try {
                        Thread.sleep(1000);
                        ActionResponse ar = os.compute().securityGroups().delete(cluster.getSecuritygroup());
                        if (ar.isSuccess()) {
                            break;
                        }
                        LOG.warn("{} Try again in a second ...", ar.getFault());

                    } catch (InterruptedException ex) {
                        // do nothing
                    }
                }
                LOG.info("SecurityGroup ({}) deleted.", cluster.getSecuritygroup());
            }

            // subnet work
            if (cluster.getSubnet() != null) {
                // get subnetwork
                Subnet subnet = CreateClusterEnvironmentOpenstack.getSubnetworkById(os, cluster.getSubnet());

                // get network
                Network net = CreateClusterEnvironmentOpenstack.getNetworkById(os, subnet.getNetworkId());
                // get router
                Router router = CreateClusterEnvironmentOpenstack.getRouterbyNetwork(os, net);

                // get a list of all ports which are handled by the used router
                List<? extends Port> portlist = CreateClusterEnvironmentOpenstack.getPortsbyRouterAndNetwork(os, router, net);

                // detach interface  from router - list should contain only 1 port)
                for (Port port : portlist) {
                    os.networking().router().detachInterface(router.getId(), subnet.getId(), port.getId());

                }
                // delete subnet
                ActionResponse ar = os.networking().subnet().delete(subnet.getId());
                if (ar.isSuccess()) {
                    LOG.info("Subnet (ID:{}) deleted!", subnet.getId());
                } else {
                    LOG.warn("Can't remove subnet (ID:{}) : {}", subnet.getId(), ar.getFault());
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
