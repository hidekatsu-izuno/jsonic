package net.arnx.jsonic.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class BeanInfoTest {

	@Test
	public void test() {
		BeanInfo bi = BeanInfo.get(Class.class);
		assertNull(bi.getProperty("class"));
		assertNull(bi.getProperty("classloader"));
		assertTrue(bi.getProperties().isEmpty());
		assertFalse(bi.getMethods().isEmpty());

		bi = BeanInfo.get(BeanTest1.class);
		assertNull(bi.getProperty("class"));
		assertTrue(bi.getProperties().isEmpty());
		assertFalse(bi.getMethods().isEmpty());

		System.out.println(bi.getMethods());
	}

	static class BeanTest1 {
	}
}
