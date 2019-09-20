package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.model.Configuration;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ConfigurationOpenstack extends Configuration {
    public ConfigurationOpenstack() {
    }

    private OpenStackCredentials openstackCredentials;
    private String router;
    private String securityGroup;

    public OpenStackCredentials getOpenstackCredentials() {

        LOG.info(openstackCredentials.getUsername());

        return openstackCredentials;
    }

    public void setOpenstackCredentials(final OpenStackCredentials openstackCredentials) {
        LOG.error(openstackCredentials.getPassword());

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
