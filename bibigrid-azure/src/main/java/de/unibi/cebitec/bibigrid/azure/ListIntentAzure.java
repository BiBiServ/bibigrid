package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import de.unibi.cebitec.bibigrid.core.intents.ListIntent;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.Instance;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.InstanceTypeNotFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the general ListIntent interface for an Azure based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class ListIntentAzure extends ListIntent {
    ListIntentAzure(final ProviderModule providerModule, final ConfigurationAzure config) {
        super(providerModule, config);
    }

    @Override
    protected List<Instance> getInstances() {
        Azure compute = AzureUtils.getComputeService(config);
        if (compute == null)
            return null;
        return compute.virtualMachines().list().stream().map(i -> new InstanceAzure(null, i)).collect(Collectors.toList());
    }

    @Override
    protected void loadInstanceConfiguration(Instance instance) {
        VirtualMachine internalInstance = ((InstanceAzure) instance).getInternal();
        Configuration.InstanceConfiguration instanceConfiguration = new Configuration.InstanceConfiguration();
        instanceConfiguration.setType(internalInstance.size().toString());
        try {
            instanceConfiguration.setProviderType(providerModule.getInstanceType(config, internalInstance.size().toString()));
        } catch (InstanceTypeNotFoundException ignored) {
        }
        // TODO: instanceConfiguration.setImage(...);
        instance.setConfiguration(instanceConfiguration);
    }
}