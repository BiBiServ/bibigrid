package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.*;
import de.unibi.cebitec.bibigrid.core.intents.ListIntent;
import de.unibi.cebitec.bibigrid.core.model.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
        try {
            String projectId = config.getGoogleProjectId();
            if (config.getAvailabilityZone() != null) {
                InstanceList instances = compute.instances().list(projectId, config.getAvailabilityZone()).execute();
                if (instances != null && instances.getItems() != null) {
                    instances.getItems().forEach(this::checkInstance);
                }
            } else {
                InstanceAggregatedList aggregatedInstances = compute.instances().aggregatedList(projectId).execute();
                if (aggregatedInstances != null && aggregatedInstances.getItems() != null) {
                    for (InstancesScopedList instances : aggregatedInstances.getItems().values()) {
                        if (instances != null && instances.getInstances() != null) {
                            instances.getInstances().forEach(this::checkInstance);
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Failed to load instances. {}", e);
        }
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
                cluster.setMasterInstance(instance.getName());
                ZonedDateTime creationDateTime = ZonedDateTime.parse(instance.getCreationTimestamp());
                creationDateTime = creationDateTime.withZoneSameInstant(ZoneOffset.systemDefault());
                cluster.setStarted(creationDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss")));
            } else {
                LOG.error("Detect two master instances ({},{}) for cluster '{}' ",
                        cluster.getMasterInstance(), instance.getName(), clusterId);
                System.exit(1);
            }
        } else {
            cluster.addSlaveInstance(instance.getName());
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
        for (String t : tags.getItems()) {
            String[] parts = t.split(GoogleCloudUtils.TAG_SEPARATOR);
            if (parts.length == 2 && parts[0].equalsIgnoreCase(name)) {
                return parts[1];
            }
        }
        return null;
    }
}