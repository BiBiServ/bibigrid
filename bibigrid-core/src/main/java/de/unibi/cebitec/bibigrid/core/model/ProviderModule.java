package de.unibi.cebitec.bibigrid.core.model;

import de.unibi.cebitec.bibigrid.core.CommandLineValidator;
import de.unibi.cebitec.bibigrid.core.intents.*;
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
    public abstract CommandLineValidator getCommandLineValidator(final CommandLine commandLine,
                                                                 final DefaultPropertiesFile defaultPropertiesFile,
                                                                 final IntentMode intentMode);

    public abstract ListIntent getListIntent(Configuration config);

    public abstract TerminateIntent getTerminateIntent(Configuration config);

    public abstract PrepareIntent getPrepareIntent(Configuration config);

    public abstract CreateCluster getCreateIntent(Configuration config);

    public abstract ValidateIntent getValidateIntent(Configuration config);

    public final InstanceType getInstanceType(Configuration config, String type) throws InstanceTypeNotFoundException {
        getInstanceTypes(config);
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

    public final Collection<InstanceType> getInstanceTypes(Configuration config) {
        if (instanceTypes == null) {
            instanceTypes = getInstanceTypeMap(config);
        }
        return instanceTypes.values();
    }

    protected abstract Map<String, InstanceType> getInstanceTypeMap(Configuration config);
}
