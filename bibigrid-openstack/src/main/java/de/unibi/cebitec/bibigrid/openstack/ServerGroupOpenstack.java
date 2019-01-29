package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.model.ServerGroup;

public class ServerGroupOpenstack extends ServerGroup {

    private final org.openstack4j.model.compute.ServerGroup serverGroup;

    public ServerGroupOpenstack(org.openstack4j.model.compute.ServerGroup serverGroup) {
        this.serverGroup = serverGroup;
    }

    @Override
    public String getId() {
        return serverGroup.getId();
    }

    @Override
    public String getName() {
        return serverGroup.getName();
    }
}
