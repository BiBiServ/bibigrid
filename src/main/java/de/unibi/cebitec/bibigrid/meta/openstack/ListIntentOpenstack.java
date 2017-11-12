package de.unibi.cebitec.bibigrid.meta.openstack;

import de.unibi.cebitec.bibigrid.meta.ListIntent;
import de.unibi.cebitec.bibigrid.model.Configuration;
import de.unibi.cebitec.bibigrid.model.Cluster;
import java.text.SimpleDateFormat;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.SecGroupExtension;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.Subnet;

/**
 * Implements ListIntent for Openstack.
 *
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de Jan Krueger -
 * jkrueger(at)cebitec.unj-bielefeld.de
 */
public class ListIntentOpenstack extends OpenStackIntent implements ListIntent {

    public static SimpleDateFormat dateformatter = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
    private final Map<String, Cluster> clustermap = new HashMap<>();
    
    public ListIntentOpenstack(Configuration conf) {
        super(conf);
        searchCluster();
    }

    @Override
    public Map<String, Cluster> getList() {
        return clustermap;
    }

    
    
    private ListIntentOpenstack searchCluster() {
        String keypairName = conf.getKeypair();
        Cluster cluster;

        /*
         * Instances
         */
        for (Server serv : os.compute().servers().list()) {
            // check if instance is a BiBiGrid instance and extract clusterid from it
            String name = serv.getName();
            Map<String, String> metadata = serv.getMetadata();

            if (name != null && (name.startsWith("bibigrid-master-") || name.startsWith("bibigrid-slave-"))) {
                String[] t = name.split("-");
                String clusterid = t[t.length - 1];
                // check if entry already available
                if (clustermap.containsKey(clusterid)) {
                    cluster = clustermap.get(clusterid);
                } else {
                    cluster = new Cluster();
                    clustermap.put(clusterid, cluster);
                }
                // get user from metadata map 
                if (cluster.getUser() == null) {
                    cluster.setUser(metadata.get("user"));
                }

                // master / slave
                if (name.contains("master")) {
                    cluster.setMasterInstance(serv.getId());
                    cluster.setStarted(dateformatter.format(serv.getCreated()));
                    cluster.setKeyName(serv.getKeyName());
                    Map <String,List<? extends Address>> madr = serv.getAddresses().getAddresses();
                    // map should contain only one  network
                    if (madr.keySet().size() == 1) {
                        for (Address address : madr.get((String)(madr.keySet().toArray()[0]))) {
                            if (address.getType().equals("floating")) {
                                cluster.setPublicIp(address.getAddr());
                            }
                        }
                        
                    } else {
                      LOG.warn("No or more than one network associated with instance {}",serv.getId());
                    }
                    
                    

                } else if (name.contains("slave")) {
                    cluster.addSlaveInstance(serv.getId());
                }
            }
        }
        /*
         * Security Group
         */
        for (SecGroupExtension sg : os.compute().securityGroups().list()) { 
            String name = sg.getName();
            if (name != null && name.startsWith("sg-")) {
                String[] t = name.split("-");
                String clusterid = t[t.length - 1];
                // check if entry already available
                if (clustermap.containsKey(clusterid)) {
                    cluster = clustermap.get(clusterid);
                } else {
                    cluster = new Cluster();
                    clustermap.put(clusterid, cluster);
                }                       
                cluster.setSecurityGroup(sg.getId());
                
            }
        }
        
        /*
         * Network
         */
        
        for (Network net : os.networking().network().list()) {
            String name = net.getName();       
            if (name != null && name.startsWith(CreateClusterEnvironmentOpenstack.NETWORKPREFIX)) {
                String[] t = name.split("-");
                String clusterid = t[t.length - 1];
                // check if entry already available
                if (clustermap.containsKey(clusterid)) {
                    cluster = clustermap.get(clusterid);
                } else {
                    cluster = new Cluster();
                    clustermap.put(clusterid, cluster);
                }                       
                cluster.setNet(net.getId());
                
            }
            
        }
        /*
         * Subnet Work
         */
        for (Subnet subnet : os.networking().subnet().list()) {
            String name = subnet.getName();       
            if (name != null && name.startsWith(CreateClusterEnvironmentOpenstack.SUBNETWORKPREFIX)) {
                String[] t = name.split("-");
                String clusterid = t[t.length - 1];
                // check if entry already available
                if (clustermap.containsKey(clusterid)) {
                    cluster = clustermap.get(clusterid);
                } else {
                    cluster = new Cluster();
                    clustermap.put(clusterid, cluster);
                }                       
                cluster.setSubnet(subnet.getId());
                
            }
        }
        
        /*
         * Router
         */
        for (Router router : os.networking().router().list()) {
            String name = router.getName();       
            if (name != null && name.startsWith(CreateClusterEnvironmentOpenstack.ROUTERPREFIX)) {
                String[] t = name.split("-");
                String clusterid = t[t.length - 1];
                // check if entry already available
                if (clustermap.containsKey(clusterid)) {
                    cluster = clustermap.get(clusterid);
                } else {
                    cluster = new Cluster();
                    clustermap.put(clusterid, cluster);
                }                       
                cluster.setRouter(router.getId());
                
            }
        }        
        
        return this;
    }
    
    @Override
    public String toString() {
        StringBuilder display = new StringBuilder();
        Formatter formatter = new Formatter(display, Locale.US);
        if (clustermap.isEmpty()) {
            display.append("No BiBiGrid cluster found!\n");
        } else {
            display.append("\n");
            formatter.format("%15s | %10s | %19s | %20s | %15s | %7s | %2s | %6s | %3s | %5s%n", 
                    "cluster-id", "user", "launch date", "key name", "floating-ip","# inst", "sg", "router", "net", " subnet");
            display.append(new String(new char[115]).replace('\0', '-')).append("\n");

            for (String id : clustermap.keySet()) {
                Cluster v = clustermap.get(id);
                formatter.format("%15s | %10s | %19s | %20s | %15s | %7d | %2s | %6s | %3s | %5s%n",
                        id,
                        (v.getUser() == null) ? "<NA>" :v.getUser(),
                        (v.getStarted() == null) ? "-" : v.getStarted(),
                        (v.getKeyName() == null ? "-" : v.getKeyName()),
                        (v.getPublicIp() == null ? "-" : v.getPublicIp()),
                        ((v.getMasterInstance() != null ? 1 : 0) + v.getSlaveInstances().size()),
                        (v.getSecurityGroup() == null ? "-" : "+"),
                        (v.getRouter() == null ? "-" : "+"),
                        (v.getNet()== null ? "-" : "+"),
                        (v.getSubnet() == null ? "-" : "+"));
            }
        }
        return display.toString();
    }


}
