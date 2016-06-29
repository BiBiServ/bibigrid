/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.meta.openstack;

import de.unibi.cebitec.bibigrid.model.Configuration;
import de.unibi.cebitec.bibigrid.model.OpenStackCredentials;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;

/**
 *
 * @author jkrueger
 */
abstract class OpenStackIntent {
    
    protected Configuration conf;
    protected OSClient os;
    
    public OpenStackIntent(Configuration conf) {
        this.conf = conf;
        os = buildOSClient(conf);
        
    }
    
    public static OSClient buildOSClient(Configuration conf){
        OpenStackCredentials osc = conf.getOpenstackCredentials();
        
        OSFactory.enableHttpLoggingFilter(conf.isLogHttpRequests());
        if (osc.getDomain() != null) {
            //v3
            return OSFactory.builderV3()
                       .endpoint(conf.getOpenstackCredentials().getEndpoint())
                       .credentials(osc.getUsername(),osc.getPassword(),Identifier.byName(osc.getDomain()))
                       .scopeToProject(Identifier.byName(osc.getTenantName()), Identifier.byName(osc.getDomain()))
                       .authenticate();
            
        } else {
            return OSFactory.builderV2()
                       .endpoint(conf.getOpenstackCredentials().getEndpoint())
                       .credentials(osc.getUsername(),osc.getPassword())
                       .tenantName(osc.getTenantName())
                       .authenticate();
            
        }
        
        
    }
    
    
    
}
