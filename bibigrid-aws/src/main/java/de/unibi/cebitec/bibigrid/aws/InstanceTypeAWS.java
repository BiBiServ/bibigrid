package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.services.ec2.model.InstanceType;
import de.unibi.cebitec.bibigrid.core.model.exceptions.InstanceTypeNotFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the general InstanceType for a AWS based cluster.
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public class InstanceTypeAWS extends de.unibi.cebitec.bibigrid.core.model.InstanceType {
    private static final Map<InstanceType, InstanceTypeAWS> typeSpecMap = new HashMap<>();

    private static void addTypeSpec(InstanceType type, int cores, int ephemerals, boolean swap, boolean pvm,
                                    boolean hvm, boolean clusterInstance) {
        typeSpecMap.put(type, new InstanceTypeAWS(type, cores, ephemerals, swap, pvm, hvm, clusterInstance));
    }

    static {
        // General use
        addTypeSpec(InstanceType.M4Large, 2, 0, false, false, true, false);
        addTypeSpec(InstanceType.M4Xlarge, 4, 0, false, false, true, false);
        addTypeSpec(InstanceType.M42xlarge, 8, 0, false, false, true, false);
        addTypeSpec(InstanceType.M44xlarge, 16, 0, false, false, true, false);
        addTypeSpec(InstanceType.M410xlarge, 40, 0, false, false, true, false);

        addTypeSpec(InstanceType.M3Medium, 1, 1, false, true, true, false);
        addTypeSpec(InstanceType.M3Large, 2, 1, false, true, true, false);
        addTypeSpec(InstanceType.M3Xlarge, 4, 2, false, true, true, false);
        addTypeSpec(InstanceType.M32xlarge, 8, 2, false, true, true, false);

        addTypeSpec(InstanceType.M1Small, 1, 1, false, true, false, false);
        addTypeSpec(InstanceType.M1Medium, 1, 1, false, true, false, false);
        addTypeSpec(InstanceType.M1Large, 2, 2, false, true, false, false);
        addTypeSpec(InstanceType.M1Xlarge, 4, 4, false, true, false, false);

        //compute optimized
        addTypeSpec(InstanceType.C4Large, 2, 0, false, false, true, true);
        addTypeSpec(InstanceType.C4Xlarge, 4, 0, false, false, true, true);
        addTypeSpec(InstanceType.C42xlarge, 8, 0, false, false, true, true);
        addTypeSpec(InstanceType.C44xlarge, 16, 0, false, false, true, true);
        addTypeSpec(InstanceType.C48xlarge, 36, 0, false, false, true, true);

        addTypeSpec(InstanceType.C3Large, 2, 2, false, true, true, true);
        addTypeSpec(InstanceType.C3Xlarge, 4, 2, false, true, true, true);
        addTypeSpec(InstanceType.C32xlarge, 8, 2, false, true, true, true);
        addTypeSpec(InstanceType.C34xlarge, 16, 2, false, true, true, true);
        addTypeSpec(InstanceType.C38xlarge, 32, 2, false, true, true, true);

        addTypeSpec(InstanceType.C1Medium, 2, 1, false, true, false, false);
        addTypeSpec(InstanceType.C1Xlarge, 8, 4, false, true, false, false);
        addTypeSpec(InstanceType.Cc28xlarge, 32, 4, false, false, true, true);

        // GPU 
        addTypeSpec(InstanceType.G22xlarge, 8, 1, false, false, true, true);
        addTypeSpec(InstanceType.Cg14xlarge, 16, 2, false, false, true, true);

        // Memory optimized
        addTypeSpec(InstanceType.M2Xlarge, 2, 1, false, true, false, false);
        addTypeSpec(InstanceType.M22xlarge, 4, 1, false, true, false, false);
        addTypeSpec(InstanceType.M24xlarge, 8, 2, false, true, false, false);
        addTypeSpec(InstanceType.Cr18xlarge, 32, 2, false, false, true, true);

        addTypeSpec(InstanceType.R3Large, 2, 1, false, false, true, true);
        addTypeSpec(InstanceType.R3Xlarge, 4, 1, false, false, true, true);
        addTypeSpec(InstanceType.R32xlarge, 8, 1, false, false, true, true);
        addTypeSpec(InstanceType.R34xlarge, 16, 1, false, false, true, true);
        addTypeSpec(InstanceType.R38xlarge, 32, 2, false, false, true, true);

        // Storage optimized
        addTypeSpec(InstanceType.D2Xlarge, 4, 3, false, false, true, true);
        addTypeSpec(InstanceType.D22xlarge, 8, 6, false, false, true, true);
        addTypeSpec(InstanceType.D24xlarge, 16, 12, false, false, true, true);
        addTypeSpec(InstanceType.D28xlarge, 36, 24, false, false, true, true);

        addTypeSpec(InstanceType.Hi14xlarge, 16, 2, false, true, true, true);
        addTypeSpec(InstanceType.Hs18xlarge, 16, 24, false, true, true, true);

        // I2 Instances
        addTypeSpec(InstanceType.I2Xlarge, 4, 1, false, false, true, true);
        addTypeSpec(InstanceType.I22xlarge, 8, 2, false, false, true, true);
        addTypeSpec(InstanceType.I24xlarge, 16, 4, false, false, true, true);
        addTypeSpec(InstanceType.I28xlarge, 32, 8, false, false, true, true);

        // t1,t2
        addTypeSpec(InstanceType.T1Micro, 1, 0, false, true, false, false);
        addTypeSpec(InstanceType.T2Micro, 1, 0, false, false, true, false);
        addTypeSpec(InstanceType.T2Small, 1, 0, false, false, true, false);
        addTypeSpec(InstanceType.T2Medium, 2, 0, false, false, true, false);
        addTypeSpec(InstanceType.T2Large, 2, 0, false, false, true, false);
    }

    private InstanceTypeAWS(InstanceType type, int cores, int ephemerals, boolean swap, boolean pvm, boolean hvm,
                            boolean clusterInstance) {
        value = type.toString();
        this.instanceCores = cores;
        this.ephemerals = ephemerals;
        this.clusterInstance = clusterInstance;
        this.pvm = pvm;
        this.hvm = hvm;
        this.swap = swap;
    }

    static InstanceTypeAWS getByType(String type) throws InstanceTypeNotFoundException {
        try {
            InstanceType enumType = InstanceType.fromValue(type);
            if (typeSpecMap.containsKey(enumType)) {
                return typeSpecMap.get(enumType);
            }
        } catch (IllegalArgumentException ignored) {
        }
        throw new InstanceTypeNotFoundException("Invalid instance type " + type);
    }
}
