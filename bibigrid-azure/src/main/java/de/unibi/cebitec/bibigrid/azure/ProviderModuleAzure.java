package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import de.unibi.cebitec.bibigrid.core.CommandLineValidator;
import de.unibi.cebitec.bibigrid.core.intents.*;
import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.util.DefaultPropertiesFile;
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
    public CommandLineValidator getCommandLineValidator(final CommandLine commandLine,
                                                        final DefaultPropertiesFile defaultPropertiesFile,
                                                        final IntentMode intentMode) {
        return new CommandLineValidatorAzure(commandLine, defaultPropertiesFile, intentMode, this);
    }

    @Override
    public Client getClient(Configuration config) throws ClientConnectionFailedException {
        return new ClientAzure((ConfigurationAzure) config);
    }

    @Override
    public ListIntent getListIntent(Client client, Configuration config) {
        return new ListIntentAzure(this, client, (ConfigurationAzure) config);
    }

    @Override
    public TerminateIntent getTerminateIntent(Client client, Configuration config) {
        return new TerminateIntentAzure(this, client, (ConfigurationAzure) config);
    }

    @Override
    public PrepareIntent getPrepareIntent(Client client, Configuration config) {
        return new PrepareIntentAzure(this, client, (ConfigurationAzure) config);
    }

    @Override
    public CreateCluster getCreateIntent(Client client, Configuration config) {
        return new CreateClusterAzure(this, client, (ConfigurationAzure) config);
    }

    @Override
    public CreateClusterEnvironment getClusterEnvironment(Client client, CreateCluster cluster) throws ConfigurationException {
        return new CreateClusterEnvironmentAzure(client, (CreateClusterAzure) cluster);
    }

    @Override
    public ValidateIntent getValidateIntent(Client client, Configuration config) {
        return new ValidateIntentAzure(client, (ConfigurationAzure) config);
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
