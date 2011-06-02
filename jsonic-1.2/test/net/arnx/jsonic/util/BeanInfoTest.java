package net.arnx.jsonic.util;

import static org.junit.Assert.*;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.arnx.jsonic.JSON;

import org.junit.Test;


public class BeanInfoTest {
	
	@Test
	public void testToUpperCamel() throws Exception {
		assertEquals("A", BeanInfo.toUpperCamel("a"));
		assertEquals("Aaa", BeanInfo.toUpperCamel("aaa"));
		assertEquals("AaaA", BeanInfo.toUpperCamel("aaaA"));
		assertEquals("AaaAAA", BeanInfo.toUpperCamel("aaaAAA"));
		assertEquals("AaaAAA", BeanInfo.toUpperCamel("aaa_AAA"));
	}
	
	@Test
	public void testToLowerCamel() throws Exception {
		assertEquals("a", BeanInfo.toLowerCamel("a"));
		assertEquals("aaa", BeanInfo.toLowerCamel("aaa"));
		assertEquals("aaaA", BeanInfo.toLowerCamel("aaaA"));
		assertEquals("aaaAAA", BeanInfo.toLowerCamel("aaaAAA"));
		assertEquals("aaaAAA", BeanInfo.toLowerCamel("aaa_AAA"));
		assertEquals("AAA", BeanInfo.toLowerCamel("AAA"));
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
		
		assertEquals(String.class, BeanInfo.getRawType(String.class));
		assertEquals(String[].class, BeanInfo.getRawType(String[].class));
		assertEquals(List.class, BeanInfo.getRawType(this.getClass().getField("t1").getGenericType()));
		assertEquals(Map.class, BeanInfo.getRawType(this.getClass().getField("t2").getGenericType()));
		assertEquals(List.class, BeanInfo.getRawType(this.getClass().getField("t3").getGenericType()));
		assertEquals(List.class, BeanInfo.getRawType(this.getClass().getField("t4").getGenericType()));
		assertEquals(List.class, BeanInfo.getRawType(((ParameterizedType)this.getClass().getField("t4").getGenericType()).getActualTypeArguments()[0]));
		assertEquals(List.class, BeanInfo.getRawType(this.getClass().getField("t5").getGenericType()));
		assertEquals(Object.class, BeanInfo.getRawType(((ParameterizedType)this.getClass().getField("t5").getGenericType()).getActualTypeArguments()[0]));
		assertEquals(List[].class, BeanInfo.getRawType(this.getClass().getField("t6").getGenericType()));
		assertEquals(List[].class, BeanInfo.getRawType(this.getClass().getField("t7").getGenericType()));
		assertEquals(List[][].class, BeanInfo.getRawType(this.getClass().getField("t8").getGenericType()));
	}
	
	
}
