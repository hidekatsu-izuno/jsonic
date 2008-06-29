package net.arnx.jsonic;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.regex.Pattern;
import java.io.*;

import javax.xml.parsers.DocumentBuilderFactory;

import net.arnx.jsonic.JSON;

import org.junit.Test;
import org.w3c.dom.Document;

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
		list.add(ExampleEnum.Example0);
		
		assertEquals("[\"\",1,1.0,\"c\",\"char[]\",\"string\",true,false,null,{},[],\"\\\\.*\",\"boolean\",0]", JSON.encode(list));
		
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
		assertEquals("{\"a\":100,\"b\":null,\"c\":false,\"class_\":null,\"d\":null,\"e\":\"en\",\"f\":null,\"g\":null,\"h\":null,\"if\":null}", JSON.encode(test));

		Document doc = DocumentBuilderFactory
			.newInstance()
			.newDocumentBuilder()
			.parse(this.getClass().getResourceAsStream("Sample.xml"));
		assertEquals("{\"tagName\":\"html\",\"attributes\":{\"lang\":\"ja\",\"xmlns:hoge\":\"aaa\"},\"childNodes\":[\"\\n\",{\"tagName\":\"head\",\"childNodes\":[\"\\n\\t\",{\"tagName\":\"title\",\"childNodes\":[\"タイトル\"]},\"\\n\"]},\"\\n\",{\"tagName\":\"body\",\"childNodes\":[\"\\n\\t本文\\n\\t\",{\"tagName\":\"p\",\"childNodes\":[\"サンプル1\"]},\"\\n\\t\",{\"tagName\":\"p\",\"childNodes\":[\"サンプル2\"]},\"\\n\\t本文\\n\\t\",{\"tagName\":\"hoge:p\",\"attributes\":{\"hoge:x\":\"aaa\"},\"childNodes\":[\"サンプル3\"]},\"\\n\"]},\"\\n\"]}", JSON.encode(doc));

		list = new ArrayList<Object>();
		list.add(new URI("http://www.google.co.jp/"));
		list.add(new URL("http://www.google.co.jp/"));
		list.add(InetAddress.getByName("localhost"));
		list.add(Charset.forName("UTF-8"));
		assertEquals("[\"http://www.google.co.jp/\",\"http://www.google.co.jp/\",\"127.0.0.1\",\"UTF-8\"]", JSON.encode(list));
		assertEquals("[\"http://www.google.co.jp/\",\"http://www.google.co.jp/\",\"127.0.0.1\",\"UTF-8\"]", JSON.encode(list.iterator()));

		Vector v = new Vector(list);
		assertEquals("[\"http://www.google.co.jp/\",\"http://www.google.co.jp/\",\"127.0.0.1\",\"UTF-8\"]", JSON.encode(v.elements()));
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
		test.setH(TimeZone.getTimeZone("JST"));
		test.class_ = boolean.class;
		
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
		test.setH(TimeZone.getTimeZone("Asia/Tokyo"));
		test.class_ = Object.class;
		
		assertEquals(test, JSON.decode("{\"a\":null,\"b\":\"hoge-hoge\",\"c\":false,\"class\":\"java.lang.Object\",\"d\":null,\"e\":[\"ja\", \"JP\"],\"g\":\"\\\\.*\",\"h\":\"Asia/Tokyo\"}", TestBean.class));
		
		GenericsBean gb = new GenericsBean();
		List<String> list2 = new ArrayList<String>();
		list2.add("1");
		list2.add("false");
		gb.setList(list2);
		
		Map<String, String> gmap = new HashMap<String, String>();
		gmap.put("1", "1");
		gmap.put("true", "true");
		gb.setMap(gmap);
		
		Map<String, Integer> map2 = new HashMap<String, Integer>();
		map2.put("0", 0);
		map2.put("1", 1);
		gb.map2 = map2;
		
		Map<Integer, String> map3 = new HashMap<Integer, String>();
		map3.put(0, "false");
		map3.put(1, "true");
		gb.map3 = map3;
		
		List<List<String>> glist2 = new ArrayList<List<String>>();
		glist2.add(new ArrayList<String>() {
			{
				add("1");
				add("false");
			}
		});
		gb.setGenericsList(glist2);
		
		GenericsBean out = JSON.decode("{\"list\": [1, false], \"map\": {\"1\": 1, \"true\": true}, \"genericsList\": [[1, false]], \"map2\": [false, true], \"map3\": {\"0\": false, \"1\": true}}", GenericsBean.class);
		assertEquals(gb, out);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testWrite() throws Exception {
		JSON json = new JSON();
		ArrayList<Object> list = new ArrayList<Object>();
		assertEquals("[]", json.set(list).write(new StringBuilder()).toString());
		
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

		assertEquals("[\n\t1,\n\t1.0,\n\t\"c\",\n\t\"char[]\",\n\t\"string\",\n\ttrue,\n\tfalse,\n\tnull," 
				+ "\n\t{\n\t\t\"a\": \"a\",\n\t\t\"b\": [1, 2, 3, 4, 5],\n\t\t\"c\": {\n\t\t\t\"a\": \"a\"\n\t\t}\n\t},\n\t[1, 2, 3, 4, 5]\n]",
				json.set(list).write(new StringBuilder(), true).toString());
		
		assertEquals("true", json.set(true).write(new StringBuilder(), true).toString());
		assertEquals("true", json.set(true).write(new StringBuilder(), false).toString());

		assertEquals("[\"NaN\",\"Infinity\",\"-Infinity\"]", json.set(
				new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}).write(new StringBuilder()).toString());
		
		Date d = new Date();
		assertEquals("[" + Long.toString(d.getTime()) + "]", json.set(new Date[] {d}).write(new StringBuilder()).toString());
		
		
		assertEquals("[\"AQID\"]", json.set(new byte[][] {{1,2,3}}).write(new StringBuilder()).toString());
	}
	
	@Test
	@SuppressWarnings({ "unchecked", "serial" })
	public void testParse() throws Exception {
		Locale.setDefault(Locale.JAPANESE);
		JSON json = new JSON();
		
		try {
			CharSequence cs = null;
			assertNull(json.load(cs).get());
			fail();
		} catch (NullPointerException e) {
			System.out.println(e);
			assertNotNull(e);
		}
		
		try {
			Reader reader = null;
			assertNull(null, json.load(reader).get());
			fail();
		} catch (NullPointerException e) {
			System.out.println(e);
			assertNotNull(e);			
		}
		
		assertEquals(new LinkedHashMap(), json.load("").get());
		
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
			assertEquals(list, json.load("[{\"maa\": \"bbb\"}, [], 1, \"str\\'ing\", true, false, null").get());
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);			
		}
		
		try {
			assertEquals(list, json.load("[{'\u006daa': 'bbb'}, [], 1, 'str\\'ing', true, false, null]").get());
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}
		
		assertEquals(list, json.load("[{\u006daa: \"bbb\"}, [], 1, \"str'ing\", true, false, null]").get());
		
		assertEquals(list, json.load("[{\"\u006daa\": \"bbb\"}, [/**/], 1, \"str'ing\", true, false, null]").get());

		assertEquals(list, json.load("[{'\u006Daa': 'bbb'}, [], 1, \"str'ing\", true, false, null]").get());
		
		try {
			assertEquals(list, json.load("[{'\u006daa\": 'bbb'}, [], 1, \"str'ing\", true, false, null]").get());
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);			
		}
		
		try {
			assertEquals(list, json.load("[{\"maa': 'bbb'}, [], 1, \"str'ing\", true, false, null]").get());
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		assertEquals(list, json.load("[{   \t\u006da\u0061   : 'bbb'}, [], 1, \"str'ing\", true, false, null]").get());
		
		list.set(0, new HashMap() {
			{
				put("float", "bbb");
			}
		});

		assertEquals(list, json.load("[{float   : 'bbb'}, [], 1, \"str'ing\", true, false, null]").get());
		
		list.set(0, new HashMap() {
			{
				put("0float", "bbb");
			}
		});

		try {
			assertEquals(list, json.load("[{0float   : 'bbb'}, [], 1, \"str'ing\", true, false, null]").get());
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
		assertEquals(list, json.load("[{float0   : 'bbb'}, [], 1, \"str'ing\", true, false, null]").get());
		
		assertEquals(new HashMap() {{put("true", true);}}, json.load("  true: true  ").get());
		assertEquals(new HashMap() {{put("number", new BigDecimal(-100));}}, json.load(" number: -100  ").get());
		
		try {
			assertEquals(new HashMap() {{put("true", true);}}, json.load("  {true: true  ").get());
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		try {
			assertEquals(new HashMap() {{put("number", new BigDecimal(-100));}}, json.load(" number: -100  }").get());
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
		}, json.load(" numbers: { number: -100 } ").get());
		
		try {
			assertEquals(new HashMap() {
				{
					put("numbers", new HashMap() {
						{
							put("number", new BigDecimal(-100));
						}
					});
				}
			}, json.load(" numbers: { number: -100 ").get());
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}
		
		assertEquals(list, json.load("/*\n x\r */[/* x */{float0 //b\n  :/***/ 'bbb'}//d\r\r\r\r,#d\r\r\r\r"
				+ " [/*#\n x\r */], 1, \"str\\'in\\g\",/*\n x\r */ true/*\n x\r */, false, null/*\n x\r */] /*\n x\r */ #  aaaa").get());
		
		NamedBean nb = new NamedBean();
		nb.namedPropertyAaa = 100;
		assertEquals(nb, json.load("{\"namedPropertyAaa\":100}").get(NamedBean.class));
		assertEquals(nb, json.load("{\"named property aaa\":100}").get(NamedBean.class));
		assertEquals(nb, json.load("{\"named_property_aaa\":100}").get(NamedBean.class));
		assertEquals(nb, json.load("{\"Named Property Aaa\":100}").get(NamedBean.class));
		
		Map map1 = new LinkedHashMap() {
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
		assertEquals(map1, json.load("map: {string: string_aaa  \t \nint:100}\n list:[ string, 100]").get());
		assertEquals(map1, json.load("map {string: string_aaa  \t \nint:100}\n list:[ string\n 100]").get());
		assertEquals(map1, json.load("\"map\" {string: string_aaa  \t \nint:100}\n list:[ string\n 100]").get());
		assertEquals(map1, json.load("'map' {string: string_aaa  \t \nint:100}\n list:[ string\n 100]").get());

		Map map2 = new LinkedHashMap() {
			{
				put("emap", new LinkedHashMap());
				put("map", new LinkedHashMap() {
					{
						put("string", null);
						put("int", null);
					}
				});
				put("elist", new ArrayList());
				put("list", new ArrayList() {
					{
						add(null);
						add("string");
						add(null);
					}
				});
			}
		};
		
		Map map4 = new LinkedHashMap();
		
		json = new JSON(1);
		assertEquals(map4, json.load("{'1': '1'}").get());

		json = new JSON(2);
		map4.put("1", "1");
		assertEquals(map4, json.load("{'1': '1'}").get());
		
		List map4_2 = new ArrayList();
		map4.put("2", map4_2);
		assertEquals(map4, json.load("{'1': '1', '2': ['2']}").get());
		
		json = new JSON(3);
		map4_2.add("2");
		assertEquals(map4, json.load("{'1': '1', '2': ['2']}").get());
		
		Map map4_3 = new LinkedHashMap();
		List map4_3_1 = new ArrayList();
		map4_3.put("3_1", map4_3_1);
		map4.put("3", map4_3);
		assertEquals(map4, json.load("{'1': '1', '2': ['2'], '3': {'3_1': ['3']}}").get());

		json = new JSON(4);
		map4_3_1.add("3");
		assertEquals(map4, json.load("{'1': '1', '2': ['2'], '3': {'3_1': ['3']}}").get());
		
		json = new JSON(32);
		assertEquals(map2, json.load("emap:{}, map: {string: , int:}, elist:[],list: [,string, ]").get());
		
		Map map3 = new LinkedHashMap() {
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
		assertEquals(map3, json.load("# database settings\ndatabase {\n  description: 'ms sql server\n\tconnecter settings'\n  user: sa\n  password:"
				+ " xxxx // you need to replace your password.\n}\n/* {\"database\": {\"description\": \"ms sql server\", \"user\": \"sa\", \"password\": \"xxxx\"}} */\n").get());

		InheritedBean ibean = new InheritedBean();
		ibean.map0 = new LinkedHashMap();
		ibean.map0.put("10", new BigDecimal("10"));
		ibean.map1 = new LinkedHashMap();
		ibean.map1.put("11", new BigDecimal("11"));
		ibean.map2 = new SuperLinkedHashMap();
		ibean.map2.put("12", new BigDecimal("12"));
		ibean.list0 = new ArrayList();
		ibean.list0.add(new BigDecimal("13"));
		ibean.list1 = new ArrayList();
		ibean.list1.add(new BigDecimal("14"));
		ibean.list2 = new SuperArrayList();
		ibean.list2.add(new BigDecimal("15"));
		assertEquals(ibean, json.load("{map0:{'10':10},map1:{'11':11},map2:{'12':12},list0:[13],list1:[14],list2:[15]}").get(InheritedBean.class));
		
		List list2 = new ArrayList();
		list2.add("あいうえお");
		
		assertEquals(list2, json.load(this.getClass().getResourceAsStream("UTF-8.json")).get());
		assertEquals(list2, json.load(this.getClass().getResourceAsStream("UTF-16BE.json")).get());
		assertEquals(list2, json.load(this.getClass().getResourceAsStream("UTF-16LE.json")).get());
		assertEquals(list2, json.load(this.getClass().getResourceAsStream("UTF-32BE.json")).get());
		assertEquals(list2, json.load(this.getClass().getResourceAsStream("UTF-32LE.json")).get());
		
		assertEquals(list2, json.load(this.getClass().getResourceAsStream("UTF-8_BOM.json")).get());
		assertEquals(list2, json.load(this.getClass().getResourceAsStream("UTF-16BE_BOM.json")).get());
		assertEquals(list2, json.load(this.getClass().getResourceAsStream("UTF-16LE_BOM.json")).get());
		assertEquals(list2, json.load(this.getClass().getResourceAsStream("UTF-32BE_BOM.json")).get());
		assertEquals(list2, json.load(this.getClass().getResourceAsStream("UTF-32LE_BOM.json")).get());
	}

	@Test
	@SuppressWarnings({ "unchecked", "serial", "unused" })
	public void testConvert() throws Exception {
		JSON json = new JSON();
		
		// boolean
		assertEquals(Boolean.TRUE, json.set(100).get(boolean.class));
		assertEquals(Boolean.FALSE, json.set(0).get(boolean.class));
		assertEquals(Boolean.FALSE, json.set("f").get(boolean.class));
		assertEquals(Boolean.FALSE, json.set("off").get(boolean.class));
		assertEquals(Boolean.FALSE, json.set("no").get(boolean.class));
		assertEquals(Boolean.FALSE, json.set("NaN").get(boolean.class));
		assertEquals(Boolean.FALSE, json.set("false").get(boolean.class));
		assertEquals(Boolean.FALSE, json.set("").get(boolean.class));
		assertEquals(Boolean.FALSE, json.set(null).get(boolean.class));
		
		// Boolean
		assertEquals(Boolean.TRUE, json.set(100).get(Boolean.class));
		assertEquals(Boolean.FALSE, json.set(0).get(Boolean.class));
		assertEquals(Boolean.FALSE, json.set("off").get(Boolean.class));
		assertEquals(Boolean.FALSE, json.set("no").get(Boolean.class));
		assertEquals(Boolean.FALSE, json.set("NaN").get(Boolean.class));
		assertEquals(Boolean.FALSE, json.set("false").get(Boolean.class));
		assertEquals(Boolean.FALSE, json.set("").get(Boolean.class));
		assertNull(json.set(null).get(Boolean.class));
		
		// byte
		assertEquals((byte)0, json.set(null).get(byte.class));
		assertEquals((byte)0, json.set("0").get(byte.class));
		assertEquals((byte)0, json.set("+0").get(byte.class));
		assertEquals((byte)0, json.set("-0").get(byte.class));
		assertEquals((byte)5, json.set(new BigDecimal("5")).get(byte.class));
		assertEquals((byte)5, json.set(new BigDecimal("5.00")).get(byte.class));
		assertEquals((byte)0xFF, json.set("0xFF").get(byte.class));
		assertEquals((byte)0xFF, json.set("+0xFF").get(byte.class));
		try {
			json.set(new BigDecimal("5.01")).get(byte.class);
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}
		
		// Byte
		assertEquals(null, json.set(null).get(Byte.class));
		assertEquals((byte)0, json.set("0").get(Byte.class));
		assertEquals((byte)0, json.set("+0").get(Byte.class));
		assertEquals((byte)0, json.set("-0").get(Byte.class));
		assertEquals((byte)5, json.set(new BigDecimal("5")).get(Byte.class));
		assertEquals((byte)5, json.set(new BigDecimal("5.00")).get(Byte.class));
		assertEquals((byte)0xFF, json.set("0xFF").get(Byte.class));
		assertEquals((byte)0xFF, json.set("+0xFF").get(Byte.class));
		try {
			json.set(new BigDecimal("5.01")).get(Byte.class);
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}
		
		// short
		assertEquals((short)0, json.set(null).get(short.class));
		assertEquals((short)0, json.set("0").get(short.class));
		assertEquals((short)0, json.set("+0").get(short.class));
		assertEquals((short)0, json.set("-0").get(short.class));
		assertEquals((short)100, json.set(new BigDecimal("100")).get(short.class));
		assertEquals((short)100, json.set(new BigDecimal("100.00")).get(short.class));
		assertEquals((short)100, json.set("100").get(short.class));
		assertEquals((short)100, json.set("+100").get(short.class));
		assertEquals((short)0xFF, json.set("0xFF").get(short.class));
		assertEquals((short)0xFF, json.set("+0xFF").get(short.class));
		try {
			json.set(new BigDecimal("100.01")).get(short.class);
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}
		
		// Short
		assertEquals(null, json.set(null).get(Short.class));
		assertEquals((short)0, json.set("0").get(Short.class));
		assertEquals((short)0, json.set("+0").get(Short.class));
		assertEquals((short)0, json.set("-0").get(Short.class));
		assertEquals((short)100, json.set(new BigDecimal("100")).get(Short.class));
		assertEquals((short)100, json.set(new BigDecimal("100.00")).get(Short.class));
		assertEquals((short)100, json.set("100").get(Short.class));
		assertEquals((short)100, json.set("+100").get(Short.class));
		assertEquals((short)0xFF, json.set("0xFF").get(Short.class));
		assertEquals((short)0xFF, json.set("+0xFF").get(Short.class));
		try {
			json.set(new BigDecimal("100.01")).get(Short.class);
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}
		
		// int
		assertEquals(0, json.set(null).get(int.class));
		assertEquals(0, json.set("0").get(int.class));
		assertEquals(0, json.set("+0").get(int.class));
		assertEquals(0, json.set("-0").get(int.class));
		assertEquals(100, json.set(new BigDecimal("100")).get(int.class));
		assertEquals(100, json.set(new BigDecimal("100.00")).get(int.class));
		assertEquals(100, json.set("100").get(int.class));
		assertEquals(100, json.set("+100").get(int.class));
		assertEquals(0xFF, json.set("0xFF").get(int.class));
		assertEquals(0xFF, json.set("+0xFF").get(int.class));
		try {
			json.set(new BigDecimal("100.01")).get(int.class);
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}
		
		// Integer
		assertEquals(null, json.set(null).get(Integer.class));
		assertEquals(0, json.set("0").get(Integer.class));
		assertEquals(0, json.set("+0").get(Integer.class));
		assertEquals(0, json.set("-0").get(Integer.class));
		assertEquals(100, json.set(new BigDecimal("100")).get(Integer.class));
		assertEquals(100, json.set(new BigDecimal("100.00")).get(Integer.class));
		assertEquals(100, json.set("100").get(Integer.class));
		assertEquals(100, json.set("+100").get(Integer.class));
		assertEquals(0xFF, json.set("0xFF").get(Integer.class));
		assertEquals(0xFF, json.set("+0xFF").get(Integer.class));
		try {
			json.set(new BigDecimal("100.01")).get(Integer.class);
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}
		
		// long
		assertEquals(0l, json.set(null).get(long.class));
		assertEquals(0l, json.set("0").get(long.class));
		assertEquals(0l, json.set("+0").get(long.class));
		assertEquals(0l, json.set("-0").get(long.class));
		assertEquals(100l, json.set(new BigDecimal("100")).get(long.class));
		assertEquals(100l, json.set(new BigDecimal("100.00")).get(long.class));
		assertEquals(100l, json.set("100").get(long.class));
		assertEquals(100l, json.set("+100").get(long.class));
		assertEquals((long)0xFF, json.set("0xFF").get(long.class));
		assertEquals((long)0xFF, json.set("+0xFF").get(long.class));
		try {
			json.set(new BigDecimal("100.01")).get(long.class);
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}
		
		// Long
		assertEquals(null, json.set(null).get(Long.class));
		assertEquals(0l, json.set("0").get(Long.class));
		assertEquals(0l, json.set("+0").get(Long.class));
		assertEquals(0l, json.set("-0").get(Long.class));
		assertEquals(100l, json.set(new BigDecimal("100")).get(Long.class));
		assertEquals(100l, json.set(new BigDecimal("100.00")).get(Long.class));
		assertEquals(100l, json.set("100").get(Long.class));
		assertEquals(100l, json.set("+100").get(Long.class));
		assertEquals((long)0xFF, json.set("0xFF").get(Long.class));
		assertEquals((long)0xFF, json.set("+0xFF").get(Long.class));
		try {
			json.set(new BigDecimal("100.01")).get(Long.class);
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}
		
		// BigInteger
		assertEquals(null, json.set(null).get(BigInteger.class));
		assertEquals(new BigInteger("100"), json.set(new BigDecimal("100")).get(BigInteger.class));
		assertEquals(new BigInteger("100"), json.set(new BigDecimal("100.00")).get(BigInteger.class));
		assertEquals(new BigInteger("100"), json.set("100").get(BigInteger.class));
		assertEquals(new BigInteger("100"), json.set("+100").get(BigInteger.class));
		assertEquals(new BigInteger("FF", 16), json.set("0xFF").get(BigInteger.class));
		assertEquals(new BigInteger("FF", 16), json.set("+0xFF").get(BigInteger.class));
		try {
			json.set(new BigDecimal("100.01")).get(BigInteger.class);
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}
		
		// BigDecimal
		assertEquals(null, json.set(null).get(BigDecimal.class));
		assertEquals(new BigDecimal("100"), json.set(new BigDecimal("100")).get(BigDecimal.class));
		assertEquals(new BigDecimal("100"), json.set(new BigDecimal("100.00")).get(BigDecimal.class));
		assertEquals(new BigDecimal("100"), json.set("100").get(BigDecimal.class));
		assertEquals(new BigDecimal("100"), json.set("+100").get(BigDecimal.class));
		assertEquals(new BigDecimal("100.01"), json.set("100.01").get(BigDecimal.class));
		
		// Date
		assertEquals(toDate(1, 1, 1, 0, 0, 0, 0), json.convertChild('$', "1", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 0, 0, 0, 0), json.convertChild('$', "00", Date.class, Date.class));
		assertEquals(toDate(1, 1, 1, 0, 0, 0, 0), json.convertChild('$', "001", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 0, 0, 0, 0), json.convertChild('$', "2000", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 0, 0, 0, 0), json.convertChild('$', "200001", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 0, 0, 0, 0), json.convertChild('$', "20000101", Date.class, Date.class));
		
		assertEquals(toDate(2000, 1, 1, 12, 0, 0, 0), json.convertChild('$', "2000010112", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 0, 0), json.convertChild('$', "200001011205", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 6, 0), json.convertChild('$', "20000101120506", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 6, 0), json.convertChild('$', "20000101120506+0900", Date.class, Date.class));
		
		assertEquals(toDate(2000, 1, 1, 12, 0, 0, 0), json.convertChild('$', "20000101T12", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 0, 0), json.convertChild('$', "20000101T1205", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 6, 0), json.convertChild('$', "20000101T120506", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 6, 0), json.convertChild('$', "20000101T120506+0900", Date.class, Date.class));
		
		assertEquals(toDate(2000, 1, 1, 0, 0, 0, 0), json.convertChild('$', "2000-01", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 0, 0, 0, 0), json.convertChild('$', "2000-01-01", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 0, 0, 0), json.convertChild('$', "2000-01-01T12", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 0, 0), json.convertChild('$', "2000-01-01T12:05", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 0, 0), json.convertChild('$', "2000-01-01T12:05+09:00", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 6, 0), json.convertChild('$', "2000-01-01T12:05:06", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 6, 0), json.convertChild('$', "2000-01-01T12:05:06+09:00", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 6, 100), json.convertChild('$', "2000-01-01T12:05:06.100", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 6, 100), json.convertChild('$', "2000-01-01T12:05:06.100+09:00", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 0, 0, 0, 0), json.convertChild('$', "2000年1月1日", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 0, 0, 0, 0), json.convertChild('$', "2000年1月1日(月)", Date.class, Date.class));
		
		assertEquals(toDate(2007, 12, 24, 20, 13, 15, 0), json.convertChild('$', "Mon Dec 24 2007 20:13:15", Date.class, Date.class));
		assertEquals(toDate(2007, 12, 24, 20, 13, 15, 0), json.convertChild('$', "Mon Dec 24 2007 20:13:15 GMT+0900", Date.class, Date.class));
		assertEquals(toDate(2007, 12, 24, 20, 13, 15, 0), json.convertChild('$', "Mon, 24 Dec 2007 11:13:15 GMT", Date.class, Date.class));
		assertEquals(toDate(2007, 12, 24, 20, 13, 54, 0), json.convertChild('$', "Mon Dec 24 20:13:54 UTC+0900 2007", Date.class, Date.class));
		assertEquals(toDate(2007, 12, 24, 20, 13, 54, 0), json.convertChild('$', "Mon, 24 Dec 2007 11:13:54 UTC", Date.class, Date.class));

		long t = toDate(2007, 12, 24, 20, 13, 15, 0).getTime();
		assertEquals(new java.sql.Date(t), json.convertChild('$', "Mon Dec 24 2007 20:13:15", java.sql.Date.class, java.sql.Date.class));
		assertEquals(new Timestamp(t), json.convertChild('$', "Mon Dec 24 2007 20:13:15", Timestamp.class, Timestamp.class));
		t = toDate(1970, 1, 1, 20, 13, 15, 0).getTime();
		assertEquals(new Time(t), json.convertChild('$', "20:13:15", Time.class, Time.class));
		assertEquals(TimeZone.getTimeZone("JST"), json.convertChild('$', "JST", TimeZone.class, TimeZone.class));
		
		assertEquals(ExampleEnum.Example1, json.convertChild('$', "Example1", ExampleEnum.class, ExampleEnum.class));
		assertEquals(ExampleEnum.Example1, json.convertChild('$', 1, ExampleEnum.class, ExampleEnum.class));
				
		try {
			json.convertChild('$', 5, ExampleEnum.class, ExampleEnum.class);
			fail();		
		} catch (JSONConvertException e) {
			System.out.println(e);
			assertNotNull(e);
		}
		
		try {
			json.convertChild('$', "aaa", int.class, int.class);
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}
		
		try {
			Object test = new Object() {
				public int aaa;
			};
			
			Map map = new LinkedHashMap();
			map.put("aaa", "aaa");
			json.convertChild('$', map, test.getClass(), test.getClass());
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}
		
		// URI
		assertEquals(new URI("http://www.google.co.jp"), json.set("http://www.google.co.jp").get(URI.class));
		assertEquals(new URI("/aaa/bbb.json"), json.set("/aaa/bbb.json").get(URI.class));
		
		// URL
		assertEquals(new URL("http://www.google.co.jp"), json.set("http://www.google.co.jp").get(URL.class));
		
		// InetAddress
		assertEquals(InetAddress.getByName("localhost"), json.set("localhost").get(InetAddress.class));
		assertEquals(InetAddress.getByName("127.0.0.1"), json.set("127.0.0.1").get(InetAddress.class));
		
		// Charset
		assertEquals(Charset.forName("UTF-8"), json.set("UTF-8").get(Charset.class));		
		
		// object
		try {
			Object test = new Object() {
				public int[] aaa;
			};
			
			Map map = new LinkedHashMap();
			ArrayList list = new ArrayList();
			list.add("aaa");
			map.put("aaa", list);
			json.convertChild('$', map, test.getClass(), test.getClass());
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}
	}
	
	@Test
	public void testBase64() throws Exception {
		JSON json = new JSON();
		
		Random rand = new Random();
		
		for (int i = 0; i < 100; i++) {
			byte[][] input = new byte[1][i];
			rand.nextBytes(input[0]);
			
			byte[][] output = (byte[][])json.load(json.set(input).toString()).get(byte[][].class);
			assertEquals(toHexString(input[0]), toHexString(output[0]));
		}
	}
	
	private Date toDate(int year, int month, int date, int hour, int minute, int second, int msec) {
		Calendar c = Calendar.getInstance();
		c.clear();
		c.set(year, month-1, date, hour, minute, second);
		c.set(Calendar.MILLISECOND, msec);
		return c.getTime();
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
		json.load(this.getClass().getResourceAsStream("KEN_ALL.json"));
		System.out.println("time: " + (System.currentTimeMillis()-start));
		
		start = System.currentTimeMillis();
		json.load(this.getClass().getResourceAsStream("KEN_ALL.json")).get(String[][].class);
		System.out.println("time: " + (System.currentTimeMillis()-start));
	}
	
	public List<List<Object>> t1;
	public Map<Map<String, Object>, List<Object>> t2;
	public List<?> t3;
	public List<? extends List> t4;
	public List<? super List> t5;
	public List[] t6;
	public List<List>[] t7;
	public List<List>[][] t8;
	
	public List<Integer> tx;
	
	@Test
	public void testGetRawType() throws Exception {
		List<List<Object>> xt1 = new ArrayList<List<Object>>();
		List<Object> xt1_1 = new ArrayList<Object>();
		xt1_1.add("a");
		xt1.add(xt1_1);
		
		assertEquals(xt1, JSON.decode("[['a']]", this.getClass().getField("t1").getGenericType()));
		
		Method getRawType = JSON.class.getDeclaredMethod("getRawType", Type.class);
		getRawType.setAccessible(true);
		
		assertEquals(String.class, getRawType.invoke(null, String.class));
		assertEquals(String[].class, getRawType.invoke(null, String[].class));
		assertEquals(List.class, getRawType.invoke(null, this.getClass().getField("t1").getGenericType()));
		assertEquals(Map.class, getRawType.invoke(null, this.getClass().getField("t2").getGenericType()));
		assertEquals(List.class, getRawType.invoke(null, this.getClass().getField("t3").getGenericType()));
		assertEquals(List.class, getRawType.invoke(null, this.getClass().getField("t4").getGenericType()));
		assertEquals(List.class, getRawType.invoke(null, ((ParameterizedType)this.getClass().getField("t4").getGenericType()).getActualTypeArguments()[0]));
		assertEquals(List.class, getRawType.invoke(null, this.getClass().getField("t5").getGenericType()));
		assertEquals(Object.class, getRawType.invoke(null, ((ParameterizedType)this.getClass().getField("t5").getGenericType()).getActualTypeArguments()[0]));
		assertEquals(List[].class, getRawType.invoke(null, this.getClass().getField("t6").getGenericType()));
		assertEquals(List[].class, getRawType.invoke(null, this.getClass().getField("t7").getGenericType()));
		assertEquals(List[][].class, getRawType.invoke(null, this.getClass().getField("t8").getGenericType()));
		
		List<BigDecimal> listA = new ArrayList<BigDecimal>();
		listA.add(new BigDecimal("1"));
		listA.add(new BigDecimal("2"));
		listA.add(new BigDecimal("3"));
		listA.add(new BigDecimal("4"));
		listA.add(new BigDecimal("5"));
		
		List<Integer> listB = new ArrayList<Integer>();
		listB.add(1);
		listB.add(2);
		listB.add(3);
		listB.add(4);
		listB.add(5);
		
		assertEquals(listA, JSON.decode("[1,2,3,4,5]", this.getClass().getField("tx").getType()));
		assertEquals(listB, JSON.decode("[1,2,3,4,5]", this.getClass().getField("tx").getGenericType()));
		
		assertEquals(listA, JSON.decode(new ByteArrayInputStream("[1,2,3,4,5]".getBytes("UTF-8")), this.getClass().getField("tx").getType()));
		assertEquals(listB, JSON.decode(new ByteArrayInputStream("[1,2,3,4,5]".getBytes("UTF-8")), this.getClass().getField("tx").getGenericType()));

		assertEquals(listA, JSON.decode(new StringReader("[1,2,3,4,5]"), this.getClass().getField("tx").getType()));
		assertEquals(listB, JSON.decode(new StringReader("[1,2,3,4,5]"), this.getClass().getField("tx").getGenericType()));
		
		JSON json = new JSON();
		assertEquals(listA, json.load("[1,2,3,4,5]").get(this.getClass().getField("tx").getType()));
		assertEquals(listB, json.load("[1,2,3,4,5]").get(this.getClass().getField("tx").getGenericType()));

		assertEquals(listA, json.load(new ByteArrayInputStream("[1,2,3,4,5]".getBytes("UTF-8"))).get(this.getClass().getField("tx").getType()));
		assertEquals(listB, json.load(new ByteArrayInputStream("[1,2,3,4,5]".getBytes("UTF-8"))).get(this.getClass().getField("tx").getGenericType()));

		assertEquals(listA, json.load(new StringReader("[1,2,3,4,5]")).get(this.getClass().getField("tx").getType()));
		assertEquals(listB, json.load(new StringReader("[1,2,3,4,5]")).get(this.getClass().getField("tx").getGenericType()));
	}
}

@SuppressWarnings({"unchecked","unused"})
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
	
	private TimeZone h;
	public TimeZone getH() { return h; }
	public void setH(TimeZone h) { this.h = h; }

	public Class class_;
	
	private String if_;
	public String getIf() { return if_; }
	public void setIf(String if_) { this.if_ = if_; }
	
	private int x = 10;
	int y = 100;
	protected int z = 1000;
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + a;
		result = prime * result + ((b == null) ? 0 : b.hashCode());
		result = prime * result + (c ? 1231 : 1237);
		result = prime * result + ((class_ == null) ? 0 : class_.hashCode());
		result = prime * result + ((d == null) ? 0 : d.hashCode());
		result = prime * result + ((e == null) ? 0 : e.hashCode());
		result = prime * result + ((f == null) ? 0 : f.hashCode());
		result = prime * result + ((g == null) ? 0 : g.pattern().hashCode());
		result = prime * result + ((h == null) ? 0 : h.hashCode());
		result = prime * result + ((if_ == null) ? 0 : if_.hashCode());
		result = prime * result + x;
		result = prime * result + y;
		result = prime * result + z;
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
		if (class_ == null) {
			if (other.class_ != null)
				return false;
		} else if (!class_.equals(other.class_))
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
		if (g == null) {
			if (other.g != null)
				return false;
		} else if (!g.pattern().equals(other.g.pattern()))
			return false;
		if (h == null) {
			if (other.h != null)
				return false;
		} else if (!h.equals(other.h))
			return false;
		if (if_ == null) {
			if (other.if_ != null)
				return false;
		} else if (!if_.equals(other.if_))
			return false;
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		if (z != other.z)
			return false;
		return true;
	}
	
	public String toString() {
		return JSON.encode(this);
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
	
	public String toString() {
		return JSON.encode(this);
	}
}

class GenericsBean {
	private List<String> list = null;
	private Map<String, String> map = null;
	private List<List<String>> glist = null;
	public Map<String, Integer> map2 = null;
	public Map<Integer, String> map3 = null;
	
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
		result = PRIME * result + ((map2 == null) ? 0 : map2.hashCode());
		result = PRIME * result + ((map3 == null) ? 0 : map3.hashCode());
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
		if (map2 == null) {
			if (other.map2 != null)
				return false;
		} else if (!map2.equals(other.map2))
			return false;
		if (map3 == null) {
			if (other.map3 != null)
				return false;
		} else if (!map3.equals(other.map3))
			return false;
		return true;
	}
	
	public String toString() {
		return JSON.encode(this);
	}
}

class InheritedBean {
	public Map<String, Object> map0;
	public LinkedHashMap map1;
	public SuperLinkedHashMap map2;
	public List<Object> list0;
	public ArrayList list1;
	public SuperArrayList list2;
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((list0 == null) ? 0 : list0.hashCode());
		result = prime * result + ((list1 == null) ? 0 : list1.hashCode());
		result = prime * result + ((list2 == null) ? 0 : list2.hashCode());
		result = prime * result + ((map0 == null) ? 0 : map0.hashCode());
		result = prime * result + ((map1 == null) ? 0 : map1.hashCode());
		result = prime * result + ((map2 == null) ? 0 : map2.hashCode());
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
		final InheritedBean other = (InheritedBean) obj;
		if (list0 == null) {
			if (other.list0 != null)
				return false;
		} else if (!list0.equals(other.list0))
			return false;
		if (list1 == null) {
			if (other.list1 != null)
				return false;
		} else if (!list1.equals(other.list1))
			return false;
		if (list2 == null) {
			if (other.list2 != null)
				return false;
		} else if (!list2.equals(other.list2))
			return false;
		if (map0 == null) {
			if (other.map0 != null)
				return false;
		} else if (!map0.equals(other.map0))
			return false;
		if (map1 == null) {
			if (other.map1 != null)
				return false;
		} else if (!map1.equals(other.map1))
			return false;
		if (map2 == null) {
			if (other.map2 != null)
				return false;
		} else if (!map2.equals(other.map2))
			return false;
		return true;
	}
}

class SuperLinkedHashMap extends LinkedHashMap {
	private static final long serialVersionUID = 1L;
}

class SuperArrayList extends ArrayList {
	private static final long serialVersionUID = 1L;
}

enum ExampleEnum {
	Example0, Example1, Example2
}
