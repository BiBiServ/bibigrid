package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import de.unibi.cebitec.bibigrid.core.model.Client;

/**
 * Implementation of the general ValidateIntent interface for an Openstack based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class ValidateIntentOpenstack extends ValidateIntent {
    ValidateIntentOpenstack(final Client client, final ConfigurationOpenstack config) {
        super(client, config);
    }
}
