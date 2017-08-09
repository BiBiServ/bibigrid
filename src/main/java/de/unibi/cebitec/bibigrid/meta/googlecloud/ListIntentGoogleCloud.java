package de.unibi.cebitec.bibigrid.meta.googlecloud;

import com.google.api.gax.paging.Page;
import com.google.cloud.compute.*;
import de.unibi.cebitec.bibigrid.meta.ListIntent;
import de.unibi.cebitec.bibigrid.model.Cluster;
import de.unibi.cebitec.bibigrid.model.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Implementation of the general ListIntent interface for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class ListIntentGoogleCloud implements ListIntent {
    public static final Logger log = LoggerFactory.getLogger(de.unibi.cebitec.bibigrid.ctrl.ListIntent.class);
    private static final SimpleDateFormat dateformatter = new SimpleDateFormat("dd/MM/yy HH:mm:ss");

    private final Configuration conf;
    private final Map<String, Cluster> clustermap = new HashMap<>();
    private final Compute compute;

    public ListIntentGoogleCloud(final Configuration conf) {
        this.conf = conf;
        compute = GoogleCloudUtils.getComputeService(conf);
        if (compute != null)
            searchCluster();
    }

    public Map<String, Cluster> getList() {
        return clustermap;
    }

    @Override
    public String toString() {
        StringBuilder display = new StringBuilder();
        Formatter formatter = new Formatter(display, Locale.US);
        if (clustermap.isEmpty()) {
            display.append("No BiBiGrid cluster found!\n");
        } else {
            display.append("\n");
            formatter.format("%15s | %10s | %19s | %20s | %7s | %11s | %11s%n",
                    "cluster-id", "user", "launch date", "key name", "# inst", "group-id", "subnet-id");
            display.append(new String(new char[115]).replace('\0', '-')).append("\n");

            for (String id : clustermap.keySet()) {
                Cluster v = clustermap.get(id);
                formatter.format("%15s | %10s | %19s | %20s | %7d | %11s | %11s%n",
                        id,
                        (v.getUser() == null) ? "<NA>" : v.getUser(),
                        (v.getStarted() == null) ? "-" : v.getStarted(),
                        (v.getKeyname() == null ? "-" : v.getKeyname()),
                        ((v.getMasterinstance() != null ? 1 : 0) + v.getSlaveinstances().size()),
                        "-",
                        (v.getSubnet() == null ? "-" : v.getSubnet()));
            }
        }
        return display.toString();
    }

    private void searchCluster() {
        Page<Instance> instancePage = conf.getAvailabilityZone() != null ?
                compute.listInstances(conf.getAvailabilityZone()) :
                compute.listInstances();
        for (Instance instance : instancePage.iterateAll()) {
            checkInstance(instance);
        }
    }

    private void checkInstance(Instance instance) {
        // check for cluster ID
        String clusterid = getValueforName(instance.getTags(), "bibigrid-id");
        if (clusterid == null)
            return;
        String name = getValueforName(instance.getTags(), "name");
        String user = getValueforName(instance.getTags(), "user");
        Cluster cluster = clustermap.containsKey(clusterid) ? clustermap.get(clusterid) : new Cluster();

        // Check whether master or slave instance
        if (name != null && name.contains("master-")) {
            if (cluster.getMasterinstance() == null) {
                cluster.setMasterinstance(instance.getInstanceId().getInstance());
                cluster.setStarted(dateformatter.format(new Date(instance.getCreationTimestamp())));
            } else {
                log.error("Detect two master instances ({},{}) for cluster '{}' ",
                        cluster.getMasterinstance(), instance.getInstanceId(), clusterid);
                System.exit(1);
            }
        } else {
            cluster.addSlaveInstance(instance.getInstanceId().getInstance());
        }

        //keyname - should be always the same for all instances of one cluster
        String keyName = ""; // TODO: getKeyName
        if (cluster.getKeyname() != null) {
            if (!cluster.getKeyname().equals(keyName)) {
                log.error("Detect two different keynames ({},{}) for cluster '{}'",
                        cluster.getKeyname(), keyName, clusterid);
            }
        } else {
            cluster.setKeyname(keyName);
        }

        // user - should be always the same for all instances of one cluser
        if (user != null) {
            if (cluster.getUser() == null) {
                cluster.setUser(user);
            } else if (!cluster.getUser().equals(user)) {
                log.error("Detect two different users ({},{}) for cluster '{}'",
                        cluster.getUser(), user, clusterid);
            }
        }
        clustermap.put(clusterid, cluster);
    }

    private static String getValueforName(Tags tags, String name) {
        for (String t : tags.getValues()) {
            String[] parts = t.split(GoogleCloudUtils.TAG_SEPARATOR);
            if (parts.length == 2 && parts[0].equalsIgnoreCase(name)) {
                return parts[1];
            }
        }
        return null;
    }
}