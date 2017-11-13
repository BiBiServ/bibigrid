package de.unibi.cebitec.bibigrid.meta;

/**
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public interface TerminateIntent {

    /**
     * Terminate a cluster.
     *
     * @return Return true in case of success, false otherwise
     */
    boolean terminate();
}
