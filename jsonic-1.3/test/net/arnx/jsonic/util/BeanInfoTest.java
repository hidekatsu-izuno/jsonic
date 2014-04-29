package net.arnx.jsonic.util;

import static org.junit.Assert.*;

import java.net.URLClassLoader;

import org.junit.Test;

public class BeanInfoTest {

	@Test
	public void test() {
		BeanInfo bi = BeanInfo.get(Class.class);
		assertNull(bi.getProperty("class"));
		assertNull(bi.getProperty("classloader"));
		assertTrue(bi.getProperties().isEmpty());

		bi = BeanInfo.get(ClassLoader.class);
		assertNull(bi.getProperty("class"));
		assertTrue(bi.getProperties().isEmpty());
		assertTrue(bi.getMethods().isEmpty());

		bi = BeanInfo.get(URLClassLoader.class);
		assertNull(bi.getProperty("class"));
		assertTrue(bi.getProperties().isEmpty());
		assertTrue(bi.getMethods().isEmpty());

		bi = BeanInfo.get(BeanTest.class);
		assertNull(bi.getProperty("class"));
	}

	static class BeanTest {
	}
}
