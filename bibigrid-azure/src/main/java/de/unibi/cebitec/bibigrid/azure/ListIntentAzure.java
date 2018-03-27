package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.management.compute.VirtualMachine;
import de.unibi.cebitec.bibigrid.core.intents.ListIntent;
import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.InstanceTypeNotFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the general ListIntent interface for an Azure based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class ListIntentAzure extends ListIntent {
    ListIntentAzure(final ProviderModule providerModule, Client client, final Configuration config) {
        super(providerModule, client, config);
    }

    @Override
    protected List<Instance> getInstances() {
        return ((ClientAzure) client).getInternal().virtualMachines().list()
                .stream().map(i -> new InstanceAzure(null, i)).collect(Collectors.toList());
    }

    @Override
    protected void loadInstanceConfiguration(Instance instance) {
        VirtualMachine internalInstance = ((InstanceAzure) instance).getInternal();
        Configuration.InstanceConfiguration instanceConfiguration = new Configuration.InstanceConfiguration();
        instanceConfiguration.setType(internalInstance.size().toString());
        try {
            instanceConfiguration.setProviderType(providerModule.getInstanceType(client, config, internalInstance.size().toString()));
        } catch (InstanceTypeNotFoundException ignored) {
        }
        // TODO: instanceConfiguration.setImage(...);
        instance.setConfiguration(instanceConfiguration);
    }
}