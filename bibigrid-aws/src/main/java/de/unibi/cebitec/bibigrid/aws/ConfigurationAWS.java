package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.auth.AWSCredentials;
import de.unibi.cebitec.bibigrid.core.model.Configuration;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class ConfigurationAWS extends Configuration {
    /* AWS related Configuration options */
    private AWSCredentials credentials;
    private double bidPrice;
    private double bidPriceMaster;
    private boolean publicSlaveIps;

    AWSCredentials getCredentials() {
        return credentials;
    }

    void setCredentials(AWSCredentials credentials) {
        this.credentials = credentials;
    }

    double getBidPrice() {
        return bidPrice;
    }

    void setBidPrice(double bidPrice) {
        this.bidPrice = bidPrice;
    }

    double getBidPriceMaster() {
        return bidPriceMaster <= 0.0 ? bidPrice : bidPriceMaster;
    }

    void setBidPriceMaster(double bidPriceMaster) {
        this.bidPriceMaster = bidPriceMaster;
    }

    boolean isPublicSlaveIps() {
        return publicSlaveIps;
    }

    void setPublicSlaveIps(boolean publicSlaveIps) {
        this.publicSlaveIps = publicSlaveIps;
    }
}
