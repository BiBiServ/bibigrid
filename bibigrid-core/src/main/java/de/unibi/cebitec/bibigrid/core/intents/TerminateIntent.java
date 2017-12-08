package de.unibi.cebitec.bibigrid.core.intents;

/**
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 */
public interface TerminateIntent extends Intent {
    /**
     * Terminate a cluster.
     *
     * @return Return true in case of success, false otherwise
     */
    boolean terminate();
}
