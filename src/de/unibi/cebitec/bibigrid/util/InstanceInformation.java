package de.unibi.cebitec.bibigrid.util;

import com.amazonaws.services.ec2.model.InstanceType;
import java.util.HashMap;
import java.util.Map;

public class InstanceInformation {

    private static final Map<InstanceType, InstanceSpecification> count = new HashMap<>();

    static {
        
        count.put(InstanceType.T1Micro, new InstanceSpecification(1,0));
        count.put(InstanceType.M1Small, new InstanceSpecification(1,1));
        count.put(InstanceType.M1Medium, new InstanceSpecification(1,1));
        count.put(InstanceType.M1Large, new InstanceSpecification(2,2));
        count.put(InstanceType.M1Xlarge, new InstanceSpecification(4,4));
        count.put(InstanceType.M3Xlarge, new InstanceSpecification(4,0));
        count.put(InstanceType.M32xlarge, new InstanceSpecification(8,0));
        count.put(InstanceType.M2Xlarge, new InstanceSpecification(2,1));
        count.put(InstanceType.M22xlarge, new InstanceSpecification(4,1));
        count.put(InstanceType.M24xlarge, new InstanceSpecification(8,2));
        count.put(InstanceType.C1Medium, new InstanceSpecification(2,1));
        count.put(InstanceType.C1Xlarge, new InstanceSpecification(8,4));
        count.put(InstanceType.Cc14xlarge, new InstanceSpecification(8,2));
        count.put(InstanceType.Cc28xlarge, new InstanceSpecification(16,4));
        count.put(InstanceType.Cg14xlarge, new InstanceSpecification(16,2));
        count.put(InstanceType.Hi14xlarge, new InstanceSpecification(16,2));
        
        
    }

    private InstanceInformation() {
    }

    public static InstanceSpecification getSpecs(InstanceType type) {
        return count.get(type);
    }
    
    public static class InstanceSpecification {
        
        public int instanceCores;
        public int ephemerals;
        public InstanceSpecification(int cores, int ephemerals) {
           this.instanceCores = cores;
           this.ephemerals = ephemerals;
        }
    }
}
