package de.unibi.cebitec.bibigrid.core.model;

import de.unibi.cebitec.bibigrid.core.CommandLineValidator;
import de.unibi.cebitec.bibigrid.core.intents.CreateCluster;
import de.unibi.cebitec.bibigrid.core.intents.ListIntent;
import de.unibi.cebitec.bibigrid.core.intents.TerminateIntent;
import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import de.unibi.cebitec.bibigrid.core.model.exceptions.InstanceTypeNotFoundException;
import de.unibi.cebitec.bibigrid.core.util.DefaultPropertiesFile;
import org.apache.commons.cli.CommandLine;

/**
 * Provider module interface for accessing the implementation details for a specific cloud provider.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public interface ProviderModule {
    /**
     * The name of the provider used to identify this module in the command line.
     *
     * @return The name of the provider.
     */
    String getName();

    /**
     * Get the command line validator implementation for the specified provider, that can handle
     * provider specific parameters.
     */
    CommandLineValidator getCommandLineValidator(final CommandLine commandLine,
                                                 final DefaultPropertiesFile defaultPropertiesFile,
                                                 final IntentMode intentMode);

    ListIntent getListIntent(Configuration config);

    TerminateIntent getTerminateIntent(Configuration config);

    CreateCluster getCreateIntent(Configuration config);

    ValidateIntent getValidateIntent(Configuration config);

    InstanceType getInstanceType(Configuration config, String type) throws InstanceTypeNotFoundException;

    /**
     * Returns the block device base path for the specific provider implementation.
     *
     * @return Block device base path for ex. "/dev/xvd" in AWS.
     */
    String getBlockDeviceBase();
}
