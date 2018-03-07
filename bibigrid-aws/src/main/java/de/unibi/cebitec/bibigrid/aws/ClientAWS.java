package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class ClientAWS extends Client {
    private static final Logger LOG = LoggerFactory.getLogger(ClientAWS.class);

    private AmazonEC2 internalClient;

    ClientAWS(ConfigurationAWS config) throws ClientConnectionFailedException {
        try {
            String endpoint = "ec2." + config.getRegion() + ".amazonaws.com";
            internalClient = AmazonEC2Client.builder()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, config.getRegion()))
                    .withCredentials(new AWSStaticCredentialsProvider(config.getCredentials()))
                    .build();
            LOG.info("AWS connection established.");
        } catch (Exception e) {
            throw new ClientConnectionFailedException("Failed to connect AWS client.", e);
        }
    }

    public AmazonEC2 getInternal() {
        return internalClient;
    }
}
