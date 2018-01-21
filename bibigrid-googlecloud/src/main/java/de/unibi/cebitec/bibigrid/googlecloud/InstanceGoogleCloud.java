package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.cloud.compute.Instance;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class InstanceGoogleCloud extends de.unibi.cebitec.bibigrid.core.model.Instance {
    private final Instance internalInstance;

    InstanceGoogleCloud(Instance internalInstance) {
        this.internalInstance = internalInstance;
    }

    @Override
    public String getPublicIp() {
        return GoogleCloudUtils.getInstancePublicIp(internalInstance);
    }

    @Override
    public String getPrivateIp() {
        return GoogleCloudUtils.getInstancePrivateIp(internalInstance);
    }

    @Override
    public String getHostname() {
        return GoogleCloudUtils.getInstanceFQDN(internalInstance);
    }
}
