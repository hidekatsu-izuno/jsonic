/*
 * Copyright 2007 Hidekatsu Izuno
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

import java.io.Reader;
import java.io.Flushable;
import java.io.IOException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.Date;
import java.util.Calendar;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.math.BigDecimal;
import java.text.ParseException;
import net.arnx.jsonic.util.*;

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
 * // invokes method by a json array.
 * JSON json = new JSON();
 * Object result = json.invoke(data, "method", "[true, 1]");
 * </pre>
 * 
 * <h4>Summary of encoding rules for java type into json type</h4>
 * <table border="1" cellpadding="1" cellspacing="0">
 * <tr>
 * 	<th bgcolor="#CCCCFF" align="left">java type</th>
 * 	<th bgcolor="#CCCCFF" align="left">json type</th>
 * </tr>
 * <tr><td>java.util.Map</td><td rowspan="2">object</td></tr>
 * <tr><td>java.lang.Serializable (public property or field)</td></tr>
 * <tr><td>java.lang.Object[]</td><td rowspan="3">array</td></tr>
 * <tr><td>java.util.Collection</td></tr>
 * <tr><td>boolean[], short[], int[], long[], float[], double[]</td></tr>
 * <tr><td>java.lang.CharSequence</td><td rowspan="7">string</td></tr>
 * <tr><td>char[]</td></tr>
 * <tr><td>java.lang.Character</td></tr>
 * <tr><td>char</td></tr>
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
 * @version 0.9.4
 * @see <a href="http://www.rfc-editor.org/rfc/rfc4627.txt">RFC 4627</a>
 * @see <a href="http://www.apache.org/licenses/LICENSE-2.0">the Apache License, Version 2.0</a>
 */
@SuppressWarnings("unchecked")
public class JSON {
	private DynamicInvoker invoker = new DynamicInvoker() {
		protected void handleConvertError(String key, Object value, Class c, Type type, Exception e) throws Exception {
			JSON.this.handleConvertError(key, value, c, type, e);
		}
	};
		
	public JSON() {
		this(null);
	}
	
	public JSON(Object context) {
		setContext(context);
	}
	
	private boolean prettyPrint = false;
	
	/**
	 * Output json string is to human-readable format.
	 * 
	 * @param value true to format human-readable, false to shorten.
	 */
	public void setPrettyPrint(boolean value) {
		this.prettyPrint = value;
	}
	
	private int maxDepth = 255;
	
	/**
	 * Sets maximum depth for the nest level.
	 * 
	 * @param value maximum depth for the nest level.
	 */
	public void setMaxDepth(int value) {
		if (value <= 0) {
			throw new IllegalArgumentException("max depth should be larger than 0.");
		}
		this.maxDepth = value;
	}
	
	public void setContext(Object value) {
		invoker.setContext(value);
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
			// never occur
		}
		
		return value;
	}
	
	/**
	 * Decodes a json string into a object.
	 * 
	 * @param source a json string to decode
	 * @return a decoded object
	 * @exception ParseException if the beginning of the specified string cannot be parsed.
	 */
	public static Object decode(String source) throws JSONParseException {
		Object value = null;
		try {
			value = (new JSON()).parse(new CharSequenceJSONSource(source));
		} catch (IOException e) {
			// never occur
		}
		return value;
	}
	
	/**
	 * Decodes a json string into a typed object.
	 * 
	 * @param source a json string to decode
	 * @param c class for converting
	 * @return a decoded object
	 * @exception ParseException if the beginning of the specified string cannot be parsed.
	 */
	public static <T> T decode(String source, Class<? extends T> c) throws Exception {
		JSON json = new JSON(c);
		return (T)json.convert(json.parse(new CharSequenceJSONSource(source)), c, c);
	}
	
	/**
	 * Decodes a json string into a typed object.
	 * 
	 * @param source a json string to decode
	 * @param c class for converting
	 * @param t type specified generics parameters
	 * @return a decoded object
	 * @exception ParseException if the beginning of the specified string cannot be parsed.
	 */
	public static <T> T decode(String source, Class<? extends T> c, Type t) throws Exception {
		JSON json = new JSON(c);
		return (T)json.convert(json.parse(new CharSequenceJSONSource(source)), c, t);
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
			o = Base64.encode((byte[])o);
		} else if (o instanceof Object[]) {
			o = Arrays.asList((Object[])o);
		} else if (o instanceof Pattern) {
			o = ((Pattern)o).pattern();
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
			
			boolean access = invoker.tryAccess(c);
			
			TreeMap<String, Object> map = new TreeMap<String, Object>();
			
			Field[] fields = c.getFields();
			for (Field f : fields) {
				if (!Modifier.isStatic(f.getModifiers())
						&& Modifier.isPublic(f.getModifiers())
						&& !Modifier.isTransient(f.getModifiers())) {
					try {
						if (access) f.setAccessible(true);
						Object value =  f.get(o);
						
						if (value != o) {
							map.put(f.getName(), value);
						}
					} catch (Exception e) {
						// no handle
					}
				}
			}

			Method[] methods = c.getMethods();
			for (Method m : methods) {
				String name = m.getName();
				if (!Modifier.isStatic(m.getModifiers())
						&& !name.equals("getClass")
						&& ((name.startsWith("get") 
								&& name.length() > 3 
								&& Character.isUpperCase(name.charAt(3)) 
								&& !m.getReturnType().equals(void.class))
							|| 
							((name.startsWith("is") 
								&& name.length() > 2 
								&& Character.isUpperCase(name.charAt(2))
								&& m.getReturnType().equals(boolean.class))))
						&& m.getParameterTypes().length == 0
						) {
					try {
						if (access) m.setAccessible(true);
						Object value = m.invoke(o, (Object[])null);
						
						if (value != o) {
							String key = null;
							int prefix = name.startsWith("get") ? 3 : 2;
							if (!(name.length() > prefix+1 && Character.isUpperCase(name.charAt(prefix+1)))) {
								char[] carray = name.toCharArray();
								carray[prefix] = Character.toLowerCase(carray[prefix]);
								key = new String(carray, prefix, carray.length-prefix);
							} else {
								key = name.substring(prefix);
							}
							
							map.put(key, value);
						}
					} catch (Exception e) {
						// no handle
					}
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
		Map.Entry entry = null;
		for (Iterator i = map.entrySet().iterator(); i.hasNext(); ) {
			entry = (Map.Entry)i.next();
			if (this.prettyPrint) {
				ap.append('\n');
				for (int j = 0; j < level+1; j++) ap.append('\t');
			}
			formatString((String)entry.getKey(), ap).append(':');
			if (this.prettyPrint) ap.append(' ');
			Object item = entry.getValue();
			if (item == o) item = null;
			format(item, ap, level+1);
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
	
	public Object parse(Reader reader) throws IOException, JSONParseException {
		return parse(new ReaderJSONSource(reader));
	}
	
	public <T> T parse(CharSequence s, Class<? extends T> c) throws Exception {
		return (T)convert(parse(new CharSequenceJSONSource(s)), c, c);
	}
	
	public <T> T parse(Reader reader, Class<? extends T> c) throws Exception {
		return (T)convert(parse(new ReaderJSONSource(reader)), c, c);
	}
	
	public <T> T parse(CharSequence s, Class<? extends T> c, Type t) throws Exception {
		return (T)convert(parse(new CharSequenceJSONSource(s)), c, t);
	}
	
	public <T> T parse(Reader reader, Class<? extends T> c, Type t) throws Exception {
		return (T)convert(parse(new ReaderJSONSource(reader)), c, t);
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
				break;
			case '[':
				if (o == null) {
					s.back();
					o = parseArray(s, sb);
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
					o = parseObject(s, sb);
					break;
				}
				throw new JSONParseException(getMessage("json.parse.UnexpectedChar", c), s);
			}
		}
		return o;
	}	
	
	private Map<String, Object> parseObject(JSONSource s, StringBuilder sb) throws IOException, JSONParseException {
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
				break;
			case '{':
				if (point == 0) {
					start = '{';
					point = 1;
				} else if (point == 2 || point == 3){
					s.back();
					map.put(key, parseObject(s, sb));
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
					map.put(key, parseArray(s, sb));
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
	
	private List<Object> parseArray(JSONSource s, StringBuilder sb) throws IOException, JSONParseException {
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
				break;
			case '[':
				if (point == 0) {
					point = 1;
				} else if (point == 1 || point == 3) {
					s.back();
					list.add(parseArray(s, sb));
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
					list.add(parseObject(s, sb));
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
			case '\\':
				if (point == 1) {
					s.back();
					sb.append(parseEscape(s));
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
	
	protected Object convert(Object value, Class c, Type type) throws Exception {
		return invoker.convert(value, c, type);
	}
	
	protected void handleConvertError(String key, Object value, Class c, Type type, Exception e) throws Exception {
		// no handle
	}
	
	/**
	 * Invokes the targeted method for the specified object, with the specified json parameters.
	 * 
	 * @param o the object the underlying method is invoked from
	 * @param methodName the invoked method name
	 * @param json the parameters used for the method call. json should be array, or appended '[' and ']'.
	 */
	public Object invoke(Object o, String methodName, CharSequence json) throws Exception {
		List values = (json != null) ? parseArray(new CharSequenceJSONSource(json), new StringBuilder()) : null;
		return invoker.invoke(o, methodName, values);
	}
	
	protected boolean limit(Method method) {
		return invoker.limit(method);
	}
	
	private static String getMessage(String id, Object... arguments) {
		ResourceBundle bundle = ResourceBundle.getBundle(JSON.class.getName());
		return MessageFormat.format(bundle.getString(id), arguments);
	}

	static interface JSONSource {
		int next() throws IOException;
		void back();
		long getLineNumber();
		long getColumnNumber();
		long getOffset();
	}

	private static class CharSequenceJSONSource implements JSONSource {
		private int lines = 1;
		private int columns = 1;
		private int offset = 0;
		
		private CharSequence cs;
		
		public CharSequenceJSONSource(CharSequence cs) {
			if (cs == null) {
				throw new IllegalArgumentException("input is null");
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

	private static class ReaderJSONSource implements JSONSource{
		private long lines = 1l;
		private long columns = 1l;
		private long offset = 0;

		private Reader reader;
		private char[] buf = new char[256];
		private int start = 0;
		private int end = 0;
		
		public ReaderJSONSource(Reader reader) {
			if (reader == null) {
				throw new IllegalArgumentException("input is null");
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
}