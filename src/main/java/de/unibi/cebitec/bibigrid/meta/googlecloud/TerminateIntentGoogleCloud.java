package de.unibi.cebitec.bibigrid.meta.googlecloud;

import com.google.cloud.compute.Compute;
import de.unibi.cebitec.bibigrid.meta.TerminateIntent;
import de.unibi.cebitec.bibigrid.model.Configuration;

/**
 * Implementation of the general TerminateIntent interface for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class TerminateIntentGoogleCloud implements TerminateIntent {
    private final Compute compute;

    public TerminateIntentGoogleCloud(final Configuration conf) {
        compute = GoogleCloudUtils.getComputeService(conf);
        // TODO: stub
    }

    public boolean terminate() {
        // TODO: stub
        return false;
    }
}