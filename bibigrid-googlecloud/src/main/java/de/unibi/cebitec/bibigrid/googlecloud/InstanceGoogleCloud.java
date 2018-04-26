package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.services.compute.model.AccessConfig;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.NetworkInterface;
import de.unibi.cebitec.bibigrid.core.model.Configuration;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class InstanceGoogleCloud extends de.unibi.cebitec.bibigrid.core.model.Instance {
    private static final Pattern INSTANCE_LINK_PATTERN =
            Pattern.compile(".*?projects/([^/]+)/zones/([^/]+)/instances/([^/]+)");

    private final Instance internalInstance;
    private Map<String, String> tags;

    InstanceGoogleCloud(Configuration.InstanceConfiguration configuration, Instance internalInstance) {
        super(configuration);
        this.internalInstance = internalInstance;
    }

    Instance getInternal() {
        return internalInstance;
    }

    @Override
    public String getPublicIp() {
        List<NetworkInterface> interfaces = internalInstance.getNetworkInterfaces();
        if (interfaces.isEmpty())
            return null;
        List<AccessConfig> accessConfigs = interfaces.get(0).getAccessConfigs();
        return accessConfigs.isEmpty() ? null : accessConfigs.get(0).getNatIP();
    }

    @Override
    public String getPrivateIp() {
        List<NetworkInterface> interfaces = internalInstance.getNetworkInterfaces();
        return interfaces.isEmpty() ? null : interfaces.get(0).getNetworkIP();
    }

    @Override
    public String getHostname() {
        // Get the internal fully qualified domain name (FQDN) for an instance.
        // https://cloud.google.com/compute/docs/vpc/internal-dns#instance_fully_qualified_domain_names
        Matcher matcher = INSTANCE_LINK_PATTERN.matcher(internalInstance.getSelfLink());
        return matcher.find() ? matcher.group(3) + ".c." + matcher.group(1) + ".internal" : null;
    }

    @Override
    public String getId() {
        return internalInstance.getId().toString();
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

    @Override
    public String getKeyName() {
        return null;
    }
}
