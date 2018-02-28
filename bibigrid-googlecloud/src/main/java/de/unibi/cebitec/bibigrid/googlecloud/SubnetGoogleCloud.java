package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.services.compute.model.Subnetwork;
import de.unibi.cebitec.bibigrid.core.model.Subnet;

public class SubnetGoogleCloud extends Subnet {
    private final Subnetwork internalSubnet;

    SubnetGoogleCloud(Subnetwork internalSubnet) {
        this.internalSubnet = internalSubnet;
    }

    Subnetwork getInternal() {
        return internalSubnet;
    }

    @Override
    public String getId() {
        return internalSubnet.getSelfLink();
    }

    @Override
    public String getCidr() {
        return internalSubnet.getIpCidrRange();
    }

    @Override
    public String getName() {
        return internalSubnet.getName();
    }
}
