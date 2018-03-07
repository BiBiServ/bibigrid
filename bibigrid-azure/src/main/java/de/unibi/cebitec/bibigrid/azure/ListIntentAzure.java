package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.management.Azure;
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
    private final Azure compute;

    ListIntentAzure(final ProviderModule providerModule, Client client, final ConfigurationAzure config) {
        super(providerModule, client, config);
        compute = ((ClientAzure) client).getInternal();
    }

    @Override
    protected List<Network> getNetworks() {
        if (compute == null) {
            return null;
        }
        return compute.networks().list().stream().map(NetworkAzure::new).collect(Collectors.toList());
    }

    @Override
    protected List<Subnet> getSubnets() {
        if (compute == null) {
            return null;
        }
        return compute.networks().list().stream()
                .flatMap(network -> network.subnets().values().stream())
                .map(SubnetAzure::new).collect(Collectors.toList());
    }

    @Override
    protected List<Instance> getInstances() {
        if (compute == null) {
            return null;
        }
        return compute.virtualMachines().list().stream().map(i -> new InstanceAzure(null, i)).collect(Collectors.toList());
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