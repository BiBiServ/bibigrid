package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.model.InstanceSpecification;
import de.unibi.cebitec.bibigrid.core.model.InstanceType;

import java.util.ArrayList;
import java.util.List;

import de.unibi.cebitec.bibigrid.core.model.exceptions.InstanceTypeNotFoundException;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Flavor;

/**
 * Provides a list of all supported flavors supported by configured openstack installation/region.
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public class InstanceTypeOpenstack extends InstanceType {
    private OSClient os;

    InstanceTypeOpenstack(ConfigurationOpenstack config, String type) throws InstanceTypeNotFoundException {
        os = OpenStackUtils.buildOSClient(config);
        for (Flavor f : listFlavors()) {
            if (f.getName().equalsIgnoreCase(type)) {
                value = f.getName();
                spec = new InstanceSpecification(f.getVcpus(), (f.getEphemeral() == 0) ? 0 : 1, (f.getSwap() > 0), false, false, false);
                return;
            }
        }
        throw new InstanceTypeNotFoundException("Invalid instance type " + type);
    }

    private List<Flavor> listFlavors() {
        List<Flavor> ret = new ArrayList<>();
        ret.addAll(os.compute().flavors().list());
        return ret;
    }
}