package de.unibi.cebitec.bibigrid.azure;

import com.microsoft.azure.management.compute.VirtualMachineSize;
import de.unibi.cebitec.bibigrid.core.model.InstanceType;

/**
 * Implementation of the general InstanceType for an Azure based cluster.
 * <p/>
 * See: <a href="https://docs.microsoft.com/en-US/azure/virtual-machines/linux/sizes-general">
 * https://docs.microsoft.com/en-US/azure/virtual-machines/linux/sizes-general</a>
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class InstanceTypeAzure extends InstanceType {
    InstanceTypeAzure(VirtualMachineSize flavor) {
        value = flavor.name();
        cpuCores = flavor.numberOfCores();
        ephemerals = 0;
        clusterInstance = false;
        pvm = false;
        hvm = false;
        swap = false;
        maxRam = flavor.memoryInMB();
        maxDiskSpace = flavor.osDiskSizeInMB(); // TODO: or resourceDiskSizeInMB?
    }
}
