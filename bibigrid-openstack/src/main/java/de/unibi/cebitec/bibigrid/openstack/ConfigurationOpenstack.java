package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.model.Configuration;

import java.io.IOException;

/**
 * Extends general configuration with openstack specific options
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ConfigurationOpenstack extends Configuration {
    public ConfigurationOpenstack() throws IOException {
        super();
    }

    private OpenStackCredentials openstackCredentials;
    private String router;
    private String securityGroup;

    public OpenStackCredentials getOpenstackCredentials() {
        return openstackCredentials;
    }

    public void setOpenstackCredentials(final OpenStackCredentials openstackCredentials) {
        this.openstackCredentials = openstackCredentials;
    }

    public String getRouter() {
        return router;
    }

    public void setRouter(String router) {
        this.router = router != null ? router.trim() : null;
    }

    public String getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(String securityGroup) {
        this.securityGroup = securityGroup != null ? securityGroup.trim() : null;
    }
}
