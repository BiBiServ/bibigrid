package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.management.compute.VirtualMachine;
import de.unibi.cebitec.bibigrid.core.model.Instance;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class InstanceAzure extends Instance {
    public static final String TAG_CREATION = "creation";

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

    @Override
    public String getName() {
        return internalInstance.computerName();
    }

    @Override
    public String getTag(String key) {
        return internalInstance.tags() != null ? internalInstance.tags().getOrDefault(key, null) : null;
    }

    @Override
    public ZonedDateTime getCreationTimestamp() {
        ZonedDateTime creationDateTime = ZonedDateTime.parse(getTag(TAG_CREATION));
        return creationDateTime.withZoneSameInstant(ZoneOffset.systemDefault().normalized());
    }
}
