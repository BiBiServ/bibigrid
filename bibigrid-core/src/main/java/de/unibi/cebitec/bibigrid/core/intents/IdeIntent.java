package de.unibi.cebitec.bibigrid.core.intents;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import de.unibi.cebitec.bibigrid.core.model.Client;
import de.unibi.cebitec.bibigrid.core.model.Cluster;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.util.JSchLogger;
import de.unibi.cebitec.bibigrid.core.util.SshFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Intent for starting and tunneling the cloud9 installation on a cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class IdeIntent extends Intent {
    private static final Logger LOG = LoggerFactory.getLogger(IdeIntent.class);
    private static final int PORT = 8181;

    private final ProviderModule providerModule;
    private final Client client;
    private final Configuration config;

    public IdeIntent(ProviderModule providerModule, Client client, Configuration config) {
        this.providerModule = providerModule;
        this.client = client;
        this.config = config;
    }

    public void start() {
        if (config.getId() == null) {
            LOG.error("ClusterId not found. Please provide a valid cluster id.");
            return;
        }
        String id = config.getId();
        final Map<String, Cluster> clusters = providerModule.getListIntent(client, config).getList();
        if (!clusters.containsKey(id)) {
            LOG.error("Cluster with id {} not found. Please provide a valid cluster id.", id);
            return;
        }

        String masterIp = config.isUseMasterWithPublicIp() ? clusters.get(id).getPublicIp() :
                clusters.get(id).getPrivateIp();

        boolean sshPortIsReady = SshFactory.pollSshPortIsAvailable(masterIp);
        if (!sshPortIsReady) {
            LOG.error("Failed to poll master ssh port.");
            return;
        }
        startPortForwarding(masterIp);
    }

    private void startPortForwarding(final String masterIp) {

        try {

            LOG.info("Trying to connect to master ...");
            sleep(4);
            // Create new Session to avoid packet corruption.
            Session sshSession = SshFactory.createSshSession(config, masterIp);
            if (sshSession != null) {
                sshSession.setPortForwardingL(PORT, "localhost", PORT);
                // Start connection attempt
                sshSession.connect();
                LOG.info("Connected to master!");
                LOG.info("You can now open the Web IDE at http://localhost:{}", PORT);
                openBrowser();
                LOG.info("Press any key, to close this session...");
                //noinspection ResultOfMethodCallIgnored
                System.in.read();
                sshSession.disconnect();
            }
        } catch (JSchException e) {
            LOG.error("Failed to start {} IDE on master.", config.isTheia() ? "Theia" : "Cloud9", e);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openBrowser() {
        try {
            Desktop.getDesktop().browse(new URL("http://localhost:" + PORT).toURI());
        } catch (Exception ignored) {
        }
    }
}
