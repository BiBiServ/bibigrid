package de.unibi.cebitec.bibigrid.meta.openstack;

import de.unibi.cebitec.bibigrid.meta.TerminateIntent;
import de.unibi.cebitec.bibigrid.model.Configuration;
import java.util.ArrayList;
import java.util.List;
import static org.openstack4j.api.Builders.port;

import org.openstack4j.api.compute.ComputeSecurityGroupService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.SecGroupExtension;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.Subnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements TermintaeItent for Openstack.
 *
 * @author Johannes Steiner (1st version), 
 *  Jan Krueger (jkrueger@cebitec.uni-bielefeld.de)
 */
public class TerminateIntentOpenstack extends OpenStackIntent implements TerminateIntent {

    public static final Logger LOG = LoggerFactory.getLogger(TerminateIntentOpenstack.class);

    private final String os_region;
//    private final String provider = "openstack-nova";

    public TerminateIntentOpenstack(Configuration conf) {
        super(conf);
        os_region = conf.getRegion();
    }

    @Override
    public boolean terminate() {
        if (conf.getClusterId() != null && !conf.getClusterId().isEmpty()) {
            LOG.info("Terminating cluster with ID: {}", conf.getClusterId());
            List<Server> l = getServers(conf.getClusterId());
            for (Server serv : l) {
                os.compute().servers().delete(serv.getId());
                LOG.info("Terminated " + serv.getName());
            }

            // clean up security groups
            ComputeSecurityGroupService securityGroups = os.compute().securityGroups();
            // iterate over all Security Groups ... 
            for (SecGroupExtension securityGroup : securityGroups.list()) {
                // only clean the security group with id "sg-<clusterid>
                if (securityGroup.getName().equals("sg-" + conf.getClusterId().trim())) {

                    while (true) {
                        try {
                            Thread.sleep(1000);
                            ActionResponse ar = securityGroups.delete(securityGroup.getId());
                            if (ar.isSuccess()) {
                                break;
                            }
                            LOG.warn("{} Try again in a second ...", ar.getFault());

                        } catch (InterruptedException ex) {
                            // do nothing
                        }
                    }
                    LOG.info("SecurityGroup ({}) deleted.", securityGroup.getName());
                }
            }

            // clean up router, interface, subnet, network
            Network net = CreateClusterEnvironmentOpenstack.getNetworkByName(os, CreateClusterEnvironmentOpenstack.NETWORKPREFIX);
            if (net != null) {
                // get router
                Router router = CreateClusterEnvironmentOpenstack.getRouterbyNetwork(os, net);
                // get subnetwork
                Subnet subnet = CreateClusterEnvironmentOpenstack.getSubnetworkByName(os, CreateClusterEnvironmentOpenstack.SUBNETWORKPREFIX);
                // get a list of all ports which are handled by the used router
                List<? extends Port> portlist = CreateClusterEnvironmentOpenstack.getPortsbyRouterAndNetwork(os, router, net);
                if (subnet != null) {
                    // detach interface  from router - list should contain only 1 port)
                    for (Port port : portlist) {
                        os.networking().router().detachInterface(router.getId(), subnet.getId(), port.getId());
                        // should the port also be removed ?
                    }
                    // delete subnet
                    ActionResponse ar = os.networking().subnet().delete(subnet.getId());
                    if (ar.isSuccess()) {
                        LOG.info("Subnet (ID:{}) deleted!",subnet.getId());
                    } else {
                        LOG.warn("Can't remove subnet (ID:{}) : {}", subnet.getId(), ar.getFault());
                    }
                }
                // delete network
                ActionResponse ar = os.networking().network().delete(net.getId());
                if (ar.isSuccess()) {
                    LOG.info("Network (ID:{}) deleted!",net.getId());
                } else {
                    LOG.warn("Can't remove network (ID:{}) : {}", net.getId(), ar.getFault());
                } 
            } else {
                LOG.warn("No network with clusterid [{}] found.", conf.getClusterId());
            }
            // search for router and remove it
            Router router = CreateClusterEnvironmentOpenstack.getRouterByName(os, CreateClusterEnvironmentOpenstack.ROUTERPREFIX);
            if (router != null) {
                ActionResponse ar = os.networking().router().delete(router.getId());
                if (ar.isSuccess()) {
                    LOG.info("Router (ID:{}) deleted!",router.getId());
                } else {
                    LOG.warn("Can't remove router (ID:{}) : {}", router.getId(), ar.getFault());
                }
            }

            LOG.info("Cluster (ID: {}) successfully terminated", conf.getClusterId().trim());
            return true;
        }

        return false;
    }

    private List<Server> getServers(String clusterID) {
        List<Server> ret = new ArrayList<>();

        for (Server server : os.compute().servers().list()) {
            String name = server.getName();
            if (name.substring(name.lastIndexOf("-") + 1, name.length()).equals(clusterID.trim())) {
                ret.add(server);
            }
        }

        if (ret.isEmpty()) {
            LOG.warn("No instances with clusterid  [{}] found.", conf.getClusterId().trim());
        }
        return ret;
    }

}
