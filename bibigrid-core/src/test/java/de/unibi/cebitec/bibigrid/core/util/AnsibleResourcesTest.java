package de.unibi.cebitec.bibigrid.core.util;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;

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
        for (File file : resources.getFiles().values()) {
            assertTrue(file.exists());
            FileInputStream stream = new FileInputStream(file);
            assertNotNull(stream);
            stream.close();
        }
    }
}