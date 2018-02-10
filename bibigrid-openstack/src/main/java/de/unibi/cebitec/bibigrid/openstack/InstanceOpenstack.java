package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.model.Instance;
import org.openstack4j.model.compute.Addresses;
import org.openstack4j.model.compute.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

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

    InstanceOpenstack(Server server) {
        this.server = server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    String getId() {
        return server.getId();
    }

    @Override
    public String getPrivateIp() {
        return ip;
    }

    void setPrivateIp(String ip) {
        this.ip = ip;
    }

    @Override
    public String getPublicIp() {
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
            LOG.warn("ip must be a valid IPv4 address string.");
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

    public String getKeyName() {
        return server.getKeyName();
    }

    public Addresses getAddresses() {
        return server.getAddresses();
    }
}
