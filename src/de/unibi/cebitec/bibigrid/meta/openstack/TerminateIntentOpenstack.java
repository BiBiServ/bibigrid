/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.meta.openstack;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import static de.unibi.cebitec.bibigrid.ctrl.TerminateIntent.log;
import de.unibi.cebitec.bibigrid.meta.TerminateIntent;
import de.unibi.cebitec.bibigrid.model.Configuration;
import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jsteiner
 */
public class TerminateIntentOpenstack implements TerminateIntent {
    
    public static final Logger log = LoggerFactory.getLogger(TerminateIntentOpenstack.class);
    
    private NovaApi novaClient;
    
    private final String os_region = "regionOne";
    private final String provider = "openstack-nova", username = "bibiserv:bibiserv", password = "OpenstackBibiserv";
    
    private final String endPoint;

//    private final String nodeID;
    Configuration conf;
    
    public TerminateIntentOpenstack(String endPoint, Configuration conf) {
        this.endPoint = endPoint;
        this.conf = conf;
    }
    
    @Override
    public boolean terminate() {
        connect();
        ServerApi s = novaClient.getServerApi(os_region);
        log.info("Terminating instance with ID: {}", conf.getClusterId());
        s.delete(conf.getClusterId());
        return true;
    }
    
    void connect() {
        Iterable<Module> modules = ImmutableSet.<Module>of(
                new SshjSshClientModule(),
                new SLF4JLoggingModule());
        
        novaClient = ContextBuilder.newBuilder(provider)
                .endpoint(endPoint)
                .credentials(username, password)
                .modules(modules)
                .buildApi(NovaApi.class);
    }
    
}
