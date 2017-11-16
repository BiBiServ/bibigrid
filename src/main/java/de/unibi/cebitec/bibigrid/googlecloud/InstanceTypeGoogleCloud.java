package de.unibi.cebitec.bibigrid.googlecloud;

import de.unibi.cebitec.bibigrid.model.InstanceSpecification;
import de.unibi.cebitec.bibigrid.model.InstanceType;
import de.unibi.cebitec.bibigrid.model.exceptions.InstanceTypeNotFoundException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the general InstanceType for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class InstanceTypeGoogleCloud extends InstanceType {
    private static final Map<String, InstanceSpecification> typeSpecMap = new HashMap<>();

    private static void addType(String id, int cores) {
        // TODO: decide ephemeral size
        typeSpecMap.put(id, new InstanceSpecification(cores, 0, false, false, true, true));
    }

    static {
        for (int cores : Arrays.asList(1, 2, 4, 8, 16, 32, 64))
            addType("n1-standard-" + cores, cores);
        for (int cores : Arrays.asList(2, 4, 8, 16, 32, 64))
            addType("n1-highmem-" + cores, cores);
        for (int cores : Arrays.asList(2, 4, 8, 16, 32, 64))
            addType("n1-highcpu-" + cores, cores);

        addType("f1-micro", 1);
        addType("g1-small", 1);
    }

    public InstanceTypeGoogleCloud(String type) throws InstanceTypeNotFoundException {
        try {
            value = type;
            spec = typeSpecMap.get(type);
        } catch (Exception e) {
            throw new InstanceTypeNotFoundException("Invalid instance type " + type);
        }
    }
}
