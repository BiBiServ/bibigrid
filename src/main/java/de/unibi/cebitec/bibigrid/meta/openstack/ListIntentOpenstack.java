/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.meta.openstack;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import de.unibi.cebitec.bibigrid.meta.ListIntent;
import de.unibi.cebitec.bibigrid.model.Configuration;
import de.unibi.cebitec.bibigrid.model.CurrentClusters;
import org.jclouds.ContextBuilder;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.sshj.config.SshjSshClientModule;
import static de.unibi.cebitec.bibigrid.ctrl.ListIntent.log;

/**
 *
 * @author jsteiner
 */
public class ListIntentOpenstack implements ListIntent {

    private Configuration conf;
    private final String provider = "openstack-nova";
    NovaApi novaApi;

    public ListIntentOpenstack(Configuration conf) {
        this.conf = conf;
         Iterable<Module> modules = ImmutableSet.<Module>of(
                new SshjSshClientModule(),
                new SLF4JLoggingModule());
        novaApi = ContextBuilder.newBuilder(provider)
                .endpoint(conf.getOpenstackCredentials().getEndpoint())
                .credentials(conf.getOpenstackCredentials().getTenantName() + ":" + conf.getOpenstackCredentials().getUsername(), conf.getOpenstackCredentials().getPassword())
                .modules(modules)
                .buildApi(NovaApi.class);
    }

    @Override
    public boolean list() {
        Iterable<Module> modules = ImmutableSet.<Module>of(
                new SshjSshClientModule(),
                new SLF4JLoggingModule());
        CurrentClusters cc = new CurrentClusters(novaApi, conf);
        log.info(cc.printClusterList());
        return true;
    }

}
