package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.intents.ListIntent;
import de.unibi.cebitec.bibigrid.model.Configuration;
import de.unibi.cebitec.bibigrid.model.Cluster;

import java.text.SimpleDateFormat;
import java.util.*;

import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.SecGroupExtension;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.Subnet;

/**
 * Implements ListIntent for Openstack.
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class ListIntentOpenstack extends OpenStackIntent implements ListIntent {
    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
    private Map<String, Cluster> clusterMap;

    ListIntentOpenstack(Configuration conf) {
        super(conf);
    }

    @Override
    public Map<String, Cluster> getList() {
        searchClusterIfNecessary();
        return clusterMap;
    }

    private void searchClusterIfNecessary() {
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
                String clusterId = t[t.length - 1];
                // check if entry already available
                if (clusterMap.containsKey(clusterId)) {
                    cluster = clusterMap.get(clusterId);
                } else {
                    cluster = new Cluster();
                    clusterMap.put(clusterId, cluster);
                }
                // get user from metadata map 
                if (cluster.getUser() == null) {
                    cluster.setUser(metadata.get("user"));
                }
                // master / slave
                if (name.contains("master")) {
                    cluster.setMasterInstance(server.getId());
                    cluster.setStarted(dateFormatter.format(server.getCreated()));
                    cluster.setKeyName(server.getKeyName());
                    Map<String, List<? extends Address>> madr = server.getAddresses().getAddresses();
                    // map should contain only one  network
                    if (madr.keySet().size() == 1) {
                        for (Address address : madr.get((String) (madr.keySet().toArray()[0]))) {
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
            if (name != null && name.startsWith("sg-")) {
                String[] t = name.split("-");
                String clusterId = t[t.length - 1];
                // check if entry already available
                if (clusterMap.containsKey(clusterId)) {
                    cluster = clusterMap.get(clusterId);
                } else {
                    cluster = new Cluster();
                    clusterMap.put(clusterId, cluster);
                }
                cluster.setSecurityGroup(sg.getId());
            }
        }
        // Network
        for (Network net : os.networking().network().list()) {
            String name = net.getName();
            if (name != null && name.startsWith(CreateClusterEnvironmentOpenstack.NETWORK_PREFIX)) {
                String[] t = name.split("-");
                String clusterId = t[t.length - 1];
                // check if entry already available
                if (clusterMap.containsKey(clusterId)) {
                    cluster = clusterMap.get(clusterId);
                } else {
                    cluster = new Cluster();
                    clusterMap.put(clusterId, cluster);
                }
                cluster.setNet(net.getId());
            }
        }
        // Subnet Work
        for (Subnet subnet : os.networking().subnet().list()) {
            String name = subnet.getName();
            if (name != null && name.startsWith(CreateClusterEnvironmentOpenstack.SUBNET_PREFIX)) {
                String[] t = name.split("-");
                String clusterId = t[t.length - 1];
                // check if entry already available
                if (clusterMap.containsKey(clusterId)) {
                    cluster = clusterMap.get(clusterId);
                } else {
                    cluster = new Cluster();
                    clusterMap.put(clusterId, cluster);
                }
                cluster.setSubnet(subnet.getId());
            }
        }
        // Router
        for (Router router : os.networking().router().list()) {
            String name = router.getName();
            if (name != null && name.startsWith(CreateClusterEnvironmentOpenstack.ROUTER_PREFIX)) {
                String[] t = name.split("-");
                String clusterId = t[t.length - 1];
                // check if entry already available
                if (clusterMap.containsKey(clusterId)) {
                    cluster = clusterMap.get(clusterId);
                } else {
                    cluster = new Cluster();
                    clusterMap.put(clusterId, cluster);
                }
                cluster.setRouter(router.getId());
            }
        }
    }

    @Override
    public String toString() {
        searchClusterIfNecessary();
        if (clusterMap.isEmpty()) {
            return "No BiBiGrid cluster found!\n";
        }
        StringBuilder display = new StringBuilder();
        Formatter formatter = new Formatter(display, Locale.US);
        display.append("\n");
        formatter.format("%15s | %10s | %19s | %20s | %15s | %7s | %2s | %6s | %3s | %5s%n",
                "cluster-id", "user", "launch date", "key name", "floating-ip", "# inst", "sg", "router", "net", " subnet");
        display.append(new String(new char[115]).replace('\0', '-')).append("\n");
        for (String id : clusterMap.keySet()) {
            Cluster v = clusterMap.get(id);
            formatter.format("%15s | %10s | %19s | %20s | %15s | %7d | %2s | %6s | %3s | %5s%n",
                    id,
                    (v.getUser() == null) ? "<NA>" : v.getUser(),
                    (v.getStarted() == null) ? "-" : v.getStarted(),
                    (v.getKeyName() == null ? "-" : v.getKeyName()),
                    (v.getPublicIp() == null ? "-" : v.getPublicIp()),
                    ((v.getMasterInstance() != null ? 1 : 0) + v.getSlaveInstances().size()),
                    (v.getSecurityGroup() == null ? "-" : "+"),
                    (v.getRouter() == null ? "-" : "+"),
                    (v.getNet() == null ? "-" : "+"),
                    (v.getSubnet() == null ? "-" : "+"));
        }
        return display.toString();
    }
}
