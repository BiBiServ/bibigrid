package de.unibi.cebitec.bibigrid.azure;

import de.unibi.cebitec.bibigrid.core.model.Subnet;

public class SubnetAzure extends Subnet {
    private final com.microsoft.azure.management.network.Subnet internalSubnet;

    SubnetAzure(com.microsoft.azure.management.network.Subnet internalSubnet) {
        this.internalSubnet = internalSubnet;
    }

    @Override
    public String getId() {
        return internalSubnet.name();
    }

    @Override
    public String getName() {
        return internalSubnet.name();
    }

    @Override
    public String getCidr() {
        return internalSubnet.addressPrefix();
    }

    public com.microsoft.azure.management.network.Subnet getInternal() {
        return internalSubnet;
    }
}
