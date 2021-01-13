package de.unibi.cebitec.bibigrid.openstack.intents;

import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.openstack.ClientOpenstack;
import org.openstack4j.api.OSClient;

public class ValidateIntentOpenstack extends ValidateIntent {
    private final OSClient.OSClientV3 os;

    public ValidateIntentOpenstack(ProviderModule providerModule, Client client, Configuration config) {
        super(client, config);
        os = ((ClientOpenstack) providerModule.getClient()).getInternal();
    }
}
