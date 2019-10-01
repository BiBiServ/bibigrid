package de.unibi.cebitec.bibigrid.core.util;

import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class AnsibleResourcesTest {
    private AnsibleResources resources;

    @Before
    public void setUp() throws Exception {
        resources = new AnsibleResources();
    }

    @Test
    public void filesExist() throws Exception {
        for (String filepath : resources.getFiles()) {
            InputStream stream = resources.getFileStream(filepath);
            assertNotNull(stream);
            stream.close();
        }
    }
}