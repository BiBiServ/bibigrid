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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.jclouds.ContextBuilder;
import org.jclouds.http.HttpResponseException;

import org.jclouds.sshj.config.SshjSshClientModule;
import org.openstack4j.api.compute.ComputeSecurityGroupService;
import org.openstack4j.model.compute.SecGroupExtension;
import org.openstack4j.model.compute.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jsteiner
 */
public class TerminateIntentOpenstack extends OpenStackIntent implements TerminateIntent {

    public static final Logger log = LoggerFactory.getLogger(TerminateIntentOpenstack.class);

    private final String os_region;
    private final String provider = "openstack-nova";

    public TerminateIntentOpenstack(Configuration conf) {
        super(conf);
        os_region = conf.getRegion();
    }

    @Override
    public boolean terminate() {
        /**
         * What to do if the createIntent fails and the terminateIntent gets not
         * called via commandline -t ? ...
         */

        if (!conf.getClusterId().isEmpty()) {
            log.info("Terminating cluster with ID: {}", conf.getClusterId());
            List<Server> l = getServers(conf.getClusterId());
            for (Server serv : l) {
                os.compute().servers().delete(serv.getId());
                log.info("Terminated " + serv.getName());
            }
            /**
             * For deleting a SecurityGroup you first have to delete all its
             * rule and then delete the sg itself.
             */
            ComputeSecurityGroupService securityGroups = os.compute().securityGroups();

            for (SecGroupExtension securityGroup : securityGroups.list()) {
                if (securityGroup.getName().equals("sg-" + conf.getClusterId().trim())) {

//                    for (SecurityGroupRule rule : securityGroup.getRules()) {
//                        sgApi.deleteRule(rule.getId()); // first delete rules, then the sg itself
//                    }
                    int tries = 15;
                    while (true) {
                        try {
                            Thread.sleep(1000);
                            securityGroups.delete(securityGroup.getId()); // delete whole sg
                            break;
                        } catch (InterruptedException ex) {
                            java.util.logging.Logger.getLogger(TerminateIntentOpenstack.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (HttpResponseException he) {
                            log.info("Waiting for detaching SecurityGroup ...");
                            if (tries == 0) {
                                log.error("Can't detach SecurityGroup aborting.");
                                log.error(he.getMessage());
                                return false;
                            }
                            tries--;
                        }
                    }
                    log.info("SecurityGroup ({}) deleted.", securityGroup.getName());
                }
            }
            log.info("Cluster (ID: {}) successfully terminated", conf.getClusterId().trim());
            return true;
//        } 
//        else {
//            /**
//             * ... maybe this?
//             */
//            CurrentClusters cc = new CurrentClusters(os, conf);
//            Map<String, Cluster> clusters = cc.getClusterMap();
//            Cluster c = null;
//            for (String id : clusters.keySet()) {
//                if (id.contains(conf.getKeypair())) {
//                    c = clusters.get(id);
//                    break;
//                }
//            }
//
//            for (String slave : c.getSlaveinstances()) {
//                os..delete(slave);
//                log.info("Deleted Slave-Instance (ID: {})", slave);
//            }
//
//            s.delete(c.getMasterinstance());
//            log.info("Deleted Master-Instance (ID: {})", c.getMasterinstance());
//            return true;
//        }
        }
        return false;
    }

    private List<Server> getServers(String clusterID) {
        List<Server> ret = new ArrayList<>();

        for (Server server : os.compute().servers().list()) {
            String name = server.getName();
            if (name.substring(name.lastIndexOf("-") + 1, name.length()).equals(clusterID.trim())) {
                ret.add(server);
            }
        }

        if (ret.isEmpty()) {
            log.error("No suitable bibigrid cluster with ID: [{}] found.", conf.getClusterId().trim());
            System.exit(1);
        }
        return ret;
    }

}
