package de.unibi.cebitec.bibigrid.core.model;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public abstract class InstanceImage {
    public abstract long getMinDiskSpace();
    public abstract int getMinRam();
    public abstract String getId();
    public abstract String getName();
}
