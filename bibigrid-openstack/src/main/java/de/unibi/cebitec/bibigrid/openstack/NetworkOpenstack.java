package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.model.Network;
import org.openstack4j.model.network.Router;

public class NetworkOpenstack extends Network {
    private final org.openstack4j.model.network.Network internalNetwork;
    private final Router internalRouter;

    NetworkOpenstack(org.openstack4j.model.network.Network internalNetwork, Router internalRouter) {
        this.internalNetwork = internalNetwork;
        this.internalRouter = internalRouter;
    }

    @Override
    public String getId() {
        return internalNetwork.getId();
    }

    @Override
    public String getName() {
        return internalNetwork.getName();
    }

    @Override
    public String getCidr() {
        return null;
    }

    public Router getRouter() {
        return internalRouter;
    }
}
