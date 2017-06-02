package de.unibi.cebitec.bibigrid.meta.googlecloud;

import com.google.cloud.compute.Compute;
import de.unibi.cebitec.bibigrid.meta.ValidateIntent;
import de.unibi.cebitec.bibigrid.model.Configuration;

/**
 * Implementation of the general ValidateIntent interface for a Google based cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class ValidateIntentGoogleCloud implements ValidateIntent {
    private final Compute compute;

    public ValidateIntentGoogleCloud(final Configuration conf) {
        compute = GoogleCloudUtils.getComputeService(conf);
        // TODO: stub
    }

    public boolean validate() {
        // TODO: stub
        return false;
    }
}