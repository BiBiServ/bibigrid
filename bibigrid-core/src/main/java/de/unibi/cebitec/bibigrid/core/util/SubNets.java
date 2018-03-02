package de.unibi.cebitec.bibigrid.core.util;

import java.util.Collection;

/**
 * Provides a function that returns next subnet
 *
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
@SuppressWarnings("WeakerAccess")
public class SubNets {
    private final long networkSize;
    private final long subnetSize;
    private final long subnetBase;
    private long current = 0;
    private final long numberOfSubnets;

    public SubNets(String cidr, long subnetSize) {
        long[] tmp = parseCidr(cidr);
        long ip = tmp[0];
        networkSize = tmp[1];
        long networkMask = tmp[2];
        this.subnetSize = subnetSize;
        subnetBase = ip & networkMask;
        numberOfSubnets = Math.round((Math.pow(2, subnetSize - networkSize) - 1));
        if (networkSize >= subnetSize) {
            throw new RuntimeException("Network size is smaller than subnet size");
        }
    }

    /**
     * Return next subnet address or -1 if there isn't one left.
     */
    public long next() {
        return current > numberOfSubnets ? -1 : subnetBase + ((current++) << (subnetSize - networkSize));
    }

    /**
     * Return next free subnet.
     */
    public String nextCidr() {
        long l = next();
        return l == -1 ? null : longAsIPV4String(l) + "/" + subnetSize;
    }

    /**
     * Return next free CIDR that is not part of the given Collection of CIDR.
     */
    public String nextCidr(Collection<String> coc) {
        String nextCidr;
        while ((nextCidr = nextCidr()) != null) {
            if (!coc.contains(nextCidr)) {
                return nextCidr;
            }
        }
        return null;
    }

    /**
     * Parse the given CIDR and return an array of long values.
     *
     * @return {ip,network size,network mask}
     */
    public static long[] parseCidr(String cidr) {
        String[] cidrParts = cidr.split("/");
        if (cidrParts.length != 2) {
            throw new RuntimeException("CIDR has invalid format - expected d.d.d.d/m, but is " + cidr);
        }
        String[] ips = cidrParts[0].split("\\.");
        if (ips.length != 4) {
            throw new RuntimeException("CIDR has invalid format - expected d.d.d.d/m, but is " + cidr);
        }
        long ip = (Long.parseLong(ips[0]) << 24) + (Long.parseLong(ips[1]) << 16) + (Long.parseLong(ips[2]) << 8) + Long.parseLong(ips[3]);
        long size = Integer.parseInt(cidrParts[1]);
        long mask = createMask(size);

        return new long[]{ip, size, mask};
    }

    /**
     * Return a networkMask on base of given network size.
     */
    public static long createMask(long networkSize) {
        return Math.round((Math.pow(2, networkSize) - 1)) << (32 - networkSize);
    }

    /**
     * Return a long value as IPV4 String.
     *
     * @return IPV4 String in form XX.XX.XX.XX
     */
    public static String longAsIPV4String(long l) {
        return ((l & 0xFF000000L) >> 24) + "." + ((l & 0xFF0000L) >> 16) + "." + ((l & 0xFF00L) >> 8) + "." + (l & 0xFFL);
    }
}
