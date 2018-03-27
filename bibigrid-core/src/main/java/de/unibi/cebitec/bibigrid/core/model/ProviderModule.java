package de.unibi.cebitec.bibigrid.core.model;

import de.unibi.cebitec.bibigrid.core.CommandLineValidator;
import de.unibi.cebitec.bibigrid.core.intents.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.InstanceTypeNotFoundException;
import de.unibi.cebitec.bibigrid.core.util.DefaultPropertiesFile;
import org.apache.commons.cli.CommandLine;

import java.util.Collection;
import java.util.Map;

/**
 * Provider module for accessing the implementation details for a specific cloud provider.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public abstract class ProviderModule {
    private Map<String, InstanceType> instanceTypes;

    /**
     * The name of the provider used to identify this module in the command line.
     *
     * @return The name of the provider.
     */
    public abstract String getName();

    /**
     * Get the command line validator implementation for the specified provider, that can handle
     * provider specific parameters.
     */
    public abstract CommandLineValidator getCommandLineValidator(
            final CommandLine commandLine, final DefaultPropertiesFile defaultPropertiesFile, final IntentMode intentMode)
            throws ConfigurationException;

    public abstract Client getClient(Configuration config) throws ClientConnectionFailedException;

    public abstract ListIntent getListIntent(Client client, Configuration config);

    public abstract TerminateIntent getTerminateIntent(Client client, Configuration config);

    public abstract PrepareIntent getPrepareIntent(Client client, Configuration config);

    public abstract CreateCluster getCreateIntent(Client client, Configuration config);

    public abstract CreateClusterEnvironment getClusterEnvironment(Client client, CreateCluster cluster)
            throws ConfigurationException;

    public ValidateIntent getValidateIntent(Client client, Configuration config) {
        return new ValidateIntent(client, config);
    }

    public final InstanceType getInstanceType(Client client, Configuration config, String type) throws InstanceTypeNotFoundException {
        getInstanceTypes(client, config);
        if (instanceTypes == null || !instanceTypes.containsKey(type)) {
            throw new InstanceTypeNotFoundException("Invalid instance type " + type);
        }
        return instanceTypes.get(type);
    }

    /**
     * Returns the block device base path for the specific provider implementation.
     *
     * @return Block device base path for ex. "/dev/xvd" in AWS.
     */
    public abstract String getBlockDeviceBase();

    public final Collection<InstanceType> getInstanceTypes(Client client, Configuration config) {
        if (instanceTypes == null) {
            instanceTypes = getInstanceTypeMap(client, config);
        }
        return instanceTypes.values();
    }

    protected abstract Map<String, InstanceType> getInstanceTypeMap(Client client, Configuration config);
}
