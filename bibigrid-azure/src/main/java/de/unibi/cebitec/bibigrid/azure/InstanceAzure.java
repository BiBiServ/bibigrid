package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.management.compute.VirtualMachine;
import de.unibi.cebitec.bibigrid.core.model.Instance;

public class InstanceAzure extends Instance {
    private final VirtualMachine internalInstance;

    public InstanceAzure(VirtualMachine internalInstance) {
        this.internalInstance = internalInstance;
    }

    @Override
    public String getPublicIp() {
        return internalInstance.getPrimaryPublicIPAddress().ipAddress();
    }

    @Override
    public String getPrivateIp() {
        return internalInstance.getPrimaryNetworkInterface().primaryPrivateIP();
    }

    @Override
    public String getHostname() {
        return internalInstance.getPrimaryPublicIPAddress().fqdn();
    }
}
