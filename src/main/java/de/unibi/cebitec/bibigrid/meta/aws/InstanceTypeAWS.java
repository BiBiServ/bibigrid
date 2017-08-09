/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unibi.cebitec.bibigrid.meta.aws;

import de.unibi.cebitec.bibigrid.model.InstanceType;
import de.unibi.cebitec.bibigrid.util.InstanceInformation;
import de.unibi.cebitec.bibigrid.util.InstanceInformation.InstanceSpecification;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jsteiner
 */
public class InstanceTypeAWS extends InstanceType {

    private static final Map<com.amazonaws.services.ec2.model.InstanceType, InstanceSpecification> count = new HashMap<>();

    static {
        // General use

        count.put(com.amazonaws.services.ec2.model.InstanceType.M4Large, new InstanceInformation.InstanceSpecification(2, 0,false, false, true, false));
        count.put(com.amazonaws.services.ec2.model.InstanceType.M4Xlarge, new InstanceInformation.InstanceSpecification(4, 0,false, false, true, false));
        count.put(com.amazonaws.services.ec2.model.InstanceType.M42xlarge, new InstanceInformation.InstanceSpecification(8, 0,false, false, true, false));
        count.put(com.amazonaws.services.ec2.model.InstanceType.M44xlarge, new InstanceInformation.InstanceSpecification(16, 0,false, false, true, false));
        count.put(com.amazonaws.services.ec2.model.InstanceType.M410xlarge, new InstanceInformation.InstanceSpecification(40, 0,false, false, true, false));

        count.put(com.amazonaws.services.ec2.model.InstanceType.M3Medium, new InstanceInformation.InstanceSpecification(1, 1,false, true, true, false));
        count.put(com.amazonaws.services.ec2.model.InstanceType.M3Large, new InstanceInformation.InstanceSpecification(2, 1, false,true, true, false));
        count.put(com.amazonaws.services.ec2.model.InstanceType.M3Xlarge, new InstanceInformation.InstanceSpecification(4, 2,false, true, true, false));
        count.put(com.amazonaws.services.ec2.model.InstanceType.M32xlarge, new InstanceInformation.InstanceSpecification(8, 2,false, true, true, false));

        count.put(com.amazonaws.services.ec2.model.InstanceType.M1Small, new InstanceInformation.InstanceSpecification(1, 1,false, true, false, false));
        count.put(com.amazonaws.services.ec2.model.InstanceType.M1Medium, new InstanceInformation.InstanceSpecification(1, 1,false, true, false, false));
        count.put(com.amazonaws.services.ec2.model.InstanceType.M1Large, new InstanceInformation.InstanceSpecification(2, 2,false, true, false, false));
        count.put(com.amazonaws.services.ec2.model.InstanceType.M1Xlarge, new InstanceInformation.InstanceSpecification(4, 4,false, true, false, false));

        //compute optimized
        count.put(com.amazonaws.services.ec2.model.InstanceType.C4Large, new InstanceInformation.InstanceSpecification(2, 0,false, false, true, true));
        count.put(com.amazonaws.services.ec2.model.InstanceType.C4Xlarge, new InstanceInformation.InstanceSpecification(4, 0,false, false, true, true));
        count.put(com.amazonaws.services.ec2.model.InstanceType.C42xlarge, new InstanceInformation.InstanceSpecification(8, 0,false, false, true, true));
        count.put(com.amazonaws.services.ec2.model.InstanceType.C44xlarge, new InstanceInformation.InstanceSpecification(16, 0,false, false, true, true));
        count.put(com.amazonaws.services.ec2.model.InstanceType.C48xlarge, new InstanceInformation.InstanceSpecification(36, 0,false, false, true, true));

        count.put(com.amazonaws.services.ec2.model.InstanceType.C3Large, new InstanceInformation.InstanceSpecification(2, 2,false, true, true, true));
        count.put(com.amazonaws.services.ec2.model.InstanceType.C3Xlarge, new InstanceInformation.InstanceSpecification(4, 2,false, true, true, true));
        count.put(com.amazonaws.services.ec2.model.InstanceType.C32xlarge, new InstanceInformation.InstanceSpecification(8, 2,false, true, true, true));
        count.put(com.amazonaws.services.ec2.model.InstanceType.C34xlarge, new InstanceInformation.InstanceSpecification(16, 2,false, true, true, true));
        count.put(com.amazonaws.services.ec2.model.InstanceType.C38xlarge, new InstanceInformation.InstanceSpecification(32, 2,false, true, true, true));

        count.put(com.amazonaws.services.ec2.model.InstanceType.C1Medium, new InstanceInformation.InstanceSpecification(2, 1,false, true, false, false));
        count.put(com.amazonaws.services.ec2.model.InstanceType.C1Xlarge, new InstanceInformation.InstanceSpecification(8, 4,false, true, false, false));
        count.put(com.amazonaws.services.ec2.model.InstanceType.Cc28xlarge, new InstanceInformation.InstanceSpecification(32, 4,false, false, true, true));

        // GPU 
        count.put(com.amazonaws.services.ec2.model.InstanceType.G22xlarge, new InstanceInformation.InstanceSpecification(8, 1,false, false, true, true));
        count.put(com.amazonaws.services.ec2.model.InstanceType.Cg14xlarge, new InstanceInformation.InstanceSpecification(16, 2,false, false, true, true));

        // Memory optimized
        count.put(com.amazonaws.services.ec2.model.InstanceType.M2Xlarge, new InstanceInformation.InstanceSpecification(2, 1,false, true, false, false));
        count.put(com.amazonaws.services.ec2.model.InstanceType.M22xlarge, new InstanceInformation.InstanceSpecification(4, 1,false, true, false, false));
        count.put(com.amazonaws.services.ec2.model.InstanceType.M24xlarge, new InstanceInformation.InstanceSpecification(8, 2,false, true, false, false));
        count.put(com.amazonaws.services.ec2.model.InstanceType.Cr18xlarge, new InstanceInformation.InstanceSpecification(32, 2,false, false, true, true));

        count.put(com.amazonaws.services.ec2.model.InstanceType.R3Large, new InstanceInformation.InstanceSpecification(2, 1,false, false, true, true));
        count.put(com.amazonaws.services.ec2.model.InstanceType.R3Xlarge, new InstanceInformation.InstanceSpecification(4, 1,false, false, true, true));
        count.put(com.amazonaws.services.ec2.model.InstanceType.R32xlarge, new InstanceInformation.InstanceSpecification(8, 1, false,false, true, true));
        count.put(com.amazonaws.services.ec2.model.InstanceType.R34xlarge, new InstanceInformation.InstanceSpecification(16, 1, false,false, true, true));
        count.put(com.amazonaws.services.ec2.model.InstanceType.R38xlarge, new InstanceInformation.InstanceSpecification(32, 2, false,false, true, true));

        // Storage optimized
        count.put(com.amazonaws.services.ec2.model.InstanceType.D2Xlarge, new InstanceInformation.InstanceSpecification(4, 3, false,false, true, true));
        count.put(com.amazonaws.services.ec2.model.InstanceType.D22xlarge, new InstanceInformation.InstanceSpecification(8, 6, false,false, true, true));
        count.put(com.amazonaws.services.ec2.model.InstanceType.D24xlarge, new InstanceInformation.InstanceSpecification(16, 12,false, false, true, true));
        count.put(com.amazonaws.services.ec2.model.InstanceType.D28xlarge, new InstanceInformation.InstanceSpecification(36, 24,false, false, true, true));

        count.put(com.amazonaws.services.ec2.model.InstanceType.Hi14xlarge, new InstanceInformation.InstanceSpecification(16, 2,false, true, true, true));
        count.put(com.amazonaws.services.ec2.model.InstanceType.Hs18xlarge, new InstanceInformation.InstanceSpecification(16, 24,false, true, true, true));

        // I2 Instances
        count.put(com.amazonaws.services.ec2.model.InstanceType.I2Xlarge, new InstanceInformation.InstanceSpecification(4, 1, false,false, true, true));
        count.put(com.amazonaws.services.ec2.model.InstanceType.I22xlarge, new InstanceInformation.InstanceSpecification(8, 2, false,false, true, true));
        count.put(com.amazonaws.services.ec2.model.InstanceType.I24xlarge, new InstanceInformation.InstanceSpecification(16, 4,false, false, true, true));
        count.put(com.amazonaws.services.ec2.model.InstanceType.I28xlarge, new InstanceInformation.InstanceSpecification(32, 8,false, false, true, true));

        // t1,t2
        count.put(com.amazonaws.services.ec2.model.InstanceType.T1Micro, new InstanceInformation.InstanceSpecification(1, 0,false, true, false, false));
        count.put(com.amazonaws.services.ec2.model.InstanceType.T2Micro, new InstanceInformation.InstanceSpecification(1, 0,false, false, true, false));
        count.put(com.amazonaws.services.ec2.model.InstanceType.T2Small, new InstanceInformation.InstanceSpecification(1, 0,false, false, true, false));
        count.put(com.amazonaws.services.ec2.model.InstanceType.T2Medium, new InstanceInformation.InstanceSpecification(2, 0,false, false, true, false));
        count.put(com.amazonaws.services.ec2.model.InstanceType.T2Large, new InstanceInformation.InstanceSpecification(2, 0,false, false, true, false));
    }

    public InstanceTypeAWS(String type) throws Exception {
        try {
            com.amazonaws.services.ec2.model.InstanceType tmp =  com.amazonaws.services.ec2.model.InstanceType.fromValue(type);
            value = tmp.toString();
            spec = count.get(tmp);
        } catch (Exception e) {
            throw new Exception("Invalid instance type");
        }
    }

}
