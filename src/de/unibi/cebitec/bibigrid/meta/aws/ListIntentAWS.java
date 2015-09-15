/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.meta.aws;

import com.amazonaws.services.ec2.AmazonEC2Client;
import static de.unibi.cebitec.bibigrid.ctrl.ListIntent.log;
import de.unibi.cebitec.bibigrid.meta.ListIntent;
import de.unibi.cebitec.bibigrid.model.Configuration;
import de.unibi.cebitec.bibigrid.model.CurrentClusters;

/**
 *
 * @author jsteiner
 */
public class ListIntentAWS implements ListIntent {

    private final Configuration conf;

    public ListIntentAWS(final Configuration conf) {
        this.conf = conf;
    }

    @Override
    public boolean list() {
////////////////////////////////////////////////////////////////////////
        ///// create client 
        AmazonEC2Client ec2 = new AmazonEC2Client(conf.getCredentials());
        ec2.setEndpoint("ec2." + conf.getRegion() + ".amazonaws.com");

        log.info("Search for BiBiGrid cluster in '{}' ...", conf.getRegion());

        ////////////////////////////////////////////////////////////////////////
        ///// print cluster info
        CurrentClusters cc = new CurrentClusters(ec2);
        log.info(cc.printClusterList());
        return true;
    }

}
