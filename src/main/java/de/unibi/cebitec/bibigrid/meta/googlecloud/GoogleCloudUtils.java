package de.unibi.cebitec.bibigrid.meta.googlecloud;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.compute.Compute;
import com.google.cloud.compute.ComputeOptions;
import de.unibi.cebitec.bibigrid.model.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Utility methods for the google cloud.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
final class GoogleCloudUtils {
    static Compute getComputeService(final Configuration conf) {
        ComputeOptions.Builder optionsBuilder = ComputeOptions.newBuilder();
        optionsBuilder.setProjectId(conf.getGoogleProjectId());
        try {
            optionsBuilder.setCredentials(GoogleCredentials.fromStream(
                    new FileInputStream(conf.getGoogleCredentialsFile())));
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: proper error message
            return null;
        }
        return optionsBuilder.build().getService();
    }
}
