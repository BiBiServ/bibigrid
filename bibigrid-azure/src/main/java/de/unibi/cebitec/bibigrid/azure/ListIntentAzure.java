package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.management.Azure;
import de.unibi.cebitec.bibigrid.core.intents.ListIntent;
import de.unibi.cebitec.bibigrid.core.model.Instance;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the general ListIntent interface for an Azure based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class ListIntentAzure extends ListIntent {
    private final ConfigurationAzure config;

    ListIntentAzure(final ConfigurationAzure config) {
        this.config = config;
    }

    @Override
    protected List<Instance> getInstances() {
        Azure compute = AzureUtils.getComputeService(config);
        if (compute == null)
            return null;
        return compute.virtualMachines().list().stream().map(i -> new InstanceAzure(null, i)).collect(Collectors.toList());
    }
}