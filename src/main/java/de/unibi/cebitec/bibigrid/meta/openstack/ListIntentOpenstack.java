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
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;

import org.jclouds.sshj.config.SshjSshClientModule;
import static de.unibi.cebitec.bibigrid.ctrl.ListIntent.log;

/**
 *
 * @author jsteiner
 */
public class ListIntentOpenstack extends OpenStackIntent implements ListIntent {

    public ListIntentOpenstack(Configuration conf) {
        super(conf);
    }

    @Override
    public boolean list() {
        Iterable<Module> modules = ImmutableSet.<Module>of(
                new SshjSshClientModule(),
                new SLF4JLoggingModule());
        CurrentClusters cc = new CurrentClusters(os, conf);
        log.info(cc.printClusterList());
        return true;
    }

}
