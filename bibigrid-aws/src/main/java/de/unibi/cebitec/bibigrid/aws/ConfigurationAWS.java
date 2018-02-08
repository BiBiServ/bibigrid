package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.auth.AWSCredentials;
import de.unibi.cebitec.bibigrid.core.model.Configuration;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ConfigurationAWS extends Configuration {
    public ConfigurationAWS() {
    }

    private AWSCredentials credentials;
    private String awsCredentialsFile;
    private double bidPrice;
    private double bidPriceMaster;
    private boolean publicSlaveIps;

    public String getAwsCredentialsFile() {
        return awsCredentialsFile;
    }

    public void setAwsCredentialsFile(String awsCredentialsFile) {
        this.awsCredentialsFile = awsCredentialsFile;
    }

    public AWSCredentials getCredentials() {
        return credentials;
    }

    public void setCredentials(AWSCredentials credentials) {
        this.credentials = credentials;
    }

    public double getBidPrice() {
        return bidPrice;
    }

    public void setBidPrice(double bidPrice) {
        this.bidPrice = bidPrice;
    }

    public double getBidPriceMaster() {
        return bidPriceMaster <= 0.0 ? bidPrice : bidPriceMaster;
    }

    public void setBidPriceMaster(double bidPriceMaster) {
        this.bidPriceMaster = bidPriceMaster;
    }

    public boolean isPublicSlaveIps() {
        return publicSlaveIps;
    }

    public void setPublicSlaveIps(boolean publicSlaveIps) {
        this.publicSlaveIps = publicSlaveIps;
    }
}
