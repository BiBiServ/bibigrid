package de.unibi.cebitec.bibigrid.googlecloud;

import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import de.unibi.cebitec.bibigrid.core.model.Client;

/**
 * Implementation of the general ValidateIntent interface for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class ValidateIntentGoogleCloud extends ValidateIntent {
    ValidateIntentGoogleCloud(final Client client, final ConfigurationGoogleCloud config) {
        super(client, config);
    }
}