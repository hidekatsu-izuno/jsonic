package net.arnx.jsonic;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Test;
import static org.junit.Assert.*;

public class ClassUtilTest {
	@Test
	public void testFindClass() throws Exception {
		URLClassLoader cl = new URLClassLoader(new URL[] { new File("./lib/commmons-logging.jar").toURI().toURL() });
		
		ClassUtil.clear();
		
		ClassLoader current = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(cl);
		
		assertNotNull(ClassUtil.findClass("org.apache.commons.logging.Log"));
		assertNotNull(ClassUtil.findClass("java.net.InetAddress"));
		assertNull(ClassUtil.findClass("org.apache.commons.logging.Log2"));
		
		Thread.currentThread().setContextClassLoader(current);
		assertEquals(1, ClassUtil.cache.size());
		assertEquals(3, ClassUtil.cache.get(cl).size());
		assertNull(ClassUtil.cache.get(cl).get("org.apache.commons.logging.Log2"));
		cl = null;
		
		System.gc();
		
		Thread.sleep(1000);

		assertTrue(ClassUtil.cache.isEmpty());
	}
}