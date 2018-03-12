package de.unibi.cebitec.bibigrid.azure;

import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import de.unibi.cebitec.bibigrid.core.model.Client;

/**
 * Implementation of the general ValidateIntent interface for an Azure based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class ValidateIntentAzure extends ValidateIntent {
    ValidateIntentAzure(final Client client, final ConfigurationAzure config) {
        super(client, config);
    }
}