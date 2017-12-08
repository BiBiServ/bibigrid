package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.services.ec2.AmazonEC2Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class IntentUtils {
    private static final Logger LOG = LoggerFactory.getLogger(IntentUtils.class);

    static AmazonEC2Client getClient(ConfigurationAWS config) {
        AmazonEC2Client ec2 = new AmazonEC2Client(config.getCredentials());
        ec2.setEndpoint("ec2." + config.getRegion() + ".amazonaws.com");
        return ec2;
    }

    static void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException ie) {
            LOG.error("Thread.sleep interrupted!");
        }
    }
}
