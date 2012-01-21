package net.arnx.jsonic.internal.util;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.arnx.jsonic.JSON;
import net.arnx.jsonic.internal.util.ClassUtil;

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
		
		Field cacheField = ClassUtil.class.getDeclaredField("cache");
		cacheField.setAccessible(true);
		
		@SuppressWarnings("unchecked")
		Map<ClassLoader, Map<String, Class<?>>> cache = (Map<ClassLoader, Map<String, Class<?>>>)cacheField.get(null);
		
		assertEquals(1, cache.size());
		assertEquals(3, cache.get(cl).size());
		assertNull(cache.get(cl).get("org.apache.commons.logging.Log2"));
		cl = null;
		
		System.gc();
		
		Thread.sleep(1000);

		assertTrue(cache.isEmpty());
	}
	
	
	@Test
	public void testToUpperCamel() throws Exception {
		assertEquals("A", ClassUtil.toUpperCamel("a"));
		assertEquals("Aaa", ClassUtil.toUpperCamel("aaa"));
		assertEquals("AaaA", ClassUtil.toUpperCamel("aaaA"));
		assertEquals("AaaAAA", ClassUtil.toUpperCamel("aaaAAA"));
		assertEquals("AaaAAA", ClassUtil.toUpperCamel("aaa_AAA"));
	}
	
	@Test
	public void testToLowerCamel() throws Exception {
		assertEquals("a", ClassUtil.toLowerCamel("a"));
		assertEquals("aaa", ClassUtil.toLowerCamel("aaa"));
		assertEquals("aaaA", ClassUtil.toLowerCamel("aaaA"));
		assertEquals("aaaAAA", ClassUtil.toLowerCamel("aaaAAA"));
		assertEquals("aaaAAA", ClassUtil.toLowerCamel("aaa_AAA"));
		assertEquals("AAA", ClassUtil.toLowerCamel("AAA"));
	}
	
	public List<List<Object>> t1;
	public Map<Map<String, Object>, List<Object>> t2;
	public List<?> t3;
	public List<? extends List<?>> t4;
	public List<? super List<?>> t5;
	public List<?>[] t6;
	public List<List<?>>[] t7;
	public List<List<?>>[][] t8;
	
	public List<Integer> tx;
	
	@Test
	public void testGetRawType() throws Exception {
		List<List<Object>> xt1 = new ArrayList<List<Object>>();
		List<Object> xt1_1 = new ArrayList<Object>();
		xt1_1.add("a");
		xt1.add(xt1_1);
		
		assertEquals(xt1, JSON.decode("[['a']]", this.getClass().getField("t1").getGenericType()));
		
		assertEquals(String.class, ClassUtil.getRawType(String.class));
		assertEquals(String[].class, ClassUtil.getRawType(String[].class));
		assertEquals(List.class, ClassUtil.getRawType(this.getClass().getField("t1").getGenericType()));
		assertEquals(Map.class, ClassUtil.getRawType(this.getClass().getField("t2").getGenericType()));
		assertEquals(List.class, ClassUtil.getRawType(this.getClass().getField("t3").getGenericType()));
		assertEquals(List.class, ClassUtil.getRawType(this.getClass().getField("t4").getGenericType()));
		assertEquals(List.class, ClassUtil.getRawType(((ParameterizedType)this.getClass().getField("t4").getGenericType()).getActualTypeArguments()[0]));
		assertEquals(List.class, ClassUtil.getRawType(this.getClass().getField("t5").getGenericType()));
		assertEquals(Object.class, ClassUtil.getRawType(((ParameterizedType)this.getClass().getField("t5").getGenericType()).getActualTypeArguments()[0]));
		assertEquals(List[].class, ClassUtil.getRawType(this.getClass().getField("t6").getGenericType()));
		assertEquals(List[].class, ClassUtil.getRawType(this.getClass().getField("t7").getGenericType()));
		assertEquals(List[][].class, ClassUtil.getRawType(this.getClass().getField("t8").getGenericType()));
	}
}