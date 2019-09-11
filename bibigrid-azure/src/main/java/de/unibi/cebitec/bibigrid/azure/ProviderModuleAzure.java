package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import de.unibi.cebitec.bibigrid.core.Validator;
import de.unibi.cebitec.bibigrid.core.intents.*;
import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.util.ConfigurationFile;
import org.apache.commons.cli.CommandLine;

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
    public Client getClient(Configuration config) throws ClientConnectionFailedException {
        return new ClientAzure(config);
    }

    @Override
    public ListIntent getListIntent(Client client, Configuration config) {
        return new ListIntentAzure(this, client, config);
    }

    @Override
    public TerminateIntent getTerminateIntent(Client client, Configuration config) {
        return new TerminateIntentAzure(this, client, config);
    }

    @Override
    public PrepareIntent getPrepareIntent(Client client, Configuration config) {
        return new PrepareIntentAzure(this, client, config);
    }

    @Override
    public CreateCluster getCreateIntent(Client client, Configuration config) {
        return new CreateClusterAzure(this, client, config);
    }

    @Override
    public CreateClusterEnvironment getClusterEnvironment(Client client, CreateCluster cluster) throws ConfigurationException {
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
