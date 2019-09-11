package de.unibi.cebitec.bibigrid.googlecloud;

import de.unibi.cebitec.bibigrid.core.Validator;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.IntentMode;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.util.ConfigurationFile;
import org.apache.commons.cli.CommandLine;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de, jkrueger(at)cebitec.uni-bielefeld.de
 *
 */
public final class ValidatorGoogleCloud extends Validator {
    private final ConfigurationGoogleCloud googleCloudConfig;

    ValidatorGoogleCloud(final Configuration config, final ProviderModule providerModule)
            throws ConfigurationException {
        super( config, providerModule);
        googleCloudConfig = (ConfigurationGoogleCloud) config;
    }

    @Override
    protected Class<ConfigurationGoogleCloud> getProviderConfigurationClass() {
        return ConfigurationGoogleCloud.class;
    }

    @Override
    protected List<String> getRequiredOptions() {
        return null;
    }

    @Override
    protected boolean validateProviderParameters() {
        return parseGoogleProjectIdParameter() && parseGoogleImageProjectIdParameter();
    }

    private boolean parseGoogleProjectIdParameter() {
        return !isStringNullOrEmpty(googleCloudConfig.getGoogleProjectId());
    }

    private boolean parseGoogleImageProjectIdParameter() {
        return !isStringNullOrEmpty(googleCloudConfig.getGoogleImageProjectId());
    }
}
