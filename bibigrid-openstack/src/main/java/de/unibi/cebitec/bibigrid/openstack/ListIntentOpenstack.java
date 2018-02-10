package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.intents.CreateCluster;
import de.unibi.cebitec.bibigrid.core.intents.CreateClusterEnvironment;
import de.unibi.cebitec.bibigrid.core.intents.ListIntent;
import de.unibi.cebitec.bibigrid.core.model.Cluster;

import java.util.*;

import de.unibi.cebitec.bibigrid.core.model.Instance;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.SecGroupExtension;
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
        // String keypairName = conf.getKeypair();
        Cluster cluster;
        // Instances
        os.compute().servers().list().forEach(x -> checkInstance(new InstanceOpenstack(x)));
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
            if (name != null && name.startsWith(CreateClusterEnvironment.SUBNET_PREFIX)) {
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

    @Override
    protected List<Instance> getInstances() {
        return null;
    }

    private void checkInstance(InstanceOpenstack instance) {
        // check if instance is a BiBiGrid instance and extract clusterId from it
        String clusterId = getClusterIdForInstance(instance);
        if (clusterId == null)
            return;
        String name = instance.getName();
        Cluster cluster = getOrCreateCluster(clusterId);
        // get user from metadata map
        if (cluster.getUser() == null) {
            cluster.setUser(instance.getTag(Instance.TAG_USER));
        }
        // master / slave
        if (name.startsWith(CreateCluster.MASTER_NAME_PREFIX)) {
            cluster.setMasterInstance(instance.getId());
            cluster.setStarted(instance.getCreationTimestamp().format(dateTimeFormatter));
            cluster.setKeyName(instance.getKeyName());
            Map<String, List<? extends Address>> addressMap = instance.getAddresses().getAddresses();
            // map should contain only one network
            if (addressMap.keySet().size() == 1) {
                for (Address address : addressMap.values().iterator().next()) {
                    if (address.getType().equals("floating")) {
                        cluster.setPublicIp(address.getAddr());
                    }
                }
            } else {
                LOG.warn("No or more than one network associated with instance {}", instance.getId());
            }
        } else if (name.startsWith(CreateCluster.SLAVE_NAME_PREFIX)) {
            cluster.addSlaveInstance(instance.getId());
        }
    }
}
