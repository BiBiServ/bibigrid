package de.unibi.cebitec.bibigrid.core.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class SubNetsTest {
    private static final String IP = "1.2.3.4";
    private static final long IP_LONG = (1L << 24) + (2 << 16) + (3 << 8) + 4;
    private static final long MASK = 4160749568L;

    @Test
    public void parseCidrTest() throws Exception {
        assertArrayEquals(new long[]{IP_LONG, 5, MASK}, SubNets.parseCidr(IP + "/5"));
    }

    @Test
    public void createMaskTest() throws Exception {
        assertEquals(MASK, SubNets.createMask(5));
    }

    @Test
    public void longAsIPV4StringTest() throws Exception {
        assertEquals(IP, SubNets.longAsIPV4String(IP_LONG));
    }

    @Test
    public void malformedCidrTest() throws Exception {
        try {
            SubNets.parseCidr(null);
            fail("Did not throw RuntimeException");
        } catch (RuntimeException ignored) {
        }
        try {
            SubNets.parseCidr("");
            fail("Did not throw RuntimeException");
        } catch (RuntimeException ignored) {
        }
        try {
            SubNets.parseCidr("1.2.3.4");
            fail("Did not throw RuntimeException");
        } catch (RuntimeException ignored) {
        }
        try {
            SubNets.parseCidr("1.2.3.4/10/2");
            fail("Did not throw RuntimeException");
        } catch (RuntimeException ignored) {
        }
        try {
            SubNets.parseCidr("1.2.3/10");
            fail("Did not throw RuntimeException");
        } catch (RuntimeException ignored) {
        }
        try {
            SubNets.parseCidr("1.2/10");
            fail("Did not throw RuntimeException");
        } catch (RuntimeException ignored) {
        }
        try {
            SubNets.parseCidr("1/10");
            fail("Did not throw RuntimeException");
        } catch (RuntimeException ignored) {
        }
    }
}
