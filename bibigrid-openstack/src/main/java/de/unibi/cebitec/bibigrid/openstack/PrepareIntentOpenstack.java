package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.intents.PrepareIntent;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class PrepareIntentOpenstack extends PrepareIntent {
    PrepareIntentOpenstack(ProviderModule providerModule, ConfigurationOpenstack config) {
        super(providerModule, config);
    }
}
