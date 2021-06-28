package de.unibi.cebitec.bibigrid.core.model;

import de.unibi.cebitec.bibigrid.core.Validator;
import de.unibi.cebitec.bibigrid.core.intents.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.InstanceTypeNotFoundException;

import java.util.Collection;
import java.util.Map;

/**
 * Provider module for accessing the implementation details for a specific cloud provider.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de, jkrueger(at)cebitec.uni-bielefeld.de
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
     * Assigned during the createClient() implementation of the inheritors.
     */
    public Client client;

    /**
     * Get the configuration implementation for the specified provider, that have provider specific parameters.
     *
     * @param path String path to config file
     * @return config loaded
     * @throws ConfigurationException exception if config is wrong
     */
    public Configuration getConfiguration(String path) throws ConfigurationException {
        return Configuration.loadConfiguration(getConfigurationClass(), path);
    }

    public abstract Class<? extends Configuration> getConfigurationClass();

    /**
     * Get the validator implementation for the specified provider, that can handle provider specific parameters.
     *
     * @return validator to check cmdline and config
     * @throws ConfigurationException exception if config is wrong
     */
    public abstract Validator getValidator(Configuration config, ProviderModule module) throws ConfigurationException;

    public abstract void createClient(Configuration config) throws ClientConnectionFailedException;

    public Client getClient() {
        return client;
    }

    public abstract ListIntent getListIntent(Map<String, Cluster> clusterMap);

    public abstract TerminateIntent getTerminateIntent(Configuration config, Map<String, Cluster> clusterMap);

    public abstract PrepareIntent getPrepareIntent(Configuration config);

    public abstract CreateCluster getCreateIntent(Configuration config, String clusterId);

    public abstract ScaleWorkerIntent getScaleWorkerIntent(Configuration config, String clusterId, int batchIndex, int count, String scaling);

    public abstract LoadClusterConfigurationIntent getLoadClusterConfigurationIntent(Configuration config);

    public abstract CreateClusterEnvironment getClusterEnvironment(CreateCluster cluster)
            throws ConfigurationException;

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
     * @return Block device base path for ex. "/dev/xvd" in AWS, "/dev/vd" in OpenStack
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
