package de.unibi.cebitec.bibigrid.meta;

/**
 * @author Johanes Steiner
 */
public interface TerminateIntent {

    /**
     * Terminate a cluster.
     *
     * @return Return true in case of success, false otherwise
     */
    boolean terminate();
}
