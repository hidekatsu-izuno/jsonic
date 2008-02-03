package net.arnx.jsonic;

import static org.junit.Assert.*;

import java.lang.reflect.Type;
import java.math.BigDecimal;
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
		assertEquals("{\"a\":100,\"b\":null,\"c\":false,\"class_\":null,\"d\":null,\"e\":\"en\",\"f\":null,\"g\":null,\"h\":null,\"if\":null}", JSON.encode(test));

		Document doc = DocumentBuilderFactory
			.newInstance()
			.newDocumentBuilder()
			.parse(this.getClass().getResourceAsStream("Sample.xml"));
		assertEquals("{\"tagName\":\"html\",\"@lang\":\"ja\",\"@xmlns:hoge\":\"aaa\",\"childNodes\":[\"\\n\",{\"tagName\":\"head\",\"childNodes\":[\"\\n\\t\",{\"tagName\":\"title\",\"childNodes\":[\"タイトル\"]},\"\\n\"]},\"\\n\",{\"tagName\":\"body\",\"childNodes\":[\"\\n\\t本文\\n\\t\",{\"tagName\":\"p\",\"childNodes\":[\"サンプル1\"]},\"\\n\\t\",{\"tagName\":\"p\",\"childNodes\":[\"サンプル2\"]},\"\\n\\t本文\\n\\t\",{\"tagName\":\"hoge:p\",\"@hoge:x\":\"aaa\",\"childNodes\":[\"サンプル3\"]},\"\\n\"]},\"\\n\"]}", JSON.encode(doc));
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
		} catch (IllegalArgumentException e) {
			System.out.println(e);
			assertNotNull(e);
		}
		
		json.setPrettyPrint(false);
		try {
			assertEquals("true", json.format(true, new StringBuilder()).toString());
			fail();
		} catch (IllegalArgumentException e) {
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
		Locale.setDefault(Locale.JAPANESE);
		JSON json = new JSON(this);
		
		try {
			CharSequence cs = null;
			assertEquals(null, json.parse(cs));
			fail();
		} catch (NullPointerException e) {
			System.out.println(e);
			assertNotNull(e);
		}
		
		try {
			Reader reader = null;
			assertEquals(null, json.parse(reader));
			fail();
		} catch (NullPointerException e) {
			System.out.println(e);
			assertNotNull(e);			
		}
		
		assertEquals(new LinkedHashMap(), json.parse(""));
		
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
		
		try {
			assertEquals(list, json.parse("[{'\u006daa': 'bbb'}, [], 1, 'str\\'ing', true, false, null]"));
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}
		
		assertEquals(list, json.parse("[{\u006daa: \"bbb\"}, [], 1, \"str'ing\", true, false, null]"));
		
		assertEquals(list, json.parse("[{\"\u006daa\": \"bbb\"}, [/**/], 1, \"str'ing\", true, false, null]"));

		assertEquals(list, json.parse("[{'\u006Daa': 'bbb'}, [], 1, \"str'ing\", true, false, null]"));
		
		try {
			assertEquals(list, json.parse("[{'\u006daa\": 'bbb'}, [], 1, \"str'ing\", true, false, null]"));
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);			
		}
		
		try {
			assertEquals(list, json.parse("[{\"maa': 'bbb'}, [], 1, \"str'ing\", true, false, null]"));
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertNotNull(e);
		}

		assertEquals(list, json.parse("[{   \t\u006da\u0061   : 'bbb'}, [], 1, \"str'ing\", true, false, null]"));
		
		list.set(0, new HashMap() {
			{
				put("float", "bbb");
			}
		});

		assertEquals(list, json.parse("[{float   : 'bbb'}, [], 1, \"str'ing\", true, false, null]"));
		
		list.set(0, new HashMap() {
			{
				put("0float", "bbb");
			}
		});

		try {
			assertEquals(list, json.parse("[{0float   : 'bbb'}, [], 1, \"str'ing\", true, false, null]"));
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
		assertEquals(list, json.parse("[{float0   : 'bbb'}, [], 1, \"str'ing\", true, false, null]"));
		
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
				+ " [/*#\n x\r */], 1, \"str\\'in\\g\",/*\n x\r */ true/*\n x\r */, false, null/*\n x\r */] /*\n x\r */ #  aaaa"));
		
		NamedBean nb = new NamedBean();
		nb.namedPropertyAaa = 100;
		assertEquals(nb, json.parse("{\"namedPropertyAaa\":100}", NamedBean.class));
		assertEquals(nb, json.parse("{\"named property aaa\":100}", NamedBean.class));
		assertEquals(nb, json.parse("{\"named_property_aaa\":100}", NamedBean.class));
		assertEquals(nb, json.parse("{\"Named Property Aaa\":100}", NamedBean.class));
		
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
		assertEquals(map1, json.parse("map: {string: string_aaa  \t \nint:100}\n list:[ string, 100]"));
		assertEquals(map1, json.parse("map {string: string_aaa  \t \nint:100}\n list:[ string\n 100]"));
		assertEquals(map1, json.parse("\"map\" {string: string_aaa  \t \nint:100}\n list:[ string\n 100]"));
		assertEquals(map1, json.parse("'map' {string: string_aaa  \t \nint:100}\n list:[ string\n 100]"));

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
		
		assertEquals(map2, json.parse("emap:{}, map: {string: , int:}, elist:[],list: [,string, ]"));
		//assertEquals(map, json.parse("emap:{}\n\n map: {string: \n int:}, elist:[]\nlist: [,string, ]"));
		
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
		assertEquals(map3, json.parse("# database settings\ndatabase {\n  description: 'ms sql server\n\tconnecter settings'\n  user: sa\n  password:"
				+ " xxxx // you need to replace your password.\n}\n/* {\"database\": {\"description\": \"ms sql server\", \"user\": \"sa\", \"password\": \"xxxx\"}} */\n"));

		List list2 = new ArrayList();
		list2.add("あいうえお");
		
		assertEquals(list2, json.parse(this.getClass().getResourceAsStream("UTF-8.json")));
		assertEquals(list2, json.parse(this.getClass().getResourceAsStream("UTF-16BE.json")));
		assertEquals(list2, json.parse(this.getClass().getResourceAsStream("UTF-16LE.json")));
		assertEquals(list2, json.parse(this.getClass().getResourceAsStream("UTF-32BE.json")));
		assertEquals(list2, json.parse(this.getClass().getResourceAsStream("UTF-32LE.json")));
		
		assertEquals(list2, json.parse(this.getClass().getResourceAsStream("UTF-8_BOM.json")));
		assertEquals(list2, json.parse(this.getClass().getResourceAsStream("UTF-16BE_BOM.json")));
		assertEquals(list2, json.parse(this.getClass().getResourceAsStream("UTF-16LE_BOM.json")));
		assertEquals(list2, json.parse(this.getClass().getResourceAsStream("UTF-32BE_BOM.json")));
		assertEquals(list2, json.parse(this.getClass().getResourceAsStream("UTF-32LE_BOM.json")));
	}

	@Test
	@SuppressWarnings({ "unchecked", "serial", "unused" })
	public void testConvert() throws Exception {
		final int[] count = new int[1];
		count[0] = 0;
		
		JSON json = new JSON() {
			protected void handleConvertError(Object key, Object value, Class c, Type type, Exception e) throws Exception {
				count[0]++;
				throw e;
			}
		};
		
		// boolean
		assertEquals(Boolean.TRUE, json.convert(null, 100, boolean.class, boolean.class));
		assertEquals(Boolean.FALSE, json.convert(null, 0, boolean.class, boolean.class));
		assertEquals(Boolean.FALSE, json.convert(null, "off", boolean.class, boolean.class));
		assertEquals(Boolean.FALSE, json.convert(null, "no", boolean.class, boolean.class));
		assertEquals(Boolean.FALSE, json.convert(null, "NaN", boolean.class, boolean.class));
		assertEquals(Boolean.FALSE, json.convert(null, "false", boolean.class, boolean.class));
		assertEquals(Boolean.FALSE, json.convert(null, "", boolean.class, boolean.class));
		assertEquals(Boolean.FALSE, json.convert(null, null, boolean.class, boolean.class));
		
		// Boolean
		assertEquals(Boolean.TRUE, json.convert(null, 100, Boolean.class, Boolean.class));
		assertEquals(Boolean.FALSE, json.convert(null, 0, Boolean.class, Boolean.class));
		assertEquals(Boolean.FALSE, json.convert(null, "off", Boolean.class, Boolean.class));
		assertEquals(Boolean.FALSE, json.convert(null, "no", Boolean.class, Boolean.class));
		assertEquals(Boolean.FALSE, json.convert(null, "NaN", Boolean.class, Boolean.class));
		assertEquals(Boolean.FALSE, json.convert(null, "false", Boolean.class, Boolean.class));
		assertEquals(Boolean.FALSE, json.convert(null, "", Boolean.class, Boolean.class));
		assertEquals(null, json.convert(null, null, Boolean.class, Boolean.class));
		
		// Date
		assertEquals(toDate(1, 1, 1, 0, 0, 0, 0), json.convert(null, "1", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 0, 0, 0, 0), json.convert(null, "00", Date.class, Date.class));
		assertEquals(toDate(1, 1, 1, 0, 0, 0, 0), json.convert(null, "001", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 0, 0, 0, 0), json.convert(null, "2000", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 0, 0, 0, 0), json.convert(null, "200001", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 0, 0, 0, 0), json.convert(null, "20000101", Date.class, Date.class));
		
		assertEquals(toDate(2000, 1, 1, 12, 0, 0, 0), json.convert(null, "2000010112", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 0, 0), json.convert(null, "200001011205", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 6, 0), json.convert(null, "20000101120506", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 6, 0), json.convert(null, "20000101120506+0900", Date.class, Date.class));
		
		assertEquals(toDate(2000, 1, 1, 12, 0, 0, 0), json.convert(null, "20000101T12", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 0, 0), json.convert(null, "20000101T1205", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 6, 0), json.convert(null, "20000101T120506", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 6, 0), json.convert(null, "20000101T120506+0900", Date.class, Date.class));
		
		assertEquals(toDate(2000, 1, 1, 0, 0, 0, 0), json.convert(null, "2000-01", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 0, 0, 0, 0), json.convert(null, "2000-01-01", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 0, 0, 0), json.convert(null, "2000-01-01T12", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 0, 0), json.convert(null, "2000-01-01T12:05", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 0, 0), json.convert(null, "2000-01-01T12:05+09:00", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 6, 0), json.convert(null, "2000-01-01T12:05:06", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 6, 0), json.convert(null, "2000-01-01T12:05:06+09:00", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 6, 100), json.convert(null, "2000-01-01T12:05:06.100", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 12, 5, 6, 100), json.convert(null, "2000-01-01T12:05:06.100+09:00", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 0, 0, 0, 0), json.convert(null, "2000年1月1日", Date.class, Date.class));
		assertEquals(toDate(2000, 1, 1, 0, 0, 0, 0), json.convert(null, "2000年1月1日(月)", Date.class, Date.class));
		
		assertEquals(toDate(2007, 12, 24, 20, 13, 15, 0), json.convert(null, "Mon Dec 24 2007 20:13:15", Date.class, Date.class));
		assertEquals(toDate(2007, 12, 24, 20, 13, 15, 0), json.convert(null, "Mon Dec 24 2007 20:13:15 GMT+0900", Date.class, Date.class));
		assertEquals(toDate(2007, 12, 24, 20, 13, 15, 0), json.convert(null, "Mon, 24 Dec 2007 11:13:15 GMT", Date.class, Date.class));
		assertEquals(toDate(2007, 12, 24, 20, 13, 54, 0), json.convert(null, "Mon Dec 24 20:13:54 UTC+0900 2007", Date.class, Date.class));
		assertEquals(toDate(2007, 12, 24, 20, 13, 54, 0), json.convert(null, "Mon, 24 Dec 2007 11:13:54 UTC", Date.class, Date.class));

		long t = toDate(2007, 12, 24, 20, 13, 15, 0).getTime();
		assertEquals(new java.sql.Date(t), json.convert(null, "Mon Dec 24 2007 20:13:15", java.sql.Date.class, java.sql.Date.class));
		assertEquals(new Timestamp(t), json.convert(null, "Mon Dec 24 2007 20:13:15", Timestamp.class, Timestamp.class));
		t = toDate(1970, 1, 1, 20, 13, 15, 0).getTime();
		assertEquals(new Time(t), json.convert(null, "20:13:15", Time.class, Time.class));
		assertEquals(TimeZone.getTimeZone("JST"), json.convert(null, "JST", TimeZone.class, TimeZone.class));
		
		try {
			json.convert(null, "aaa", int.class, int.class);
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertEquals(1, count[0]);			
		}
		
		count[0] = 0;
		try {
			Object test = new Object() {
				public int aaa;
			};
			
			json.setContext(this);
			Map map = new LinkedHashMap();
			map.put("aaa", "aaa");
			json.convert(null, map, test.getClass(), test.getClass());
			fail();
		} catch (Exception e) {
			System.out.println(e);
			assertEquals(2, count[0]);			
		}
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
	
	public String toString() {
		return JSON.encode(this);
	}
}