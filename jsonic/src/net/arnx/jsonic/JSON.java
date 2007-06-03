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

import java.lang.Package;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.text.DateFormat;
import java.text.ParseException;
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
 * @version 0.9
 * @see <a href="http://www.rfc-editor.org/rfc/rfc4627.txt">RFC 4627</a>
 * @see <a href="http://www.apache.org/licenses/LICENSE-2.0">the Apache License, Version 2.0</a>
 */
public class JSON {
	private Object context = null;
	
	public JSON() {
	}
	
	public JSON(Object context) {
		this.context = context;
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

	/**
	 * Encodes a object into a json string.
	 * 
	 * @param source a object to encode.
	 * @return a json string
	 */
	public static String encode(Object source) {
		JSON json = new JSON();
		json.setPrettyPrint(false);
		return json.format(source, new StringBuilder()).toString();
	}
	
	/**
	 * Encodes a object into a json string.
	 * 
	 * @param source a object to encode.
	 * @param prettyPrint output a json string with indent, space or break.
	 * @return a json string
	 */
	public static String encode(Object source, boolean prettyPrint) {
		JSON json = new JSON();
		json.setPrettyPrint(prettyPrint);
		return json.format(source, new StringBuilder()).toString();
	}
	
	/**
	 * Decodes a json string into a object.
	 * 
	 * @param source a json string to decode
	 * @return a decoded object
	 * @exception ParseException if the beginning of the specified string cannot be parsed.
	 */
	public static Object decode(String source) throws ParseException {
		return (new JSON()).parse(source);
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
		return (new JSON()).parse(source, c);
	}
	
	public String format(Object source) {
		return format(source, new StringBuilder()).toString();
	}
	
	public StringBuilder format(Object source, StringBuilder sb) {
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
		
		Package pk = null;
		if (context != null) {
			pk = context.getClass().getPackage();
		} else if (source != null) {
			pk = source.getClass().getPackage();
		}
		
		format(pk, source, sb, 0);
		return sb;
	}
	
	private void format(Package pk, Object o, StringBuilder sb, int level) {
		if (level > this.maxDepth) {
			throw new IllegalArgumentException("nest level is over max depth.");
		}		
		
		if (o instanceof Character) {
			o = o.toString();
		} else if (o instanceof char[]) {
			o = new String((char[])o);
		}
		
		if (o == null) {
			sb.append("null");
		} else if (o instanceof CharSequence) {
			formatString(sb, (CharSequence)o);
		} else if (o instanceof Float) {
			float f = ((Float)o).floatValue();
			if (Float.isNaN(f)) {
				sb.append((this.extendedMode) ? "NaN" : "\"NaN\"");
			} else if (f == Float.POSITIVE_INFINITY) {
				sb.append((this.extendedMode) ? "Infinity" : "\"Infinity\"");
			} else if (f == Float.NEGATIVE_INFINITY) {
				sb.append((this.extendedMode) ? "-Infinity" : "\"-Infinity\"");
			} else {
				sb.append(f);
			}
		} else if (o instanceof Double) {
			double d = ((Double)o).doubleValue();
			if (Double.isNaN(d)) {
				sb.append((this.extendedMode) ? "NaN" : "\"NaN\"");
			} else if (d == Double.POSITIVE_INFINITY) {
				sb.append((this.extendedMode) ? "Infinity" : "\"Infinity\"");
			} else if (d == Double.NEGATIVE_INFINITY) {
				sb.append((this.extendedMode) ? "-Infinity" : "\"-Infinity\"");
			} else {
				sb.append(d);
			}
		} else if (o instanceof Byte) {
			sb.append(((Byte)o).byteValue() & 0xFF);
		} else if (o instanceof Number) {
			sb.append(o);
		} else if (o instanceof Boolean) {
			sb.append(o);
		} else if (o instanceof Date) {
			if (this.extendedMode) {
				sb.append("new Date(").append(((Date)o).getTime()).append(")");
			} else {
				sb.append(((Date)o).getTime());
			}
		} else if (o instanceof Calendar) {
			if (this.extendedMode) {
				sb.append("new Date(").append(((Calendar)o).getTimeInMillis()).append(")");
			} else {
				sb.append(((Calendar)o).getTimeInMillis());
			}
		} else if (o instanceof Locale) {
			Locale locale = (Locale)o;
			if (locale.getLanguage() != null && locale.getLanguage().length() > 0) {
				if (locale.getCountry() != null && locale.getCountry().length() > 0) {
					format(pk, locale.getLanguage() + "-" + locale.getCountry(), sb, level);
				} else {
					format(pk, locale.getLanguage(), sb, level);
				}
			} else {
				sb.append("null");
			}
		} else if (o instanceof Pattern) {
			formatString(sb, ((Pattern)o).pattern());
		} else if (o instanceof Object[]) {
			Object[] array = (Object[])o;
			sb.append('[');
			if (array.length == 0) {
				sb.append(']');
			} else {
				if (this.prettyPrint) sb.append('\n');
				
				for (int i = 0; i < array.length; i++) {
					if (array[i] != o) {
						if (this.prettyPrint) {
							for (int j = 0; j < level+1; j++) sb.append('\t');
						}
						format(pk, array[i], sb, level+1);
						sb.append(',');
						if (this.prettyPrint) sb.append('\n');
					}
				}
				if (this.prettyPrint) {
					sb.setLength(sb.length()-2);
					sb.append('\n');
					for (int j = 0; j < level; j++) sb.append('\t');
					sb.append(']');
				} else {
					sb.setCharAt(sb.length()-1, ']');
				}
			}
		} else if (o instanceof boolean[]) {
			boolean[] array = (boolean[])o;
			sb.append('[');
			if (array.length == 0) {
				sb.append(']');
			} else {
				for (int i = 0; i < array.length; i++) {
			        if (this.prettyPrint && i != 0) sb.append(' ');
					sb.append(array[i]);
					sb.append(',');
				}
				sb.setCharAt(sb.length()-1, ']');
			}
		} else if (o instanceof byte[]) {
			sb.append('"').append(encodeBase64((byte[])o)).append('"');
		} else if (o instanceof short[]) {
			short[] array = (short[])o;
			sb.append('[');
			if (array.length == 0) {
				sb.append(']');
			} else {
				for (int i = 0; i < array.length; i++) {
			        if (this.prettyPrint && i != 0) sb.append(' ');
					sb.append(array[i]).append(',');
				}
				sb.setCharAt(sb.length()-1, ']');
			}
		} else if (o instanceof int[]) {
			int[] array = (int[])o;
			sb.append('[');
			if (array.length == 0) {
				sb.append(']');
			} else {
				for (int i = 0; i < array.length; i++) {
			        if (this.prettyPrint && i != 0) sb.append(' ');
					sb.append(array[i]).append(',');
				}
				sb.setCharAt(sb.length()-1, ']');
			}
		} else if (o instanceof long[]) {
			long[] array = (long[])o;
			sb.append('[');
			if (array.length == 0) {
				sb.append(']');
			} else {
				for (int i = 0; i < array.length; i++) {
			        if (this.prettyPrint && i != 0) sb.append(' ');
					sb.append(array[i]).append(',');
				}
				sb.setCharAt(sb.length()-1, ']');
			}
		} else if (o instanceof float[]) {
			float[] array = (float[])o;
			sb.append('[');
			if (array.length == 0) {
				sb.append(']');
			} else {
				for (int i = 0; i < array.length; i++) {
			        if (this.prettyPrint && i != 0) sb.append(' ');
					if (Float.isNaN(array[i])) {
						sb.append((this.extendedMode) ? "NaN" : "\"NaN\"");
					} else if (array[i] == Float.POSITIVE_INFINITY) {
						sb.append((this.extendedMode) ? "Infinity" : "\"Infinity\"");
					} else if (array[i] == Float.NEGATIVE_INFINITY) {
						sb.append((this.extendedMode) ? "-Infinity" : "\"-Infinity\"");
					} else {
						sb.append(array[i]);
					}
					sb.append(',');
				}
				sb.setCharAt(sb.length()-1, ']');
			}
		} else if (o instanceof double[]) {
			double[] array = (double[])o;
			sb.append('[');
			if (array.length == 0) {
				sb.append(']');
			} else {
				for (int i = 0; i < array.length; i++) {
			        if (this.prettyPrint && i != 0) sb.append(' ');
					if (Double.isNaN(array[i])) {
						sb.append((this.extendedMode) ? "NaN" : "\"NaN\"");
					} else if (array[i] == Double.POSITIVE_INFINITY) {
						sb.append((this.extendedMode) ? "Infinity" : "\"Infinity\"");
					} else if (array[i] == Double.NEGATIVE_INFINITY) {
						sb.append((this.extendedMode) ? "-Infinity" : "\"-Infinity\"");
					} else {
						sb.append(array[i]);
					}
					sb.append(',');
				}
				sb.setCharAt(sb.length()-1, ']');
			}
		} else if (o instanceof Collection) {
			Collection array = (Collection)o;
			sb.append('[');
			if (array.size() == 0) {
				sb.append(']');
			} else {
				if (this.prettyPrint) sb.append('\n');
				
				for (Object item : array) {
					if (item != o) {
						if (this.prettyPrint) {
							for (int j = 0; j < level+1; j++) sb.append('\t');
						}
						format(pk, item, sb, level+1);
						sb.append(',');
						if (this.prettyPrint) sb.append('\n');
					}
				}
				
				if (this.prettyPrint) {
					sb.setLength(sb.length()-2);
					sb.append('\n');
					for (int j = 0; j < level; j++) sb.append('\t');
					sb.append(']');
				} else {
					sb.setCharAt(sb.length()-1, ']');
				}
			}
		} else if (o instanceof Map) {			
			Map map = (Map)o;
			sb.append('{');
			if (map.isEmpty()) {
				sb.append('}');
			} else {
				if (this.prettyPrint) sb.append('\n');
				
				for (Object entry : map.entrySet()) {
					Object value = ((Map.Entry)entry).getValue();
					
					if (value != o) {
						if (this.prettyPrint) {
							for (int j = 0; j < level+1; j++) sb.append('\t');
						}
						formatString(sb, (String)((Map.Entry)entry).getKey()).append(':');
						if (this.prettyPrint) sb.append(' ');
						format(pk, value, sb, level+1);
						sb.append(',');
						if (this.prettyPrint) sb.append('\n');
					}
				}
				
				if (this.prettyPrint) {
					sb.setLength(sb.length()-2);
					sb.append('\n');
					for (int j = 0; j < level; j++) sb.append('\t');
					sb.append('}');
				} else {
					sb.setCharAt(sb.length()-1, '}');
				}
			}
		} else {
			Class c = o.getClass();
			boolean access = (!Modifier.isPublic(c.getModifiers()) 
					&& !Modifier.isPrivate(c.getModifiers())
					&& c.getPackage().equals(pk));
			
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
			
			sb.append('{');
			if (map.isEmpty()) {
				sb.append('}');
			} else {
				if (this.prettyPrint) sb.append('\n');
				
				for (Object o2 : map.entrySet()) {
					Map.Entry entry = (Map.Entry)o2;
					
					if (this.prettyPrint) {
						for (int j = 0; j < level+1; j++) sb.append('\t');
					}
					formatString(sb, (String)entry.getKey()).append(':');
					if (this.prettyPrint) sb.append(' ');
		        	format(pk, entry.getValue(), sb, level+1);
					sb.append(',');
					if (this.prettyPrint) sb.append('\n');
				}
			
				if (this.prettyPrint) {
					sb.setLength(sb.length()-2);
					sb.append('\n');
					for (int j = 0; j < level; j++) sb.append('\t');
					sb.append('}');
				} else {
					sb.setCharAt(sb.length()-1, '}');
				}
			}
		}
	}
	
	private StringBuilder formatString(StringBuilder sb, CharSequence cs) {
		sb.append('"');
		for (int i = 0; i < cs.length(); i++) {
			char c = cs.charAt(i);
			switch (c) {
			case '"': 
			case '\\': 
				sb.append('\\').append(c);
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			default: 
				sb.append(c);
			}
		}
		sb.append('"');
		
		return sb;
	}

	public Object parse(CharSequence source) throws ParseException {
		if (source == null) {
			throw new IllegalArgumentException("source text is null.");
		}
		
		int[] pc = new int[3]; // position counter
		Object o = null;
		while (pc[0] < source.length()) {
			char c = source.charAt(pc[0]);
			switch (c) {
			case '\n':
				line(pc);
				break;
			case '\r':
				if (pc[0]+1 == source.length() || source.charAt(pc[0]+1) != '\n') {
					line(pc);
					break;
				}
			case ' ':
			case '\t':
				next(pc, 1);
				break;
			case '{':
				if (o == null) {
					o = parseObject(source, pc);
					break;
				}
				handleParseError("unexpected char: "+c, source, pc[0], pc[1], pc[2]);
			case '[':
				if (o == null) {
					o = parseArray(source, pc);
					break;
				}
				handleParseError("unexpected char: "+c, source, pc[0], pc[1], pc[2]);
			case '\'':
			case '"':
				if (this.extendedMode) {
					o = parseString(source, pc);
					break;
				}
			case 't':
			case 'f':
			case 'n':
				if (this.extendedMode) {
					o = parseLiteral(source, pc);
					break;
				}
			default:
				if (this.extendedMode) {
					if ((c == '-') || (c >= '0' && c <= '9')) {
						o = parseNumber(source, pc);
						break;
					} else if (c == '/') {
						skipComment(source, pc);
						break;
					}
					handleParseError("unexpected char: "+c, source, pc[0], pc[1], pc[2]);
				} else {
					handleParseError("a JSON text must start with a object or array", source, pc[0], pc[1], pc[2]);
				}
			}
		}
		if (o == null) {
			handleParseError("source text is empty.", source, pc[0], pc[1], pc[2]);
		}
		return o;
	}	
	
	@SuppressWarnings("unchecked")
	public <T> T parse(CharSequence source, Class<? extends T> c) throws Exception {
		return (T)convert(c.getPackage(), parse(source), c, c);
	}
	
	/**
	 * Invokes the targeted method for the specified object, with the specified json parameters.
	 * 
	 * @param o the object the underlying method is invoked from
	 * @param methodName the invoked method name
	 * @param json the parameters used for the method call. json should be array, or appended '[' and ']'.
	 */
	public Object invoke(Object o, String methodName, CharSequence json) throws Exception {
		List values = (json != null) ? parseArray(json, new int[3]) : null;
		
		if (values == null) {
			values = Collections.EMPTY_LIST;
		}
		
		Class target = o.getClass();
		Method method = null;
		do {
			for (Method m : target.getDeclaredMethods()) {
				if (methodName.equals(m.getName())
						&& !Modifier.isStatic(m.getModifiers())
						&& Modifier.isPublic(m.getModifiers())) {
					if (method == null && values.size() == m.getParameterTypes().length) {
						method = m;
					}
				}
			}
			
			target = target.getSuperclass();
		} while (method == null && target != null);
		
		if (method == null || limit(method)) {
			throw new NoSuchMethodException();
		}
		
		Class c = o.getClass();
		Package pk = c.getPackage();
		boolean access = (!Modifier.isPublic(c.getModifiers()) 
				&& !Modifier.isPrivate(c.getModifiers()));
		
		Class[] paramTypes = method.getParameterTypes();
		Object[] params = new Object[paramTypes.length];
		for (int i = 0; i < params.length; i++) {
			params[i] = convert(pk, values.get(i), paramTypes[i], paramTypes[i]);
		}
		
		if (access) method.setAccessible(true);
		return method.invoke(o, params);
	}
	
	private Map<String, Object> parseObject(CharSequence s, int[] pc) throws ParseException {
		Map<String, Object> map = new HashMap<String, Object>();
		String key = null;
		int point = 0; // 0 '{' 1 'key' 2 ':' 3 'value' 4 ',' ... '}' E
		
		while (pc[0] < s.length()) {
			char c = s.charAt(pc[0]);
			switch (c) {
			case '\n':
				line(pc);
				break;
			case '\r':
				if (pc[0]+1 == s.length() || s.charAt(pc[0]+1) != '\n') {
					line(pc);
					break;
				}
			case ' ':
			case '\t':
				next(pc, 1);
				break;
			case '{':
				if (point == 0) {
					next(pc, 1);
					point = 1;
					break;
				} else if (point == 3){
					map.put(key, parseObject(s, pc));
					point = 4;
					break;
				}
				handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
			case ':':
				if (point == 2) {
					next(pc, 1);
					point = 3;
					break;
				}
				handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
			case ',':
				if (point == 4) {
					next(pc, 1);
					point = 1;
					break;
				}
				handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
			case '}':
				if (point == 1 || point == 4) {
					next(pc, 1);
					point = Integer.MAX_VALUE;
					break;
				}
				handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
			case '\'':
				if (!this.extendedMode) {
					handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
				}
			case '"':
				if (point == 1) {
					key = parseString(s, pc);
					point = 2;
					break;
				} else if (point == 3) {
					map.put(key, parseString(s, pc));
					point = 4;
					break;
				}
				handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
			case '[':
				if (point == 3) {
					map.put(key, parseArray(s, pc));
					point = 4;
					break;
				}
				handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
			case 't':
			case 'f':
			case 'n':
				if (point == 3) {
					map.put(key, parseLiteral(s, pc));
					point = 4;
					break;
				}
				if (!this.extendedMode) {
					handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
				}
			default:
				if (point == 3 && ((c == '-') || (c >= '0' && c <= '9'))) {
					map.put(key, parseNumber(s, pc));
					point = 4;
					break;
				} else if (this.extendedMode) {
					if (point == 1 && (Character.isUnicodeIdentifierStart(c) || c == '$' || c == '_' || c == '\\')) {
						key = parseIdentifier(s, pc);
						point = 2;
						break;
					} else if (c == '/') {
						skipComment(s, pc);
						break;
					}
				}
				handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
			}
			
			if (point == Integer.MAX_VALUE) {
				break;
			}
		}
		
		if (point != Integer.MAX_VALUE) {
			handleParseError("object is not closed.", s, pc[0], pc[1], pc[2]);
		}
		return map;
	}

	
	private List<Object> parseArray(CharSequence s, int[] pc) throws ParseException {
		List<Object> list = new ArrayList<Object>();
		int point = 0; // 0 '[' 1 'value' 2 ',' ... ']' E
		
		while (pc[0] < s.length()) {
			char c = s.charAt(pc[0]);
			switch (c) {
			case '\n':
				line(pc);
				break;
			case '\r':
				if (pc[0]+1 == s.length() || s.charAt(pc[0]+1) != '\n') {
					line(pc);
					break;
				}
			case ' ':
			case '\t':
				next(pc, 1);
				break;
			case '[':
				if (point == 0) {
					next(pc, 1);
					point = 1;
					break;
				} else if (point == 1) {
					list.add(parseArray(s, pc));
					point = 2;
					break;
				}
				handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
			case ',':
				if (point == 2) {
					next(pc, 1);
					point = 1;
					break;
				}
				handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
			case ']':
				if (point == 1 || point == 2) {
					next(pc, 1);
					point = Integer.MAX_VALUE;
					break;
				}
				handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
			case '{':
				if (point == 1){
					list.add(parseObject(s, pc));
					point = 2;
					break;
				}
				handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
			case '\'':
				if (!this.extendedMode) {
					handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
				}
			case '"':
				if (point == 1) {
					list.add(parseString(s, pc));
					point = 2;
					break;
				}
				handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
			case 't':
			case 'f':
			case 'n':
				if (point == 1) {
					list.add(parseLiteral(s, pc));
					point = 2;
					break;
				}
				handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
			default:
				if (point == 1 && ((c == '-') || (c >= '0' && c <= '9'))) {
					list.add(parseNumber(s, pc));
					point = 2;
					break;
				} else if (this.extendedMode && c == '/') {
					skipComment(s, pc);
					break;
				}
				handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
			}
			
			if (point == Integer.MAX_VALUE) {
				break;
			}
		}
		
		if (point != Integer.MAX_VALUE) {
			handleParseError("array is not closed.", s, pc[0], pc[1], pc[2]);
		}
		return list;
	}
	
	private String parseString(CharSequence s, int[] pc) throws ParseException {
		StringBuilder sb = new StringBuilder();
		char start = '\0';
		int point = 0; // 0 '"' 1 'value' 2 'value' ... '"' E
		
		while (pc[0] < s.length()) {
			char c = s.charAt(pc[0]);
			switch(c) {
			case '\\':
				next(pc, 1);
				if ((point == 1 || point == 2) && pc[0]+1 < s.length()) {
					c = s.charAt(pc[0]);
					switch (c) {
					case '"':
					case '\\':
					case '/':
						sb.append(c);
						next(pc, 1);
						break;
					case 'b':
						sb.append('\b');
						next(pc, 1);
						break;
					case 'f':
						sb.append('\f');
						next(pc, 1);
						break;
					case 'n':
						sb.append('\n');
						next(pc, 1);
						break;
					case 'r':
						sb.append('\r');
						next(pc, 1);
						break;
					case 't':
						sb.append('\t');
						next(pc, 1);
						break;
					case 'u':
						try {
							sb.append((char)Integer.parseInt(s.subSequence(pc[0]+1, pc[0]+5).toString(), 16));
							next(pc, 5);
						} catch (Exception e) {
							handleParseError("illegal unicode escape", s, pc[0], pc[1], pc[2]);
						}
						break;
					default:
						if (this.extendedMode) {
							sb.append(c);
							next(pc, 1);
							break;
						}
						handleParseError("illegal escape character", s, pc[0], pc[1], pc[2]);
					}
					point = 2;
					break;
				}
				handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
			case '\'':
				if (!this.extendedMode) {
					if (c >= 0x20) {
						sb.append(c);
						next(pc, 1);
						point = 2;
						break;
					}
					handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
				}
			case '"':
				if (point == 0) {
					start = c;
					next(pc, 1);
					point = 1;
					break;
				} else if ((point == 1 || point == 2) && start == c) {
					next(pc, 1);
					point = Integer.MAX_VALUE;
					break;
				}
				handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
			default:
				if (c >= 0x20) {
					sb.append(c);
					next(pc, 1);
					point = 2;
					break;
				}
				handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
			}
			
			if (point == Integer.MAX_VALUE) {
				break;
			}
		}
		
		return sb.toString();
	}

	private String parseIdentifier(CharSequence s, int[] pc) throws ParseException {
		StringBuilder sb = new StringBuilder();
		int point = 0; // 0 'IdStart' 1 'IdPart' ... !'IdPart' E
		
		while (pc[0] < s.length()) {
			char c = s.charAt(pc[0]);
			switch(c) {
			case '\\':
				if ((point == 0 || point == 1) && pc[0]+1 < s.length()) {
					c = s.charAt(pc[0]);
					switch (c) {
					case 'u':
						try {
							sb.append((char)Integer.parseInt(s.subSequence(pc[0]+1, pc[0]+5).toString(), 16));
							next(pc, 5);
						} catch (Exception e) {
							handleParseError("illegal unicode escape", s, pc[0], pc[1], pc[2]);
						}
						break;
					default:
						handleParseError("illegal escape character", s, pc[0], pc[1], pc[2]);
					}
					point = 1;
					break;
				}
				handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
			default:
				if ((Character.isUnicodeIdentifierStart(c) || c == '$' || c == '_')
						|| (point == 1 && Character.isUnicodeIdentifierPart(c))) {
					sb.append(c);
					next(pc, 1);
					point = 1;
					break;
				} else {
					point = Integer.MAX_VALUE;
					break;
				}
			}
			
			if (point == Integer.MAX_VALUE) {
				break;
			}
		}
		
		String value = sb.toString();
		
		if (value.length() == 0) {
			handleParseError("unexpected char: "+s.charAt(pc[0]), s, pc[0], pc[1], pc[2]);
		}
		
		return value;
	}	
	
	private Number parseNumber(CharSequence s, int[] pc) throws ParseException {
		int start = pc[0];
		int point = 0; // 0 '(-)' 1 '0' | ('[1-9]' 2 '[0-9]*') 3 '(.)' 4 '[0-9]' 5 '[0-9]*' 6 'e|E' 7 '[+|-]' 8 '[0-9]' E
		
		while (pc[0] < s.length()) {
			char c = s.charAt(pc[0]);
			
			switch(c) {
			case '+':
				if (point == 7) {
					next(pc, 1);
					point = 8;
					break;
				}
				handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
			case '-':
				if (point == 0) {
					next(pc, 1);
					point = 1;
					break;
				} else if (point == 7) {
					next(pc, 1);
					point = 8;
					break;
				}
				handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
			case '.':
				if (point == 2 || point == 3) {
					next(pc, 1);
					point = 4;
					break;
				}
				handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
			case 'e':
			case 'E':
				if (point == 2 || point == 3 || point == 5 || point == 6) {
					next(pc, 1);
					point = 7;
					break;
				}
				handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
			default:
				if (c >= '0' && c <= '9') {
					if (point == 0 || point == 1) {
						next(pc, 1);
						point = (c == '0') ? 3 : 2;
					} else if (point == 2 || point == 5) {
						next(pc, 1);
					} else if (point == 4) {
						next(pc, 1);
						point = 5;
					} else if (point == 7 || point == 8) {
						next(pc, 1);
						point = Integer.MAX_VALUE;
					} else {
						handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);					
					}
				} else if (point == 2 || point == 3 || point == 5 || point == 6) {
					point = Integer.MAX_VALUE;
				} else {
					handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);					
				}
			}
			
			if (point == Integer.MAX_VALUE) {
				break;
			}
		}
		
		return new BigDecimal(s.subSequence(start, pc[0]).toString());
	}
	
	private Boolean parseLiteral(CharSequence s, int[] pc) throws ParseException {
		Boolean literal = null;
		
		char c = s.charAt(pc[0]);
		switch (c) {
		case 't':
			if (s.length() > pc[0]+3 
					&& s.charAt(pc[0]+1) == 'r' && s.charAt(pc[0]+2) == 'u' && s.charAt(pc[0]+3) == 'e') {
				next(pc, 4);
				literal = Boolean.TRUE;
				break;
			}
			handleParseError("'true' expected", s, pc[0], pc[1], pc[2]);	
		case 'f':
			if (s.length() > pc[0]+4 
					&& s.charAt(pc[0]+1) == 'a' && s.charAt(pc[0]+2) == 'l' && s.charAt(pc[0]+3) == 's' && s.charAt(pc[0]+4) == 'e') {
				next(pc, 5);
				literal = Boolean.FALSE;
				break;
			}
			handleParseError("'false' expected", s, pc[0], pc[1], pc[2]);	
		case 'n':
			if (s.length() > pc[0]+3
					&& s.charAt(pc[0]+1) == 'u' && s.charAt(pc[0]+2) == 'l' && s.charAt(pc[0]+3) == 'l') {
				next(pc, 4);
				literal = null;
				break;
			}
			handleParseError("'null' expected", s, pc[0], pc[1], pc[2]);	
		default:
			handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
		}
		
		return literal;
	}
	
	private void skipComment(CharSequence s, int[] pc) throws ParseException {
		int point = 0; // 0 '/*' 1  '*/' E or  0 '//' 1  '\r|\n|\r\n' E
		boolean isMulti = false;
		
		while (pc[0] < s.length()) {
			char c = s.charAt(pc[0]);
			switch(c) {
			case '/':
				if (point == 0 && pc[0]+1 < s.length() 
						&& (s.charAt(pc[0]+1) == '*' || s.charAt(pc[0]+1) == '/')) {
					isMulti = (s.charAt(pc[0]+1) == '*');
					point = 1;
					next(pc, 2);
					break;
				}
			default:
				if (point == 1) {
					switch (c) {
					case '*':
						if (isMulti && pc[0]+1 < s.length() && s.charAt(pc[0]+1) == '/') {
							point = Integer.MAX_VALUE;
							next(pc, 2);
							break;
						}
					case '\n':
						line(pc);
						if (!isMulti) point = Integer.MAX_VALUE;
						break;
					case '\r':
						if (pc[0]+1 == s.length() || s.charAt(pc[0]+1) != '\n') {
							line(pc);
							if (!isMulti) point = Integer.MAX_VALUE;
							break;
						}
					default:
						next(pc, 1);
					}
					break;
				}
				handleParseError("unexpected char: "+c, s, pc[0], pc[1], pc[2]);
			}
			
			if (point == Integer.MAX_VALUE) {
				break;
			}
		}	
	}
	
	private void next(int[] pc, int n) {
		pc[0]+=n;
		pc[2]+=n;
	}
	
	private void line(int[] pc) {
		pc[0]++;
		pc[1]++;
		pc[2] = 0;
	}
	
	protected void handleParseError(String message, CharSequence s, int pos, int line, int col) throws ParseException {
		StringBuilder sb = new StringBuilder();
		sb.append(line+1).append(": ").append(message).append('\n');

		int start = pos-col;
		if (start >= 16) start = pos - 16;
		if (pos+1 < s.length()) {
			sb.append(s.subSequence(start, pos+1));
		} else {
			sb.append(s.subSequence(start, pos));
		}
		sb.append(" <=");
		
		throw new ParseException(sb.toString(), pos);
	}
	
	@SuppressWarnings("unchecked")
	protected Object convert(Package pk, Object value, Class c, Type type) throws Exception {
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
				} else if (String.class.equals(c) || CharSequence.class.equals(c)) {
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
				} else if (Pattern.class.equals(c)) {
					data = Pattern.compile(value.toString());
				} else if (Locale.class.equals(c)) {
					String[] s = null;
					if (value instanceof Collection || value.getClass().isArray()) {
						s = (String[])convert(pk, value, String[].class, String[].class);
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
								collection.add(convert(pk, o, cClasses, cType));
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
							Array.set(array, i++, convert(pk, o, cClass, cType));
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
								
								map.put(convert(pk, entry2.getKey(), cClasses[0], cTypes[0]),
										convert(pk, entry2.getValue(), cClasses[1], cTypes[1]));
							}
						} else {
							map.putAll((Map)value);
						}
						data = map;
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
							
							boolean access = (!Modifier.isPublic(c.getModifiers()) 
									&& !Modifier.isPrivate(c.getModifiers())
									&& c.getPackage().equals(pk));
							
							for (Object o2 : ((Map)value).entrySet()) {
								Map.Entry entry = (Map.Entry)o2;
								Object target = map.get(entry.getKey());
								if (target instanceof Method) {
									Method m = (Method)target;
									try {
										if (access) m.setAccessible(true);
										m.invoke(o, convert(pk, entry.getValue(), m.getParameterTypes()[0], m.getGenericParameterTypes()[0]));
									} catch (Exception e) {
										handleConvertError((String)entry.getKey(), entry.getValue(), m.getParameterTypes()[0], m.getGenericParameterTypes()[0], e);
									}
								} else {
									Field f = (Field)target;
									try {
										if (access) f.setAccessible(true);
										f.set(o, convert(pk, entry.getValue(), f.getType(), f.getGenericType()));
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
		} else if (c.getEnclosingClass() != null
				&& this.context != null
				&& this.context.getClass().equals(c.getEnclosingClass())) {
			Constructor con = c.getDeclaredConstructor(c.getEnclosingClass());
			if (!Modifier.isPublic(con.getModifiers())) con.setAccessible(true);
			instance = con.newInstance(this.context);
		} else {
			Constructor con = c.getDeclaredConstructor((Class[])null);
			if (!Modifier.isPublic(con.getModifiers())) con.setAccessible(true);
			instance = con.newInstance((Object[])null);
		}
		
		return instance;
	}
	
	protected boolean limit(Method method) {
		return method.getDeclaringClass().equals(Object.class);
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
