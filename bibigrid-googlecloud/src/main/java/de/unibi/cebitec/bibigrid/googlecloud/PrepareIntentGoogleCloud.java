package de.unibi.cebitec.bibigrid.googlecloud;

import de.unibi.cebitec.bibigrid.core.intents.PrepareIntent;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class PrepareIntentGoogleCloud extends PrepareIntent {
    PrepareIntentGoogleCloud(ProviderModule providerModule, ConfigurationGoogleCloud config) {
        super(providerModule, config);
    }
}
