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
    public static final int DEFAULT_IDE_PORT = 8181;
    public static final int DEFAULT_IDE_PORT_END = 8383;

    private final ProviderModule providerModule;
    private final Client client;
    private final Configuration config;

    private int idePort = DEFAULT_IDE_PORT;
    private int idePortLast = DEFAULT_IDE_PORT_END;

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
        if (!config.isIDE()) {
            LOG.error("IDE not set in configuration.");
            return;
        }
        startPortForwarding(masterIp);
    }

    /**
     * To use the IDE in a browser locally, ports have to be forwarded to connect to the remote.
     * @param masterIp IP of the master instance
     */
    private void startPortForwarding(final String masterIp) {

        try {

            LOG.info("Trying to connect to master ...");
            sleep(4);
            // Create new Session to avoid packet corruption.
            Session sshSession = SshFactory.createSshSession(config, masterIp);
            if (sshSession != null) {
                this.idePort = config.getIdeConf().getPort_start();
                this.idePortLast = config.getIdeConf().getPort_end();
                while (!portAvailable(idePort)) {
                    if(idePort > idePortLast) {
                        LOG.warn("There is no free port available to forward to Theia IDE.");
                        break;
                    }
                    idePort++;
                }
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
            LOG.error("Failed to start Theia IDE on master.", e);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if IDE port (default 8181) is already binded.
     * @param port check if port is already listened on
     * @return true, if port available
     */
    private boolean portAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket()) {
            // serverSocket.setReuseAddress(false); required only on OSX
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName("localhost"), port), 1);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public int getIdePort() {
        return idePort;
    }

    public int getIdePortLast() {
        return idePortLast;
    }

    private void openBrowser() {
        try {
            Desktop.getDesktop().browse(new URL("http://localhost:" + idePort).toURI());
        } catch (Exception ignored) {
        }
    }
}
