package net.arnx.jsonic.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class BeanInfoTest {

	@Test
	public void test() {
		BeanInfo bi = BeanInfo.get(Class.class);
		assertNull(bi.getProperty("classloader"));
	}

}
