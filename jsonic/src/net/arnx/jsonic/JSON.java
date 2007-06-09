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
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.SortedMap;
import java.util.List;
import java.util.Date;
import java.util.Calendar;
import java.util.Locale;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.math.BigInteger;
import java.math.BigDecimal;

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
 * <tr><td>java.lang.CharSequence</td><td rowspan="5">string</td></tr>
 * <tr><td>char[]</td></tr>
 * <tr><td>java.lang.Character</td></tr>
 * <tr><td>char</td></tr>
 * <tr><td>java.util.regex.Pattern</td></tr>
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
 * @version 0.9.3
 * @see <a href="http://www.rfc-editor.org/rfc/rfc4627.txt">RFC 4627</a>
 * @see <a href="http://www.apache.org/licenses/LICENSE-2.0">the Apache License, Version 2.0</a>
 */
public class JSON {
	private static final Map<String, Object> LITERALS = new HashMap<String, Object>();

	static {
		LITERALS.put("null", null);
		LITERALS.put("true", Boolean.TRUE);
		LITERALS.put("false", Boolean.FALSE);
	}
	
	private Class contextClass = null;
	private Object context = null;
	
	public JSON() {
	}
	
	public JSON(Object context) {
		this.setContext(context);
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
	
	private boolean extendedMode = false;
	
	/**
	 * Sets extended mode.
	 * 
	 * @param value true to enable extension mode, false to disable
	 */
	public void setExtendedMode(boolean value) {
		this.extendedMode = value;
	}
	
	public void setContext(Object value) {
		if (value instanceof Class) {
			this.context = null;
			this.contextClass = (Class)value;
		} else {
			this.context = value;
			this.contextClass = (value != null) ? value.getClass() : null;
		}
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
	public static Object decode(String source) {
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
	@SuppressWarnings("unchecked")
	public static <T> T decode(String source, Class<? extends T> c) throws Exception {
		JSON json = new JSON(c);
		return (T)json.convert(json.parse(new CharSequenceJSONSource(source)), c, c);
	}
	
	public String format(Object source) {
		String value = null;
		try {
			value = format(source, new StringBuilder(1024)).toString();
		} catch (Exception e) {
			// never occur
		}
		return value;
	}
	
	public Appendable format(Object source, Appendable ap) throws IOException {
		if (!this.extendedMode && (source == null
				|| source instanceof CharSequence
				|| source instanceof Character
				|| source instanceof char[]
				|| source instanceof byte[]
				|| source instanceof Number
				|| source instanceof Locale
				|| source instanceof Date
				|| source instanceof Calendar
				|| source instanceof Boolean)) {
				throw new IllegalArgumentException("source object has to be encoded a object or array.");
		}
		
		return format(source, ap, 0);
	}
	
	private Appendable format(Object o, Appendable ap, int level) throws IOException {
		if (level > this.maxDepth) {
			throw new IllegalArgumentException("nest level is over max depth.");
		}
		
		if (o instanceof Class) {
			o = ((Class)o).getName();
		} else if (o instanceof Character || o instanceof Type) {
			o = o.toString();
		} else if (o instanceof char[]) {
			o = new String((char[])o);
		} else if (o instanceof Object[]) {
			o = Arrays.asList((Object[])o);
		} else if (o instanceof Pattern) {
			o = ((Pattern)o).pattern();
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
		
		if (o == null) {
			ap.append("null");
		} else if (o instanceof CharSequence) {
			formatString((CharSequence)o, ap);
		} else if (o instanceof Double || o instanceof Float) {
			double d = ((Number)o).doubleValue();
			if (!this.extendedMode && (Double.isNaN(d) || Double.isInfinite(d))) {
				ap.append('"').append(Double.toString(d)).append('"');
			} else {
				ap.append(Double.toString(d));
			}
		} else if (o instanceof Byte) {
			ap.append(Integer.toString(((Byte)o).byteValue() & 0xFF));
		} else if (o instanceof Number) {
			ap.append(o.toString());
		} else if (o instanceof Boolean) {
			ap.append(o.toString());
		} else if (o instanceof Date) {
			if (this.extendedMode) ap.append("new Date(");
			ap.append(Long.toString(((Date)o).getTime()));
			if (this.extendedMode) ap.append(")");
		} else if (o instanceof Calendar) {
			if (this.extendedMode) ap.append("new Date(");
			ap.append(Long.toString(((Calendar)o).getTimeInMillis()));
			if (this.extendedMode) ap.append(")");
		} else if (o.getClass().isArray()) {
			if (o instanceof boolean[]) {
				boolean[] array = (boolean[])o;
				ap.append('[');
				for (int i = 0; i < array.length; i++) {
					ap.append(Boolean.toString(array[i]));
					if (i != array.length-1) {
						ap.append(',');
						if (this.prettyPrint) ap.append(' ');
					}
				}
				ap.append(']');
			} else if (o instanceof byte[]) {
				ap.append('"').append(encodeBase64((byte[])o)).append('"');
			} else if (o instanceof short[]) {
				short[] array = (short[])o;
				ap.append('[');
				for (int i = 0; i < array.length; i++) {
					ap.append(Short.toString(array[i]));
					if (i != array.length-1) {
						ap.append(',');
						if (this.prettyPrint) ap.append(' ');
					}
				}
				ap.append(']');
			} else if (o instanceof int[]) {
				int[] array = (int[])o;
				ap.append('[');
				for (int i = 0; i < array.length; i++) {
					ap.append(Integer.toString(array[i]));
					if (i != array.length-1) {
						ap.append(',');
						if (this.prettyPrint) ap.append(' ');
					}
				}
				ap.append(']');
			} else if (o instanceof long[]) {
				long[] array = (long[])o;
				ap.append('[');
				for (int i = 0; i < array.length; i++) {
					ap.append(Long.toString(array[i]));
					if (i != array.length-1) {
						ap.append(',');
						if (this.prettyPrint) ap.append(' ');
					}
				}
				ap.append(']');
			} else if (o instanceof float[]) {
				float[] array = (float[])o;
				ap.append('[');
				for (int i = 0; i < array.length; i++) {
					if (!this.extendedMode && (Float.isNaN(array[i]) || Float.isInfinite(array[i]))) {
						ap.append('"').append(Float.toString(array[i])).append('"');
					} else {
						ap.append(Float.toString(array[i]));
					}
					if (i != array.length-1) {
						ap.append(',');
						if (this.prettyPrint) ap.append(' ');
					}
				}
				ap.append(']');
			} else if (o instanceof double[]) {
				double[] array = (double[])o;
				ap.append('[');
				for (int i = 0; i < array.length; i++) {
					if (!this.extendedMode && (Double.isNaN(array[i]) || Double.isInfinite(array[i]))) {
						ap.append('"').append(Double.toString(array[i])).append('"');
					} else {
						ap.append(Double.toString(array[i]));
					}
					if (i != array.length-1) {
						ap.append(',');
						if (this.prettyPrint) ap.append(' ');
					}
				}
				ap.append(']');
			}
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
			
			boolean access = tryAccess(c);
			
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
						&& Modifier.isPublic(m.getModifiers())
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

	public Object parse(CharSequence cs) {
		if (cs == null) {
			throw new IllegalArgumentException("source text is null.");
		}
		Object value = null;
		try {
			value = parse(new CharSequenceJSONSource(cs));
		} catch (IOException e) {
			// never occur
		}
		return value;
	}
	
	public Object parse(Reader reader) throws IOException {
		if (reader == null) {
			throw new IllegalArgumentException("source text is null.");
		}
		return parse(new ReaderJSONSource(reader));
	}
	
	private Object parse(JSONSource s) throws IOException {
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
			case '{':
				if (o == null) {
					s.back();
					o = parseObject(s);
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			case '[':
				if (o == null) {
					s.back();
					o = parseArray(s);
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			case '\'':
			case '"':
				if (this.extendedMode) {
					s.back();
					o = parseString(s);
					break;
				}
			case 't':
			case 'f':
			case 'n':
				if (this.extendedMode) {
					s.back();
					o = parseLiteral(s);
					break;
				}
			default:
				if (this.extendedMode) {
					if ((c == '-') || (c >= '0' && c <= '9')) {
						s.back();
						o = parseNumber(s);
						break;
					} else if (c == '/') {
						s.back();
						skipComment(s);
						break;
					}
					handleParseError(new JSONParseException("unexpected char: "+c, s));
				} else {
					handleParseError(new JSONParseException("a JSON text must start with a object or array", s));
				}
			}
		}
		if (o == null) {
			handleParseError(new JSONParseException("source text is empty.", s));
		}
		return o;
	}	
	
	@SuppressWarnings("unchecked")
	public <T> T parse(CharSequence s, Class<? extends T> c) throws Exception {
		return (T)convert(parse(new CharSequenceJSONSource(s)), c, c);
	}
	
	private Map<String, Object> parseObject(JSONSource s) throws IOException {
		int point = 0; // 0 '{' 1 'key' 2 ':' 3 'value' 4 ',' ... '}' E
		Map<String, Object> map = new HashMap<String, Object>();
		String key = null;
		
		int n = -1;
		while (point != Integer.MAX_VALUE && (n = s.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case '\r':
			case '\n':
			case ' ':
			case '\t':
				break;
			case '{':
				if (point == 0) {
					point = 1;
					break;
				} else if (point == 3){
					s.back();
					map.put(key, parseObject(s));
					point = 4;
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			case ':':
				if (point == 2) {
					point = 3;
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			case ',':
				if (point == 4) {
					point = 1;
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			case '}':
				if (point == 1 || point == 4) {
					point = Integer.MAX_VALUE;
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			case '\'':
				if (!this.extendedMode) {
					handleParseError(new JSONParseException("unexpected char: "+c, s));
				}
			case '"':
				if (point == 1) {
					s.back();
					key = parseString(s);
					point = 2;
					break;
				} else if (point == 3) {
					s.back();
					map.put(key, parseString(s));
					point = 4;
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			case '[':
				if (point == 3) {
					s.back();
					map.put(key, parseArray(s));
					point = 4;
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			case 't':
			case 'f':
			case 'n':
				if (point == 3) {
					s.back();
					map.put(key, parseLiteral(s));
					point = 4;
					break;
				}
				if (!this.extendedMode) {
					handleParseError(new JSONParseException("unexpected char: "+c, s));
				}
			default:
				if (point == 3 && ((c == '-') || (c >= '0' && c <= '9'))) {
					s.back();
					map.put(key, parseNumber(s));
					point = 4;
					break;
				} else if (this.extendedMode) {
					if (point == 1 && (Character.isUnicodeIdentifierStart(c) || c == '$' || c == '_' || c == '\\')) {
						s.back();
						key = (String)parseLiteral(s);
						point = 2;
						break;
					} else if (c == '/') {
						s.back();
						skipComment(s);
						break;
					}
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			}
		}
		
		if (point != Integer.MAX_VALUE) {
			handleParseError(new JSONParseException("object is not closed.", s));
		}
		return map;
	}

	
	private List<Object> parseArray(JSONSource s) throws IOException {
		int point = 0; // 0 '[' 1 'value' 2 ',' ... ']' E
		List<Object> list = new ArrayList<Object>();
		
		int n = -1;
		while (point != Integer.MAX_VALUE && (n = s.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case '\r':
			case '\n':
			case ' ':
			case '\t':
				break;
			case '[':
				if (point == 0) {
					point = 1;
					break;
				} else if (point == 1) {
					s.back();
					list.add(parseArray(s));
					point = 2;
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			case ',':
				if (point == 2) {
					point = 1;
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			case ']':
				if (point == 1 || point == 2) {
					point = Integer.MAX_VALUE;
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			case '{':
				if (point == 1){
					s.back();
					list.add(parseObject(s));
					point = 2;
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			case '\'':
				if (!this.extendedMode) {
					handleParseError(new JSONParseException("unexpected char: "+c, s));
				}
			case '"':
				if (point == 1) {
					s.back();
					list.add(parseString(s));
					point = 2;
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			case 't':
			case 'f':
			case 'n':
				if (point == 1) {
					s.back();
					list.add(parseLiteral(s));
					point = 2;
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			default:
				if (point == 1 && ((c == '-') || (c >= '0' && c <= '9'))) {
					s.back();
					list.add(parseNumber(s));
					point = 2;
					break;
				} else if (this.extendedMode && c == '/') {
					s.back();
					skipComment(s);
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			}
		}
		
		if (point != Integer.MAX_VALUE) {
			handleParseError(new JSONParseException("array is not closed.", s));
		}
		return list;
	}
	
	private String parseString(JSONSource s) throws IOException {
		int point = 0; // 0 '"' 1 'c' ... '"' E
		StringBuilder sb = new StringBuilder();
		char start = '\0';
		
		int n = -1;
		while (point != Integer.MAX_VALUE && (n = s.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case '\\':
				if (point == 1) {
					s.back();
					sb.append(parseEscape(s));
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			case '\'':
				if (!this.extendedMode) {
					if (point == 1 && c >= 0x20) {
						sb.append(c);
						break;
					}
					handleParseError(new JSONParseException("unexpected char: "+c, s));
				}
			case '"':
				if (point == 0) {
					start = c;
					point = 1;
					break;
				} else if (point == 1 && start == c) {
					point = Integer.MAX_VALUE;
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			default:
				if (point == 1 && c >= 0x20) {
					sb.append(c);
					point = 1;
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			}
		}
		
		return sb.toString();
	}
	
	
	private Object parseLiteral(JSONSource s) throws IOException {
		int point = 0; // 0 'IdStart' 1 'IdPart' ... !'IdPart' E
		StringBuilder sb = new StringBuilder();
		
		int n = -1;
		while (point != Integer.MAX_VALUE && (n = s.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case '\\':
				s.back();
				c = parseEscape(s);
			default:
				if (point == 0 && (Character.isUnicodeIdentifierStart(c) || c == '$' || c == '_')
					|| (point == 1 && Character.isUnicodeIdentifierPart(c))){
					sb.append(c);
					point = 1;
				} else {
					s.back();
					point = Integer.MAX_VALUE;
				}
			}
		}
		
		String literal = sb.toString();
		return (LITERALS.containsKey(literal)) ? LITERALS.get(literal) : literal;
	}	
	
	private Number parseNumber(JSONSource s) throws IOException {
		int point = 0; // 0 '(-)' 1 '0' | ('[1-9]' 2 '[0-9]*') 3 '(.)' 4 '[0-9]' 5 '[0-9]*' 6 'e|E' 7 '[+|-]' 8 '[0-9]' E
		StringBuilder sb = new StringBuilder();
		
		int n = -1;
		while (point != Integer.MAX_VALUE && (n = s.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case '+':
				if (point == 7) {
					sb.append(c);
					point = 8;
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			case '-':
				if (point == 0) {
					sb.append(c);
					point = 1;
					break;
				} else if (point == 7) {
					sb.append(c);
					point = 8;
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			case '.':
				if (point == 2 || point == 3) {
					sb.append(c);
					point = 4;
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			case 'e':
			case 'E':
				if (point == 2 || point == 3 || point == 5 || point == 6) {
					sb.append(c);
					point = 7;
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
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
						point = Integer.MAX_VALUE;
					} else {
						handleParseError(new JSONParseException("unexpected char: "+c, s));
					}
				} else if (point == 2 || point == 3 || point == 5 || point == 6) {
					s.back();
					point = Integer.MAX_VALUE;
				} else {
					handleParseError(new JSONParseException("unexpected char: "+c, s));
				}
			}
		}
		
		return new BigDecimal(sb.toString());
	}
	
	private char parseEscape(JSONSource s) throws IOException {
		int point = 0; // 0 '\' 1 'u' 2 'x' 3 'x' 4 'x' 5 'x' E
		char escape = '\0';
		
		int n = -1;
		while (point != Integer.MAX_VALUE && (n = s.next()) != -1) {
			char c = (char)n;
			if (point == 0) {
				if (c == '\\') {
					point = 1;
				} else {
					handleParseError(new JSONParseException("unexpected char: "+c, s));
				}
			} else if (point == 1) {
				switch(c) {
				case '"':
				case '\\':
				case '/':
					escape = c;
					point = Integer.MAX_VALUE;
					break;
				case 'b':
					escape = '\b';
					point = Integer.MAX_VALUE;
					break;
				case 'f':
					escape = '\f';
					point = Integer.MAX_VALUE;
					break;
				case 'n':
					escape = '\n';
					point = Integer.MAX_VALUE;
					break;
				case 'r':
					escape = '\r';
					point = Integer.MAX_VALUE;
					break;
				case 't':
					escape = '\t';
					point = Integer.MAX_VALUE;
					break;
				case 'u':
					point = 2;
					break;
				default:
					if (this.extendedMode) {
						escape = c;
						point = Integer.MAX_VALUE;
						break;
					}
					handleParseError(new JSONParseException("unexpected char: "+c, s));
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
						point = Integer.MAX_VALUE;
					}
				} else {
					handleParseError(new JSONParseException("illegal unicode escape", s));
				}
			}
		}
		
		return escape;
	}
	
	private void skipComment(JSONSource s) throws IOException {
		int point = 0; // 0 '/' 1 '*' 2  '*' 3 '/' E or  0 '/' 1 '/' 4  '\r|\n|\r\n' E
		
		int n = -1;
		while (point != Integer.MAX_VALUE && (n = s.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case '/':
				if (point == 0) {
					point = 1;
					break;
				} else if (point == 1) {
					point = 4;
					break;
				} else if (point == 3) {
					point = Integer.MAX_VALUE;
					break;
				} else if (point == 2 || point == 4) {
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			case '*':
				if (point == 1) {
					point = 2;
					break;
				} else if (point == 2) {
					point = 3;
					break;
				} else if (point == 3 || point == 4) {
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			case '\n':
			case '\r':
				if (point == 2 || point == 3) {
					point = 2;
					break;
				} else if (point == 4) {
					point = Integer.MAX_VALUE;
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			default:
				if (point == 2 || point == 4) {
					break;
				} else if (point == 3) {
					point = 2;
					break;
				}
				handleParseError(new JSONParseException("unexpected char: "+c, s));
			}
		}	
	}
	
	protected void handleParseError(JSONParseException e) {
		throw e;
	}
	
	@SuppressWarnings("unchecked")
	protected Object convert(Object value, Class c, Type type) throws Exception {
		Object data = null;
		
		try {
			if (c.isPrimitive()) {
				if (c.equals(boolean.class)) {
					if (value == null) {
						data = false;
					} else if (value instanceof Number) {
						data = !value.equals(0);
					} else {
						String s = value.toString();
						if (s.length() == 0
							|| s.equalsIgnoreCase("false")
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
						data = new Byte(value.toString());
					}
				} else if (c.equals(short.class)) {
					if (value == null) {
						data = 0;
					} else if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1 : 0;
					} else if (value instanceof Number) {
						data = ((Number)value).shortValue();
					} else {
						data = new Short(value.toString());
					}
				} else if (c.equals(int.class)) {
					if (value == null) {
						data = 0;
					} else if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1 : 0;
					} else if (value instanceof Number) {
						data = ((Number)value).intValue();
					} else {
						data = new Integer(value.toString());
					}
				} else if (c.equals(long.class)) {
					if (value == null) {
						data = 0l;
					} else if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1l : 0l;
					} else if (value instanceof Number) {
						data = ((Number)value).longValue();
					} else {
						data = new Long(value.toString());
					}
				} else if (c.equals(float.class)) {
					if (value == null) {
						data = 0.0f;
					} else if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1.0f : Float.NaN;
					} else if (value instanceof Number) {
						data = ((Number)value).floatValue();
					} else {
						data = new Float(value.toString());
					}
				} else if (c.equals(double.class)) {
					if (value == null) {
						data = 0.0;
					} else if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1.0 : Double.NaN;
					} else if (value instanceof Number) {
						data = ((Number)value).doubleValue();
					} else {
						data = new Double(value.toString());
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
						data = new Byte(value.toString());
					}
				} else if (Short.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1 : 0;
					} else if (value instanceof Number) {
						data = ((Number)value).shortValue();
					} else {
						data = new Short(value.toString());
					}				
				} else if (Integer.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1 : 0;
					} else if (value instanceof Number) {
						data = ((Number)value).intValue();
					} else {
						data = new Integer(value.toString());
					}
				} else if (Long.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1l : 0l;
					} else if (value instanceof Number) {
						data = ((Number)value).longValue();
					} else {
						data = new Long(value.toString());
					}
				} else if (Float.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1.0f : Float.NaN;
					} else if (value instanceof Number) {
						data = ((Number)value).floatValue();
					} else {
						data = new Float(value.toString());
					}
				} else if (Double.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1.0 : Double.NaN;
					} else if (value instanceof Number) {
						data = ((Number)value).doubleValue();
					} else {
						data = new Double(value.toString());
					}
				} else if (BigInteger.class.equals(c)) {				
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? BigInteger.ONE : BigInteger.ZERO;
					} else if (value instanceof BigDecimal) {
						data = ((BigDecimal)value).toBigInteger();
					} else {
						data = (new BigDecimal(value.toString())).toBigInteger();
					}
				} else if (BigDecimal.class.equals(c) || Number.class.equals(c)) {
					data = new BigDecimal(value.toString());
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
					try {
						a.append(value.toString());
					} catch (Exception e) {
						// no handle
					}
				} else if (Date.class.isAssignableFrom(c)) {
					if (value instanceof Number) {
						Date date = (Date)create(c);
						date.setTime(((Number)value).longValue());
						data = date;
					} else {
						DateFormat format = DateFormat.getDateTimeInstance();
						Date date = (Date)create(c);
						date.setTime(format.parse(value.toString()).getTime());
						data = date;
					}
				} else if (Calendar.class.isAssignableFrom(c)) {
					Calendar cal = (Calendar)create(c);
					if (value instanceof Number) {
						cal.setTimeInMillis(((Number)value).longValue());
					} else {
						DateFormat format = DateFormat.getDateTimeInstance();
						try {
							cal.setTime(format.parse(value.toString()));
						} catch (Exception e) {
							data = null;
						}
					}
					data = cal;
				} else if (Collection.class.isAssignableFrom(c)) {
					if (value instanceof Collection) {
						Collection collection = (Collection)create(c);
						if (type instanceof ParameterizedType) {
							ParameterizedType pType = (ParameterizedType)type;
							Type[] cTypes = pType.getActualTypeArguments();
							Type cType = (cTypes != null && cTypes.length > 0) ? cTypes[0] : Object.class;
							Class cClasses = null;
							if (cType instanceof ParameterizedType) {
								cClasses = (Class)((ParameterizedType)cType).getRawType();
							} else if (cType instanceof Class) {
								cClasses = (Class)cType;
							} else {
								cClasses = Object.class;
							}
							for (Object o : (Collection)value) {
								collection.add(convert(o, cClasses, cType));
							}
						} else {
							collection.addAll((Collection)value);
						}
						data = collection;
					}
				} else if (c.isArray()) {
					if (value instanceof Collection) {
						Object array = Array.newInstance(c.getComponentType(), ((Collection)value).size());
						int i = 0;
						Class cClass = c.getComponentType();
						Type cType = (type instanceof GenericArrayType) ? 
								((GenericArrayType)type).getGenericComponentType() : cClass;
						
						for (Object o : (Collection)value) {
							Array.set(array, i++, convert(o, cClass, cType));
						}
						data = array;
					} else if (value instanceof CharSequence && byte.class.equals(c.getComponentType())) {
						data = decodeBase64((CharSequence)value);
					}
				} else if (Map.class.isAssignableFrom(c)) {
					if (value instanceof Map) {
						Map map = (Map)create(c);
						if (type instanceof ParameterizedType) {
							ParameterizedType pType = (ParameterizedType)type;
							Type[] cTypes = pType.getActualTypeArguments();
							Class[] cClasses = new Class[2];
							for (int i = 0; i < cClasses.length; i++) {
								if (cTypes[i] instanceof ParameterizedType) {
									cClasses[i] = (Class)((ParameterizedType)cTypes[i]).getRawType();
								} else if (cTypes[i] instanceof Class) {
									cClasses[i] = (Class)cTypes[i];
								} else {
									cClasses[i] = Object.class;
								}
							}
							for (Object entry : ((Map)value).entrySet()) {
								Map.Entry entry2 = (Map.Entry)entry;
								
								map.put(convert(entry2.getKey(), cClasses[0], cTypes[0]),
										convert(entry2.getValue(), cClasses[1], cTypes[1]));
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
						s = (String[])convert(value, String[].class, String[].class);
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
				} else {
					if (value instanceof Map) {
						Object o = create(c);
						if (o != null) {
							Map<String, Object> map = new HashMap<String, Object>();
							
							for (Field f : c.getFields()) {
								if (!Modifier.isStatic(f.getModifiers())
										&& Modifier.isPublic(f.getModifiers())
										&& !Modifier.isTransient(f.getModifiers())) {
									map.put(f.getName(), f);
								}
							}
							
							for (Method m : c.getMethods()) {
								String name = m.getName();
								if (!Modifier.isStatic(m.getModifiers())
										&& Modifier.isPublic(m.getModifiers())
										&& name.startsWith("set")
										&& name.length() > 3
										&& Character.isUpperCase(name.charAt(3))
										&& m.getParameterTypes().length == 1
										&& m.getReturnType().equals(void.class)) {
									
									String key = null;
									if (!(name.length() > 4 && Character.isUpperCase(name.charAt(4)))) {
										char[] carray = name.toCharArray();
										carray[3] = Character.toLowerCase(carray[3]);
										key = new String(carray, 3, carray.length-3);
									} else {
										key = name.substring(3);
									}
									
									map.put(key, m);
								}
							}
							
							boolean access = tryAccess(c);
							
							for (Object o2 : ((Map)value).entrySet()) {
								Map.Entry entry = (Map.Entry)o2;
								Object target = map.get(entry.getKey());
								if (target instanceof Method) {
									Method m = (Method)target;
									try {
										if (access) m.setAccessible(true);
										m.invoke(o, convert(entry.getValue(), m.getParameterTypes()[0], m.getGenericParameterTypes()[0]));
									} catch (Exception e) {
										handleConvertError((String)entry.getKey(), entry.getValue(), m.getParameterTypes()[0], m.getGenericParameterTypes()[0], e);
									}
								} else {
									Field f = (Field)target;
									try {
										if (access) f.setAccessible(true);
										f.set(o, convert(entry.getValue(), f.getType(), f.getGenericType()));
									} catch (Exception e) {
										handleConvertError((String)entry.getKey(), entry.getValue(), f.getType(), f.getGenericType(), e);
									}
								}
							}
							data = o;
						}
					}
				}
			}
		} catch (Exception e) {
			handleConvertError(null, value, c, type, e);
		}
		
		return data;
	}
	
	protected void handleConvertError(String key, Object value, Class c, Type type, Exception e) throws Exception {
		// no handle
	}
	
	protected Object create(Class<?> c) throws Exception {
		Object instance = null;
		
		if (c.isInterface()) {
			if (SortedMap.class.equals(c)) {
				instance = new TreeMap();
			} else if (Map.class.equals(c)) {
				instance = new HashMap();
			} else if (SortedSet.class.equals(c)) {
				instance = new TreeSet();
			} else if (Set.class.equals(c)) {
				instance = new HashSet();
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
		} else if (c.isMemberClass()) {
			Class eClass = c.getEnclosingClass();
			Constructor con = c.getDeclaredConstructor(eClass);
			if (!Modifier.isPublic(con.getModifiers()) && tryAccess(c)) {
				con.setAccessible(true);
			}
			instance = con.newInstance((eClass.equals(this.contextClass)) ? this.context : null);
		} else {
			Constructor con = c.getDeclaredConstructor((Class[])null);
			if (!Modifier.isPublic(con.getModifiers()) && tryAccess(c)) {
				con.setAccessible(true);
			}
			instance = con.newInstance((Object[])null);
		}
		
		return instance;
	}
	
	/**
	 * Invokes the targeted method for the specified object, with the specified json parameters.
	 * 
	 * @param o the object the underlying method is invoked from
	 * @param methodName the invoked method name
	 * @param json the parameters used for the method call. json should be array, or appended '[' and ']'.
	 */
	public Object invoke(Object o, String methodName, CharSequence json) throws Exception {
		List values = (json != null) ? parseArray(new CharSequenceJSONSource(json)) : null;
		return invokeDynamic(o, methodName, values);
	}
		
	protected Object invokeDynamic(Object o, String methodName, List values) throws Exception {
		if (values == null) {
			values = Collections.EMPTY_LIST;
		}
		
		Class target = o.getClass();
		Method method = null;
		loop: do {
			for (Method m : target.getDeclaredMethods()) {
				if (methodName.equals(m.getName())
						&& !Modifier.isStatic(m.getModifiers())
						&& Modifier.isPublic(m.getModifiers())) {
					if (method == null && values.size() == m.getParameterTypes().length) {
						method = m;
						break loop;
					}
				}
			}
			
			target = target.getSuperclass();
		} while (method == null && target != null);
		
		if (method == null || limit(method)) {
			throw new NoSuchMethodException();
		}
		
		Class c = o.getClass();
		
		Class[] paramTypes = method.getParameterTypes();
		Object[] params = new Object[paramTypes.length];
		for (int i = 0; i < params.length; i++) {
			params[i] = convert(values.get(i), paramTypes[i], paramTypes[i]);
		}
		
		boolean access = (!Modifier.isPublic(c.getModifiers()) 
				&& !Modifier.isPrivate(c.getModifiers())
				&& this.context != null
				&& c.getPackage().equals(this.context.getClass().getPackage()));
		
		if (access) method.setAccessible(true);
		return method.invoke(o, params);
	}
	
	protected boolean limit(Method method) {
		return method.getDeclaringClass().equals(Object.class);
	}
	
	private boolean tryAccess(Class c) {
		int modifier = c.getModifiers();
		if (this.contextClass == null || Modifier.isPublic(modifier)) return false;
		
		if (Modifier.isPrivate(modifier)) {
			return this.contextClass.equals(c.getEnclosingClass());
		}
		return c.getPackage().equals(this.contextClass.getPackage());
	}

	private static String encodeBase64(byte[] data) {
		if (data == null) return null;
		
		char[] buffer = new char[data.length / 3 * 4 + ((data.length % 3 == 0) ? 0 : 4)];
		
		String map = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

		int buf = 0;
		for (int i = 0; i < data.length; i++) {
			switch (i % 3) {
				case 0 :
					buffer[i / 3 * 4] = map.charAt((data[i] & 0xFC) >> 2);
					buf = (data[i] & 0x03) << 4;
					if (i + 1 == data.length) {
						buffer[i / 3 * 4 + 1] = map.charAt(buf);
						buffer[i / 3 * 4 + 2] = '=';
						buffer[i / 3 * 4 + 3] = '=';
					}
					break;
				case 1 :
					buf += (data[i] & 0xF0) >> 4;
					buffer[i / 3 * 4 + 1] = map.charAt(buf);
					buf = (data[i] & 0x0F) << 2;
					if (i + 1 == data.length) {
						buffer[i / 3 * 4 + 2] = map.charAt(buf);
						buffer[i / 3 * 4 + 3] = '=';
					}
					break;
				case 2 :
					buf += (data[i] & 0xC0) >> 6;
					buffer[i / 3 * 4 + 2] = map.charAt(buf);
					buffer[i / 3 * 4 + 3] = map.charAt(data[i] & 0x3F);
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
}

interface JSONSource {
	int next() throws IOException;
	void back();
	long getLines();
	long getColumns();
}

class CharSequenceJSONSource implements JSONSource {
	private int lines = 0;
	private int columns = 0;
	
	private CharSequence cs;
	private int count = 0;
	
	public CharSequenceJSONSource(CharSequence cs) {
		this.cs = cs;
	}
	
	public int next() {
		if (count < cs.length()) {
			char c = cs.charAt(count++);
			if (c == '\r' || (c == '\n' && count > 1 && cs.charAt(count-2) != '\r')) {
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
		count--;
		columns--;
	}
	
	public long getLines() {
		return lines;
	}
	
	public long getColumns() {
		return columns;
	}
	
	public String toString() {
		return cs.subSequence(count-columns, count).toString();
	}
}

class ReaderJSONSource implements JSONSource{
	private long lines = 0;
	private long columns = 0;

	private Reader reader;
	private char[] buf = new char[256];
	private int start = 0;
	private int end = 0;
	
	public ReaderJSONSource(Reader reader) {
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
		start = (start+1) % buf.length;
		return c;
	}
	
	public void back() {
		columns--;
		start = (start+buf.length-1) % buf.length;
	}
	
	public long getLines() {
		return lines;
	}
	
	public long getColumns() {
		return columns;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		int maxlength = (columns < buf.length) ? (int)columns : buf.length-1;
		for (int i = maxlength; i >= 0; i--) {
			sb.append(buf[(start-2+buf.length-i) % (buf.length-1)]);
		}
		return sb.toString();
	}
}
