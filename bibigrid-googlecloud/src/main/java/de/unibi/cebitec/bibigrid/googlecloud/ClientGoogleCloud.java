package de.unibi.cebitec.bibigrid.googlecloud;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.ComputeScopes;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.util.Collections;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
class ClientGoogleCloud extends Client {
    private static final Logger LOG = LoggerFactory.getLogger(ClientGoogleCloud.class);

    private Compute internalClient;

    ClientGoogleCloud(ConfigurationGoogleCloud config) throws ClientConnectionFailedException {
        try {
            if (config.isDebugRequests()) {
                HttpRequestLogHandler.attachToCloudHttpTransport();
            }
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(config.getGoogleCredentialsFile()));
            if (credential.createScopedRequired()) {
                credential = credential.createScoped(Collections.singletonList(ComputeScopes.COMPUTE));
            }
            internalClient = new Compute.Builder(httpTransport, JacksonFactory.getDefaultInstance(), credential)
                    .setApplicationName(config.getGoogleProjectId()).build();
            LOG.info("Google compute connection established.");
        } catch (Exception e) {
            throw new ClientConnectionFailedException("Failed to connect google compute client.", e);
        }
    }

    public Compute getInternal() {
        return internalClient;
    }
}
