package de.unibi.cebitec.bibigrid.azure;

import de.unibi.cebitec.bibigrid.core.intents.PrepareIntent;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class PrepareIntentAzure extends PrepareIntent {
    PrepareIntentAzure(ProviderModule providerModule, ConfigurationAzure config) {
        super(providerModule, config);
    }
}
