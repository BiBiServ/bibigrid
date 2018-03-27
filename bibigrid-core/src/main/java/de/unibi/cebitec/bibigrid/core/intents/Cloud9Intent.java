package de.unibi.cebitec.bibigrid.core.intents;

import com.jcraft.jsch.ChannelExec;
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
import java.nio.file.FileSystems;
import java.util.Map;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

/**
 * Intent for starting and tunneling the cloud9 installation on a cluster.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class Cloud9Intent extends Intent {
    private static final Logger LOG = LoggerFactory.getLogger(Cloud9Intent.class);
    private static final int PORT = 8181;

    private final ProviderModule providerModule;
    private final Client client;
    private final Configuration config;

    public Cloud9Intent(ProviderModule providerModule, Client client, Configuration config) {
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
        startCloud9(masterIp);
    }

    private void startCloud9(final String masterIp) {
        JSch ssh = new JSch();
        JSch.setLogger(new JSchLogger());
        try {
            ssh.addIdentity(config.getSshPrivateKeyFile());
            LOG.info("Trying to connect to master...");
            sleep(4);
            // Create new Session to avoid packet corruption.
            Session sshSession = SshFactory.createNewSshSession(ssh, masterIp, config.getSshUser(),
                    FileSystems.getDefault().getPath(config.getSshPrivateKeyFile()));
            if (sshSession != null) {
                sshSession.setPortForwardingL(PORT, "localhost", PORT);
                // Start connection attempt
                sshSession.connect();
                LOG.info("Connected to master!");
                ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");
                channel.setCommand("cloud9 --listen localhost -w ~/");
                LOG.info(V, "Connecting ssh channel...");
                channel.connect();
                LOG.info("You can now open the cloud9 IDE at http://localhost:{}", PORT);
                openBrowser();
                LOG.info("Press any key, to terminate this session...");
                //noinspection ResultOfMethodCallIgnored
                System.in.read();
                // TODO: kill process?
                channel.disconnect();
                sshSession.disconnect();
            }
        } catch (JSchException e) {
            LOG.error("Failed to start cloud9 on master. {}", e);
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
