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

		bi = BeanInfo.get(BeanTest1.class);
		assertNull(bi.getProperty("class"));
	}

	static class BeanTest1 {
	}
}
