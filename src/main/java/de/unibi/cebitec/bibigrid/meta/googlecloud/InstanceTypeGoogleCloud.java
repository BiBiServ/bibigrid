package de.unibi.cebitec.bibigrid.meta.googlecloud;

import de.unibi.cebitec.bibigrid.model.Configuration;
import de.unibi.cebitec.bibigrid.model.InstanceType;
import de.unibi.cebitec.bibigrid.util.InstanceInformation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the general InstanceType interface for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class InstanceTypeGoogleCloud extends InstanceType {
    private static final Map<String, InstanceInformation.InstanceSpecification> count = new HashMap<>();

    private static void addType(String id, int cores) {
        // TODO: decide ephemeral size
        count.put(id, new InstanceInformation.InstanceSpecification(cores, 0, false, false, true, true));
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

    public InstanceTypeGoogleCloud(Configuration conf, String type) throws Exception {
        try {
            value = type;
            spec = count.get(type);
        } catch (Exception e) {
            throw new Exception("Invalid instance type");
        }
    }
}
