package de.unibi.cebitec.bibigrid.core.intents;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class CreateClusterTest {
    @Test
    public void generateClusterId() {
        String clusterId = CreateCluster.generateClusterId();
        System.out.println(clusterId);
        Assert.assertNotNull(clusterId);
    }

}