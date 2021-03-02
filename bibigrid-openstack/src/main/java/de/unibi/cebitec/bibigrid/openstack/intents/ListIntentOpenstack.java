package de.unibi.cebitec.bibigrid.openstack.intents;

import de.unibi.cebitec.bibigrid.core.intents.ListIntent;

import java.util.*;

import de.unibi.cebitec.bibigrid.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements ListIntent for Openstack.
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class ListIntentOpenstack extends ListIntent {
    private static final Logger LOG = LoggerFactory.getLogger(ListIntentOpenstack.class);

    public ListIntentOpenstack(Map<String, Cluster> clusterMap) {
        super(clusterMap);
    }
}
