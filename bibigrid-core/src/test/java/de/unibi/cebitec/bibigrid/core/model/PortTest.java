package de.unibi.cebitec.bibigrid.core.model;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class PortTest {
    private static final String IP = "127.0.0.1";
    private static final int PORT = 1234;

    @Test
    public void toStringTest() throws Exception {
        assertEquals("127.0.0.1:1234 (TCP)", new Port(IP, PORT).toString());
    }

    @Test
    public void protocolTest() throws Exception {
        assertEquals(Port.Protocol.TCP, new Port(IP, PORT).getType());
        assertEquals(Port.Protocol.TCP, new Port(IP, PORT, Port.Protocol.TCP).getType());
        assertEquals(Port.Protocol.UDP, new Port(IP, PORT, Port.Protocol.UDP).getType());
        assertEquals(Port.Protocol.ICMP, new Port(IP, PORT, Port.Protocol.ICMP).getType());
    }
}
