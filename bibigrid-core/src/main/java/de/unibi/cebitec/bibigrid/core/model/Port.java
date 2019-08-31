package de.unibi.cebitec.bibigrid.core.model;

import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple definition of an network. Used to build security rules.
 *
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class Port {
    protected static final Logger LOG = LoggerFactory.getLogger(Port.class);

    public enum Protocol {
        TCP, UDP, ICMP
    }

    private String ipRange;
    private int number;
    private Protocol type;

    public Port() {
        ipRange = "0.0.0.0/0";
        number = 0;
        type = Protocol.TCP;
    }

    public Port(String ipRange, int number) {
        this.ipRange = ipRange;
        this.number = number;
        type = Protocol.TCP;
    }

    public Port(String ipRange, int number, Protocol type) {
        this.ipRange = ipRange;
        this.number = number;
        this.type = type;
    }

    public String getIpRange() {
        return ipRange;
    }

    public void setIpRange(String ipRange) throws ConfigurationException {
        try {
            //Pattern p = Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})");
            Pattern p = Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})(?:/(\\d{1,2}))?");
            if (ipRange.equalsIgnoreCase("current")) {
                this.ipRange = InetAddress.getLocalHost().getHostAddress() + "/32";
            } else {
                Matcher m = p.matcher(ipRange);
                //noinspection ResultOfMethodCallIgnored
                m.matches();
                for (int i = 1; i <= 4; i++) {
                    checkStringAsInt(m.group(i), 255);
                }
                if (m.groupCount()==5 && m.group(5)!=null) {
                    checkStringAsInt(m.group(5), 32);
                    this.ipRange = ipRange;
                } else {
                    this.ipRange = ipRange + "/32";
                }
            }
        } catch (Exception e) {
            LOG.error("Could not parse the supplied ipRange: '{}'. A valid ipRange has the following " +
                    "pattern: (current|'ip4v-address'|'ip4v-range/CIDR') {}", ipRange, e.getMessage());
            throw new ConfigurationException(e);
        }
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public Protocol getType() {
        return type;
    }

    public void setType(Protocol type) {
        this.type = type;
    }

    private int checkStringAsInt(String s, int max) throws Exception {
        int v = Integer.parseInt(s);
        if (v < 0 || v > max) {
            throw new Exception();
        }
        return v;
    }

    @Override
    public String toString() {
        return String.format("%s:%s (%s)", ipRange, number, type);
    }
}
