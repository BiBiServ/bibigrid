package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.intents.ScaleWorkerIntent;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;

public class ScaleWorkerOpenstack extends ScaleWorkerIntent {

    ScaleWorkerOpenstack(final ProviderModule providerModule, final Configuration config, Client client, String clusterId, int batchIndex, int count, String scaling) {
        super(providerModule, config, client, clusterId, batchIndex, count, scaling);

    }
}
