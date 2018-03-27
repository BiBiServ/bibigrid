package de.unibi.cebitec.bibigrid.aws;

import de.unibi.cebitec.bibigrid.core.model.Configuration;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ConfigurationAWS extends Configuration {
    public ConfigurationAWS() {
    }

    private double bidPrice;
    private double bidPriceMaster;
    private boolean publicSlaveIps;

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
