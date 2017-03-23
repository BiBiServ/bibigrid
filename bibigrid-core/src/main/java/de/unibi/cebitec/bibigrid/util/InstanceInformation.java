package de.unibi.cebitec.bibigrid.util;

import com.amazonaws.services.ec2.model.InstanceType;
import java.util.HashMap;
import java.util.Map;

public class InstanceInformation {

    private static final Map<InstanceType, InstanceSpecification> count = new HashMap<>();

    static {
        // General use

        count.put(InstanceType.M4Large, new InstanceSpecification(2, 0, false, true, false));
        count.put(InstanceType.M4Xlarge, new InstanceSpecification(4, 0, false, true, false));
        count.put(InstanceType.M42xlarge, new InstanceSpecification(8, 0, false, true, false));
        count.put(InstanceType.M44xlarge, new InstanceSpecification(16, 0, false, true, false));
        count.put(InstanceType.M410xlarge, new InstanceSpecification(40, 0, false, true, false));

        count.put(InstanceType.M3Medium, new InstanceSpecification(1, 1, true, true, false));
        count.put(InstanceType.M3Large, new InstanceSpecification(2, 1, true, true, false));
        count.put(InstanceType.M3Xlarge, new InstanceSpecification(4, 2, true, true, false));
        count.put(InstanceType.M32xlarge, new InstanceSpecification(8, 2, true, true, false));

        count.put(InstanceType.M1Small, new InstanceSpecification(1, 1, true, false, false));
        count.put(InstanceType.M1Medium, new InstanceSpecification(1, 1, true, false, false));
        count.put(InstanceType.M1Large, new InstanceSpecification(2, 2, true, false, false));
        count.put(InstanceType.M1Xlarge, new InstanceSpecification(4, 4, true, false, false));

        //compute optimized
        count.put(InstanceType.C4Large, new InstanceSpecification(2, 0, false, true, true));
        count.put(InstanceType.C4Xlarge, new InstanceSpecification(4, 0, false, true, true));
        count.put(InstanceType.C42xlarge, new InstanceSpecification(8, 0, false, true, true));
        count.put(InstanceType.C44xlarge, new InstanceSpecification(16, 0, false, true, true));
        count.put(InstanceType.C48xlarge, new InstanceSpecification(36, 0, false, true, true));

        count.put(InstanceType.C3Large, new InstanceSpecification(2, 2, true, true, true));
        count.put(InstanceType.C3Xlarge, new InstanceSpecification(4, 2, true, true, true));
        count.put(InstanceType.C32xlarge, new InstanceSpecification(8, 2, true, true, true));
        count.put(InstanceType.C34xlarge, new InstanceSpecification(16, 2, true, true, true));
        count.put(InstanceType.C38xlarge, new InstanceSpecification(32, 2, true, true, true));

        count.put(InstanceType.C1Medium, new InstanceSpecification(2, 1, true, false, false));
        count.put(InstanceType.C1Xlarge, new InstanceSpecification(8, 4, true, false, false));
        count.put(InstanceType.Cc28xlarge, new InstanceSpecification(32, 4, false, true, true));

        // GPU 
        count.put(InstanceType.G22xlarge, new InstanceSpecification(8, 1, false, true, true));
        count.put(InstanceType.Cg14xlarge, new InstanceSpecification(16, 2, false, true, true));

        // Memory optimized
        count.put(InstanceType.M2Xlarge, new InstanceSpecification(2, 1, true, false, false));
        count.put(InstanceType.M22xlarge, new InstanceSpecification(4, 1, true, false, false));
        count.put(InstanceType.M24xlarge, new InstanceSpecification(8, 2, true, false, false));
        count.put(InstanceType.Cr18xlarge, new InstanceSpecification(32, 2, false, true, true));

        count.put(InstanceType.R3Large, new InstanceSpecification(2, 1, false, true, true));
        count.put(InstanceType.R3Xlarge, new InstanceSpecification(4, 1, false, true, true));
        count.put(InstanceType.R32xlarge, new InstanceSpecification(8, 1, false, true, true));
        count.put(InstanceType.R34xlarge, new InstanceSpecification(16, 1, false, true, true));
        count.put(InstanceType.R38xlarge, new InstanceSpecification(32, 2, false, true, true));

        // Storage optimized
        count.put(InstanceType.D2Xlarge, new InstanceSpecification(4, 3, false, true, true));
        count.put(InstanceType.D22xlarge, new InstanceSpecification(8, 6, false, true, true));
        count.put(InstanceType.D24xlarge, new InstanceSpecification(16, 12, false, true, true));
        count.put(InstanceType.D28xlarge, new InstanceSpecification(36, 24, false, true, true));

        count.put(InstanceType.Hi14xlarge, new InstanceSpecification(16, 2, true, true, true));
        count.put(InstanceType.Hs18xlarge, new InstanceSpecification(16, 24, true, true, true));

        // I2 Instances
        count.put(InstanceType.I2Xlarge, new InstanceSpecification(4, 1, false, true, true));
        count.put(InstanceType.I22xlarge, new InstanceSpecification(8, 2, false, true, true));
        count.put(InstanceType.I24xlarge, new InstanceSpecification(16, 4, false, true, true));
        count.put(InstanceType.I28xlarge, new InstanceSpecification(32, 8, false, true, true));

        // t1,t2
        count.put(InstanceType.T1Micro, new InstanceSpecification(1, 0, true, false, false));
        count.put(InstanceType.T2Micro, new InstanceSpecification(1, 0, false, true, false));
        count.put(InstanceType.T2Small, new InstanceSpecification(1, 0, false, true, false));
        count.put(InstanceType.T2Medium, new InstanceSpecification(2, 0, false, true, false));
        count.put(InstanceType.T2Large, new InstanceSpecification(2, 0, false, true, false));
    }

    private InstanceInformation() {
    }

    public static InstanceSpecification getSpecs(InstanceType type) {
        return count.get(type);
    }

    public static class InstanceSpecification {

        public int instanceCores;
        public int ephemerals;
        public boolean clusterInstance;
        public boolean pvm;
        public boolean hvm;

        public InstanceSpecification(int cores, int ephemerals, boolean pvm, boolean hvm, boolean clusterInstance) {
            this.instanceCores = cores;
            this.ephemerals = ephemerals;
            this.clusterInstance = clusterInstance;
            this.pvm = pvm;
            this.hvm = hvm;
        }
    }
}
