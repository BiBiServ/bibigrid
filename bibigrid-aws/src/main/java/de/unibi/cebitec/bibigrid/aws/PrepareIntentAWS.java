package de.unibi.cebitec.bibigrid.aws;

import de.unibi.cebitec.bibigrid.core.intents.PrepareIntent;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class PrepareIntentAWS extends PrepareIntent {
    PrepareIntentAWS(ProviderModule providerModule, ConfigurationAWS config) {
        super(providerModule, config);
    }
}
