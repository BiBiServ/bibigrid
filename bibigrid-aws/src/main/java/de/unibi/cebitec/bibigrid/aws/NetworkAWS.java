package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.services.ec2.model.Vpc;
import de.unibi.cebitec.bibigrid.core.model.Network;

public class NetworkAWS extends Network {
    private final Vpc internalVpc;

    NetworkAWS(Vpc internalVpc) {
        this.internalVpc = internalVpc;
    }

    @Override
    public String getId() {
        return internalVpc.getVpcId();
    }

    @Override
    public String getName() {
        return internalVpc.getVpcId();
    }

    @Override
    public String getCidr() {
        return internalVpc.getCidrBlock();
    }
}
