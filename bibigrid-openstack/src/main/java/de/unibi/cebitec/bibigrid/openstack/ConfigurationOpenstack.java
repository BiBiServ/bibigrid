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
    private String network;
    private String securityGroup;

    public OpenStackCredentials getOpenstackCredentials() {
        return openstackCredentials;
    }

    public void setOpenstackCredentials(final OpenStackCredentials openstackCredentials) {
        this.openstackCredentials = openstackCredentials;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getRouter() {
        return router;
    }

    public void setRouter(String router) {
        this.router = router;
    }

    public String getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(String securityGroup) {
        this.securityGroup = securityGroup;
    }
}
