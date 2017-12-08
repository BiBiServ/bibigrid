package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.model.Configuration;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class ConfigurationOpenstack extends Configuration {
    private OpenStackCredentials openstackCredentials;

    OpenStackCredentials getOpenstackCredentials() {
        return openstackCredentials;
    }

    void setOpenstackCredentials(final OpenStackCredentials openstackCredentials) {
        this.openstackCredentials = openstackCredentials;
    }
}
