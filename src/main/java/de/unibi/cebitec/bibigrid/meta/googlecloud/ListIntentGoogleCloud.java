package de.unibi.cebitec.bibigrid.meta.googlecloud;

import de.unibi.cebitec.bibigrid.meta.ListIntent;
import de.unibi.cebitec.bibigrid.model.Cluster;
import de.unibi.cebitec.bibigrid.model.Configuration;

import java.util.Map;

/**
 * Implementation of the general ListIntent interface for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class ListIntentGoogleCloud implements ListIntent {
    public ListIntentGoogleCloud(final Configuration conf) {
        // TODO: stub
    }

    public Map<String, Cluster> getList() {
        // TODO: stub
        return null;
    }

    @Override
    public String toString() {
        // TODO: stub
        return null;
    }
}