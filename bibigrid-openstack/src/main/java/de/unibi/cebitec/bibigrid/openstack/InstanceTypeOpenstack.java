package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.model.InstanceType;

import de.unibi.cebitec.bibigrid.core.model.exceptions.InstanceTypeNotFoundException;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Flavor;

/**
 * Provides a list of all supported flavors supported by configured openstack installation/region.
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
class InstanceTypeOpenstack extends InstanceType {
    InstanceTypeOpenstack(ConfigurationOpenstack config, String type) throws InstanceTypeNotFoundException {
        OSClient os = OpenStackUtils.buildOSClient(config);
        for (Flavor f : os.compute().flavors().list()) {
            if (f.getName().equalsIgnoreCase(type)) {
                value = f.getName();
                instanceCores = f.getVcpus();
                ephemerals = Math.min(1, Math.max(0, f.getEphemeral()));
                clusterInstance = false;
                pvm = false;
                hvm = false;
                swap = f.getSwap() > 0;
                return;
            }
        }
        throw new InstanceTypeNotFoundException("Invalid instance type " + type);
    }
}
