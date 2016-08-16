package de.unibi.cebitec.bibigrid.meta.openstack;

import de.unibi.cebitec.bibigrid.meta.TerminateIntent;
import de.unibi.cebitec.bibigrid.model.Configuration;
import java.util.ArrayList;
import java.util.List;
import org.jclouds.http.HttpResponseException;

import org.openstack4j.api.compute.ComputeSecurityGroupService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.SecGroupExtension;
import org.openstack4j.model.compute.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Johannes Steiner (1st version), Jan Krueger (jkrueger@cebitec.uni-bielefeld.de)
 */
public class TerminateIntentOpenstack extends OpenStackIntent implements TerminateIntent {

    public static final Logger LOG = LoggerFactory.getLogger(TerminateIntentOpenstack.class);

    private final String os_region;
//    private final String provider = "openstack-nova";

    public TerminateIntentOpenstack(Configuration conf) {
        super(conf);
        os_region = conf.getRegion();
    }

    @Override
    public boolean terminate() {
        if (conf.getClusterId() != null && !conf.getClusterId().isEmpty()) {
            LOG.info("Terminating cluster with ID: {}", conf.getClusterId());
            List<Server> l = getServers(conf.getClusterId());
            for (Server serv : l) {
                os.compute().servers().delete(serv.getId());
                LOG.info("Terminated " + serv.getName());
            }

     
            ComputeSecurityGroupService securityGroups = os.compute().securityGroups();
           // iterate over all Security Groups ... 
            for (SecGroupExtension securityGroup : securityGroups.list()) {
                // only clean the security group with id "sg-<clusterid>
                if (securityGroup.getName().equals("sg-" + conf.getClusterId().trim())) {

                    int tries = 15;
                    while (true) {
                        try {
                            Thread.sleep(1000);
                            ActionResponse ar = securityGroups.delete(securityGroup.getId()); 
                            if (ar.isSuccess()) {
                                break;
                            }
                            LOG.warn("{} Try again in a second ...",ar.getFault());
                            
                        } catch (InterruptedException ex) {
                            // do nothing
                        } catch (HttpResponseException he) {
                            LOG.info("Waiting for detaching SecurityGroup ...");
                            if (tries == 0) {
                                LOG.error("Can't detach SecurityGroup aborting.");
                                LOG.error(he.getMessage());
                                return false;
                            }
                            tries--;
                        }
                    }
                    LOG.info("SecurityGroup ({}) deleted.", securityGroup.getName());
                } 
            }
           
            LOG.info("Cluster (ID: {}) successfully terminated", conf.getClusterId().trim());
            return true;
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
            LOG.error("No suitable bibigrid cluster with ID: [{}] found.", conf.getClusterId().trim());
            System.exit(1);
        }
        return ret;
    }

}
