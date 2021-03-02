package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.Instance;
import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public final class InstanceOpenstack extends Instance {
    private static final Logger LOG = LoggerFactory.getLogger(InstanceOpenstack.class);

    private Server server;
    private boolean active = false;
    private boolean error = false;
    private String ip;
    private String publicIp;
    private String neutronHostname;

    InstanceOpenstack(Configuration.InstanceConfiguration instanceConfiguration, Server server) {
        super(instanceConfiguration);
        this.server = server;
    }

    public Server getInternal() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    @Override
    public String getId() {
        return server.getId();
    }

    @Override
    public String getPrivateIp() {
        if (ip == null) {
            Map<String, List<? extends Address>> addressMap = server.getAddresses().getAddresses();
            // map should contain only one network
            if (addressMap.keySet().size() == 1) {
                for (Address address : addressMap.values().iterator().next()) {
                    if (address.getType().equals("fixed")) {
                        ip = address.getAddr();
                        break;
                    }
                }
            } else {
                LOG.warn("No or more than one network associated with instance '{}'.", getId());
            }
        }
        return ip;
    }

    void setPrivateIp(String ip) {
        this.ip = ip;
    }

    @Override
    public String getPublicIp() {
        if (publicIp == null) {
            Map<String, List<? extends Address>> addressMap = server.getAddresses().getAddresses();
            // map should contain only one network
            if (addressMap.keySet().size() == 1) {
                for (Address address : addressMap.values().iterator().next()) {
                    if (address.getType().equals("floating")) {
                        publicIp = address.getAddr();
                        break;
                    }
                }
            } else {
                LOG.warn("No or more than one network associated with instance '{}'.", getId());
            }
        }
        return publicIp;
    }

    void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    @Override
    public String getHostname() {
        return server.getName();
    }

    String getNeutronHostname() {
        return neutronHostname;
    }

    void setNeutronHostname(String neutronHostname) {
        this.neutronHostname = neutronHostname;
    }

    void updateNeutronHostname() {
        if (ip != null && ip.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
            String[] t = ip.split("\\.");
            neutronHostname = "host-" + String.join("-", t);
        } else {
            LOG.warn("Ip must be a valid IPv4 address string.");
        }
    }

    boolean isActive() {
        return active;
    }

    void setActive(boolean active) {
        this.active = active;
    }

    boolean hasError() {
        return error;
    }

    void setError(boolean error) {
        this.error = error;
    }

    @Override
    public String getName() {
        return server.getName();
    }

    @Override
    public String getTag(String key) {
        return server.getMetadata().getOrDefault(key, null);
    }

    @Override
    public ZonedDateTime getCreationTimestamp() {
        ZonedDateTime creationDateTime = ZonedDateTime.ofInstant(server.getCreated().toInstant(),
                ZoneId.of("Z").normalized());
        return creationDateTime.withZoneSameInstant(ZoneOffset.systemDefault().normalized());
    }

    @Override
    public String getKeyName() {
        return server.getKeyName();
    }
}
