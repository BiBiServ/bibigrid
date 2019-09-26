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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
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
    private static final int DEFAULT_IDE_PORT = 8181;

    private final ProviderModule providerModule;
    private final Client client;
    private final Configuration config;
    private int idePort;

    public IdeIntent(ProviderModule providerModule, Client client, Configuration config) {
        this.providerModule = providerModule;
        this.client = client;
        this.config = config;
    }

    public void start() {
        if (config.getClusterIds().length == 0) {
            LOG.error("ClusterId not found. Please provide a valid cluster id.");
            return;
        }
        String clusterId = config.getClusterIds()[0];
        final Map<String, Cluster> clusters = providerModule.getListIntent(client, config).getList();
        if (!clusters.containsKey(clusterId)) {
            LOG.error("Cluster with id {} not found. Please provide a valid cluster id.", clusterId);
            return;
        }
        String masterIp = clusters.get(clusterId).getPublicIp();
        boolean sshPortIsReady = SshFactory.pollSshPortIsAvailable(masterIp);
        if (!sshPortIsReady) {
            LOG.error("Failed to poll master ssh port.");
            return;
        }
        startPortForwarding(masterIp);
    }

    private void startPortForwarding(final String masterIp) {
        JSch ssh = new JSch();
        JSch.setLogger(new JSchLogger());
        try {
            ssh.addIdentity(config.getSshPrivateKeyFile());
            LOG.info("Trying to connect to master ...");
            sleep(4);
            // Create new Session to avoid packet corruption.
            Session sshSession = SshFactory.createNewSshSession(ssh, masterIp, config.getSshUser(),
                    Paths.get(config.getSshPrivateKeyFile()));
            if (sshSession != null) {
                this.setIDEPort(DEFAULT_IDE_PORT);
                sshSession.setPortForwardingL(idePort, "localhost", DEFAULT_IDE_PORT);
                // Start connection attempt
                sshSession.connect();
                LOG.info("Connected to master!");
                LOG.info("You can now open the Web IDE at http://localhost:{}", idePort);
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

    /**
     * Checks if IDE port (default 8181) is already binded.
     * Uses next port (+1) if available
     * @param port check if port is already listened on
     */
    private void setIDEPort(int port) {
        try (ServerSocket serverSocket = new ServerSocket()) {
            // serverSocket.setReuseAddress(false); required only on OSX
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), port), 1);
            this.idePort = port;
        } catch (Exception ex) {
            setIDEPort(++port);
        }
    }

    private void openBrowser() {
        try {
            Desktop.getDesktop().browse(new URL("http://localhost:" + idePort).toURI());
        } catch (Exception ignored) {
        }
    }
}
