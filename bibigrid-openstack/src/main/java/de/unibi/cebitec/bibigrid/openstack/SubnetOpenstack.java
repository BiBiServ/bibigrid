package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.model.Subnet;

public class SubnetOpenstack extends Subnet {
    private final org.openstack4j.model.network.Subnet internalSubnet;

    SubnetOpenstack(org.openstack4j.model.network.Subnet internalSubnet) {
        this.internalSubnet = internalSubnet;
    }

    @Override
    public String getId() {
        return internalSubnet.getId();
    }

    @Override
    public String getName() {
        return internalSubnet.getName();
    }

    @Override
    public String getCidr() {
        return internalSubnet.getCidr();
    }
}
