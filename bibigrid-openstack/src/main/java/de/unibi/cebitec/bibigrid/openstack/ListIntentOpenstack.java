package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.intents.CreateClusterEnvironment;
import de.unibi.cebitec.bibigrid.core.intents.ListIntent;
import de.unibi.cebitec.bibigrid.core.model.Cluster;

import java.util.*;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.SecGroupExtension;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.Subnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements ListIntent for Openstack.
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class ListIntentOpenstack extends ListIntent {
    private static final Logger LOG = LoggerFactory.getLogger(ListIntentOpenstack.class);
    private final OSClient os;

    ListIntentOpenstack(ConfigurationOpenstack config) {
        os = OpenStackUtils.buildOSClient(config);
    }

    @Override
    protected void searchClusterIfNecessary() {
        if (clusterMap != null) {
            return;
        }
        clusterMap = new HashMap<>();
        // String keypairName = conf.getKeypair();
        Cluster cluster;
        // Instances
        for (Server server : os.compute().servers().list()) {
            // check if instance is a BiBiGrid instance and extract clusterId from it
            String name = server.getName();
            Map<String, String> metadata = server.getMetadata();
            if (name != null && (name.startsWith("bibigrid-master-") || name.startsWith("bibigrid-slave-"))) {
                String[] t = name.split("-");
                cluster = getOrCreateCluster(t[t.length - 1]);
                // get user from metadata map 
                if (cluster.getUser() == null) {
                    cluster.setUser(metadata.get("user"));
                }
                // master / slave
                if (name.contains("master")) {
                    cluster.setMasterInstance(server.getId());
                    cluster.setStarted(dateFormatter.format(server.getCreated()));
                    cluster.setKeyName(server.getKeyName());
                    Map<String, List<? extends Address>> addressMap = server.getAddresses().getAddresses();
                    // map should contain only one  network
                    if (addressMap.keySet().size() == 1) {
                        for (Address address : addressMap.values().iterator().next()) {
                            if (address.getType().equals("floating")) {
                                cluster.setPublicIp(address.getAddr());
                            }
                        }
                    } else {
                        LOG.warn("No or more than one network associated with instance {}", server.getId());
                    }
                } else if (name.contains("slave")) {
                    cluster.addSlaveInstance(server.getId());
                }
            }
        }
        // Security Group
        for (SecGroupExtension sg : os.compute().securityGroups().list()) {
            String name = sg.getName();
            if (name != null && name.startsWith(CreateClusterEnvironment.SECURITY_GROUP_PREFIX)) {
                String[] t = name.split("-");
                cluster = getOrCreateCluster(t[t.length - 1]);
                cluster.setSecurityGroup(sg.getId());
            }
        }
        // Network
        for (Network net : os.networking().network().list()) {
            String name = net.getName();
            if (name != null && name.startsWith(CreateClusterEnvironmentOpenstack.NETWORK_PREFIX)) {
                String[] t = name.split("-");
                cluster = getOrCreateCluster(t[t.length - 1]);
                cluster.setNet(net.getId());
            }
        }
        // Subnet
        for (Subnet subnet : os.networking().subnet().list()) {
            String name = subnet.getName();
            if (name != null && name.startsWith(CreateClusterEnvironmentOpenstack.SUBNET_PREFIX)) {
                String[] t = name.split("-");
                cluster = getOrCreateCluster(t[t.length - 1]);
                cluster.setSubnet(subnet.getId());
            }
        }
        // Router
        for (Router router : os.networking().router().list()) {
            String name = router.getName();
            if (name != null && name.startsWith(CreateClusterEnvironmentOpenstack.ROUTER_PREFIX)) {
                String[] t = name.split("-");
                cluster = getOrCreateCluster(t[t.length - 1]);
                cluster.setRouter(router.getId());
            }
        }
    }

    private Cluster getOrCreateCluster(String clusterId) {
        Cluster cluster;
        // check if entry already available
        if (clusterMap.containsKey(clusterId)) {
            cluster = clusterMap.get(clusterId);
        } else {
            cluster = new Cluster();
            clusterMap.put(clusterId, cluster);
        }
        return cluster;
    }
}
