package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.gax.paging.Page;
import com.google.cloud.compute.*;
import de.unibi.cebitec.bibigrid.core.intents.ListIntent;
import de.unibi.cebitec.bibigrid.core.model.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Implementation of the general ListIntent interface for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class ListIntentGoogleCloud extends ListIntent {
    private static final Logger LOG = LoggerFactory.getLogger(ListIntentGoogleCloud.class);
    private final ConfigurationGoogleCloud config;

    ListIntentGoogleCloud(final ConfigurationGoogleCloud config) {
        this.config = config;
    }

    @Override
    protected void searchClusterIfNecessary() {
        if (clusterMap != null) {
            return;
        }
        clusterMap = new HashMap<>();
        Compute compute = GoogleCloudUtils.getComputeService(config);
        if (compute == null)
            return;
        Page<Instance> instancePage = config.getAvailabilityZone() != null ?
                compute.listInstances(config.getAvailabilityZone()) :
                compute.listInstances();
        instancePage.iterateAll().forEach(this::checkInstance);
    }

    private void checkInstance(Instance instance) {
        // check for cluster ID
        String clusterId = getValueForName(instance.getTags(), "bibigrid-id");
        if (clusterId == null)
            return;
        String name = getValueForName(instance.getTags(), "name");
        String user = getValueForName(instance.getTags(), "user");
        Cluster cluster = clusterMap.containsKey(clusterId) ? clusterMap.get(clusterId) : new Cluster();

        // Check whether master or slave instance
        if (name != null && name.contains("master-")) {
            if (cluster.getMasterInstance() == null) {
                cluster.setMasterInstance(instance.getInstanceId().getInstance());
                cluster.setStarted(dateFormatter.format(new Date(instance.getCreationTimestamp())));
            } else {
                LOG.error("Detect two master instances ({},{}) for cluster '{}' ",
                        cluster.getMasterInstance(), instance.getInstanceId(), clusterId);
                System.exit(1);
            }
        } else {
            cluster.addSlaveInstance(instance.getInstanceId().getInstance());
        }
        // user - should be always the same for all instances of one cluster
        if (user != null) {
            if (cluster.getUser() == null) {
                cluster.setUser(user);
            } else if (!cluster.getUser().equals(user)) {
                LOG.error("Detect two different users ({},{}) for cluster '{}'",
                        cluster.getUser(), user, clusterId);
            }
        }
        clusterMap.put(clusterId, cluster);
    }

    private static String getValueForName(Tags tags, String name) {
        for (String t : tags.getValues()) {
            String[] parts = t.split(GoogleCloudUtils.TAG_SEPARATOR);
            if (parts.length == 2 && parts[0].equalsIgnoreCase(name)) {
                return parts[1];
            }
        }
        return null;
    }
}