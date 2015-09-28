/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.meta.openstack;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import de.unibi.cebitec.bibigrid.meta.TerminateIntent;
import de.unibi.cebitec.bibigrid.model.Cluster;
import de.unibi.cebitec.bibigrid.model.Configuration;
import de.unibi.cebitec.bibigrid.model.CurrentClusters;
import java.util.Map;
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
    private final String provider = "openstack-nova";

//    private final String nodeID;
    Configuration conf;
    
    public TerminateIntentOpenstack(Configuration conf) {
        this.conf = conf;
    }
    
    @Override
    public boolean terminate() {
        connect();
        /**
         * What to do if the execution fails and the terminateIntent gets not called via commandline -t ? ...
         */
        ServerApi s = novaClient.getServerApi(os_region);
        if (!conf.getClusterId().isEmpty()) {
            log.info("Terminating instance with ID: {}", conf.getClusterId());
            return s.delete(conf.getClusterId());
        } else {
            /**
             * ... maybe this?
             */
            CurrentClusters cc = new CurrentClusters(novaClient, conf);
            Map<String, Cluster> clusters = cc.getClusterMap();
            Cluster c = null;
            for (String id : clusters.keySet()) {
                if (id.contains(conf.getKeypair())) {
                    c = clusters.get(id);
                    break;
                }
            }
            
            for (String slave : c.getSlaveinstances()) {
                s.delete(slave);
                log.info("Deleted Slave-Instance (ID: {})", slave);
            }
            
            s.delete(c.getMasterinstance());
            log.info("Deleted Master-Instance (ID: {})", c.getMasterinstance());
            return true;
        }
    }
    
    void connect() {
        Iterable<Module> modules = ImmutableSet.<Module>of(
                new SshjSshClientModule(),
                new SLF4JLoggingModule());
        
        novaClient = ContextBuilder.newBuilder(provider)
                .endpoint(conf.getOpenstackEndpoint())
                .credentials(conf.getOpenstackCredentials().getTenantName() + ":" + conf.getOpenstackCredentials().getUsername(), conf.getOpenstackCredentials().getPassword())
                .modules(modules)
                .buildApi(NovaApi.class);
    }
    
}
