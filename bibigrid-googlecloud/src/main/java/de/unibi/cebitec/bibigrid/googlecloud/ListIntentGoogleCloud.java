package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.InstanceAggregatedList;
import com.google.api.services.compute.model.InstanceList;
import com.google.api.services.compute.model.InstancesScopedList;
import de.unibi.cebitec.bibigrid.core.intents.ListIntent;
import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.InstanceTypeNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of the general ListIntent interface for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class ListIntentGoogleCloud extends ListIntent {
    private static final Logger LOG = LoggerFactory.getLogger(ListIntentGoogleCloud.class);
    private final ConfigurationGoogleCloud config;
    private final Compute compute;

    ListIntentGoogleCloud(ProviderModule providerModule, Map<String, Cluster> clusterMap) {
        super(clusterMap);
        compute = ((ClientGoogleCloud) providerModule.getClient()).getInternal();
    }

    @Override
    protected List<Instance> getInstances() {
        if (compute == null) {
            return null;
        }
        try {
            return config.getAvailabilityZone() != null ?
                    getInstancesWithZone(compute, config.getAvailabilityZone()) :
                    getInstancesWithoutZone(compute);
        } catch (IOException e) {
            LOG.error("Failed to load instances.", e);
        }
        return null;
    }

    private List<Instance> getInstancesWithZone(Compute compute, String zone) throws IOException {
        String projectId = config.getGoogleProjectId();
        InstanceList instanceList = compute.instances().list(projectId, zone).execute();
        if (instanceList == null || instanceList.getItems() == null) {
            return null;
        }
        return instanceList.getItems().stream().map(i -> new InstanceGoogleCloud(null, i)).collect(Collectors.toList());
    }

    private List<Instance> getInstancesWithoutZone(Compute compute) throws IOException {
        String projectId = config.getGoogleProjectId();
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
        return null;
    }

    @Override
    protected void loadInstanceConfiguration(Instance instance) {
        com.google.api.services.compute.model.Instance internalInstance = ((InstanceGoogleCloud) instance).getInternal();
        Configuration.InstanceConfiguration instanceConfiguration = new Configuration.InstanceConfiguration();
        instanceConfiguration.setType(internalInstance.getMachineType());
        try {
            instanceConfiguration.setProviderType(providerModule.getInstanceType(client, config, internalInstance.getMachineType()));
        } catch (InstanceTypeNotFoundException ignored) {
        }
        // TODO: instanceConfiguration.setImage(instance.getDisks().get(0).getSource());
        instance.setConfiguration(instanceConfiguration);
    }
}