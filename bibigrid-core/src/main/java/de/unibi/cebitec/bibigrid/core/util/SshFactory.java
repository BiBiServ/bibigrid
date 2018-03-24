package de.unibi.cebitec.bibigrid.core.util;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

import java.io.Console;
import java.nio.file.Path;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SshFactory {
    private static final Logger LOG = LoggerFactory.getLogger(SshFactory.class);

    public static Session createNewSshSession(JSch ssh, String dns, String user, Path identity) {
        try {
            Session sshSession = ssh.getSession(user, dns, 22);
            ssh.addIdentity(identity.toString());
            UserInfo userInfo = getConsolePasswordUserInfo();
            sshSession.setUserInfo(userInfo);
            return sshSession;
        } catch (JSchException e) {
            LOG.error(e.getMessage());
            return null;
        }
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
            return osName != null && osName.startsWith("Windows");
        } catch (final SecurityException ignored) {
            return false;
        }
    }
}
