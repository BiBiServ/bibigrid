package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;

final class IntentUtils {
    static AmazonEC2 getClient(ConfigurationAWS config) {
        String endpoint = "ec2." + config.getRegion() + ".amazonaws.com";
        return AmazonEC2Client.builder()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, config.getRegion()))
                .withCredentials(new AWSStaticCredentialsProvider(config.getCredentials()))
                .build();
    }
}
