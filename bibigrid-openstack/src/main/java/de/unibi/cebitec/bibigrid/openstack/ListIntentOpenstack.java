package de.unibi.cebitec.bibigrid.openstack;

import de.unibi.cebitec.bibigrid.core.intents.CreateClusterEnvironment;
import de.unibi.cebitec.bibigrid.core.intents.ListIntent;

import java.util.*;
import java.util.stream.Collectors;

import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.InstanceTypeNotFoundException;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.*;
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

    ListIntentOpenstack(Map<String, Cluster> clusterMap) {
        super(clusterMap);
    }
}
