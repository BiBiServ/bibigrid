package de.unibi.cebitec.bibigrid.googlecloud;

import de.unibi.cebitec.bibigrid.core.model.Network;

public class NetworkGoogleCloud extends Network {
    private final com.google.api.services.compute.model.Network internalNetwork;

    NetworkGoogleCloud(com.google.api.services.compute.model.Network internalNetwork) {
        this.internalNetwork = internalNetwork;
    }

    @Override
    public String getId() {
        return internalNetwork.getSelfLink();
    }

    @Override
    public String getName() {
        return internalNetwork.getName();
    }

    @Override
    public String getCidr() {
        return internalNetwork.getIPv4Range();
    }

    public boolean isAutoCreate() {
        return internalNetwork.getAutoCreateSubnetworks();
    }
}
