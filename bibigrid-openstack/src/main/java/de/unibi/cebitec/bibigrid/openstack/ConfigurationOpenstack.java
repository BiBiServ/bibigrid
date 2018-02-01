package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.model.Configuration;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class ConfigurationOpenstack extends Configuration {
    private OpenStackCredentials openstackCredentials;
    private String routerName;
    private String networkName;
    private String securityGroup;

    OpenStackCredentials getOpenstackCredentials() {
        return openstackCredentials;
    }

    void setOpenstackCredentials(final OpenStackCredentials openstackCredentials) {
        this.openstackCredentials = openstackCredentials;
    }

    String getNetworkName() {
        return networkName;
    }

    void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    String getRouterName() {
        return routerName;
    }

    void setRouterName(String routerName) {
        this.routerName = routerName;
    }

    String getSecurityGroup() {
        return securityGroup;
    }

    void setSecurityGroup(String securityGroup) {
        this.securityGroup = securityGroup;
    }
}
