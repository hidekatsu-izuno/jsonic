package net.arnx.jsonic;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;
import java.io.*;

import net.arnx.jsonic.JSON;

import org.junit.Test;

public class JSONTest {

	@Test
	@SuppressWarnings("unused")
	public void testEncode() throws Exception {
		ArrayList<Object> list = new ArrayList<Object>();
		assertEquals("[]", JSON.encode(list));
		
		list.add("");
		list.add(1);
		list.add(1.0);
		list.add('c');
		list.add(new char[]{'c', 'h', 'a', 'r', '[', ']'});
		list.add("string");
		list.add(true);
		list.add(false);
		list.add(null);
		list.add(new Object());
		list.add(new int[] {});
		list.add(Pattern.compile("\\.*"));
		list.add(boolean.class);
		
		assertEquals("[\"\",1,1.0,\"c\",\"char[]\",\"string\",true,false,null,{},[],\"\\\\.*\",\"boolean\"]", JSON.encode(list));
		
		assertEquals("[1,2,3]", JSON.encode(new short[] {1,2,3}));
		assertEquals("[1,2,3]", JSON.encode(new int[] {1,2,3}));
		assertEquals("[1,2,3]", JSON.encode(new long[] {1l,2l,3l}));
		assertEquals("[1.0,2.0,3.0,\"NaN\",\"Infinity\",\"-Infinity\"]", JSON.encode(
				new float[] {1.0f,2.0f,3.0f,Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY}));
		assertEquals("[1.0,2.0,3.0,\"NaN\",\"Infinity\",\"-Infinity\"]", JSON.encode(
				new double[] {1.0,2.0,3.0,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY}));
		
		assertEquals("[\"ja\"]", JSON.encode(new Object[] {Locale.JAPANESE}));
		assertEquals("[\"ja-JP\"]", JSON.encode(new Object[] {Locale.JAPAN}));
		
		Date date = new Date();
		assertEquals("[" + date.getTime() + "]", JSON.encode(new Object[] {date}));
		
		Calendar cal = Calendar.getInstance();
		assertEquals("[" + cal.getTimeInMillis() + "]", JSON.encode(new Object[] {cal}));
		
		Map<String, Object> map = new HashMap<String, Object>();
		assertEquals("{}", JSON.encode(map));
		
		map.put("value", 1);
		assertEquals("{\"value\":1}", JSON.encode(map));
		
		Object obj = new Object();
		assertEquals("{}", JSON.encode(obj));
		
		obj = new Object() {
			public int getPublicValue() {
				return 1;
			}
			
			protected int getProtectedValue() {
				return 1;
			}
			int getFriendlyValue() {
				return 1;
			}
			private int getPrivateValue() {
				return 1;
			}
		};
		assertEquals("{\"publicValue\":1}", JSON.encode(obj));
		
		obj = new Object() {
			public int publicValue = 1;
			
			public transient int transientValue = 1;
			
			protected int protectedValue = 1;
			
			int friendlyValue = 1;
			
			private int privateValue = 1;
		};
		assertEquals("{\"publicValue\":1}", JSON.encode(obj));

		obj = new Object() {
			public int publicValue = 0;
			
			public int getPublicValue() {
				return 1;
			}
			
			public Object getMine() {
				return this;
			}
		};
		assertEquals("{\"publicValue\":1}", JSON.encode(obj));
		
		TestBean test = new TestBean();
		test.setA(100);
		test.e = Locale.ENGLISH;
		assertEquals("{\"a\":100,\"b\":null,\"c\":false,\"d\":null,\"e\":\"en\",\"f\":null,\"g\":null,\"h\":null}", JSON.encode(test));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testDecodeString() throws Exception {
		ArrayList<Object> list = new ArrayList<Object>();
		list.add(new HashMap());
		list.add(new ArrayList());
		list.add(new BigDecimal("1"));
		list.add("str'ing");
		list.add("");
		list.add(true);
		list.add(false);
		list.add(null);
		
		assertEquals(list, JSON.decode("[{}, [], 1, \"str'ing\", \"\", true, false, null]"));
		assertEquals(list, JSON.decode("\r[\t{\r}\n, [\t]\r,\n1 ,\t \r\"str'ing\"\n, \"\", true\t,\rfalse\n,\tnull\r]\n"));
		
		list.clear();
		list.add(new BigDecimal("-1.1"));
		list.add(new BigDecimal("11.1"));
		list.add(new BigDecimal("11.1"));
		list.add(new BigDecimal("1.11"));

		assertEquals(list, JSON.decode("[-1.1, 1.11e1, 1.11E+1, 11.1e-1]"));
		
		Map<String, Object> map1 = new HashMap<String, Object>();
		Map<String, Object> map2 = new HashMap<String, Object>();
		Map<String, Object> map3 = new HashMap<String, Object>();
		map1.put("map2", map2);
		map1.put("1", new BigDecimal("1"));
		map2.put("'2'", new BigDecimal("2"));
		map2.put("map3", map3);
		map3.put("'3", new BigDecimal("3"));
		
		assertEquals(map1, JSON.decode("{\"map2\": {\"'2'\": 2, \"map3\": {\"'3\": 3}}, \"1\": 1}"));
		
		Object[] input = new Object[2];
		input[0] = new Date();
		input[1] = Calendar.getInstance();
		
		List output = new ArrayList();
		output.add(new BigDecimal(((Date)input[0]).getTime()));
		output.add(new BigDecimal(((Calendar)input[1]).getTimeInMillis()));
		
		assertEquals(output, JSON.decode(JSON.encode(input)));
	}

	@Test
	@SuppressWarnings({ "unchecked", "serial" })
	public void testDecodeStringClassOfQextendsT() throws Exception {
		ArrayList<Object> list = new ArrayList<Object>();
		list.add(new HashMap());
		list.add(new ArrayList());
		list.add(new BigDecimal("1"));
		list.add("string");
		list.add(true);
		list.add(false);
		list.add(null);
		
		assertEquals(list, JSON.decode(JSON.encode(list), List.class));

		TestBean test = new TestBean();
		test.setA(100);
		test.b = "hoge-hoge";
		test.setC(false);
		test.d = new Date();
		test.e = Locale.JAPAN;
		test.setG(Pattern.compile("\\.*"));
		test.setH(boolean.class);
		
		String json = JSON.encode(test);
		TestBean result = JSON.decode(json, TestBean.class);
		
		assertEquals(test, result);
		
		test = new TestBean();
		test.setA(0);
		test.b = "hoge-hoge";
		test.setC(false);
		test.d = null;
		test.e = Locale.JAPAN;
		test.setG(Pattern.compile("\\.*"));
		test.setH(Object.class);
		
		assertEquals(test, JSON.decode("{\"a\":null,\"b\":\"hoge-hoge\",\"c\":false,\"d\":null,\"e\":[\"ja\", \"JP\"],\"g\":\"\\\\.*\",\"h\":\"java.lang.Object\"}", TestBean.class));
		
		GenericsBean gb = new GenericsBean();
		List<String> list2 = new ArrayList<String>();
		list2.add("1");
		list2.add("false");
		gb.setList(list2);
		
		Map<String, String> map2 = new HashMap<String, String>();
		map2.put("1", "1");
		map2.put("true", "true");
		gb.setMap(map2);
		
		List<List<String>> glist2 = new ArrayList<List<String>>();
		glist2.add(new ArrayList<String>() {
			{
				add("1");
				add("false");
			}
		});
		gb.setGenericsList(glist2);
		
		GenericsBean out = JSON.decode("{\"list\": [1, false], \"map\": {\"1\": 1, \"true\": true}, \"genericsList\": [[1, false]]}", GenericsBean.class);
		assertEquals(gb, out);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testFormat() throws Exception {
		JSON json = new JSON();
		ArrayList<Object> list = new ArrayList<Object>();
		assertEquals("[]", json.format(list, new StringBuilder()).toString());
		
		list.add(1);
		list.add(1.0);
		list.add('c');
		list.add(new char[]{'c', 'h', 'a', 'r', '[', ']'});
		list.add("string");
		list.add(true);
		list.add(false);
		list.add(null);
		list.add(new TreeMap() {
			private static final long serialVersionUID = 1L;

			{
				put("a", "a");
				put("b", new int[] {1,2,3,4,5});
				put("c", new TreeMap() {
					private static final long serialVersionUID = 1L;
					{
						put("a", "a");
					}
				});
			}
		});
		list.add(new int[] {1,2,3,4,5});

		json.setPrettyPrint(true);
		assertEquals("[\n\t1,\n\t1.0,\n\t\"c\",\n\t\"char[]\",\n\t\"string\",\n\ttrue,\n\tfalse,\n\tnull," 
				+ "\n\t{\n\t\t\"a\": \"a\",\n\t\t\"b\": [1, 2, 3, 4, 5],\n\t\t\"c\": {\n\t\t\t\"a\": \"a\"\n\t\t}\n\t},\n\t[1, 2, 3, 4, 5]\n]",
				json.format(list, new StringBuilder()).toString());
		
		try {
			json.format(true, new StringBuilder());
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}
		
		json.setPrettyPrint(false);
		try {
			assertEquals("true", json.format(true, new StringBuilder()).toString());
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}
		assertEquals("[\"NaN\",\"Infinity\",\"-Infinity\"]", json.format(
				new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}, new StringBuilder()).toString());
		
		Date d = new Date();
		assertEquals("[" + Long.toString(d.getTime()) + "]", json.format(new Date[] {d}, new StringBuilder()).toString());
		
		
		assertEquals("[\"AQID\"]", json.format(new byte[][] {{1,2,3}}, new StringBuilder()).toString());
	}
	
	@Test
	@SuppressWarnings({ "unchecked", "serial" })
	public void testParse() throws Exception {
		Locale.setDefault(Locale.ENGLISH);
		JSON json = new JSON();
		
		ArrayList<Object> list = new ArrayList<Object>();
		list.add(new HashMap() {
			{
				put("maa", "bbb");
			}
		});
		list.add(new ArrayList());
		list.add(new BigDecimal("1"));
		list.add("str'ing");
		list.add(true);
		list.add(false);
		list.add(null);
		
		try {
			assertEquals(list, json.parse("[{\"maa\": \"bbb\"}, [], 1, \"str\\'ing\", true, false, null"));
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);			
		}
		
		assertEquals(list, json.parse("[{'\u006daa': 'bbb'}, [], 1, 'str\\'ing', true, false, null]"));
		
		assertEquals(list, json.parse("[{\u006daa: \"bbb\"}, [], 1, \"str'ing\", true, false, null]"));
		
		assertEquals(list, json.parse("[{\"\u006daa\": \"bbb\"}, [/**/], 1, \"str'ing\", true, false, null]"));

		assertEquals(list, json.parse("[{'\u006Daa': 'bbb'}, [], 1, 'str\\'in\\g', true, false, null]"));
		
		try {
			assertEquals(list, json.parse("[{'\u006daa\": 'bbb'}, [], 1, 'str\\'in\\g', true, false, null]"));
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);			
		}
		
		try {
			assertEquals(list, json.parse("[{\"maa': 'bbb'}, [], 1, 'str\\'in\\g', true, false, null]"));
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		assertEquals(list, json.parse("[{   \t\u006da\u0061   : 'bbb'}, [], 1, 'str\\'in\\g', true, false, null]"));
		
		list.set(0, new HashMap() {
			{
				put("float", "bbb");
			}
		});

		assertEquals(list, json.parse("[{float   : 'bbb'}, [], 1, 'str\\'in\\g', true, false, null]"));
		
		list.set(0, new HashMap() {
			{
				put("0float", "bbb");
			}
		});

		try {
			assertEquals(list, json.parse("[{0float   : 'bbb'}, [], 1, 'str\\'in\\g', true, false, null]"));
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);			
		}

		list.set(0, new HashMap() {
			{
				put("float0", "bbb");
			}
		});
		assertEquals(list, json.parse("[{float0   : 'bbb'}, [], 1, 'str\\'in\\g', true, false, null]"));
		
		assertEquals(new HashMap() {{put("true", true);}}, json.parse("  true: true  "));
		assertEquals(new HashMap() {{put("number", new BigDecimal(-100));}}, json.parse(" number: -100  "));
		
		try {
			assertEquals(new HashMap() {{put("true", true);}}, json.parse("  {true: true  "));
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		try {
			assertEquals(new HashMap() {{put("number", new BigDecimal(-100));}}, json.parse(" number: -100  }"));
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}
		
		assertEquals(new HashMap() {
			{
				put("numbers", new HashMap() {
					{
						put("number", new BigDecimal(-100));
					}
				});
			}
		}, json.parse(" numbers: { number: -100 } "));
		
		try {
			assertEquals(new HashMap() {
				{
					put("numbers", new HashMap() {
						{
							put("number", new BigDecimal(-100));
						}
					});
				}
			}, json.parse(" numbers: { number: -100 "));
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}
		
		assertEquals(list, json.parse("/*\n x\r */[/* x */{float0 //b\n  :/***/ 'bbb'}//d\r\r\r\r,#d\r\r\r\r"
				+ " [/*#\n x\r */], 1, 'str\\'in\\g',/*\n x\r */ true/*\n x\r */, false, null/*\n x\r */] /*\n x\r */ #  aaaa"));
		
		NamedBean nb = new NamedBean();
		nb.namedPropertyAaa = 100;
		assertEquals(nb, json.parse("{\"namedPropertyAaa\":100}", NamedBean.class));
		assertEquals(nb, json.parse("{\"named property aaa\":100}", NamedBean.class));
		assertEquals(nb, json.parse("{\"named_property_aaa\":100}", NamedBean.class));
		assertEquals(nb, json.parse("{\"Named Property Aaa\":100}", NamedBean.class));
		
		HashMap map = new LinkedHashMap() {
			{
				put("map", new LinkedHashMap() {
					{
						put("string", "string_aaa");
						put("int", new BigDecimal(100));
					}
				});
				put("list", new ArrayList() {
					{
						add("string");
						add(new BigDecimal(100));
					}
				});
			}
		};
		assertEquals(map, json.parse("map: {string: string_aaa  \t \nint:100}\n list:[ string, 100]"));
		assertEquals(map, json.parse("map {string: string_aaa  \t \nint:100}\n list:[ string\n 100]"));
		assertEquals(map, json.parse("\"map\" {string: string_aaa  \t \nint:100}\n list:[ string\n 100]"));
		assertEquals(map, json.parse("'map' {string: string_aaa  \t \nint:100}\n list:[ string\n 100]"));
		
		
		map = new LinkedHashMap() {
			{
				put("database", new LinkedHashMap() {
					{
						put("description", "ms sql server\n\tconnecter settings");
						put("user", "sa");
						put("password", "xxxx");
					}
				});
			}
		};
		assertEquals(map, json.parse("# database settings\ndatabase {\n  description: 'ms sql server\n\tconnecter settings'\n  user: sa\n  password:"
				+ " xxxx // you need to replace your password.\n}\n/* {\"database\": {\"description\": \"ms sql server\", \"user\": \"sa\", \"password\": \"xxxx\"}} */\n"));
	}

	@Test
	public void testInvoke() throws Exception {
		JSON json = new JSON();
		
		TestBean test = new TestBean();
		json.invoke(test, "setA", "[100]");
		assertEquals(100, json.invoke(test, "getA", null));
		
		json.invoke(test, "setA", "[   \r\n    100]");
		assertEquals(100, json.invoke(test, "getA", null));
	}
	
	@Test
	public void testBase64() throws Exception {
		JSON json = new JSON();
		
		Random rand = new Random();
		
		for (int i = 0; i < 100; i++) {
			byte[][] input = new byte[1][i];
			rand.nextBytes(input[0]);
			
			byte[][] output = (byte[][])json.parse(json.format(input), byte[][].class);
			
			assertEquals(toHexString(input[0]), toHexString(output[0]));
		}
	}
	
	private String toHexString(byte[] data) {
		if (data == null) return "null";
		
		StringBuilder sb = new StringBuilder();
		for (byte d : data) {
			sb.append(Integer.toHexString((int)d & 0xFF));
			sb.append(" ");
		}
		return sb.toString();
	}
	
	@Test
	public void testDecodeTime() throws Exception {
		JSON json = new JSON();
		
		long start = System.currentTimeMillis();
		json.parse(new InputStreamReader(this.getClass().getResourceAsStream("KEN_ALL.json"), "UTF-8"));
		System.out.println("time: " + (System.currentTimeMillis()-start));
	}
}

@SuppressWarnings("unused")
class TestBean {
	private int a;
	public void setA(int a) { this.a = a; }
	public int getA() { return a; }
	
	public String b;
	public String getB() { return b; }
	
	private boolean c;
	public boolean isC() { return c; }
	public void setC(boolean c) { this.c = c; }
	
	public Date d;

	public Locale e;

	private Boolean f;
	public Boolean getF() { return f; }
	public void setF(Boolean f) { this.f = f; }

	private Pattern g;
	public Pattern getG() { return g; }
	public void setG(Pattern g) { this.g = g; }

	private Class h;
	public Class getH() { return h; }
	public void setH(Class h) { this.h = h; }
	
	private int x = 10;
	int y = 100;
	protected int z = 1000;
	
	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + a;
		result = PRIME * result + ((b == null) ? 0 : b.hashCode());
		result = PRIME * result + (c ? 1231 : 1237);
		result = PRIME * result + ((d == null) ? 0 : d.hashCode());
		result = PRIME * result + ((e == null) ? 0 : e.hashCode());
		result = PRIME * result + ((f == null) ? 0 : f.hashCode());
		result = PRIME * result + ((h == null) ? 0 : h.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final TestBean other = (TestBean) obj;
		if (a != other.a)
			return false;
		if (b == null) {
			if (other.b != null)
				return false;
		} else if (!b.equals(other.b))
			return false;
		if (c != other.c)
			return false;
		if (d == null) {
			if (other.d != null)
				return false;
		} else if (!d.equals(other.d))
			return false;
		if (e == null) {
			if (other.e != null)
				return false;
		} else if (!e.equals(other.e))
			return false;
		if (f == null) {
			if (other.f != null)
				return false;
		} else if (!f.equals(other.f))
			return false;
		if (h == null) {
			if (other.h != null)
				return false;
		} else if (!h.equals(other.h))
			return false;
		return true;
	}
}

class NamedBean {
	public int namedPropertyAaa = 0;

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + namedPropertyAaa;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final NamedBean other = (NamedBean) obj;
		if (namedPropertyAaa != other.namedPropertyAaa)
			return false;
		return true;
	}
}

class GenericsBean {
	private List<String> list = null;
	private Map<String, String> map = null;
	private List<List<String>> glist = null;
	
	public List<String> getList() {
		return list;
	}
	
	public void setList(List<String> list) {
		this.list = list;
	}

	public Map<String, String> getMap() {
		return map;
	}
	
	public void setMap(Map<String, String> map) {
		this.map = map;
	}
	
	public List<List<String>> getGenericsList() {
		return glist;
	}
	
	public void setGenericsList(List<List<String>> glist) {
		this.glist = glist;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((glist == null) ? 0 : glist.hashCode());
		result = PRIME * result + ((list == null) ? 0 : list.hashCode());
		result = PRIME * result + ((map == null) ? 0 : map.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final GenericsBean other = (GenericsBean) obj;
		if (glist == null) {
			if (other.glist != null)
				return false;
		} else if (!glist.equals(other.glist))
			return false;
		if (list == null) {
			if (other.list != null)
				return false;
		} else if (!list.equals(other.list))
			return false;
		if (map == null) {
			if (other.map != null)
				return false;
		} else if (!map.equals(other.map))
			return false;
		return true;
	}
}