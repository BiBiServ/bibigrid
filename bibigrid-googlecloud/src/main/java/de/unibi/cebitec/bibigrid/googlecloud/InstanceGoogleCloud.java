package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.services.compute.model.Instance;
import de.unibi.cebitec.bibigrid.core.model.Configuration;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class InstanceGoogleCloud extends de.unibi.cebitec.bibigrid.core.model.Instance {
    private final Instance internalInstance;
    private Map<String, String> tags;

    InstanceGoogleCloud(Configuration.InstanceConfiguration configuration, Instance internalInstance) {
        super(configuration);
        this.internalInstance = internalInstance;
    }

    @Override
    public String getPublicIp() {
        return GoogleCloudUtils.getInstancePublicIp(internalInstance);
    }

    @Override
    public String getPrivateIp() {
        return GoogleCloudUtils.getInstancePrivateIp(internalInstance);
    }

    @Override
    public String getHostname() {
        return GoogleCloudUtils.getInstanceFQDN(internalInstance);
    }

    @Override
    public String getName() {
        return internalInstance.getName();
    }

    @Override
    public String getTag(String key) {
        if (tags == null) {
            tags = internalInstance.getLabels() != null ? internalInstance.getLabels() : new HashMap<>();
        }
        return tags.getOrDefault(key, null);
    }

    @Override
    public ZonedDateTime getCreationTimestamp() {
        ZonedDateTime creationDateTime = ZonedDateTime.parse(internalInstance.getCreationTimestamp());
        return creationDateTime.withZoneSameInstant(ZoneOffset.systemDefault().normalized());
    }
}
