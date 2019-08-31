package de.unibi.cebitec.bibigrid.aws;

import de.unibi.cebitec.bibigrid.core.model.Configuration;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de, jkrueger(at)cebitec.uni-bielefeld.de
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ConfigurationAWS extends Configuration {
    public ConfigurationAWS() {
    }

    private double bidPrice;
    private double bidPriceMaster;
    private boolean publicWorkerIps = false;
    private boolean useSpotInstances = false;

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

    public boolean isPublicWorkerIps() {
        return publicWorkerIps;
    }

    public void setPublicWorkerIps(boolean publicWorkerIps) {
        this.publicWorkerIps = publicWorkerIps;
    }

    public boolean isUseSpotInstances() {
        return useSpotInstances;
    }

    public void setUseSpotInstances(boolean useSpotInstances) {
        this.useSpotInstances = useSpotInstances;
        if (useSpotInstances) {
            LOG.info(V, "Using spot requests for all instance requests.");
        }
    }
}
