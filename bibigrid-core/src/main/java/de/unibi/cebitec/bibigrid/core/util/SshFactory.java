package de.unibi.cebitec.bibigrid.core.util;

import com.jcraft.jsch.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Locale;
import java.util.Scanner;


import de.unibi.cebitec.bibigrid.core.intents.CreateCluster;
import de.unibi.cebitec.bibigrid.core.model.Configuration;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

public class SshFactory {
    private static final Logger LOG = LoggerFactory.getLogger(SshFactory.class);
    private static final int SSH_POLL_ATTEMPTS = 25;

    public static Session createSshSession(String sshUser, Configuration.ClusterKeyPair ckp, String ip) throws JSchException{
        JSch jssh = new JSch();
        JSch.setLogger(new JSchLogger());
        Session sshSession = jssh.getSession(sshUser, ip, 22);
        // config.getSshUser()
        //Configuration.ClusterKeyPair ckp = config.getClusterKeyPair();
        jssh.addIdentity(ckp.getName(),ckp.getPrivateKey().getBytes(),ckp.getPublicKey().getBytes(),null);
        UserInfo userInfo = getConsolePasswordUserInfo();
        sshSession.setUserInfo(userInfo);
        return sshSession;
    }

    private static UserInfo getConsolePasswordUserInfo() {
        return new UserInfo() {
            @Override
            public String getPassphrase() {
                String passphrase;
                try {
                    Console console = System.console();
                    passphrase = new String(console.readPassword("Enter passphrase : "));
                } catch (NullPointerException e) {
                    System.err.println("Attention! Your input is not hidden. Console access is not possible, " +
                            "use Scanner class instead ... do you run within an IDE?");
                    Scanner scanner = new Scanner(System.in);
                    System.out.println("Enter passphrase :");
                    passphrase = scanner.next();
                }
                return passphrase;
            }

            @Override
            public String getPassword() {
                return null;
            }

            @Override
            public boolean promptPassword(String string) {
                return false;
            }

            @Override
            public boolean promptPassphrase(String string) {
                return true;
            }

            @Override
            public boolean promptYesNo(String string) {
                return true;
            }

            @Override
            public void showMessage(String string) {
                LOG.info("SSH: {}", string);
            }
        };
    }

    public static boolean isOsWindows() {
        try {
            String osName = System.getProperty("os.name");
            return osName != null && osName.toLowerCase(Locale.US).startsWith("windows");
        } catch (final SecurityException ignored) {
            return false;
        }
    }

    public static boolean pollSshPortIsAvailable(String masterPublicIp) {
        LOG.info(V, "Checking if SSH port is available and ready ...");
        int attempt = SSH_POLL_ATTEMPTS;
        while (attempt > 0) {
            try {
                final Socket socket = new Socket();
                socket.connect(new InetSocketAddress(masterPublicIp, 22), 2000);
                byte[] buffer = new byte[1024];
                int bytesRead = socket.getInputStream().read(buffer, 0, buffer.length);
                String sshVersion = new String(buffer, 0, bytesRead).trim();
                socket.close();
                return true;
            } catch (Exception ex) {
                attempt--;
                LOG.error(V, "Poll SSH {}", ex.getMessage());
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
            }
        }
        LOG.error("Master instance SSH port is not reachable.");
        return false;
    }

    /**
     * Executes a script on remote.
     *
     * @param sshSession transfer via ssh session
     * @param script to be executed (must be a shell/bash script)

     * @throws JSchException ssh openChannel exception
     * @throws IOException BufferedReader exceptions
     * @throws ConfigurationException if configuration was unsuccesful
     */
    public static void executeScript(final Session sshSession, String script)
            throws IOException, JSchException, ConfigurationException {

        // Add line to script to identify if script was run successfully.
        // This is necessary when running e.g. ansible within the script.
        // Ansible alway terminates with error code 0, even if ansible
        // run fails.
        script = script + "\nif [ $? == 0 ]; then echo CONFIGURATION FINISHED; else echo CONFIGURATION FAILED; fi\n";

        LOG.info("Your cloud instance will be configured now. This might take a while.");

        ChannelExec channel = (ChannelExec) sshSession.openChannel("exec");

        LineReaderRunnable stdout = new LineReaderRunnable(channel.getInputStream(), true);
        LineReaderRunnable stderr = new LineReaderRunnable(channel.getErrStream(), false);

        // Create threads ...
        Thread t_stdout = new Thread(stdout);
        Thread t_stderr = new Thread(stderr);

        // ... start them ...
        t_stdout.start();
        t_stderr.start();

        // ... start ansible ...
        channel.setCommand(script);
        // ... connect channel
        channel.connect();

        // ... wait for threads finished ...
        try {
            t_stdout.join();
            t_stderr.join();
        } catch (InterruptedException e) {
            throw new ConfigurationException("Exception occurred during evaluation of ansible output!");
        }

        // and  disconnect channel
        channel.disconnect();

        if (stdout.getReturnCode() != 0) {
            throw new ConfigurationException("Cluster configuration failed.\n" + stdout.getReturnMsg());
        }
    }
}

/**
 * Watch and parse the stdout and stderr stream at the same time.
 * Since BufferReader.readline() blocks,
 * the only solution I found is to work with separate threads for stdout and stderror of the ssh channel.
 *
 * The following code snipset seems to be more complicated than it should be (in other languages).
 * If you find a better solution feel free to replace it.
 */
class LineReaderRunnable implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(CreateCluster.class);
    private static final String LOG_MSG_BG = "\"[BIBIGRID] ";

    private BufferedReader br;

    private boolean regular;

    private int returnCode = -1;
    private String returnMsg = "";

    /**
     * Initializes new Runnable with input / error stream.
     * @param stream InputStream with regular and error Logging
     * @param regular true, if regular stream, false, if error stream
     */
    LineReaderRunnable(InputStream stream, boolean regular) {
        this.br = new BufferedReader(new InputStreamReader(stream));
        this.regular = regular;
    }

    private void work_on_line(String line) {
        if (regular) {
            if (line.contains("CONFIGURATION FINISHED")) {
                returnCode = 0;
                returnMsg = ""; // clear possible msg
            } else if (line.contains("failed:")) {
                returnMsg = line;
            }
            if (VerboseOutputFilter.SHOW_VERBOSE) {
                // in verbose mode show every line generated by ansible
                LOG.info(V, "{}", line);
            } else {
                // otherwise show only ansible msg containing "[BIBIGRID]"
                int indexOfLogMessage = line.indexOf(LOG_MSG_BG);
                if (indexOfLogMessage > 0) {
                    LOG.info("[Ansible] {}",  line.substring(indexOfLogMessage + LOG_MSG_BG.length(), line.length() - 1));
                }
            }
        } else {
            // Check for real errors and print them to the error log ...
            if (line.toLowerCase().contains("ERROR".toLowerCase())) {
                LOG.error("{}", line);
            } else { // ... and everything else as warning !
                LOG.warn(V,"{}",line);
            }
        }
    }

    private void work_on_exception(Exception e) {
        LOG.error("Evaluate stderr : " + e.getMessage());
        returnCode = 1;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = br.readLine()) != null ) {
                work_on_line(line);
            }
        } catch (IOException ex) {
            work_on_exception(ex);
        }
    }

    int getReturnCode(){
        return returnCode;
    }

    String getReturnMsg(){
        return returnMsg;
    }
}
