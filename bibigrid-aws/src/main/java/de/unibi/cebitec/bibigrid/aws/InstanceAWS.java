package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.services.ec2.model.Instance;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public final class InstanceAWS extends de.unibi.cebitec.bibigrid.core.model.Instance {
    private final Instance internalInstance;

    public InstanceAWS(Instance internalInstance) {
        this.internalInstance = internalInstance;
    }

    @Override
    public String getPublicIp() {
        return internalInstance.getPublicIpAddress();
    }

    @Override
    public String getPrivateIp() {
        return internalInstance.getPrivateIpAddress();
    }

    @Override
    public String getHostname() {
        // TODO: or getPublicDnsName?
        return internalInstance.getPrivateDnsName();
    }
}
