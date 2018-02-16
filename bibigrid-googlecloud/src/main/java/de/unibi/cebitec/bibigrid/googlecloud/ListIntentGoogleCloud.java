package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.InstanceAggregatedList;
import com.google.api.services.compute.model.InstanceList;
import com.google.api.services.compute.model.InstancesScopedList;
import de.unibi.cebitec.bibigrid.core.intents.ListIntent;
import de.unibi.cebitec.bibigrid.core.model.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    protected List<Instance> getInstances() {
        Compute compute = GoogleCloudUtils.getComputeService(config);
        if (compute == null)
            return null;
        try {
            String projectId = config.getGoogleProjectId();
            if (config.getAvailabilityZone() != null) {
                InstanceList instanceList = compute.instances().list(projectId, config.getAvailabilityZone()).execute();
                if (instanceList != null && instanceList.getItems() != null) {
                    return instanceList.getItems().stream().map(i -> new InstanceGoogleCloud(null, i)).collect(Collectors.toList());
                }
            } else {
                InstanceAggregatedList aggregatedInstances = compute.instances().aggregatedList(projectId).execute();
                if (aggregatedInstances != null && aggregatedInstances.getItems() != null) {
                    List<Instance> instances = new ArrayList<>();
                    for (InstancesScopedList instancesScopedList : aggregatedInstances.getItems().values()) {
                        if (instancesScopedList != null && instancesScopedList.getInstances() != null) {
                            instances.addAll(instancesScopedList.getInstances().stream()
                                    .map(i -> new InstanceGoogleCloud(null, i)).collect(Collectors.toList()));
                        }
                    }
                    return instances;
                }
            }
        } catch (IOException e) {
            LOG.error("Failed to load instances. {}", e);
        }
        return null;
    }
}