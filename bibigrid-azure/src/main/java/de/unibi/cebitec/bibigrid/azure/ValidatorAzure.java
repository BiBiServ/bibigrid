package de.unibi.cebitec.bibigrid.azure;

import de.unibi.cebitec.bibigrid.core.Validator;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;

import java.util.List;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public final class ValidatorAzure extends Validator {
    ValidatorAzure(final Configuration config, final ProviderModule providerModule)
            throws ConfigurationException {
        super(config, providerModule);
    }

    @Override
    protected Class<ConfigurationAzure> getProviderConfigurationClass() {
        return ConfigurationAzure.class;
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
