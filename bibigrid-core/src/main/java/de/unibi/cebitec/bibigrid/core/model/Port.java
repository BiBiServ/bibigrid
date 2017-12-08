package de.unibi.cebitec.bibigrid.core.model;

/**
 * Simple definition of an network. Used to build security rules.
 *
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class Port {
    public enum Protocol {
        TCP, UDP, ICMP
    }

    public final String ipRange;
    public final int number;
    public final Protocol type;

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

    @Override
    public String toString() {
        return String.format("%s:%s", ipRange, number);
    }
}
