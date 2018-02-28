package de.unibi.cebitec.bibigrid.core.model;

/**
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public abstract class InstanceType {
    protected String value;
    protected int cpuCores;
    protected int ephemerals;
    protected boolean swap;
    protected boolean configDrive;
    protected boolean clusterInstance;
    protected boolean pvm;
    protected boolean hvm;
    protected int maxRam;
    protected long maxDiskSpace;

    public String getValue() {
        return value;
    }

    public int getCpuCores() {
        return cpuCores;
    }

    public int getEphemerals() {
        return ephemerals;
    }

    public int getSwap() {
        return swap ? 1 : 0;
    }

    /**
     * The configuration drive is used to store instance-specific metadata and is present to the instance
     * as a disk partition.
     */
    public int getConfigDrive() {
        return configDrive ? 1 : 0;
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

    public int getMaxRam() {
        return maxRam;
    }

    public long getMaxDiskSpace() {
        return maxDiskSpace;
    }

    @Override
    public String toString() {
        return getValue();
    }
}
