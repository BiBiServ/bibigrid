package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.model.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public final class InstanceOpenstack extends Instance {
    private static final Logger LOG = LoggerFactory.getLogger(InstanceOpenstack.class);

    private boolean active = false;
    private boolean error = false;
    private String id;
    private String ip;
    private String publicIp;
    private String hostname;
    private String neutronHostname;

    InstanceOpenstack() {
    }

    InstanceOpenstack(String id) {
        this.id = id;
    }

    String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
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
        return hostname;
    }

    void setHostname(String hostname) {
        this.hostname = hostname;
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
}
