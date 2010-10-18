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
		
		Thread.currentThread().setContextClassLoader(current);
		cl = null;
		
		System.gc();
		
		Thread.sleep(1000);

		assertTrue(ClassUtil.cache.isEmpty());
	}
}