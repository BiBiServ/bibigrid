package de.unibi.cebitec.bibigrid.core.model;

public class InstanceSpecification {
    private final int instanceCores;
    private final int ephemerals;
    private final int swap;
    private final boolean clusterInstance;
    private final boolean pvm;
    private final boolean hvm;

    public InstanceSpecification(int cores, int ephemerals, boolean swap, boolean pvm, boolean hvm, boolean clusterInstance) {
        this.instanceCores = cores;
        this.ephemerals = ephemerals;
        this.clusterInstance = clusterInstance;
        this.pvm = pvm;
        this.hvm = hvm;
        this.swap = swap ? 1 : 0;
    }

    public int getInstanceCores() {
        return instanceCores;
    }

    public int getEphemerals() {
        return ephemerals;
    }

    public int getSwap() {
        return swap;
    }

    public boolean isClusterInstance() {
        return clusterInstance;
    }

    public boolean isPvm() {
        return pvm;
    }

    public boolean isHvm() {
        return hvm;
    }
}
