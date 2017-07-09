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
            formatter.format("%15s | %10s | %19s | %20s | %7s | %11s | %11s%n", "cluster-id", "user", "launch date", "key name", "# inst", "group-id", "subnet-id");
            display.append(new String(new char[115]).replace('\0', '-')).append("\n");

            for (String id : clustermap.keySet()) {
                Cluster v = clustermap.get(id);
                formatter.format("%15s | %10s | %19s | %20s | %7d | %11s | %11s%n",
                        id,
                        (v.getUser() == null) ? "<NA>" : v.getUser(),
                        (v.getStarted() == null) ? "-" : v.getStarted(),
                        (v.getKeyname() == null ? "-" : v.getKeyname()),
                        ((v.getMasterinstance() != null ? 1 : 0) + v.getSlaveinstances().size()),
                        (v.getSecuritygroup() == null ? "-" : (v.getSecuritygroup().length() > 11 ? v.getSecuritygroup().substring(0, 9) + ".." : v.getSecuritygroup())),
                        (v.getSubnet() == null ? "-" : v.getSubnet()));
            }
        }
        return display.toString();
    }

    private void searchCluster() {
        Page<Instance> instancePage;
        if (conf.getAvailabilityZone() != null) {
            instancePage = compute.listInstances(conf.getAvailabilityZone());
        } else {
            instancePage = compute.listInstances();
        }
        for (Instance i : instancePage.iterateAll()) {
            if (i.getStatus() == InstanceInfo.Status.PROVISIONING
                    || i.getStatus() == InstanceInfo.Status.STAGING
                    || i.getStatus() == InstanceInfo.Status.RUNNING
                    || i.getStatus() == InstanceInfo.Status.STOPPING
                    || i.getStatus() == InstanceInfo.Status.TERMINATED) {
                // check for cluster-id
                String clusterid = getValueforName(i.getTags(), "bibigrid-id");
                String name = getValueforName(i.getTags(), "name");
                String user = getValueforName(i.getTags(), "user");
                if (clusterid == null && name != null) { // old style : use name to determine clusterid
                    if (name.contains("master-") || name.contains("slave-")) {
                        String[] tmp = name.split("-");
                        clusterid = tmp[tmp.length - 1];
                    }
                }
                if (clusterid == null) { // not an bibigrid instance : skip
                    continue;
                }
                Cluster cluster;
                // check if map contains a value objekt
                if (clustermap.containsKey(clusterid)) {
                    cluster = clustermap.get(clusterid);
                } else {
                    cluster = new Cluster();
                }

                // master//slave instance ?
                if (name != null && name.contains("master-")) {
                    if (cluster.getMasterinstance() == null) {
                        cluster.setMasterinstance(i.getInstanceId().getInstance());

                        cluster.setStarted(dateformatter.format(new Date(i.getCreationTimestamp())));
                    } else {
                        log.error("Detect two master instances ({},{}) for cluster '{}' ", cluster.getMasterinstance(), i.getInstanceId(), clusterid); // ???
                        System.exit(1);
                    }
                } else {
                    cluster.addSlaveInstance(i.getInstanceId().getInstance());
                }

                //keyname - should be always the same for all instances of one cluster
                if (cluster.getKeyname() != null) {
                    if (!cluster.getKeyname().equals(i.getMetadata().getFingerprint())) { // TODO: getKeyName as getFingerprint?
                        log.error("Detect two different keynames ({},{}) for cluster '{}'", cluster.getKeyname(), i.getMetadata().getFingerprint(), clusterid);
                    }
                } else {
                    cluster.setKeyname(i.getMetadata().getFingerprint());
                }

                // user - should be always the same for all instances of one cluser
                if (user != null) {
                    if (cluster.getUser() == null) {
                        cluster.setUser(user);
                    } else if (!cluster.getUser().equals(user)) {
                        log.error("Detect two different users ({},{}) for cluster '{}'", cluster.getUser(), user, clusterid);
                    }
                }

                clustermap.put(clusterid, cluster);
            }
        }
    }

    private static String getValueforName(Tags tags, String name) {
        for (String t : tags.getValues()) {
            String[] parts = t.split(":");
            if (parts.length == 2 && parts[0].equalsIgnoreCase(name)) {
                return parts[1];
            }
        }
        return null;
    }
}