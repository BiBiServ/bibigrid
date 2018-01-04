package de.unibi.cebitec.bibigrid.core.intents;

import de.unibi.cebitec.bibigrid.core.model.Cluster;

import java.text.SimpleDateFormat;
import java.util.Formatter;
import java.util.Locale;
import java.util.Map;

/**
 * Creates a Map of BiBiGrid cluster instances
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public abstract class ListIntent implements Intent {
    protected static final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yy HH:mm:ss");

    protected Map<String, Cluster> clusterMap;

    /**
     * Return a Map of Cluster objects within current configuration.
     */
    public Map<String, Cluster> getList() {
        searchClusterIfNecessary();
        return clusterMap;
    }

    protected abstract void searchClusterIfNecessary();

    /**
     * Return a String representation of found cluster objects map.
     */
    @Override
    public String toString() {
        searchClusterIfNecessary();
        if (clusterMap.isEmpty()) {
            return "No BiBiGrid cluster found!\n";
        }
        StringBuilder display = new StringBuilder();
        Formatter formatter = new Formatter(display, Locale.US);
        display.append("\n");
        formatter.format("%15s | %10s | %19s | %20s | %15s | %7s | %11s | %11s | %11s | %11s%n",
                "cluster-id", "user", "launch date", "key name", "floating-ip", "# inst", "group-id", "subnet-id",
                "network-id", "router-id");
        display.append(new String(new char[115]).replace('\0', '-')).append("\n");
        for (String id : clusterMap.keySet()) {
            Cluster v = clusterMap.get(id);
            formatter.format("%15s | %10s | %19s | %20s | %15s | %7d | %11s | %11s | %11s | %11s%n",
                    id,
                    (v.getUser() == null) ? "<NA>" : v.getUser(),
                    (v.getStarted() == null) ? "-" : v.getStarted(),
                    (v.getKeyName() == null ? "-" : v.getKeyName()),
                    (v.getPublicIp() == null ? "-" : v.getPublicIp()),
                    ((v.getMasterInstance() != null ? 1 : 0) + v.getSlaveInstances().size()),
                    (v.getSecurityGroup() == null ? "-" : cutStringIfNecessary(v.getSecurityGroup())),
                    (v.getSubnet() == null ? "-" : cutStringIfNecessary(v.getSubnet())),
                    (v.getNet() == null ? "-" : cutStringIfNecessary(v.getNet())),
                    (v.getRouter() == null ? "-" : cutStringIfNecessary(v.getRouter())));
        }
        return display.toString();
    }

    private String cutStringIfNecessary(String s) {
        return s.length() > 11 ? s.substring(0, 9) + ".." : s;
    }
}
