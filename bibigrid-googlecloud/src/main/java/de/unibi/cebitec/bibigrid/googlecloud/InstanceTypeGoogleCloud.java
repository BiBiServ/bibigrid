package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.services.compute.model.MachineType;
import de.unibi.cebitec.bibigrid.core.model.InstanceType;

/**
 * Implementation of the general InstanceType for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class InstanceTypeGoogleCloud extends InstanceType {
    InstanceTypeGoogleCloud(MachineType flavor) {
        value = flavor.getName();
        cpuCores = flavor.getGuestCpus();
        ephemerals = 0;
        clusterInstance = false;
        pvm = false;
        hvm = false;
        swap = false;
        maxRam = flavor.getMemoryMb();
        maxDiskSpace = flavor.getMaximumPersistentDisksSizeGb() * 1024;
    }
}
