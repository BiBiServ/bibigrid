package de.unibi.cebitec.bibigrid;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class FactoryTest {
    /* TODO: decide if necessary
    @After
    public void tearDown() throws Exception {
        // Reflection removal of factory
        Field instanceField = Factory.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }
    */

    @Test
    public void instanceTest() throws Exception {
        assertNotNull(Factory.getInstance());
    }

    @Test
    public void getInterfaceImplementationsTest() throws Exception {
        List<Class<TestInterface>> result = Factory.getInstance().getImplementations(TestInterface.class);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.get(0).equals(TestClass1.class) || result.get(0).equals(TestClass2.class));
        assertTrue(result.get(1).equals(TestClass1.class) || result.get(1).equals(TestClass2.class));
    }

    @Test
    public void getClassImplementationsTest() throws Exception {
        List<Class<TestClass>> result = Factory.getInstance().getImplementations(TestClass.class);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).equals(TestClass3.class));
    }

    private interface TestInterface {
    }

    private abstract class TestClass {
    }

    private class TestClass1 implements TestInterface {
    }

    private class TestClass2 implements TestInterface {
    }

    private class TestClass3 extends TestClass {
    }
}
