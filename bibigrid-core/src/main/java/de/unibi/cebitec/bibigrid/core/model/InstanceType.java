package de.unibi.cebitec.bibigrid.core.model;

/**
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public abstract class InstanceType {
    protected String value;
    protected int instanceCores;
    protected int ephemerals;
    protected boolean swap;
    protected boolean clusterInstance;
    protected boolean pvm;
    protected boolean hvm;

    public String getValue() {
        return value;
    }

    public int getInstanceCores() {
        return instanceCores;
    }

    public int getEphemerals() {
        return ephemerals;
    }

    public int getSwap() {
        return swap ? 1 : 0;
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

    @Override
    public String toString() {
        return getValue();
    }
}
