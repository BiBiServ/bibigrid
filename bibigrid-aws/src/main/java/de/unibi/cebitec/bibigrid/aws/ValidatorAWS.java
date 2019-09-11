package de.unibi.cebitec.bibigrid.aws;

import de.unibi.cebitec.bibigrid.core.Validator;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;

import java.util.List;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

/**
 * AWS specific implementation for the {@link Validator}.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de, jkrueger(at)cebitec.uni-bielefeld.de
 */
public final class ValidatorAWS extends Validator {
    private final ConfigurationAWS awsConfig;

    ValidatorAWS(final Configuration config, final ProviderModule providerModule)
            throws ConfigurationException {
        super(config, providerModule);
        awsConfig = (ConfigurationAWS) config;
    }

    @Override
    protected Class<ConfigurationAWS> getProviderConfigurationClass() {
        return ConfigurationAWS.class;
    }

    @Override
    protected List<String> getRequiredOptions() {
        return null;
    }

    @Override
    protected boolean validateProviderParameters() {
        return true;
    }

}
