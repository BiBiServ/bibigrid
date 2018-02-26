package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.intents.PrepareIntent;
import de.unibi.cebitec.bibigrid.core.model.Instance;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import org.openstack4j.api.OSClient;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class PrepareIntentOpenstack extends PrepareIntent {
    private final OSClient os;

    PrepareIntentOpenstack(ProviderModule providerModule, ConfigurationOpenstack config) {
        super(providerModule, config);
        os = OpenStackUtils.buildOSClient(config);
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
