package de.unibi.cebitec.bibigrid.azure;

import de.unibi.cebitec.bibigrid.core.model.InstanceSpecification;
import de.unibi.cebitec.bibigrid.core.model.InstanceType;
import de.unibi.cebitec.bibigrid.core.model.exceptions.InstanceTypeNotFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the general InstanceType for an Azure based cluster.
 * <p/>
 * See: <a href="https://docs.microsoft.com/en-US/azure/virtual-machines/linux/sizes-general">
 * https://docs.microsoft.com/en-US/azure/virtual-machines/linux/sizes-general</a>
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class InstanceTypeAzure extends InstanceType {
    private static final Map<String, InstanceSpecification> typeSpecMap = new HashMap<>();

    private static void addType(String id, int cores) {
        // TODO: decide ephemeral size
        typeSpecMap.put(id, new InstanceSpecification(cores, 0, false, false, true, true));
    }

    static {

        // B-series
        addType("Standard_B1s", 1);
        addType("Standard_B1ms", 1);
        addType("Standard_B2s", 2);
        addType("Standard_B2ms", 2);
        addType("Standard_B4ms", 4);
        addType("Standard_B8ms", 8);
        // Dsv3-series
        // TODO
        // Dv3-series
        // TODO
        // DSv2-series
        // TODO
        // Dv2-series
        // TODO
        // DS-series
        // TODO
        // D-series
        // TODO
        // Av2-series
        // TODO
        // A-series
        addType("Standard_A0", 1);
        addType("Standard_A1", 1);
        addType("Standard_A2", 2);
        addType("Standard_A3", 4);
        addType("Standard_A4", 8);
        addType("Standard_A5", 2);
        addType("Standard_A6", 4);
        addType("Standard_A7", 8);
        // Fsv2, Fs, F
        // TODO
        // Esv3, Ev3, M, GS, G, DSv2, DS, Dv2, D
        // TODO
        // Ls
        // TODO
        // NV, NC, NCv2, ND
        // TODO
        // H, A8-11
        // TODO
    }

    InstanceTypeAzure(String type) throws InstanceTypeNotFoundException {
        try {
            value = type;
            spec = typeSpecMap.get(type);
        } catch (Exception e) {
            throw new InstanceTypeNotFoundException("Invalid instance type " + type);
        }
    }
}