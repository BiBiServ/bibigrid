package de.unibi.cebitec.bibigrid.openstack;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for OpenStack Intents
 *
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
final class OpenStackUtils {
    private static final Logger LOG = LoggerFactory.getLogger(OpenStackUtils.class);

    static OSClient buildOSClient(ConfigurationOpenstack config) {
        OSFactory.enableHttpLoggingFilter(config.isDebugRequests());
        OSClient client = config.getOpenstackCredentials().getDomain() != null ?
                buildOSClientV3(config) :
                buildOSClientV2(config);
        LOG.info("Openstack connection established.");
        return client;
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
}
