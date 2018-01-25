package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.model.Configuration;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class ConfigurationOpenstack extends Configuration {
    private OpenStackCredentials openstackCredentials;
    private String gatewayName;
    private String routerName;
    private String networkName;
    private String securityGroup;

    OpenStackCredentials getOpenstackCredentials() {
        return openstackCredentials;
    }

    void setOpenstackCredentials(final OpenStackCredentials openstackCredentials) {
        this.openstackCredentials = openstackCredentials;
    }

    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public String getRouterName() {
        return routerName;
    }

    public void setRouterName(String routerName) {
        this.routerName = routerName;
    }

    public String getGatewayName() {
        return gatewayName;
    }

    public void setGatewayName(String gatewayName) {
        this.gatewayName = gatewayName;
    }

    public String getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(String securityGroup) {
        this.securityGroup = securityGroup;
    }
}
