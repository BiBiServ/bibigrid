package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.model.Configuration;
import de.unibi.cebitec.bibigrid.model.OpenStackCredentials;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class that must be implemented by all OpenStack intents
 * 
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
abstract class OpenStackIntent {
    public static final Logger LOG = LoggerFactory.getLogger(OpenStackIntent.class);

    protected Configuration conf;
    protected OSClient os;
    
    OpenStackIntent(Configuration conf) {
        this.conf = conf;
        os = buildOSClient(conf);
    }
    
    static OSClient buildOSClient(Configuration conf){
        OSFactory.enableHttpLoggingFilter(conf.isLogHttpRequests());
        return conf.getOpenstackCredentials().getDomain() != null ?
                buildOSClientV3(conf) :
                buildOSClientV2(conf);
    }

    private static OSClient buildOSClientV2(Configuration conf) {
        OpenStackCredentials osc = conf.getOpenstackCredentials();
        return OSFactory.builderV2()
                .endpoint(conf.getOpenstackCredentials().getEndpoint())
                .credentials(osc.getUsername(),osc.getPassword())
                .tenantName(osc.getTenantName())
                .authenticate();
    }

    private static OSClient buildOSClientV3(Configuration conf) {
        OpenStackCredentials osc = conf.getOpenstackCredentials();
        return OSFactory.builderV3()
                .endpoint(conf.getOpenstackCredentials().getEndpoint())
                .credentials(osc.getUsername(),osc.getPassword(),Identifier.byName(osc.getDomain()))
                //.scopeToProject(Identifier.byName(osc.getTenantName()), Identifier.byName(osc.getDomain()))
                .scopeToProject(Identifier.byName(osc.getTenantName()), Identifier.byName(osc.getTenantDomain()))
                .authenticate();
    }
}
