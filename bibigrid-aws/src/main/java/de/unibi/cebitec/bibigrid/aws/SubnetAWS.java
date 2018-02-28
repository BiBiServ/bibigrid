package de.unibi.cebitec.bibigrid.aws;

import de.unibi.cebitec.bibigrid.core.model.Subnet;

public class SubnetAWS extends Subnet {
    private final com.amazonaws.services.ec2.model.Subnet internalSubnet;

    SubnetAWS(com.amazonaws.services.ec2.model.Subnet internalSubnet) {
        this.internalSubnet = internalSubnet;
    }

    @Override
    public String getId() {
        return internalSubnet.getSubnetId();
    }

    @Override
    public String getName() {
        return internalSubnet.getSubnetId();
    }

    @Override
    public String getCidr() {
        return internalSubnet.getCidrBlock();
    }
}
