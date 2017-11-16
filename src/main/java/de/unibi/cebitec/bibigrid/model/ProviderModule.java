package de.unibi.cebitec.bibigrid.model;

import de.unibi.cebitec.bibigrid.ctrl.CommandLineValidator;
import de.unibi.cebitec.bibigrid.meta.CreateCluster;
import de.unibi.cebitec.bibigrid.meta.ListIntent;
import de.unibi.cebitec.bibigrid.meta.TerminateIntent;
import de.unibi.cebitec.bibigrid.meta.ValidateIntent;
import de.unibi.cebitec.bibigrid.model.exceptions.InstanceTypeNotFoundException;
import de.unibi.cebitec.bibigrid.util.DefaultPropertiesFile;
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
