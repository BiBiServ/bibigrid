package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import de.unibi.cebitec.bibigrid.core.Validator;
import de.unibi.cebitec.bibigrid.core.intents.*;
import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
@SuppressWarnings("unused")
public class ProviderModuleAzure extends ProviderModule {
    @Override
    public String getName() {
        return "azure";
    }

    @Override
    public Class<? extends Configuration> getConfigurationClass() {
        return ConfigurationAzure.class;
    }

    @Override
    public Validator getValidator(Configuration config, ProviderModule module) throws ConfigurationException {
        return new ValidatorAzure(config,module);
    }


    @Override
    public void createClient(Configuration config) throws ClientConnectionFailedException {
        return new ClientAzure(config);
    }

    @Override
    public ListIntent getListIntent(Configuration config) {
        return new ListIntentAzure(this, config);
    }

    @Override
    public TerminateIntent getTerminateIntent( Configuration config) {
        return new TerminateIntentAzure(this, config);
    }

    @Override
    public PrepareIntent getPrepareIntent(Configuration config) {
        return new PrepareIntentAzure(this, config);
    }

    @Override
    public CreateCluster getCreateIntent(Configuration config) {
        return new CreateClusterAzure(this, config);
    }

    @Override
    public CreateClusterEnvironment getClusterEnvironment(CreateCluster cluster) throws ConfigurationException {
        return new CreateClusterEnvironmentAzure(client, (CreateClusterAzure) cluster);
    }

    @Override
    public String getBlockDeviceBase() {
        return "/dev/sd";
    }

    @Override
    protected Map<String, InstanceType> getInstanceTypeMap(Client client, Configuration config) {
        Azure azure = ((ClientAzure) client).getInternal();
        Map<String, InstanceType> instanceTypes = new HashMap<>();
        for (VirtualMachineSize f : azure.virtualMachines().sizes().listByRegion(config.getRegion())) {
            instanceTypes.put(f.name(), new InstanceTypeAzure(f));
        }
        return instanceTypes;
    }
}
