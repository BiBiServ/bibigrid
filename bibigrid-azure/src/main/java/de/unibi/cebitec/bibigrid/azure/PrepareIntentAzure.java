package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.PowerState;
import com.microsoft.azure.management.compute.VirtualMachine;
import de.unibi.cebitec.bibigrid.core.intents.PrepareIntent;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.Instance;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class PrepareIntentAzure extends PrepareIntent {
    private static final Logger LOG = LoggerFactory.getLogger(PrepareIntentAzure.class);

    private final Azure compute;

    PrepareIntentAzure(ProviderModule providerModule, Client client, Configuration config) {
        super(providerModule, client, config);
        compute = ((ClientAzure) client).getInternal();
    }

    @Override
    protected boolean stopInstance(Instance instance) {
        ((InstanceAzure) instance).getInternal().powerOffAsync();
        return true;
    }

    @Override
    protected void waitForInstanceShutdown(Instance instance) {
        VirtualMachine nativeInstance = ((InstanceAzure) instance).getInternal();
        do {
            nativeInstance = nativeInstance.refresh();
            PowerState state = nativeInstance.powerState();
            LOG.info(V, "Status of instance '{}': {}", instance.getName(), state);
            if (state == PowerState.STOPPED) {
                break;
            } else {
                LOG.info(V, "...");
                sleep(10);
            }
        } while (true);
    }

    @Override
    protected boolean createImageFromInstance(Instance instance, String imageName) {
        VirtualMachine nativeInstance = ((InstanceAzure) instance).getInternal();
        nativeInstance.generalize();
        compute.virtualMachineCustomImages()
                .define(imageName)
                .withRegion(config.getRegion())
                .withNewResourceGroup()
                .fromVirtualMachine(nativeInstance)
                .withTag(IMAGE_SOURCE_LABEL, instance.getConfiguration().getImage())
                .create();
        return true;
    }
}
