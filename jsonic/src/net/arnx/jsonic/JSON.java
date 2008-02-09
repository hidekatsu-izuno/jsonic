/*
 * Copyright 2007-2008 Hidekatsu Izuno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package net.arnx.jsonic;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Flushable;
import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.ResourceBundle;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.Date;
import java.util.Calendar;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * <p>The JSONIC JSON class provides JSON encoding and decoding as 
 * defined by RFC 4627.</p>
 * 
 * <p>The following example illustrates how to encode and decode. The code:</p>
 * <pre>
 * // encodes a object into a json string.
 * String s = JSON.encode(o);
 * 
 * // decodes a json string into a object.
 * Object o = JSON.decode(s);
 * 
 * // decodes a json string into a typed object.
 * Foo foo = JSON.decode(s, Foo.class);
 * </pre>
 * 
 * <p>Advanced topic:</p>
 * <pre>
 * // formats a object into a json string with indents for debug.
 * JSON json = new JSON();
 * json.setPrettyPrint(true);
 * String pretty = json.format(o);
 * 
 * //uses Reader/InputStream
 * Bar bar = (new JSON()).parse(new FileInputStream("bar.json"), Bar.class);
 * Bar bar = (new JSON()).parse(new FileReader("bar.json"), Bar.class);
 * </pre>
 * 
 * <h4>Summary of encoding rules for java type into json type</h4>
 * <table border="1" cellpadding="1" cellspacing="0">
 * <tr>
 * 	<th bgcolor="#CCCCFF" align="left">java type</th>
 * 	<th bgcolor="#CCCCFF" align="left">json type</th>
 * </tr>
 * <tr><td>java.util.Map</td><td rowspan="2">object</td></tr>
 * <tr><td>java.lang.Object (public property or field)</td></tr>
 * <tr><td>java.lang.Object[]</td><td rowspan="3">array</td></tr>
 * <tr><td>java.util.Collection</td></tr>
 * <tr><td>boolean[], short[], int[], long[], float[], double[]</td></tr>
 * <tr><td>java.lang.CharSequence</td><td rowspan="8">string</td></tr>
 * <tr><td>char[]</td></tr>
 * <tr><td>java.lang.Character</td></tr>
 * <tr><td>char</td></tr>
 * <tr><td>java.util.TimeZone</td></tr>
 * <tr><td>java.util.regex.Pattern</td></tr>
 * <tr><td>java.lang.reflect.Type</td></tr>
 * <tr><td>java.lang.reflect.Member</td></tr>
 * <tr><td>byte[]</td><td>string (base64)</td></tr>
 * <tr><td>java.util.Locale</td><td>string (language-country)</td></tr>
 * <tr><td>java.lang.Number</td><td rowspan="2">number</td></tr>
 * <tr><td>byte, short, int, long, float, double</td></tr>
 * <tr><td>java.util.Date</td><td rowspan="2">number (milliseconds since 1970)</td></tr>
 * <tr><td>java.util.Calendar</td></tr>
 * <tr><td>java.lang.Boolean</td><td rowspan="2">true/false</td></tr>
 * <tr><td>boolean</td></tr>
 * <tr><td>null</td><td>null</td></tr>
 * </table>
 * 
 * <h4>Summary of decoding rules for json type into java type</h4>
 * <table border="1" cellpadding="1" cellspacing="0">
 * <tr>
 * 	<th bgcolor="#CCCCFF" align="left">json type</th>
 * 	<th bgcolor="#CCCCFF" align="left">java type</th>
 * </tr>
 * <tr><td>object</td><td>java.util.HashMap</td></tr>
 * <tr><td>array</td><td>java.util.ArrayList</td></tr>
 * <tr><td>string</td><td>java.lang.String</td></tr>
 * <tr><td>number</td><td>java.math.BigDecimal</td></tr>
 * <tr><td>true/false</td><td>java.lang.Boolean</td></tr>
 * <tr><td>null</td><td>null</td></tr>
 * </table>
 * 
 * @author Hidekatsu Izuno
 * @version 0.9.6
 * @see <a href="http://www.rfc-editor.org/rfc/rfc4627.txt">RFC 4627</a>
 * @see <a href="http://www.apache.org/licenses/LICENSE-2.0">the Apache License, Version 2.0</a>
 */
@SuppressWarnings({"unchecked"})
public class JSON {	
	public JSON() {
		this(null);
	}
	
	public JSON(Object context) {
		setContext(context);
	}
	
	boolean prettyPrint = false;
	
	/**
	 * Output json string is to human-readable format.
	 * 
	 * @param value true to format human-readable, false to shorten.
	 */
	public void setPrettyPrint(boolean value) {
		this.prettyPrint = value;
	}
	
	int maxDepth = 32;
	
	/**
	 * Sets maximum depth for the nest level.
	 * default value is 32.
	 * 
	 * @param value maximum depth for the nest level.
	 */
	public void setMaxDepth(int value) {
		if (value <= 0) {
			throw new IllegalArgumentException(getMessage("json.TooSmallArgumentError", "maxDepth", 0));
		}
		this.maxDepth = value;
	}
	
	Class contextClass = null;
	Object context = null;
	
	/**
	 * Sets context for inner class.
	 * 
	 * @param value context object or class
	 */
	public void setContext(Object value) {
		if (value instanceof Class) {
			this.context = null;
			this.contextClass = (Class)value;
		} else {
			this.context = value;
			this.contextClass = (value != null) ? value.getClass() : null;
		}
	}

	Locale locale;
	
	/**
	 * Sets locale for conversion or message.
	 * 
	 * @param locale
	 */
	public void setLocale(Locale locale) {
		if (locale == null) {
			throw new NullPointerException();
		}
		this.locale = locale;
	}
	
	/**
	 * Encodes a object into a json string.
	 * 
	 * @param source a object to encode.
	 * @return a json string
	 */
	public static String encode(Object source) {
		return encode(source, false);
	}
	
	/**
	 * Encodes a object into a json string.
	 * 
	 * @param source a object to encode.
	 * @param prettyPrint output a json string with indent, space or break.
	 * @return a json string
	 */
	public static String encode(Object source, boolean prettyPrint) {
		String value = null;
		
		JSON json = new JSON(source);
		json.setPrettyPrint(prettyPrint);
		try {
			value = json.format(source, new StringBuilder()).toString();
		} catch (IOException e) {
			// never happen
		}
		
		return value;
	}
	
	/**
	 * Decodes a json string into a object.
	 * 
	 * @param source a json string to decode
	 * @return a decoded object
	 * @exception JSONParseException if the beginning of the specified string cannot be parsed.
	 */
	public static Object decode(String source) throws JSONParseException {
		return (new JSON()).parse(source);
	}
	
	/**
	 * Decodes a json string into a typed object.
	 * 
	 * @param source a json string to decode
	 * @param c class for converting
	 * @return a decoded object
	 * @exception JSONParseException if the beginning of the specified string cannot be parsed.
	 * @exception JSONConvertException if it cannot convert a class from a JSON value.
	 */
	public static <T> T decode(String source, Class<? extends T> c) 
		throws JSONParseException, JSONConvertException {
		Class context = c.getEnclosingClass();
		if (context == null) context = c;
		return (new JSON(context)).parse(source, c);
	}
	
	/**
	 * Decodes a json string into a typed object.
	 * 
	 * @param source a json string to decode
	 * @param c class for converting
	 * @param t type specified generics parameters
	 * @return a decoded object
	 * @exception JSONParseException if the beginning of the specified string cannot be parsed.
	 * @exception JSONConvertException if it cannot convert a class from a JSON value.
	 */
	public static <T> T decode(String source, Class<? extends T> c, Type t)
	throws JSONParseException, JSONConvertException {
		Class context = c.getEnclosingClass();
		if (context == null) context = c;
		return (new JSON(context)).parse(source, c, t);
	}
	
	public String format(Object source) throws IOException {
		return format(source, new StringBuilder(1000)).toString();
	}
	
	public Appendable format(Object source, Appendable ap) throws IOException {
		return format(source, ap, 0);
	}
	
	private Appendable format(Object o, Appendable ap, int level) throws IOException {
		if (level > this.maxDepth) {
			o = null;
		}
		
		boolean escape = true;
		if (o instanceof Class) {
			o = ((Class)o).getName();
		} else if (o instanceof Character || o instanceof Type || o instanceof Member) {
			o = o.toString();
		} else if (o instanceof char[]) {
			o = new String((char[])o);
		} else if (o instanceof byte[]) {
			escape = false;
			o = encodeBase64((byte[])o);
		} else if (o instanceof Object[]) {
			o = Arrays.asList((Object[])o);
		} else if (o instanceof Pattern) {
			o = ((Pattern)o).pattern();
		} else if (o instanceof TimeZone) {
			o = ((TimeZone)o).getID();
		} else if (o instanceof Date) {
			o = ((Date)o).getTime();
		} else if (o instanceof Calendar) {
			o = ((Calendar)o).getTimeInMillis();
		} else if (o instanceof Locale) {
			Locale locale = (Locale)o;
			if (locale.getLanguage() != null && locale.getLanguage().length() > 0) {
				if (locale.getCountry() != null && locale.getCountry().length() > 0) {
					o = locale.getLanguage() + "-" + locale.getCountry();
				} else {
					o = locale.getLanguage();
				}
			} else {
				o = null;
			}
		} else if (o instanceof Node) {
			Element elem = null;
			if (o instanceof Document) {
				elem = ((Document)o).getDocumentElement();
			} else if (o instanceof Element) {
				elem = (Element)o;
			}
			
			if (elem != null) {
				Map<String, Object> map = new LinkedHashMap<String, Object>();
				map.put("tagName", elem.getTagName());
				if (elem.hasAttributes()) {
					NamedNodeMap nmap = elem.getAttributes();
					Map<String, String> attrs = new LinkedHashMap<String, String>();
					for (int i = 0; i < nmap.getLength(); i++) {
						Node node = nmap.item(i);
						if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
							attrs.put(node.getNodeName(), node.getNodeValue());
						}
					}
					map.put("attributes", attrs);
				}
				if (elem.hasChildNodes()) {
					NodeList nlist = elem.getChildNodes();
					List childNodes = new ArrayList(nlist.getLength());
					for (int i = 0; i < nlist.getLength(); i++) {
						Node node = nlist.item(i);
						if (node.getNodeType() == Node.ELEMENT_NODE) {
							childNodes.add((Element)node);
						} else if (node.getNodeType() == Node.TEXT_NODE
								|| node.getNodeType() == Node.CDATA_SECTION_NODE) {
							childNodes.add(((CharacterData)node).getData());
						}
					}
					map.put("childNodes", childNodes);
				}
				o = map;
			}
		}
		
		if (level == 0 && (o == null
				|| o instanceof CharSequence
				|| o instanceof Boolean
				|| o instanceof Number
				|| o instanceof Date)) {
			throw new IllegalArgumentException(getMessage("json.format.IllegalRootTypeError"));
		}
		
		if (o == null) {
			ap.append("null");
		} else if (o instanceof CharSequence) {
			if (escape) {
				formatString((CharSequence)o, ap);
			} else {
				ap.append('"').append((CharSequence)o).append('"');
			}
		} else if (o instanceof Double || o instanceof Float) {
			double d = ((Number)o).doubleValue();
			if (Double.isNaN(d) || Double.isInfinite(d)) {
				ap.append('"').append(o.toString()).append('"');
			} else {
				ap.append(o.toString());
			}
		} else if (o instanceof Byte) {
			ap.append(Integer.toString(((Byte)o).byteValue() & 0xFF));
		} else if (o instanceof Number || o instanceof Boolean) {
			ap.append(o.toString());
		} else if (o.getClass().isArray()) {
			ap.append('[');
			if (o instanceof boolean[]) {
				boolean[] array = (boolean[])o;
				for (int i = 0; i < array.length; i++) {
					ap.append(String.valueOf(array[i]));
					if (i != array.length-1) {
						ap.append(',');
						if (this.prettyPrint) ap.append(' ');
					}
				}
			} else if (o instanceof short[]) {
				short[] array = (short[])o;
				for (int i = 0; i < array.length; i++) {
					ap.append(String.valueOf(array[i]));
					if (i != array.length-1) {
						ap.append(',');
						if (this.prettyPrint) ap.append(' ');
					}
				}
			} else if (o instanceof int[]) {
				int[] array = (int[])o;
				for (int i = 0; i < array.length; i++) {
					ap.append(String.valueOf(array[i]));
					if (i != array.length-1) {
						ap.append(',');
						if (this.prettyPrint) ap.append(' ');
					}
				}
			} else if (o instanceof long[]) {
				long[] array = (long[])o;
				for (int i = 0; i < array.length; i++) {
					ap.append(String.valueOf(array[i]));
					if (i != array.length-1) {
						ap.append(',');
						if (this.prettyPrint) ap.append(' ');
					}
				}
			} else if (o instanceof float[]) {
				float[] array = (float[])o;
				for (int i = 0; i < array.length; i++) {
					if (Float.isNaN(array[i]) || Float.isInfinite(array[i])) {
						ap.append('"').append(Float.toString(array[i])).append('"');
					} else {
						ap.append(String.valueOf(array[i]));
					}
					if (i != array.length-1) {
						ap.append(',');
						if (this.prettyPrint) ap.append(' ');
					}
				}
			} else if (o instanceof double[]) {
				double[] array = (double[])o;
				for (int i = 0; i < array.length; i++) {
					if (Double.isNaN(array[i]) || Double.isInfinite(array[i])) {
						ap.append('"').append(Double.toString(array[i])).append('"');
					} else {
						ap.append(String.valueOf(array[i]));
					}
					if (i != array.length-1) {
						ap.append(',');
						if (this.prettyPrint) ap.append(' ');
					}
				}
			}
			ap.append(']');
		} else if (o instanceof Collection) {
			Collection collection = (Collection)o;
			ap.append('[');
			for (Iterator i = collection.iterator(); i.hasNext(); ) {
				Object item = i.next();
				if (this.prettyPrint) {
					ap.append('\n');
					for (int j = 0; j < level+1; j++) ap.append('\t');
				}
				if (item == o) item = null;
				format(item, ap, level+1);
				if (i.hasNext()) ap.append(',');
			}
			if (this.prettyPrint && !collection.isEmpty()) {
				ap.append('\n');
				for (int j = 0; j < level; j++) ap.append('\t');
			}
			ap.append(']');
		} else if (o instanceof Map) {
			formatMap((Map)o, o, ap, level);
		} else {
			Class c = o.getClass();
			
			Map<String, Member> props = getProperties(c, false);
			boolean access = tryAccess(c);
			
			Map<String, Object> map = new TreeMap<String, Object>();
			
			for (Map.Entry<String, Member> entry : props.entrySet()) {
				Object value = null;
				try {
					if (entry.getValue() instanceof Method) {
						Method m = (Method)entry.getValue();
						if (access) m.setAccessible(true);
						value = m.invoke(o);
					} else {
						Field f = (Field)entry.getValue();
						if (access) f.setAccessible(true);
						value =  f.get(o);
					}
					map.put(entry.getKey(), value);
				} catch (Exception e) {
					// no handle
				}
			}

			formatMap(map, o, ap, level);
		}
		
		if (ap instanceof Flushable) {
			((Flushable)ap).flush();
		}
		
		return ap;
	}
	
	private Appendable formatMap(Map map, Object o, Appendable ap, int level) throws IOException {
		ap.append('{');
		for (Iterator i = map.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry entry = (Map.Entry)i.next();
			if (entry.getValue() == o) continue; 
			
			if (this.prettyPrint) {
				ap.append('\n');
				for (int j = 0; j < level+1; j++) ap.append('\t');
			}
			formatString((String)entry.getKey(), ap).append(':');
			if (this.prettyPrint) ap.append(' ');
			format(entry.getValue(), ap, level+1);
			if (i.hasNext()) ap.append(',');
		}
		if (this.prettyPrint && !map.isEmpty()) {
			ap.append('\n');
			for (int j = 0; j < level; j++) ap.append('\t');
		}
		ap.append('}');
		return ap;
	}
	
	private Appendable formatString(CharSequence s, Appendable ap) throws IOException {
		ap.append('"');
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
			case '"':
			case '\\': 
				ap.append('\\').append(c);
				break;
			case '\b':
				ap.append("\\b");
				break;
			case '\f':
				ap.append("\\f");
				break;
			case '\n':
				ap.append("\\n");
				break;
			case '\r':
				ap.append("\\r");
				break;
			case '\t':
				ap.append("\\t");
				break;
			default: 
				ap.append(c);
			}
		}
		ap.append('"');
		
		return ap;
	}

	public Object parse(CharSequence cs) throws JSONParseException {
		Object value = null;
		try {
			value = parse(new CharSequenceJSONSource(cs));
		} catch (IOException e) {
			// never occur
		}
		return value;
	}
	
	public <T> T parse(CharSequence s, Class<? extends T> c)
		throws JSONParseException, JSONConvertException {
		T value = null;
		try {
			value = (T)convert(null, parse(new CharSequenceJSONSource(s)), c, c);
		} catch (IOException e) {
			// never occur
		}
		return value;
	}
	
	public <T> T parse(CharSequence s, Class<? extends T> c, Type t)
		throws JSONParseException, JSONConvertException {
		T value = null;
		try {
			value =  (T)convert(null, parse(new CharSequenceJSONSource(s)), c, t);
		} catch (IOException e) {
			// never occur
		}
		return value;			
	}
	
	public Object parse(InputStream in) throws IOException, JSONParseException {
		if (!in.markSupported()) in = new BufferedInputStream(in);
		return parse(new ReaderJSONSource(new InputStreamReader(in, determineEncoding(in))));
	}
	
	public <T> T parse(InputStream in, Class<? extends T> c)
		throws IOException, JSONParseException, JSONConvertException {
		if (!in.markSupported()) in = new BufferedInputStream(in);
		return (T)convert(null, parse(new ReaderJSONSource(new InputStreamReader(in, determineEncoding(in)))), c, c);
	}
	
	public <T> T parse(InputStream in, Class<? extends T> c, Type t)
		throws IOException, JSONParseException, JSONConvertException {
		if (!in.markSupported()) in = new BufferedInputStream(in);
		return (T)convert(null, parse(new ReaderJSONSource(new InputStreamReader(in, determineEncoding(in)))), c, t);
	}
	
	public Object parse(Reader reader) throws IOException, JSONParseException {
		return parse(new ReaderJSONSource(reader));
	}
	
	public <T> T parse(Reader reader, Class<? extends T> c) 
		throws IOException, JSONParseException, JSONConvertException {
		return (T)convert(null, parse(new ReaderJSONSource(reader)), c, c);
	}
	
	public <T> T parse(Reader reader, Class<? extends T> c, Type t)
		throws IOException, JSONParseException, JSONConvertException {
		return (T)convert(null, parse(new ReaderJSONSource(reader)), c, t);
	}
	
	private Object parse(JSONSource s) throws IOException, JSONParseException {
		StringBuilder sb = new StringBuilder(1000);
		
		Object o = null;

		int n = -1;
		while ((n = s.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case '\r':
			case '\n':
			case ' ':
			case '\t':
			case 0xFEFF: // BOM
				break;
			case '[':
				if (o == null) {
					s.back();
					o = parseArray(s, sb, 0);
					break;
				}
				throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
			case '/':
			case '#':
				s.back();
				skipComment(s);
				break;
			default:
				if (o == null) {
					s.back();
					o = parseObject(s, sb, 0);
					break;
				}
				throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
			}
		}
		return (o == null) ? new LinkedHashMap() : o;
	}	
	
	private Map<String, Object> parseObject(JSONSource s, StringBuilder sb, int level) throws IOException, JSONParseException {
		int point = 0; // 0 '{' 1 'key' 2 ':' 3 '\n'? 4 'value' 5 '\n'? 6 ',' ... '}' E
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		String key = null;
		char start = '\0';
		
		int n = -1;
		loop:while ((n = s.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case '\r':
			case '\n':
				if (point == 5) {
					point = 6;
				}
			case ' ':
			case '\t':
			case 0xFEFF: // BOM
				break;
			case '{':
				if (point == 0) {
					start = '{';
					point = 1;
				} else if (point == 2 || point == 3){
					s.back();
					map.put(key, parseObject(s, sb, level+1));
					point = 5;
				} else {
					throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case ':':
				if (point == 2) {
					point = 3;
				} else {
					throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case ',':
				if (point == 3) {
					map.put(key, null);
					point = 1;
				} else if (point == 5 || point == 6) {
					point = 1;
				} else {
					throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '}':
				if (start == '{' && (point == 1 || point == 3 || point == 5 || point == 6)) {
					if (point == 3) {
						map.put(key, null);
					}
					break loop;
				} else {
					throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
			case '\'':
			case '"':
				if (point == 0) {
					s.back();
					point = 1;
				} else if (point == 1 || point == 6) {
					s.back();
					key = parseString(s, sb);
					point = 2;
				} else if (point == 3) {
					s.back();
					map.put(key, parseString(s, sb));
					point = 5;
				} else {
					throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '[':
				if (point == 3) {
					s.back();
					map.put(key, parseArray(s, sb, level+1));
					point = 5;
				} else {
					throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '/':
			case '#':
				s.back();
				skipComment(s);
				break;
			default:
				if (point == 0) {
					s.back();
					point = 1;
				} else if (point == 1 || point == 6) {
					s.back();
					key = parseLiteral(s, sb);
					point = 2;
				} else if (point == 3) {
					if ((c == '-') || (c >= '0' && c <= '9')) {
						s.back();
						map.put(key, parseNumber(s, sb));
					} else {
						s.back();
						String literal = parseLiteral(s, sb);
						if (literal.equals("null")) {
							map.put(key, null);
						} else if (literal.equals("true")) {
							map.put(key, Boolean.TRUE);
						} else if (literal.equals("false")) {
							map.put(key, Boolean.FALSE);
						} else {
							map.put(key, literal);
						}
					}
					point = 5;
				} else {
					throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
			}
		}
		
		if ((n == -1) ? (start != '\0') : (n != '}')) {
			throw new JSONParseException(getMessage("json.parse.ObjectNotClosedError"), s);
		}
		return map;
	}
	
	private List<Object> parseArray(JSONSource s, StringBuilder sb, int level) throws IOException, JSONParseException {
		int point = 0; // 0 '[' 1 'value' 2 '\n'? 3 ',' ... ']' E
		List<Object> list = new ArrayList<Object>();
		
		int n = -1;
		loop:while ((n = s.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case '\r':
			case '\n':
				if (point == 2) {
					point = 3;
				}
			case ' ':
			case '\t':
			case 0xFEFF: // BOM
				break;
			case '[':
				if (point == 0) {
					point = 1;
				} else if (point == 1 || point == 3) {
					s.back();
					list.add(parseArray(s, sb, level+1));
					point = 2;
				} else {
					throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case ',':
				if (point == 1) {
					list.add(null);
				} else if (point == 2 || point == 3) {
					point = 1;
				} else {
					throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case ']':
				if (point == 1) {
					if (!list.isEmpty()) list.add(null);
					break loop;					
				} else if (point == 2 || point == 3) {
					break loop;
				} else {
					throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
			case '{':
				if (point == 1 || point == 3){
					s.back();
					list.add(parseObject(s, sb, level+1));
					point = 2;
				} else {
					throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '\'':
			case '"':
				if (point == 1 || point == 3) {
					s.back();
					list.add(parseString(s, sb));
					point = 2;
				} else {
					throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '/':
			case '#':
				s.back();
				skipComment(s);
				break;
			default:
				if (point == 1 || point == 3) {
					if ((c == '-') || (c >= '0' && c <= '9')) {
						s.back();
						list.add(parseNumber(s, sb));
					} else {
						s.back();
						String literal = parseLiteral(s, sb);
						if (literal.equals("null")) {
							list.add(null);
						} else if (literal.equals("true")) {
							list.add(Boolean.TRUE);
						} else if (literal.equals("false")) {
							list.add(Boolean.FALSE);
						} else {
							list.add(literal);
						}
					}
					point = 2;
				} else {
					throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
			}
		}
		
		if (n != ']') {
			throw new JSONParseException(getMessage("json.parse.ArrayNotClosedError"), s);
		}
		return list;
	}
	
	private String parseString(JSONSource s, StringBuilder sb) throws IOException, JSONParseException {
		int point = 0; // 0 '"|'' 1 'c' ... '"|'' E
		sb.setLength(0);
		char start = '\0';
		
		int n = -1;
		loop:while ((n = s.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case 0xFEFF: // BOM
				break;
			case '\\':
				if (point == 1) {
					if (start == '"') {
						s.back();
						sb.append(parseEscape(s));
					} else {
						sb.append(c);
					}
				} else {
					throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '\'':
			case '"':
				if (point == 0) {
					start = c;
					point = 1;
					break;
				} else if (point == 1 && start == c) {
					break loop;
				}
			default:
				if (point == 1) {
					sb.append(c);
				} else {
					throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
			}
		}
		
		if (n == -1 && n != start) {
			throw new JSONParseException(getMessage("json.parse.StringNotClosedError"), s);
		}
		return sb.toString();
	}
	
	private String parseLiteral(JSONSource s, StringBuilder sb) throws IOException, JSONParseException {
		int point = 0; // 0 'IdStart' 1 'IdPart' ... !'IdPart' E
		sb.setLength(0);
		
		int n = -1;
		loop:while ((n = s.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case 0xFEFF: // BOM
				break;
			case '\\':
				s.back();
				c = parseEscape(s);
			default:
				if (point == 0 && Character.isJavaIdentifierStart(c)) {
					sb.append(c);
					point = 1;
				} else if (point == 1 && Character.isJavaIdentifierPart(c)){
					sb.append(c);
				} else {
					s.back();
					break loop;
				}
			}
		}
		return sb.toString();
	}	
	
	private Number parseNumber(JSONSource s, StringBuilder sb) throws IOException, JSONParseException {
		int point = 0; // 0 '(-)' 1 '0' | ('[1-9]' 2 '[0-9]*') 3 '(.)' 4 '[0-9]' 5 '[0-9]*' 6 'e|E' 7 '[+|-]' 8 '[0-9]' E
		sb.setLength(0);
		
		int n = -1;
		loop:while ((n = s.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case 0xFEFF: // BOM
				break;
			case '+':
				if (point == 7) {
					sb.append(c);
					point = 8;
				} else {
					throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '-':
				if (point == 0) {
					sb.append(c);
					point = 1;
				} else if (point == 7) {
					sb.append(c);
					point = 8;
				} else {
					throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '.':
				if (point == 2 || point == 3) {
					sb.append(c);
					point = 4;
				} else {
					throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case 'e':
			case 'E':
				if (point == 2 || point == 3 || point == 5 || point == 6) {
					sb.append(c);
					point = 7;
				} else {
					throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			default:
				if (c >= '0' && c <= '9') {
					if (point == 0 || point == 1) {
						sb.append(c);
						point = (c == '0') ? 3 : 2;
					} else if (point == 2 || point == 5) {
						sb.append(c);
					} else if (point == 4) {
						sb.append(c);
						point = 5;
					} else if (point == 7 || point == 8) {
						sb.append(c);
						break loop;
					} else {
						throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
				} else if (point == 2 || point == 3 || point == 5 || point == 6) {
					s.back();
					break loop;
				} else {
					throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
			}
		}
		
		return new BigDecimal(sb.toString());
	}
	
	private char parseEscape(JSONSource s) throws IOException, JSONParseException {
		int point = 0; // 0 '\' 1 'u' 2 'x' 3 'x' 4 'x' 5 'x' E
		char escape = '\0';
		
		int n = -1;
		loop:while ((n = s.next()) != -1) {
			char c = (char)n;
			if (c == 0xFEFF) continue; // BOM
			
			if (point == 0) {
				if (c == '\\') {
					point = 1;
				} else {
					throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
			} else if (point == 1) {
				switch(c) {
				case '"':
				case '\\':
				case '/':
					escape = c;
					break loop;
				case 'b':
					escape = '\b';
					break loop;
				case 'f':
					escape = '\f';
					break loop;
				case 'n':
					escape = '\n';
					break loop;
				case 'r':
					escape = '\r';
					break loop;
				case 't':
					escape = '\t';
					break loop;
				case 'u':
					point = 2;
					break;
				default:
					escape = c;
					break loop;
				}
			} else {
				int hex = (c >= '0' && c <= '9') ? c-48 :
					(c >= 'A' && c <= 'F') ? c-65+10 :
					(c >= 'a' && c <= 'f') ? c-97+10 : -1;
				if (hex != -1) {
					escape |= (hex << ((5-point)*8));
					if (point != 5) {
						point++;
					} else {
						break loop;
					}
				} else {
					throw new JSONParseException(getMessage("json.parse.IllegalUnicodeEscape", c), s);
				}
			}
		}
		
		return escape;
	}
	
	private void skipComment(JSONSource s) throws IOException, JSONParseException {
		int point = 0; // 0 '/' 1 '*' 2  '*' 3 '/' E or  0 '/' 1 '/' 4  '\r|\n|\r\n' E
		
		int n = -1;
		loop:while ((n = s.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case 0xFEFF:
				break;
			case '/':
				if (point == 0) {
					point = 1;
				} else if (point == 1) {
					point = 4;
				} else if (point == 3) {
					break loop;
				} else if (!(point == 2 || point == 4)) {
					throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '*':
				if (point == 1) {
					point = 2;
				} else if (point == 2) {
					point = 3;
				} else if (!(point == 3 || point == 4)) {
					throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '\n':
			case '\r':
				if (point == 2 || point == 3) {
					point = 2;
				} else if (point == 4) {
					break loop;
				} else {
					throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '#':
				if (point == 0) {
					point = 4;
					break;
				}
			default:
				if (point == 3) {
					point = 2;
				} else if (!(point == 2 || point == 4)) {
					throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
			}
		}	
	}
	
	private String determineEncoding(InputStream in) throws IOException {
		in.mark(4);
		byte[] check = new byte[4];
		int size = in.read(check);
		String encoding = "UTF-8";
		if (size == 2) {
			if (((check[0] & 0xFF) == 0x00 && (check[1] & 0xFF) != 0x00) 
					|| ((check[0] & 0xFF) == 0xFE && (check[1] & 0xFF) == 0xFF)) {
				encoding = "UTF-16BE";
			} else if (((check[0] & 0xFF) != 0x00 && (check[1] & 0xFF) == 0x00) 
					|| ((check[0] & 0xFF) == 0xFF && (check[1] & 0xFF) == 0xFE)) {
				encoding = "UTF-16LE";
			}
		} else if (size == 4) {
			if (((check[0] & 0xFF) == 0x00 && (check[1] & 0xFF) == 0x00)) {
				encoding = "UTF-32BE";
			} else if (((check[2] & 0xFF) == 0x00 && (check[3] & 0xFF) == 0x00)) {
				encoding = "UTF-32LE";
			} else if (((check[0] & 0xFF) == 0x00 && (check[1] & 0xFF) != 0x00) 
					|| ((check[0] & 0xFF) == 0xFE && (check[1] & 0xFF) == 0xFF)) {
				encoding = "UTF-16BE";
			} else if (((check[0] & 0xFF) != 0x00 && (check[1] & 0xFF) == 0x00) 
					|| ((check[0] & 0xFF) == 0xFF && (check[1] & 0xFF) == 0xFE)) {
				encoding = "UTF-16LE";
			}
		}
		in.reset();
		return encoding;
	}
	
	protected Object convert(Object key, Object value, Class c, Type type) throws JSONConvertException {
		Object data = null;
		
		try {
			if (c.isPrimitive()) {
				if (c.equals(boolean.class)) {
					if (value == null) {
						data = false;
					} else if (value instanceof Boolean) {
						data = value;
					} else if (value instanceof Number) {
						data = !value.equals(0);
					} else {
						String s = value.toString();
						if (s.length() == 0
							|| s.equalsIgnoreCase("false")
							|| s.equalsIgnoreCase("no")
							|| s.equalsIgnoreCase("off")
							|| s.equals("NaN")) {
							data = false;
						} else {
							data = true;
						}
					}
				} else if (c.equals(byte.class)) {
					if (value == null) {
						data = 0;
					} else if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1 : 0;
					} else if (value instanceof Number) {
						data = ((Number)value).byteValue();
					} else {
						data = Byte.valueOf(value.toString());
					}
				} else if (c.equals(short.class)) {
					if (value == null) {
						data = 0;
					} else if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1 : 0;
					} else if (value instanceof Number) {
						data = ((Number)value).shortValue();
					} else {
						data = Short.valueOf(value.toString());
					}
				} else if (c.equals(int.class)) {
					if (value == null) {
						data = 0;
					} else if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1 : 0;
					} else if (value instanceof Number) {
						data = ((Number)value).intValue();
					} else {
						data = Integer.valueOf(value.toString());
					}
				} else if (c.equals(long.class)) {
					if (value == null) {
						data = 0l;
					} else if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1l : 0l;
					} else if (value instanceof Number) {
						data = ((Number)value).longValue();
					} else {
						data = Long.valueOf(value.toString());
					}
				} else if (c.equals(float.class)) {
					if (value == null) {
						data = 0.0f;
					} else if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1.0f : Float.NaN;
					} else if (value instanceof Number) {
						data = ((Number)value).floatValue();
					} else {
						data = Float.valueOf(value.toString());
					}
				} else if (c.equals(double.class)) {
					if (value == null) {
						data = 0.0;
					} else if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1.0 : Double.NaN;
					} else if (value instanceof Number) {
						data = ((Number)value).doubleValue();
					} else {
						data = Double.valueOf(value.toString());
					}
				} else if (c.equals(char.class)) {
					if (value == null) {
						data = '\u0000';
					} else if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? '1' : '0';
					} else if (value instanceof Character) {
						data = value;
					} else if (value instanceof Number) {
						data = (char)((Number)value).intValue();
					} else {
						String s = value.toString();
						data = (s.length() > 0) ? s.charAt(0) : '\u0000';
					}
				}
			} else if (value != null) {
				if (c.isAssignableFrom(value.getClass()) && c.equals(type)) {
					data = value;
				} else if (Boolean.class.equals(c)) {
					if (value instanceof Number) {
						data = !value.equals(0);
					} else {
						String s = value.toString();
						if (s.length() == 0
							|| s.equalsIgnoreCase("false")
							|| s.equalsIgnoreCase("no")
							|| s.equalsIgnoreCase("off")
							|| s.equals("NaN")) {
							data = false;
						} else {
							data = true;
						}
					}
				} else if (Byte.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1 : 0;
					} else if (value instanceof Number) {
						data = ((Number)value).byteValue();
					} else {
						String str = value.toString().trim();
						if (str.length() > 0) data = Byte.valueOf(str);
					}
				} else if (Short.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1 : 0;
					} else if (value instanceof Number) {
						data = ((Number)value).shortValue();
					} else {
						String str = value.toString().trim();
						if (str.length() > 0) data = Short.valueOf(str);
					}				
				} else if (Integer.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1 : 0;
					} else if (value instanceof Number) {
						data = ((Number)value).intValue();
					} else {
						String str = value.toString().trim();
						if (str.length() > 0) data = Integer.valueOf(str);
					}
				} else if (Long.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1l : 0l;
					} else if (value instanceof Number) {
						data = ((Number)value).longValue();
					} else {
						String str = value.toString().trim();
						if (str.length() > 0) data = Long.valueOf(str);
					}
				} else if (Float.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1.0f : Float.NaN;
					} else if (value instanceof Number) {
						data = ((Number)value).floatValue();
					} else {
						String str = value.toString().trim();
						if (str.length() > 0) data = Float.valueOf(str);
					}
				} else if (Double.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1.0 : Double.NaN;
					} else if (value instanceof Number) {
						data = ((Number)value).doubleValue();
					} else {
						String str = value.toString().trim();
						if (str.length() > 0) data = Double.valueOf(str);
					}
				} else if (BigInteger.class.equals(c)) {				
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? BigInteger.ONE : BigInteger.ZERO;
					} else if (value instanceof BigDecimal) {
						data = ((BigDecimal)value).toBigInteger();
					} else {
						String str = value.toString().trim();
						if (str.length() > 0) data = (new BigDecimal(str)).toBigInteger();
					}
				} else if (BigDecimal.class.equals(c) || Number.class.equals(c)) {
					String str = value.toString().trim();
					if (str.length() > 0) data = new BigDecimal(str);
				} else if (Character.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? '1' : '0';
					} else if (value instanceof Number) {
						data = (char)((Number)value).intValue();
					} else {
						String s = value.toString();
						data = (s.length() > 0) ? s.charAt(0) : null;
					}
				} else if (CharSequence.class.isAssignableFrom(c)) {
					data = value.toString();
				} else if (Appendable.class.isAssignableFrom(c)) {
					Appendable a = (Appendable)create(c);
					data = a.append(value.toString());
				} else if (Date.class.isAssignableFrom(c)) {
					Date date = (Date)create(c);
					if (value instanceof Number) {
						date.setTime(((Number)value).longValue());
					} else {
						String str = value.toString().trim();
						if (str.length() > 0) {
							date.setTime(convertDate(str));
						} else {
							date = null;
						}
					}
					data = date;
				} else if (Calendar.class.isAssignableFrom(c)) {
					Calendar cal = (Calendar)create(c);
					if (value instanceof Number) {
						cal.setTimeInMillis(((Number)value).longValue());
					} else {
						String str = value.toString().trim();
						if (str.length() > 0) {
							cal.setTimeInMillis(convertDate(str));
						} else {
							cal = null;
						}
					}
					data = cal;
				} else if (TimeZone.class.equals(c)) {
					data = TimeZone.getTimeZone(value.toString().trim());
				} else if (Collection.class.isAssignableFrom(c)) {
					Collection collection = (Collection)create(c);
					if (type instanceof ParameterizedType) {
						ParameterizedType pType = (ParameterizedType)type;
						Type[] cTypes = pType.getActualTypeArguments();
						Type cType = (cTypes != null && cTypes.length > 0) ? cTypes[0] : Object.class;
						Class<?> cClasses = null;
						if (cType instanceof ParameterizedType) {
							cClasses = (Class)((ParameterizedType)cType).getRawType();
						} else if (cType instanceof Class) {
							cClasses = (Class)cType;
						} else {
							cClasses = Object.class;
							cType = cClasses;
						}
						if (value instanceof Collection) {
							int i = 0;
							for (Object o : (Collection)value) {
								collection.add(convert(i++, o, cClasses, cType));
							}
						} else {
							collection.add(convert(0, value, cClasses, cType));
						}
					} else {
						if (value instanceof Collection) {
							collection.addAll((Collection)value);
						} else {
							collection.add(value);
						}
					}
					data = collection;
				} else if (c.isArray()) {
					if (value instanceof Collection) {
						Object array = Array.newInstance(c.getComponentType(), ((Collection)value).size());
						int i = 0;
						Class<?> cClass = c.getComponentType();
						Type cType = (type instanceof GenericArrayType) ? 
								((GenericArrayType)type).getGenericComponentType() : cClass;
						
						for (Object o : (Collection)value) {
							Array.set(array, i, convert(i++, o, cClass, cType));
						}
						data = array;
					} else if (value instanceof CharSequence && byte.class.equals(c.getComponentType())) {
						data = decodeBase64((CharSequence)value);
					} else {
						Object array = Array.newInstance(c.getComponentType(), 1);
						Class<?> cClass = c.getComponentType();
						Type cType = (type instanceof GenericArrayType) ? 
								((GenericArrayType)type).getGenericComponentType() : cClass;
						Array.set(array, 0, convert(0, value, c.getComponentType(), cType));
						data = array;
					}
				} else if (Map.class.isAssignableFrom(c)) {
					if (value instanceof Map) {
						Map map = (Map)create(c);
						if (type instanceof ParameterizedType) {
							ParameterizedType pType = (ParameterizedType)type;
							Type[] cTypes = pType.getActualTypeArguments();
							Class<?>[] cClasses = new Class[2];
							for (int i = 0; i < cClasses.length; i++) {
								if (cTypes[i] instanceof ParameterizedType) {
									cClasses[i] = (Class)((ParameterizedType)cTypes[i]).getRawType();
								} else if (cTypes[i] instanceof Class) {
									cClasses[i] = (Class)cTypes[i];
								} else {
									cClasses[i] = Object.class;
									cTypes[i] = cClasses[i];
								}
							}
							for (Map.Entry entry : (Set<Map.Entry>)((Map)value).entrySet()) {
								String mKey = entry.getKey().toString();
								map.put(mKey, convert(mKey, entry.getValue(), cClasses[1], cTypes[1]));
							}
						} else {
							map.putAll((Map)value);
						}
						data = map;
					}
				} else if (Pattern.class.equals(c)) {
					data = Pattern.compile(value.toString());
				} else if (Locale.class.equals(c)) {
					String[] s = null;
					if (value instanceof Collection || value.getClass().isArray()) {
						s = (String[])convert(key, value, String[].class, String[].class);
					} else {
						s = value.toString().split("\\p{Punct}");
					}
					
					if (s.length == 0) {
						data = null;
					} else if (s.length == 1) {
						data = new Locale(s[0]);
					} else if (s.length == 2) {
						data = new Locale(s[0], s[1]);
					} else {
						data = new Locale(s[0], s[1], s[2]);
					}
				} else if (Class.class.equals(c)) {
					String s = value.toString();
					if (s.equals("boolean")) {
						data = boolean.class;
					} else if (s.equals("byte")) {
						data = byte.class;
					} else if (s.equals("short")) {
						data = short.class;
					} else if (s.equals("int")) {
						data = int.class;
					} else if (s.equals("long")) {
						data = long.class;
					} else if (s.equals("float")) {
						data = float.class;
					} else if (s.equals("double")) {
						data = double.class;
					} else {
						data = Class.forName(value.toString());
					}
				} else if (value instanceof Map) {
					Object o = create(c);
					if (o != null) {
						Map<String, Member> props = getProperties(c, true);
						
						boolean access = tryAccess(c);
						
						Map map = (Map)value;
						for (Map.Entry entry : (Set<Map.Entry>)map.entrySet()) {
							String mKey = entry.getKey().toString();
							Member target = props.get(mKey);
							if (target == null) {
								target = props.get(toLowerCamel(mKey));
								if (target == null) {
									target = props.get(mKey + "_");
									if (target == null) continue;
								}
							}
							
							if (access) ((AccessibleObject)target).setAccessible(true);
							
							if (target instanceof Method) {
								Method m = (Method)target;
								((Method)target).invoke(o, convert(mKey, entry.getValue(), m.getParameterTypes()[0], m.getGenericParameterTypes()[0]));
							} else {
								Field f = (Field)target;
								f.set(o, convert(mKey, entry.getValue(), f.getType(), f.getGenericType()));
							}
						}
						data = o;
					}
				}
			}
			
			if (data == null && (c.isPrimitive() || value != null)) {
				if (!handleConvertError(key, value, c, type, null)) {
					throw new JSONConvertException("");
				}
			}
		} catch (JSONConvertException e) {
			throw e;
		} catch (Exception e) {
			if (!handleConvertError(key, value, c, type, e)) {
				throw new JSONConvertException(e);
			}
		}
		
		return data;
	}
	
	protected boolean ignore(Class target, Member member) {
		int modifiers = member.getModifiers();
		if (Modifier.isStatic(modifiers)) return true;
		if (Modifier.isTransient(modifiers)) return true;
		if (member.getDeclaringClass().equals(Object.class)) return true;
		return false;
	}
	
	/**
	 * Catches the convertion error occured in convert method.
	 * 
	 * @param key key name
	 * @param value The converting object.
	 * @param c The converting class
	 * @param type The converting generics type
	 * @param e The exception object throwed by converting.
	 * @return If the handleError method returns false, it throws JSONConvertException. else it continues the process.
	 */
	protected boolean handleConvertError(Object key, Object value, Class c, Type type, Exception e) {
		return true;
	}
	
	protected Object create(Class c) throws Exception {
		Object instance = null;
		
		if (c.isInterface()) {
			if (SortedMap.class.equals(c)) {
				instance = new TreeMap();
			} else if (Map.class.equals(c)) {
				instance = new LinkedHashMap();
			} else if (SortedSet.class.equals(c)) {
				instance = new TreeSet();
			} else if (Set.class.equals(c)) {
				instance = new LinkedHashSet();
			} else if (List.class.equals(c)) {
				instance = new ArrayList();
			} else if (Collection.class.equals(c)) {
				instance = new ArrayList();
			} else if (Appendable.class.equals(c)) {
				instance = new StringBuilder();
			}
		} else if (Modifier.isAbstract(c.getModifiers())) {
			if (Calendar.class.equals(c)) {
				instance = Calendar.getInstance();
			}
		} else if (c.isMemberClass() || c.isAnonymousClass()) {
			Class eClass = c.getEnclosingClass();
			Constructor con = c.getDeclaredConstructor(eClass);
			if(tryAccess(c)) con.setAccessible(true);
			instance = con.newInstance((eClass.equals(this.contextClass)) ? this.context : null);
		} else {
			if (Date.class.isAssignableFrom(c)) {
				try {
					Constructor con = c.getDeclaredConstructor(long.class);
					if(tryAccess(c)) con.setAccessible(true);
					instance = con.newInstance(0l);
				} catch (NoSuchMethodException e) {
					// no handle
				}
			}
			
			if (instance == null) {
				Constructor con = c.getDeclaredConstructor();
				if(tryAccess(c)) con.setAccessible(true);
				instance = con.newInstance();
			}
		}
		
		return instance;
	}
	
	private boolean tryAccess(Class c) {
		int modifier = c.getModifiers();
		if (this.contextClass != null && !Modifier.isPublic(modifier)) {
			if (Modifier.isPrivate(modifier)) {
				return this.contextClass.equals(c.getEnclosingClass());
			}
			return c.getPackage().equals(this.contextClass.getPackage());
		}
		return false;
	}
	
	private String getMessage(String id, Object... args) {
		if (locale == null) locale = Locale.getDefault();
		ResourceBundle bundle = ResourceBundle.getBundle(JSON.class.getName(), locale);
		return MessageFormat.format(bundle.getString(id), args);
	}
	
	private static String toLowerCamel(String name) {
		StringBuilder sb = new StringBuilder(name.length());
		boolean toUpperCase = false;
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (c == ' ' || c == '_' || c == '-') {
				toUpperCase = true;
			} else if (toUpperCase) {
				sb.append(Character.toUpperCase(c));
				toUpperCase = false;
			} else {
				sb.append(c);
			}
		}
		if (sb.length() > 1 && Character.isUpperCase(sb.charAt(0)) && Character.isLowerCase(sb.charAt(1))) {
			sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
		}
		return sb.toString();
	}
	
	private Map<String, Member> getProperties(Class c, boolean isSetter) {
		Map<String, Member> props = new HashMap<String, Member>();
		
		for (Field f : c.getFields()) {
			if (ignore(c, f)) continue;
			
			props.put(f.getName(), f);
		}
		
		for (Method m : c.getMethods()) {
			if (ignore(c, m)) continue;

			String name = m.getName();
			int start = 0;
			if (isSetter) {
				if (name.startsWith("set") 
					&& name.length() > 3
					&& Character.isUpperCase(name.charAt(3))
					&& m.getParameterTypes().length == 1
					&& m.getReturnType().equals(void.class)) {
					start = 3;
				} else {
					continue;
				}
			} else {
				if (name.startsWith("get")
					&& name.length() > 3
					&& Character.isUpperCase(name.charAt(3))
					&& m.getParameterTypes().length == 0
					&& !m.getReturnType().equals(void.class)) {
					start = 3;
				} else if (name.startsWith("is")
					&& name.length() > 2
					&& Character.isUpperCase(name.charAt(2))
					&& m.getParameterTypes().length == 0
					&& m.getReturnType().equals(boolean.class)) {
					start = 2;
				} else {
					continue;
				}
			}
			
			char[] cs = name.toCharArray();
			if (cs.length < start+2 || Character.isLowerCase(cs[start+1])) {
				cs[start] = Character.toLowerCase(cs[start]);
			}
			props.put(new String(cs, start, cs.length-start), m);
		}
		
		return props;
	}
	
	private static final String BASE64_MAP = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
	
	private static String encodeBase64(byte[] data) {
		if (data == null) return null;
		
		char[] buffer = new char[data.length / 3 * 4 + ((data.length % 3 == 0) ? 0 : 4)];
		
		int buf = 0;
		for (int i = 0; i < data.length; i++) {
			switch (i % 3) {
				case 0 :
					buffer[i / 3 * 4] = BASE64_MAP.charAt((data[i] & 0xFC) >> 2);
					buf = (data[i] & 0x03) << 4;
					if (i + 1 == data.length) {
						buffer[i / 3 * 4 + 1] = BASE64_MAP.charAt(buf);
						buffer[i / 3 * 4 + 2] = '=';
						buffer[i / 3 * 4 + 3] = '=';
					}
					break;
				case 1 :
					buf += (data[i] & 0xF0) >> 4;
					buffer[i / 3 * 4 + 1] = BASE64_MAP.charAt(buf);
					buf = (data[i] & 0x0F) << 2;
					if (i + 1 == data.length) {
						buffer[i / 3 * 4 + 2] = BASE64_MAP.charAt(buf);
						buffer[i / 3 * 4 + 3] = '=';
					}
					break;
				case 2 :
					buf += (data[i] & 0xC0) >> 6;
					buffer[i / 3 * 4 + 2] = BASE64_MAP.charAt(buf);
					buffer[i / 3 * 4 + 3] = BASE64_MAP.charAt(data[i] & 0x3F);
					break;
			}
		}

		return new String(buffer);
	}
	
	private static byte[] decodeBase64(CharSequence cs) {
		int addsize = 0;
		int bufsize = 0;
		
		for (int i = 0; i < cs.length(); i++) {
			char c = cs.charAt(i);
			if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '+' || c == '/') {
				bufsize++;
			} else if (c == '=') {
				if (i+1 < cs.length() && cs.charAt(i+1) == '=' && (bufsize+2) % 4 == 0) {
					bufsize += 2;
					addsize = -2;
				} else if ((bufsize+1) % 4 == 0) {
					bufsize += 1;
					addsize = -1;
				} else {
					return null;
				}
				break;
			}
		}
		
		byte[] buffer = new byte[bufsize / 4 * 3 + addsize];
		
		int pos = 0;
		for (int i = 0; i < cs.length(); i++) {
			char c = cs.charAt(i);
			
			int data = 0;
			if (c >= 'A' && c <= 'Z') {
				data = c - 65;
			} else if (c >= 'a' && c <= 'z') {
				data = c - 97 + 26;
			} else if (c >= '0' && c <= '9') {
				data = c - 48 + 52;
			} else if (c == '+') {
				data = 62;
			} else if (c == '/') {
				data = 63;
			} else if (c == '=') {
				break;
			} else {
				continue;
			}
			
			switch (pos % 4) {
			case 0:
				buffer[pos / 4 * 3] = (byte)(data << 2);
				break;
			case 1:
				buffer[pos / 4 * 3] += (byte)(data >> 4);
				if (pos / 4 * 3 + 1 < buffer.length) {
					buffer[pos / 4 * 3 + 1] = (byte)((data & 0xF) << 4);
				}
				break;
			case 2:
				buffer[pos / 4 * 3 + 1] += (byte)(data >> 2);
				if (pos / 4 * 3 + 2 < buffer.length) {
					buffer[pos / 4 * 3 + 2] = (byte)((data & 0x3) << 6);
				}
				break;
			case 3:
				buffer[pos / 4 * 3 + 2] += (byte)data;
				break;
			}
			pos++;
		}
		
		return buffer;
	}
	
	private Long convertDate(String value) throws ParseException {
		value = value.trim();
		if (value.length() == 0) {
			return null;
		}
		value = Pattern.compile("(?:GMT|UTC)([+-][0-9]{2})([0-9]{2})")
			.matcher(value)
			.replaceFirst("GMT$1:$2");
		
		DateFormat format = null;
		if (Character.isDigit(value.charAt(0))) {
			StringBuilder sb = new StringBuilder(value.length() * 2);

			String types = "yMdHmsSZ";
			// 0: year, 1:month, 2: day, 3: hour, 4: minute, 5: sec, 6:msec, 7: timezone
			int pos = (value.length() > 2 && value.charAt(2) == ':') ? 3 : 0;
			boolean before = true;
			int count = 0;
			for (int i = 0; i < value.length(); i++) {
				char c = value.charAt(i);
				if ((pos == 4 || pos == 5 || pos == 6) 
						&& (c == '+' || c == '-')
						&& (i + 1 < value.length())
						&& (Character.isDigit(value.charAt(i+1)))) {
					
					if (!before) sb.append('\'');
					pos = 7;
					count = 0;
					before = true;
					continue;
				} else if (pos == 7 && c == ':'
						&& (i + 1 < value.length())
						&& (Character.isDigit(value.charAt(i+1)))) {
					value = value.substring(0, i) + value.substring(i+1);
					continue;
				}
				
				boolean digit = (Character.isDigit(c) && pos < 8);
				if (before != digit) {
					sb.append('\'');
					if (digit) {
						count = 0;
						pos++;
					}
				}
				
				if (digit) {
					char type = types.charAt(pos);
					if (count == ((type == 'y' || type == 'Z') ? 4 : (type == 'S') ? 3 : 2)) {
						count = 0;
						pos++;
						type = types.charAt(pos);
					}
					if (type != 'Z' || count == 0) sb.append(type);
					count++;
				} else {
					sb.append((c == '\'') ? "''" : c);
				}
				before = digit;
			}
			if (!before) sb.append('\'');
			
			format = new SimpleDateFormat(sb.toString(), Locale.ENGLISH);
		} else if (value.length() > 18) {
			if (value.charAt(3) == ',') {
				String pattern = "EEE, dd MMM yyyy HH:mm:ss Z";
				format = new SimpleDateFormat(
						(value.length() < pattern.length()) ? pattern.substring(0, value.length()) : pattern, Locale.ENGLISH);
			} else if (value.charAt(13) == ':') {
				format = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);
			} else if (value.charAt(18) == ':') {
				String pattern = "EEE MMM dd yyyy HH:mm:ss Z";
				format = new SimpleDateFormat(
						(value.length() < pattern.length()) ? pattern.substring(0, value.length()) : pattern, Locale.ENGLISH);
			} else {
				format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale);
			}
		} else {
			format = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
		}
		
		return format.parse(value).getTime();
	}	
}

interface JSONSource {
	int next() throws IOException;
	void back();
	long getLineNumber();
	long getColumnNumber();
	long getOffset();
}

class CharSequenceJSONSource implements JSONSource {
	private int lines = 1;
	private int columns = 1;
	private int offset = 0;
	
	private CharSequence cs;
	
	public CharSequenceJSONSource(CharSequence cs) {
		if (cs == null) {
			throw new NullPointerException();
		}
		this.cs = cs;
	}
	
	public int next() {
		if (offset < cs.length()) {
			char c = cs.charAt(offset++);
			if (c == '\r' || (c == '\n' && offset > 1 && cs.charAt(offset-2) != '\r')) {
				lines++;
				columns = 0;
			} else {
				columns++;
			}
			return c;
		}
		return -1;
	}
	
	public void back() {
		offset--;
		columns--;
	}
	
	public long getLineNumber() {
		return lines;
	}
	
	public long getColumnNumber() {
		return columns;
	}
	
	public long getOffset() {
		return offset;
	}
	
	public String toString() {
		return cs.subSequence(offset-columns+1, offset).toString();
	}
}

class ReaderJSONSource implements JSONSource{
	private long lines = 1l;
	private long columns = 1l;
	private long offset = 0;

	private Reader reader;
	private char[] buf = new char[256];
	private int start = 0;
	private int end = 0;
	
	public ReaderJSONSource(Reader reader) {
		if (reader == null) {
			throw new NullPointerException();
		}
		this.reader = reader;
	}
	
	public int next() throws IOException {
		if (start == end) {
			int size = reader.read(buf, start, Math.min(buf.length-start, buf.length/2));
			if (size != -1) {
				end = (end + size) % buf.length;
			} else {
				return -1;
			}
		}
		char c = buf[start];
		if (c == '\r' || (c == '\n' && buf[(start+buf.length-1) % (buf.length)] != '\r')) {
			lines++;
			columns = 0;
		} else {
			columns++;
		}
		offset++;
		start = (start+1) % buf.length;
		return c;
	}
	
	public void back() {
		columns--;
		start = (start+buf.length-1) % buf.length;
	}
	
	public long getLineNumber() {
		return lines;
	}
	
	public long getColumnNumber() {
		return columns;
	}
	
	public long getOffset() {
		return offset;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		int maxlength = (columns-1 < buf.length) ? (int)columns-1 : buf.length-1;
		for (int i = maxlength; i >= 0; i--) {
			sb.append(buf[(start-2+buf.length-i) % (buf.length-1)]);
		}
		return sb.toString();
	}
}
