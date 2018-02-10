package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.services.ec2.model.InstanceType;

import java.util.Arrays;
import java.util.List;

/**
 * Implementation of the general InstanceType for a AWS based cluster.
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
class InstanceTypeAWS extends de.unibi.cebitec.bibigrid.core.model.InstanceType {
    static List<InstanceTypeAWS> getStaticInstanceTypeList() {
        return Arrays.asList(
                // General use
                new InstanceTypeAWS(InstanceType.M4Large, 2, 0, false, true, false),
                new InstanceTypeAWS(InstanceType.M4Xlarge, 4, 0, false, true, false),
                new InstanceTypeAWS(InstanceType.M42xlarge, 8, 0, false, true, false),
                new InstanceTypeAWS(InstanceType.M44xlarge, 16, 0, false, true, false),
                new InstanceTypeAWS(InstanceType.M410xlarge, 40, 0, false, true, false),

                new InstanceTypeAWS(InstanceType.M3Medium, 1, 1, true, true, false),
                new InstanceTypeAWS(InstanceType.M3Large, 2, 1, true, true, false),
                new InstanceTypeAWS(InstanceType.M3Xlarge, 4, 2, true, true, false),
                new InstanceTypeAWS(InstanceType.M32xlarge, 8, 2, true, true, false),

                new InstanceTypeAWS(InstanceType.M1Small, 1, 1, true, false, false),
                new InstanceTypeAWS(InstanceType.M1Medium, 1, 1, true, false, false),
                new InstanceTypeAWS(InstanceType.M1Large, 2, 2, true, false, false),
                new InstanceTypeAWS(InstanceType.M1Xlarge, 4, 4, true, false, false),

                //compute optimized
                new InstanceTypeAWS(InstanceType.C4Large, 2, 0, false, true, true),
                new InstanceTypeAWS(InstanceType.C4Xlarge, 4, 0, false, true, true),
                new InstanceTypeAWS(InstanceType.C42xlarge, 8, 0, false, true, true),
                new InstanceTypeAWS(InstanceType.C44xlarge, 16, 0, false, true, true),
                new InstanceTypeAWS(InstanceType.C48xlarge, 36, 0, false, true, true),

                new InstanceTypeAWS(InstanceType.C3Large, 2, 2, true, true, true),
                new InstanceTypeAWS(InstanceType.C3Xlarge, 4, 2, true, true, true),
                new InstanceTypeAWS(InstanceType.C32xlarge, 8, 2, true, true, true),
                new InstanceTypeAWS(InstanceType.C34xlarge, 16, 2, true, true, true),
                new InstanceTypeAWS(InstanceType.C38xlarge, 32, 2, true, true, true),

                new InstanceTypeAWS(InstanceType.C1Medium, 2, 1, true, false, false),
                new InstanceTypeAWS(InstanceType.C1Xlarge, 8, 4, true, false, false),
                new InstanceTypeAWS(InstanceType.Cc28xlarge, 32, 4, false, true, true),

                // GPU
                new InstanceTypeAWS(InstanceType.G22xlarge, 8, 1, false, true, true),
                new InstanceTypeAWS(InstanceType.Cg14xlarge, 16, 2, false, true, true),

                // Memory optimized
                new InstanceTypeAWS(InstanceType.M2Xlarge, 2, 1, true, false, false),
                new InstanceTypeAWS(InstanceType.M22xlarge, 4, 1, true, false, false),
                new InstanceTypeAWS(InstanceType.M24xlarge, 8, 2, true, false, false),
                new InstanceTypeAWS(InstanceType.Cr18xlarge, 32, 2, false, true, true),

                new InstanceTypeAWS(InstanceType.R3Large, 2, 1, false, true, true),
                new InstanceTypeAWS(InstanceType.R3Xlarge, 4, 1, false, true, true),
                new InstanceTypeAWS(InstanceType.R32xlarge, 8, 1, false, true, true),
                new InstanceTypeAWS(InstanceType.R34xlarge, 16, 1, false, true, true),
                new InstanceTypeAWS(InstanceType.R38xlarge, 32, 2, false, true, true),

                // Storage optimized
                new InstanceTypeAWS(InstanceType.D2Xlarge, 4, 3, false, true, true),
                new InstanceTypeAWS(InstanceType.D22xlarge, 8, 6, false, true, true),
                new InstanceTypeAWS(InstanceType.D24xlarge, 16, 12, false, true, true),
                new InstanceTypeAWS(InstanceType.D28xlarge, 36, 24, false, true, true),

                new InstanceTypeAWS(InstanceType.Hi14xlarge, 16, 2, true, true, true),
                new InstanceTypeAWS(InstanceType.Hs18xlarge, 16, 24, true, true, true),

                // I2 Instances
                new InstanceTypeAWS(InstanceType.I2Xlarge, 4, 1, false, true, true),
                new InstanceTypeAWS(InstanceType.I22xlarge, 8, 2, false, true, true),
                new InstanceTypeAWS(InstanceType.I24xlarge, 16, 4, false, true, true),
                new InstanceTypeAWS(InstanceType.I28xlarge, 32, 8, false, true, true),

                // t1,t2
                new InstanceTypeAWS(InstanceType.T1Micro, 1, 0, true, false, false),
                new InstanceTypeAWS(InstanceType.T2Micro, 1, 0, false, true, false),
                new InstanceTypeAWS(InstanceType.T2Small, 1, 0, false, true, false),
                new InstanceTypeAWS(InstanceType.T2Medium, 2, 0, false, true, false),
                new InstanceTypeAWS(InstanceType.T2Large, 2, 0, false, true, false)
        );
    }

    private InstanceTypeAWS(InstanceType type, int cores, int ephemerals, boolean pvm, boolean hvm,
                            boolean clusterInstance) {
        value = type.toString();
        this.cpuCores = cores;
        this.ephemerals = ephemerals;
        this.clusterInstance = clusterInstance;
        this.pvm = pvm;
        this.hvm = hvm;
        this.swap = false;
        maxRam = 0; // TODO
        maxDiskSpace = 0; // TODO
    }
}
