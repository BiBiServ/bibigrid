package de.unibi.cebitec.bibigrid.core;

import de.unibi.cebitec.bibigrid.core.model.*;

import java.util.*;

import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.InstanceTypeNotFoundException;
import de.unibi.cebitec.bibigrid.core.util.ConfigurationFile;
import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Validator {
    protected static final Logger LOG = LoggerFactory.getLogger(Validator.class);
    protected final CommandLine cl;
    protected final List<String> req;
    protected final ConfigurationFile configurationFile;
    protected final IntentMode intentMode;
    private final ProviderModule providerModule;
    protected final Configuration config;
    private Configuration.SlaveInstanceConfiguration commandLineSlaveInstance;

    public Validator(final CommandLine cl, final ConfigurationFile configurationFile,
                     final IntentMode intentMode, final ProviderModule providerModule)
            throws ConfigurationException {
        this.cl = cl;
        this.configurationFile = configurationFile;
        this.intentMode = intentMode;
        this.providerModule = providerModule;
        config = configurationFile.loadConfiguration(getProviderConfigurationClass());
        if (config != null && configurationFile.isAlternativeFilepath()) {
            config.setAlternativeConfigPath(configurationFile.getPropertiesFilePath().toString());
        }
        req = getRequiredOptions();
    }

    /**
     * Create the {@link Configuration} model instance. Must be overridden by provider
     * implementations for specific configuration fields.
     */
    protected abstract Class<? extends Configuration> getProviderConfigurationClass();

    protected final boolean checkRequiredParameter(String shortParam, String value) {
        if (req.contains(shortParam) && isStringNullOrEmpty(value)) {
            LOG.error("-" + shortParam + " option is required!");
            return false;
        }
        return true;
    }

    private boolean parseTerminateParameter() {
        // terminate (cluster-id)
        if (req.contains(IntentMode.TERMINATE.getShortParam())) {
            config.setClusterIds(cl.getOptionValue(IntentMode.TERMINATE.getShortParam()).trim());

        }
        return true;
    }

    private boolean parseIdeParameter() {
        // cloud9 (cluster-id)
        if (req.contains(IntentMode.CLOUD9.getShortParam())) {
            config.setClusterIds(cl.getOptionValue(IntentMode.CLOUD9.getShortParam()).trim());

        }
        if (req.contains(IntentMode.IDE.getShortParam())) {
            config.setClusterIds(cl.getOptionValue(IntentMode.IDE.getShortParam()).trim());

        }
        return true;
    }


    public boolean validate(String mode) {
        config.setMode(mode);
        if (providerModule == null) {
            LOG.error("No provider module for mode '"+mode+"' found. ");
            return false;
        }
        if (req == null) {
            LOG.info("No requirements defined ...");
            return true;
        }
        return parseTerminateParameter() && parseIdeParameter() && validateProviderParameters();
    }

    protected abstract List<String> getRequiredOptions();

    protected abstract boolean validateProviderParameters();

    public boolean validateProviderTypes(Client client) {
        try {
            InstanceType masterType = providerModule.getInstanceType(client, config, config.getMasterInstance().getType());
            config.getMasterInstance().setProviderType(masterType);
        } catch (InstanceTypeNotFoundException e) {
            LOG.error("Invalid master instance type specified!", e);
            return false;
        }
        try {
            for (Configuration.InstanceConfiguration instanceConfiguration : config.getSlaveInstances()) {
                InstanceType slaveType = providerModule.getInstanceType(client, config, instanceConfiguration.getType());
                instanceConfiguration.setProviderType(slaveType);
            }
        } catch (InstanceTypeNotFoundException e) {
            LOG.error("Invalid slave instance type specified!", e);
            return false;
        }
        return true;
    }

    protected static boolean isStringNullOrEmpty(final String s) {
        return s == null || s.trim().isEmpty();
    }

    private int checkStringAsInt(String s, int max) throws Exception {
        int v = Integer.parseInt(s);
        if (v < 0 || v > max) {
            throw new Exception();
        }
        return v;
    }

    public Configuration getConfig() {
        return config;
    }
}
