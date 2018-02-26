package de.unibi.cebitec.bibigrid.aws;

import de.unibi.cebitec.bibigrid.core.intents.PrepareIntent;
import de.unibi.cebitec.bibigrid.core.model.Instance;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class PrepareIntentAWS extends PrepareIntent {
    PrepareIntentAWS(ProviderModule providerModule, ConfigurationAWS config) {
        super(providerModule, config);
    }

    @Override
    protected boolean stopInstance(Instance instance) {
        return false;
    }

    @Override
    protected void waitForInstanceShutdown(Instance instance) {

    }

    @Override
    protected boolean createImageFromInstance(Instance instance, String imageName) {
        return false;
    }
}
