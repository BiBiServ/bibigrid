package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class ClientOpenstack extends Client {
    private static final Logger LOG = LoggerFactory.getLogger(ClientOpenstack.class);

    private final OSClient internalClient;

    ClientOpenstack(ConfigurationOpenstack config) throws ClientConnectionFailedException {
        try {
            OSFactory.enableHttpLoggingFilter(config.isDebugRequests());
            internalClient = config.getOpenstackCredentials().getDomain() != null ?
                    buildOSClientV3(config) :
                    buildOSClientV2(config);
            LOG.info("Openstack connection established.");
        } catch (Exception e) {
            throw new ClientConnectionFailedException("Failed to connect openstack client.", e);
        }
    }

    private static OSClient buildOSClientV2(ConfigurationOpenstack config) {
        OpenStackCredentials osc = config.getOpenstackCredentials();
        return OSFactory.builderV2()
                .endpoint(config.getOpenstackCredentials().getEndpoint())
                .credentials(osc.getUsername(), osc.getPassword())
                .tenantName(osc.getTenantName())
                .authenticate();
    }

    private static OSClient buildOSClientV3(ConfigurationOpenstack config) {
        OpenStackCredentials osc = config.getOpenstackCredentials();
        return OSFactory.builderV3()
                .endpoint(config.getOpenstackCredentials().getEndpoint())
                .credentials(osc.getUsername(), osc.getPassword(), Identifier.byName(osc.getDomain()))
                //.scopeToProject(Identifier.byName(osc.getTenantName()), Identifier.byName(osc.getDomain()))
                .scopeToProject(Identifier.byName(osc.getTenantName()), Identifier.byName(osc.getTenantDomain()))
                .authenticate();
    }

    OSClient getInternal() {
        return internalClient;
    }
}
