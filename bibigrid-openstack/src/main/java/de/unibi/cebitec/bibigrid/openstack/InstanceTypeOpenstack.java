package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.model.InstanceType;

import org.openstack4j.model.compute.Flavor;

/**
 * Provides a list of all supported flavors supported by configured openstack installation/region.
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
class InstanceTypeOpenstack extends InstanceType {
    private final Flavor flavor;

    InstanceTypeOpenstack(Flavor flavor) {
        this.flavor = flavor;
        value = flavor.getName();
        cpuCores = flavor.getVcpus();
        ephemerals = Math.min(1, Math.max(0, flavor.getEphemeral()));
        clusterInstance = false;
        pvm = false;
        hvm = false;
        swap = flavor.getSwap() > 0;
        maxRam = flavor.getRam();
        maxDiskSpace = flavor.getDisk() * 1024;
        configDrive = false;
    }

    Flavor getFlavor() {
        return flavor;
    }
}
