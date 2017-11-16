package de.unibi.cebitec.bibigrid.aws;

import de.unibi.cebitec.bibigrid.model.InstanceSpecification;
import com.amazonaws.services.ec2.model.InstanceType;
import de.unibi.cebitec.bibigrid.model.exceptions.InstanceTypeNotFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the general InstanceType for a AWS based cluster.
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public class InstanceTypeAWS extends de.unibi.cebitec.bibigrid.model.InstanceType {
    private static final Map<InstanceType, InstanceSpecification> typeSpecMap = new HashMap<>();

    static {
        // General use
        typeSpecMap.put(InstanceType.M4Large, new InstanceSpecification(2, 0, false, false, true, false));
        typeSpecMap.put(InstanceType.M4Xlarge, new InstanceSpecification(4, 0, false, false, true, false));
        typeSpecMap.put(InstanceType.M42xlarge, new InstanceSpecification(8, 0, false, false, true, false));
        typeSpecMap.put(InstanceType.M44xlarge, new InstanceSpecification(16, 0, false, false, true, false));
        typeSpecMap.put(InstanceType.M410xlarge, new InstanceSpecification(40, 0, false, false, true, false));

        typeSpecMap.put(InstanceType.M3Medium, new InstanceSpecification(1, 1, false, true, true, false));
        typeSpecMap.put(InstanceType.M3Large, new InstanceSpecification(2, 1, false, true, true, false));
        typeSpecMap.put(InstanceType.M3Xlarge, new InstanceSpecification(4, 2, false, true, true, false));
        typeSpecMap.put(InstanceType.M32xlarge, new InstanceSpecification(8, 2, false, true, true, false));

        typeSpecMap.put(InstanceType.M1Small, new InstanceSpecification(1, 1, false, true, false, false));
        typeSpecMap.put(InstanceType.M1Medium, new InstanceSpecification(1, 1, false, true, false, false));
        typeSpecMap.put(InstanceType.M1Large, new InstanceSpecification(2, 2, false, true, false, false));
        typeSpecMap.put(InstanceType.M1Xlarge, new InstanceSpecification(4, 4, false, true, false, false));

        //compute optimized
        typeSpecMap.put(InstanceType.C4Large, new InstanceSpecification(2, 0, false, false, true, true));
        typeSpecMap.put(InstanceType.C4Xlarge, new InstanceSpecification(4, 0, false, false, true, true));
        typeSpecMap.put(InstanceType.C42xlarge, new InstanceSpecification(8, 0, false, false, true, true));
        typeSpecMap.put(InstanceType.C44xlarge, new InstanceSpecification(16, 0, false, false, true, true));
        typeSpecMap.put(InstanceType.C48xlarge, new InstanceSpecification(36, 0, false, false, true, true));

        typeSpecMap.put(InstanceType.C3Large, new InstanceSpecification(2, 2, false, true, true, true));
        typeSpecMap.put(InstanceType.C3Xlarge, new InstanceSpecification(4, 2, false, true, true, true));
        typeSpecMap.put(InstanceType.C32xlarge, new InstanceSpecification(8, 2, false, true, true, true));
        typeSpecMap.put(InstanceType.C34xlarge, new InstanceSpecification(16, 2, false, true, true, true));
        typeSpecMap.put(InstanceType.C38xlarge, new InstanceSpecification(32, 2, false, true, true, true));

        typeSpecMap.put(InstanceType.C1Medium, new InstanceSpecification(2, 1, false, true, false, false));
        typeSpecMap.put(InstanceType.C1Xlarge, new InstanceSpecification(8, 4, false, true, false, false));
        typeSpecMap.put(InstanceType.Cc28xlarge, new InstanceSpecification(32, 4, false, false, true, true));

        // GPU 
        typeSpecMap.put(InstanceType.G22xlarge, new InstanceSpecification(8, 1, false, false, true, true));
        typeSpecMap.put(InstanceType.Cg14xlarge, new InstanceSpecification(16, 2, false, false, true, true));

        // Memory optimized
        typeSpecMap.put(InstanceType.M2Xlarge, new InstanceSpecification(2, 1, false, true, false, false));
        typeSpecMap.put(InstanceType.M22xlarge, new InstanceSpecification(4, 1, false, true, false, false));
        typeSpecMap.put(InstanceType.M24xlarge, new InstanceSpecification(8, 2, false, true, false, false));
        typeSpecMap.put(InstanceType.Cr18xlarge, new InstanceSpecification(32, 2, false, false, true, true));

        typeSpecMap.put(InstanceType.R3Large, new InstanceSpecification(2, 1, false, false, true, true));
        typeSpecMap.put(InstanceType.R3Xlarge, new InstanceSpecification(4, 1, false, false, true, true));
        typeSpecMap.put(InstanceType.R32xlarge, new InstanceSpecification(8, 1, false, false, true, true));
        typeSpecMap.put(InstanceType.R34xlarge, new InstanceSpecification(16, 1, false, false, true, true));
        typeSpecMap.put(InstanceType.R38xlarge, new InstanceSpecification(32, 2, false, false, true, true));

        // Storage optimized
        typeSpecMap.put(InstanceType.D2Xlarge, new InstanceSpecification(4, 3, false, false, true, true));
        typeSpecMap.put(InstanceType.D22xlarge, new InstanceSpecification(8, 6, false, false, true, true));
        typeSpecMap.put(InstanceType.D24xlarge, new InstanceSpecification(16, 12, false, false, true, true));
        typeSpecMap.put(InstanceType.D28xlarge, new InstanceSpecification(36, 24, false, false, true, true));

        typeSpecMap.put(InstanceType.Hi14xlarge, new InstanceSpecification(16, 2, false, true, true, true));
        typeSpecMap.put(InstanceType.Hs18xlarge, new InstanceSpecification(16, 24, false, true, true, true));

        // I2 Instances
        typeSpecMap.put(InstanceType.I2Xlarge, new InstanceSpecification(4, 1, false, false, true, true));
        typeSpecMap.put(InstanceType.I22xlarge, new InstanceSpecification(8, 2, false, false, true, true));
        typeSpecMap.put(InstanceType.I24xlarge, new InstanceSpecification(16, 4, false, false, true, true));
        typeSpecMap.put(InstanceType.I28xlarge, new InstanceSpecification(32, 8, false, false, true, true));

        // t1,t2
        typeSpecMap.put(InstanceType.T1Micro, new InstanceSpecification(1, 0, false, true, false, false));
        typeSpecMap.put(InstanceType.T2Micro, new InstanceSpecification(1, 0, false, false, true, false));
        typeSpecMap.put(InstanceType.T2Small, new InstanceSpecification(1, 0, false, false, true, false));
        typeSpecMap.put(InstanceType.T2Medium, new InstanceSpecification(2, 0, false, false, true, false));
        typeSpecMap.put(InstanceType.T2Large, new InstanceSpecification(2, 0, false, false, true, false));
    }

    public InstanceTypeAWS(String type) throws InstanceTypeNotFoundException {
        try {
            InstanceType tmp = InstanceType.fromValue(type);
            value = tmp.toString();
            spec = typeSpecMap.get(tmp);
        } catch (Exception e) {
            throw new InstanceTypeNotFoundException("Invalid instance type " + type);
        }
    }
}
