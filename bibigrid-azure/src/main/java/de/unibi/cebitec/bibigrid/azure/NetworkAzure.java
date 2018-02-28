package de.unibi.cebitec.bibigrid.azure;

import de.unibi.cebitec.bibigrid.core.model.Network;

public class NetworkAzure extends Network {
    private final com.microsoft.azure.management.network.Network internalNetwork;

    NetworkAzure(com.microsoft.azure.management.network.Network internalNetwork) {
        this.internalNetwork = internalNetwork;
    }

    @Override
    public String getId() {
        return internalNetwork.id();
    }

    @Override
    public String getName() {
        return internalNetwork.name();
    }

    @Override
    public String getCidr() {
        return internalNetwork.addressSpaces().size() > 0 ? internalNetwork.addressSpaces().get(0) : null;
    }

    public com.microsoft.azure.management.network.Network getInternal() {
        return internalNetwork;
    }
}
