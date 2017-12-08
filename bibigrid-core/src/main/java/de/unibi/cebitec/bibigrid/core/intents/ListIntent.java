package de.unibi.cebitec.bibigrid.core.intents;

import de.unibi.cebitec.bibigrid.core.model.Cluster;

import java.util.Map;

/**
 * Creates a Map of BiBiGrid cluster instances
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public interface ListIntent extends Intent {
    /**
     * Return a Map of Cluster objects within current configuration.
     */
    Map<String, Cluster> getList();

    /**
     * Return a String representation of found cluster objects map.
     */
    @Override
    String toString();
}
