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

import java.beans.Introspector;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Flushable;
import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.Date;
import java.util.Calendar;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.ResourceBundle;
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
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;

import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
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
 * Bar bar = JSON.decode(new FileInputStream("bar.json"), Bar.class);
 * Bar bar = JSON.decode(new FileReader("bar.json"), Bar.class);
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
 * <tr><td>java.lang.CharSequence</td><td rowspan="10">string</td></tr>
 * <tr><td>char[]</td></tr>
 * <tr><td>java.lang.Character</td></tr>
 * <tr><td>char</td></tr>
 * <tr><td>java.util.TimeZone</td></tr>
 * <tr><td>java.util.regex.Pattern</td></tr>
 * <tr><td>java.lang.reflect.Type</td></tr>
 * <tr><td>java.lang.reflect.Member</td></tr>
 * <tr><td>java.net.URI</td></tr>
 * <tr><td>java.net.URL</td></tr>
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
 * <tr><td>object</td><td>java.util.LinkedHashMap</td></tr>
 * <tr><td>array</td><td>java.util.ArrayList</td></tr>
 * <tr><td>string</td><td>java.lang.String</td></tr>
 * <tr><td>number</td><td>java.math.BigDecimal</td></tr>
 * <tr><td>true/false</td><td>java.lang.Boolean</td></tr>
 * <tr><td>null</td><td>null</td></tr>
 * </table>
 * 
 * @author Hidekatsu Izuno
 * @version 1.1.0
 * @see <a href="http://www.rfc-editor.org/rfc/rfc4627.txt">RFC 4627</a>
 * @see <a href="http://www.apache.org/licenses/LICENSE-2.0">the Apache License, Version 2.0</a>
 */
public class JSON {	
	/**
	 * Setup your custom class for using static method. default: net.arnx.jsonic.JSON
	 */
	public static Class prototype = JSON.class;
	
	private static final Map<Class, Object> PRIMITIVE_MAP = new IdentityHashMap<Class, Object>();
	
	private static Class<?>[] dynaBeanClasses = null;
	
	static {
		PRIMITIVE_MAP.put(boolean.class, false);
		PRIMITIVE_MAP.put(byte.class, (byte)0);
		PRIMITIVE_MAP.put(short.class, (short)0);
		PRIMITIVE_MAP.put(int.class, 0);
		PRIMITIVE_MAP.put(long.class, 0l);
		PRIMITIVE_MAP.put(float.class, 0.0f);
		PRIMITIVE_MAP.put(double.class, 0.0);
		PRIMITIVE_MAP.put(char.class, '\0');
		
		try {
			dynaBeanClasses = new Class<?>[] {
				Class.forName("org.apache.commons.beanutils.DynaBean"),
				Class.forName("org.apache.commons.beanutils.DynaClass"),
				Class.forName("org.apache.commons.beanutils.DynaProperty")
			};
		} catch (Exception e) {
			// no handle
		}
	}
	
	private static JSON newInstance() {
		JSON instance = null;
		try {
			instance = (JSON)prototype.newInstance();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		return instance;
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
		JSON json = JSON.newInstance();
		json.setPrettyPrint(prettyPrint);		
		return json.format(source);
	}

	/**
	 * Encodes a object into a json string.
	 * 
	 * @param source a object to encode.
	 * @param out a destination to output a json string.
	 * @exception IOException if I/O Error occurred.
	 */
	public static void encode(Object source, OutputStream out) throws IOException {
		JSON.newInstance().format(source, new OutputStreamWriter(out, "UTF-8"));
	}

	/**
	 * Encodes a object into a json string.
	 * 
	 * @param source a object to encode.
	 * @param out a destination to output a json string.
	 * @param prettyPrint output a json string with indent, space or break.
	 * @exception IOException if I/O Error occurred.
	 */
	public static void encode(Object source, OutputStream out, boolean prettyPrint) throws IOException {
		JSON json = JSON.newInstance();
		json.setPrettyPrint(prettyPrint);		
		json.format(source, new OutputStreamWriter(out, "UTF-8"));
	}

	/**
	 * Encodes a object into a json string.
	 * 
	 * @param source a object to encode.
	 * @param appendable a destination to output a json string.
	 * @exception IOException if I/O Error occurred.
	 */
	public static void encode(Object source, Appendable appendable) throws IOException {
		JSON.newInstance().format(source, appendable);
	}

	/**
	 * Encodes a object into a json string.
	 * 
	 * @param source a object to encode.
	 * @param appendable a destination to output a json string.
	 * @param prettyPrint output a json string with indent, space or break.
	 * @exception IOException if I/O Error occurred.
	 */
	public static void encode(Object source, Appendable appendable, boolean prettyPrint) throws IOException {
		JSON json = JSON.newInstance();
		json.setPrettyPrint(prettyPrint);		
		json.format(source, appendable);
	}
	
	/**
	 * Decodes a json string into a object.
	 * 
	 * @param source a json string to decode
	 * @return a decoded object
	 * @exception JSONParseException if the beginning of the specified string cannot be parsed.
	 */
	public static Object decode(String source) throws JSONParseException {
		return JSON.newInstance().parse(source);
	}
	
	/**
	 * Decodes a json string into a typed object.
	 * 
	 * @param source a json string to decode
	 * @param cls class for converting
	 * @return a decoded object
	 * @exception JSONParseException if the beginning of the specified string cannot be parsed.
	 * @exception JSONConvertException if it cannot convert a class from a JSON value.
	 */
	public static <T> T decode(String source, Class<? extends T> cls) throws JSONParseException {
		return JSON.newInstance().parse(source, cls);
	}
	
	/**
	 * Decodes a json string into a typed object.
	 * 
	 * @param source a json string to decode
	 * @param type type for converting
	 * @return a decoded object
	 * @exception JSONParseException if the beginning of the specified string cannot be parsed.
	 * @exception JSONConvertException if it cannot convert a class from a JSON value.
	 */
	public static Object decode(String source, Type type) throws JSONParseException {
		return JSON.newInstance().parse(source, type);
	}

	/**
	 * Decodes a json stream into a object. (character encoding should be Unicode)
	 * 
	 * @param in a json stream to decode
	 * @return a decoded object
	 * @exception IOException if I/O error occurred.
	 * @exception JSONParseException if the beginning of the specified string cannot be parsed.
	 */
	public static Object decode(InputStream in) throws IOException, JSONParseException {
		return JSON.newInstance().parse(in);
	}

	/**
	 * Decodes a json stream into a object. (character encoding should be Unicode)
	 * 
	 * @param in a json stream to decode
	 * @param cls class for converting
	 * @return a decoded object
	 * @exception IOException if I/O error occurred.
	 * @exception JSONParseException if the beginning of the specified string cannot be parsed.
	 */
	public static <T> T decode(InputStream in, Class<? extends T> cls) throws IOException, JSONParseException {
		return JSON.newInstance().parse(in, cls);
	}

	/**
	 * Decodes a json stream into a object. (character encoding should be Unicode)
	 * 
	 * @param in a json stream to decode
	 * @param type type for converting
	 * @return a decoded object
	 * @exception IOException if I/O error occurred.
	 * @exception JSONParseException if the beginning of the specified string cannot be parsed.
	 */
	public static Object decode(InputStream in, Type type) throws IOException, JSONParseException {
		return JSON.newInstance().parse(in, type);
	}
	
	/**
	 * Decodes a json stream into a object.
	 * 
	 * @param reader a json stream to decode
	 * @return a decoded object
	 * @exception IOException if I/O error occurred.
	 * @exception JSONParseException if the beginning of the specified string cannot be parsed.
	 */
	public static Object decode(Reader reader) throws IOException, JSONParseException {
		return JSON.newInstance().parse(reader);
	}

	/**
	 * Decodes a json stream into a object.
	 * 
	 * @param reader a json stream to decode
	 * @param cls class for converting
	 * @return a decoded object
	 * @exception IOException if I/O error occurred.
	 * @exception JSONParseException if the beginning of the specified string cannot be parsed.
	 */
	public static <T> T decode(Reader reader, Class<? extends T> cls) throws IOException, JSONParseException {
		return JSON.newInstance().parse(reader, cls);
	}

	/**
	 * Decodes a json stream into a object.
	 * 
	 * @param reader a json stream to decode
	 * @param type type for converting
	 * @return a decoded object
	 * @exception IOException if I/O error occurred.
	 * @exception JSONParseException if the beginning of the specified string cannot be parsed.
	 */
	public static Object decode(Reader reader, Type type) throws IOException, JSONParseException {
		return JSON.newInstance().parse(reader, type);
	}
	
	private Object contextObject = null;
	private Locale locale;
	private boolean prettyPrint = false;	
	private int maxDepth = 32;

	public JSON() {
	}
	
	public JSON(int maxDepth) {
		setMaxDepth(maxDepth);
	}
	
	/**
	 * Sets context for inner class.
	 * 
	 * @param value context object
	 */
	public void setContext(Object value) {
		this.contextObject = value;
	}

	
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
	 * Output json string is to human-readable format.
	 * 
	 * @param value true to format human-readable, false to shorten.
	 */
	public void setPrettyPrint(boolean value) {
		this.prettyPrint = value;
	}
	
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
	
	/**
	 * Format a object into a json string.
	 * 
	 * @param source a object to encode.
	 * @return a json string
	 */
	public String format(Object source) {
		String text = null;
		try {
			text = format(source, new StringBuilder(1000)).toString();
		} catch (IOException e) {
			// no handle;
		}
		return text;
	}
	
	public OutputStream format(Object source, OutputStream out) throws IOException {
		format(source, new OutputStreamWriter(out, "UTF-8"));
		return out;
	}
	
	/**
	 * Format a object into a json string.
	 * 
	 * @param source a object to encode.
	 * @param ap a destination. example: StringBuilder, Writer, ...
	 * @return a json string
	 */
	public Appendable format(Object source, Appendable ap) throws IOException {
		Context context = new Context();
		
		if (contextObject != null) context.scope = contextObject.getClass();
		if (context.scope == null) context.scope = source.getClass().getEnclosingClass();
		if (context.scope == null) context.scope = source.getClass();
		context.enter('$');
		format(context, source, ap);
		context.exit();
		return ap;
	}
	
	private Appendable format(Context context, Object o, Appendable ap) throws IOException {
		if (context.getLevel() > this.maxDepth) {
			o = null;
		}
		
		if (o instanceof Class) {
			o = ((Class)o).getName();
		} else if (o instanceof Character
				|| o instanceof Type
				|| o instanceof Member
				|| o instanceof URL
				|| o instanceof URI
				|| o instanceof File) {
			o = o.toString();
		} else if (o instanceof Enum) {
			o = ((Enum)o).ordinal();
		} else if (o instanceof char[]) {
			o = new String((char[])o);
		} else if (o instanceof Iterable) {
			o = ((Iterable)o).iterator();
		} else if (o instanceof Object[]) {
			o = Arrays.asList((Object[])o).iterator();
		} else if (o instanceof Number) {
			NumberFormat f = context.format(NumberFormat.class);
			o = (f != null) ? f.format(o) : o;
		} else if (o instanceof Date) {
			DateFormat f = context.format(DateFormat.class);
			o = (f != null) ? f.format(o) : ((Date)o).getTime();
		} else if (o instanceof Calendar) {
			o = ((Calendar)o).getTimeInMillis();
		} else if (o instanceof Pattern) {
			o = ((Pattern)o).pattern();
		} else if (o instanceof TimeZone) {
			o = ((TimeZone)o).getID();
		} else if (o instanceof InetAddress) {
			o = ((InetAddress)o).getHostAddress();
		} else if (o instanceof Charset) {
			o = ((Charset)o).name();
		} else if (o instanceof Locale) {
			o = ((Locale)o).toString().replace('_', '-');
		} else if (o instanceof Node) {
			if (o instanceof Document) {
				o = ((Document)o).getDocumentElement();
			} else if (o instanceof Element) {
				o = (Element)o;
			} else if (o instanceof CharacterData && !(o instanceof Comment)) {
				o = ((CharacterData)o).getData();
			} else {
				o = null;
			}
		}
		
		if (context.getLevel() == 0 && (o == null
				|| o instanceof CharSequence
				|| o instanceof Boolean
				|| o instanceof Number
				|| o instanceof Date)) {
			throw new IllegalArgumentException(getMessage("json.format.IllegalRootTypeError"));
		}
		
		if (o == null) {
			ap.append("null");
		} else if (o instanceof CharSequence) {
			formatString((CharSequence)o, ap);
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
		} else if (o instanceof byte[]) {
			ap.append('"').append(Base64.encode((byte[])o)).append('"');
		} else if (o instanceof boolean[]) {
			ap.append('[');
			boolean[] array = (boolean[])o;
			for (int i = 0; i < array.length; i++) {
				ap.append(String.valueOf(array[i]));
				if (i != array.length-1) {
					ap.append(',');
					if (this.prettyPrint) ap.append(' ');
				}
			}
			ap.append(']');
		} else if (o.getClass().isArray()) {
			NumberFormat f = context.format(NumberFormat.class);			
			
			ap.append('[');
			if (o instanceof short[]) {
				short[] array = (short[])o;
				for (int i = 0; i < array.length; i++) {
					if (f != null) {
						formatString(f.format(array[i]), ap);
					} else {
						ap.append(String.valueOf(array[i]));
					}
					if (i != array.length-1) {
						ap.append(',');
						if (this.prettyPrint) ap.append(' ');
					}
				}
			} else if (o instanceof int[]) {
				int[] array = (int[])o;
				for (int i = 0; i < array.length; i++) {
					if (f != null) {
						formatString(f.format(array[i]), ap);
					} else {
						ap.append(String.valueOf(array[i]));
					}
					if (i != array.length-1) {
						ap.append(',');
						if (this.prettyPrint) ap.append(' ');
					}
				}
			} else if (o instanceof long[]) {
				long[] array = (long[])o;
				for (int i = 0; i < array.length; i++) {
					if (f != null) {
						formatString(f.format(array[i]), ap);
					} else {
						ap.append(String.valueOf(array[i]));
					}
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
					} else if (f != null) {
						formatString(f.format(array[i]), ap);
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
					} else if (f != null) {
						formatString(f.format(array[i]), ap);
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
		} else if (o instanceof Iterator) {
			Iterator<?> t = (Iterator<?>)o;
			ap.append('[');
			boolean isEmpty = !t.hasNext();
			for (int i = 0; t.hasNext(); i++) {
				Object item = t.next();
				if (this.prettyPrint) {
					ap.append('\n');
					for (int j = 0; j < context.getLevel()+1; j++) ap.append('\t');
				}
				if (item == o) item = null;
				context.enter(i);
				format(context, item, ap);
				context.exit();
				if (t.hasNext()) ap.append(',');
			}
			if (this.prettyPrint && !isEmpty) {
				ap.append('\n');
				for (int j = 0; j < context.getLevel(); j++) ap.append('\t');
			}
			ap.append(']');
		} else if (o instanceof Enumeration) {
			Enumeration<?> e = (Enumeration<?>)o;
			ap.append('[');
			boolean isEmpty = !e.hasMoreElements();
			for (int i = 0; e.hasMoreElements(); i++) {
				Object item = e.nextElement();
				if (this.prettyPrint) {
					ap.append('\n');
					for (int j = 0; j < context.getLevel()+1; j++) ap.append('\t');
				}
				if (item == o) item = null;
				context.enter(i);
				format(context, item, ap);
				context.exit();
				if (e.hasMoreElements()) ap.append(',');
			}
			if (this.prettyPrint && !isEmpty) {
				ap.append('\n');
				for (int j = 0; j < context.getLevel(); j++) ap.append('\t');
			}
			ap.append(']');
		} else if (o instanceof Element) {
			Element elem = (Element)o;
			ap.append('[');
			formatString(elem.getTagName(), ap);
			
			if (elem.hasAttributes()) {
				NamedNodeMap names = elem.getAttributes();
				ap.append(',');
				if (this.prettyPrint) {
					ap.append('\n');
					for (int j = 0; j < context.getLevel()+1; j++) ap.append('\t');
				}
				ap.append('{');
				for (int i = 0; i < names.getLength(); i++) {
					if (i != 0) {
						ap.append(',');
					}
					if (this.prettyPrint && names.getLength() > 1) {
						ap.append('\n');
						for (int j = 0; j < context.getLevel()+2; j++) ap.append('\t');
					}
					Node node = names.item(i);
					if (node instanceof Attr) {
						formatString(node.getNodeName(), ap);
						ap.append(':');
						if (this.prettyPrint) ap.append(' ');
						formatString(node.getNodeValue(), ap);
					}
				}
				if (this.prettyPrint && names.getLength() > 1) {
					ap.append('\n');
					for (int j = 0; j < context.getLevel()+1; j++) ap.append('\t');
				}
				ap.append('}');
			}
			if (elem.hasChildNodes()) {
				NodeList nodes = elem.getChildNodes();
				for (int i = 0; i < nodes.getLength(); i++) {
					Node node = nodes.item(i);
					if ((node instanceof Element) || (node instanceof CharacterData && !(node instanceof Comment))) {
						ap.append(',');
						if (this.prettyPrint) {
							ap.append('\n');
							for (int j = 0; j < context.getLevel()+1; j++) ap.append('\t');
						}
						context.enter(elem.hasAttributes() ? i+2 : i+1);
						format(context, node, ap);
						context.exit();
					}
				}
			}
			if (this.prettyPrint) {
				ap.append('\n');
				for (int j = 0; j < context.getLevel(); j++) ap.append('\t');
			}
			ap.append(']');
		} else {
			Map map = null;
			if (o instanceof Map) {
				map = (Map)o;
			} else if (dynaBeanClasses != null && dynaBeanClasses[0].isAssignableFrom(o.getClass())) {
				map = new TreeMap();
				try {
					Object dynaClass = dynaBeanClasses[0].getMethod("getDynaClass").invoke(o);
					Object[] dynaProperties = (Object[])dynaBeanClasses[1].getMethod("getDynaProperties").invoke(dynaClass);
					
					Method getName = dynaBeanClasses[2].getMethod("getName");
					Method get = dynaBeanClasses[0].getMethod("get", String.class);
					
					for (Object dp : dynaProperties) {
						try {
							Object name = getName.invoke(dp);
							map.put(name, get.invoke(o, name));
						} catch (InvocationTargetException ite) {
							// no handle
						}
					}
				} catch (Exception e) {
					// no handle
				}
			} else {
				map = context.getGetProperties(o.getClass());
			}
			
			ap.append('{');
			for (Iterator<Map.Entry> i = map.entrySet().iterator(); i.hasNext(); ) {
				Map.Entry entry = (Map.Entry)i.next();
				Object value = entry.getValue();
				JSONHint hint = null;
				if (value instanceof AnnotatedElement) {
					hint = ((AnnotatedElement)value).getAnnotation(JSONHint.class);
					try {
						if (entry.getValue() instanceof Method) {
							Method m = (Method)entry.getValue();
							value = m.invoke(o);
						} else {
							Field f = (Field)entry.getValue();
							value =  f.get(o);
						}
					} catch (Exception e) {
						// no handle
					}
				}
				if (entry.getKey() == null || value == o) continue; 
				
				if (this.prettyPrint) {
					ap.append('\n');
					for (int j = 0; j < context.getLevel()+1; j++) ap.append('\t');
				}
				formatString(entry.getKey().toString(), ap).append(':');
				if (this.prettyPrint) ap.append(' ');
				context.enter(entry.getKey(), hint);
				format(context, value, ap);
				context.exit();
				if (i.hasNext()) ap.append(',');
			}
			if (this.prettyPrint && !map.isEmpty()) {
				ap.append('\n');
				for (int j = 0; j < context.getLevel(); j++) ap.append('\t');
			}
			ap.append('}');
		}
		
		if (ap instanceof Flushable) ((Flushable)ap).flush();
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
			value = parse(new CharSequenceParserSource(cs));
		} catch (IOException e) {
			// never occur
		}
		return value; 
	}
	
	public <T> T parse(CharSequence s, Class<? extends T> cls)
		throws JSONParseException {
		return (T)parse(s, (Type)cls);
	}
	
	public Object parse(CharSequence s, Type type)
		throws JSONParseException {
		Object value = null;
		try {
			value = parse(new CharSequenceParserSource(s), type);
		} catch (IOException e) {
			// never occur
		}
		return value;
	}
	
	public Object parse(InputStream in) throws IOException, JSONParseException {
		return parse(new ReaderParserSource(in));
	}
	
	public <T> T parse(InputStream in, Class<? extends T> cls)
		throws IOException, JSONParseException, JSONConvertException {
		return (T)parse(in, (Type)cls);
	}
	
	public Object parse(InputStream in, Type type)
		throws IOException, JSONParseException, JSONConvertException {
		return parse(new ReaderParserSource(in), type);
	}
	
	public Object parse(Reader reader) throws IOException, JSONParseException {
		return parse(new ReaderParserSource(reader));
	}
	
	public <T> T parse(Reader reader, Class<? extends T> cls) 
		throws IOException, JSONParseException {
		return (T)parse(reader, (Type)cls);
	}
	
	public Object parse(Reader reader, Type type)
		throws IOException, JSONParseException {
		return parse(new ReaderParserSource(reader), type);
	}
	
	private Object parse(ParserSource s, Type type) 
		throws IOException, JSONParseException {
		return convert(parse(s), type);
	}
	
	private Object parse(ParserSource s) throws IOException, JSONParseException {
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
					o = parseArray(s, 1);
					break;
				}
				throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
			case '/':
			case '#':
				s.back();
				skipComment(s);
				break;
			default:
				if (o == null) {
					s.back();
					o = parseObject(s, 1);
					break;
				}
				throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
			}
		}
		
		return (o == null) ? new LinkedHashMap<String, Object>() : o;
	}	
	
	private Map<Object, Object> parseObject(ParserSource s, int level) throws IOException, JSONParseException {
		int point = 0; // 0 '{' 1 'key' 2 ':' 3 '\n'? 4 'value' 5 '\n'? 6 ',' ... '}' E
		Map<Object, Object> map = (level <= this.maxDepth) ? new LinkedHashMap<Object, Object>() : null;
		Object key = null;
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
				break;
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
					Object value = parseObject(s, level+1);
					if (level < this.maxDepth) map.put(key, value);
					point = 5;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case ':':
				if (point == 2) {
					point = 3;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case ',':
				if (point == 3) {
					if (level < this.maxDepth) map.put(key, null);
					point = 1;
				} else if (point == 5 || point == 6) {
					point = 1;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '}':
				if (start == '{' && (point == 1 || point == 3 || point == 5 || point == 6)) {
					if (point == 3) {
						if (level < this.maxDepth) map.put(key, null);
					}
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break loop;
			case '\'':
			case '"':
				if (point == 0) {
					s.back();
					point = 1;
				} else if (point == 1 || point == 6) {
					s.back();
					key = parseString(s);
					point = 2;
				} else if (point == 3) {
					s.back();
					String value = parseString(s);
					if (level < this.maxDepth) map.put(key, value);
					point = 5;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '[':
				if (point == 3) {
					s.back();
					List value = parseArray(s, level+1);
					if (level < this.maxDepth) map.put(key, value);
					point = 5;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '/':
			case '#':
				s.back();
				skipComment(s);
				if (point == 5) {
					point = 6;
				}
				break;
			default:
				if (point == 0) {
					s.back();
					point = 1;
				} else if (point == 1 || point == 6) {
					s.back();
					key = ((c == '-') || (c >= '0' && c <= '9')) ? parseNumber(s) : parseLiteral(s);
					point = 2;
				} else if (point == 3) {
					s.back();
					Object value = ((c == '-') || (c >= '0' && c <= '9')) ? parseNumber(s) : parseLiteral(s);
					if (level < this.maxDepth) map.put(key, value);
					point = 5;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
			}
		}
		
		if (n == -1) {
			if (point == 3 || point == 4) {
				if (level < this.maxDepth) map.put(key, null);
			} else if (point == 2) {
				throw createParseException(getMessage("json.parse.ObjectNotClosedError"), s);
			}
		}
		
		if ((n == -1) ? (start != '\0') : (n != '}')) {
			throw createParseException(getMessage("json.parse.ObjectNotClosedError"), s);
		}
		return map;
	}
	
	private List<Object> parseArray(ParserSource s, int level) throws IOException, JSONParseException {
		int point = 0; // 0 '[' 1 'value' 2 '\n'? 3 ',' ... ']' E
		List<Object> list = (level <= this.maxDepth) ? new ArrayList<Object>() : null;
		
		int n = -1;
		loop:while ((n = s.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case '\r':
			case '\n':
				if (point == 2) {
					point = 3;
				}
				break;
			case ' ':
			case '\t':
			case 0xFEFF: // BOM
				break;
			case '[':
				if (point == 0) {
					point = 1;
				} else if (point == 1 || point == 3) {
					s.back();
					List<Object> value = parseArray(s, level+1);
					if (level < this.maxDepth) list.add(value);
					point = 2;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case ',':
				if (point == 1) {
					if (level < this.maxDepth) list.add(null);
				} else if (point == 2 || point == 3) {
					point = 1;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case ']':
				if (point == 1 || point == 2 || point == 3) {
					if (level < this.maxDepth && point == 1 && !list.isEmpty()) {
						list.add(null);
					}
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break loop;					
			case '{':
				if (point == 1 || point == 3){
					s.back();
					Map<Object, Object> value = parseObject(s, level+1);
					if (level < this.maxDepth) list.add(value);
					point = 2;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '\'':
			case '"':
				if (point == 1 || point == 3) {
					s.back();
					String value = parseString(s);
					if (level < this.maxDepth) list.add(value);
					point = 2;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '/':
			case '#':
				s.back();
				skipComment(s);
				if (point == 2) {
					point = 3;
				}
				break;
			default:
				if (point == 1 || point == 3) {
					s.back();
					Object value = ((c == '-') || (c >= '0' && c <= '9')) ? parseNumber(s) : parseLiteral(s);
					if (level < this.maxDepth) list.add(value);
					point = 2;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
			}
		}
		
		if (n != ']') {
			throw createParseException(getMessage("json.parse.ArrayNotClosedError"), s);
		}
		return list;
	}
	
	private String parseString(ParserSource s) throws IOException, JSONParseException {
		int point = 0; // 0 '"|'' 1 'c' ... '"|'' E
		StringBuilder sb = s.getCachedBuilder();
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
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '\'':
			case '"':
				if (point == 0) {
					start = c;
					point = 1;
					break;
				} else if (point == 1) {
					if (start == c) {
						break loop;						
					} else {
						sb.append(c);
					}
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			default:
				if (point == 1) {
					sb.append(c);
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
			}
		}
		
		if (n != start) {
			throw createParseException(getMessage("json.parse.StringNotClosedError"), s);
		}
		return sb.toString();
	}
	
	private Object parseLiteral(ParserSource s) throws IOException, JSONParseException {
		int point = 0; // 0 'IdStart' 1 'IdPart' ... !'IdPart' E
		StringBuilder sb = s.getCachedBuilder();

		int n = -1;
		loop:while ((n = s.next()) != -1) {
			char c = (char)n;
			if (c == 0xFEFF) continue;
			
			if (c == '\\') {
				s.back();
				c = parseEscape(s);
			}
			
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
		
		String str = sb.toString();
		
		if ("null".equals(str)) return null;
		if ("true".equals(str)) return true;
		if ("false".equals(str)) return false;

		return str;
	}	
	
	private Number parseNumber(ParserSource s) throws IOException, JSONParseException {
		int point = 0; // 0 '(-)' 1 '0' | ('[1-9]' 2 '[0-9]*') 3 '(.)' 4 '[0-9]' 5 '[0-9]*' 6 'e|E' 7 '[+|-]' 8 '[0-9]' E
		StringBuilder sb = s.getCachedBuilder();
		
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
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
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
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '.':
				if (point == 2 || point == 3) {
					sb.append(c);
					point = 4;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case 'e':
			case 'E':
				if (point == 2 || point == 3 || point == 5 || point == 6) {
					sb.append(c);
					point = 7;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
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
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
				} else if (point == 2 || point == 3 || point == 5 || point == 6) {
					s.back();
					break loop;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
			}
		}
		
		return new BigDecimal(sb.toString());
	}
	
	private char parseEscape(ParserSource s) throws IOException, JSONParseException {
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
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
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
					escape |= (hex << ((5-point)*4));
					if (point != 5) {
						point++;
					} else {
						break loop;
					}
				} else {
					throw createParseException(getMessage("json.parse.IllegalUnicodeEscape", c), s);
				}
			}
		}
		
		return escape;
	}
	
	private void skipComment(ParserSource s) throws IOException, JSONParseException {
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
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '*':
				if (point == 1) {
					point = 2;
				} else if (point == 2) {
					point = 3;
				} else if (!(point == 3 || point == 4)) {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '\n':
			case '\r':
				if (point == 2 || point == 3) {
					point = 2;
				} else if (point == 4) {
					break loop;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '#':
				if (point == 0) {
					point = 4;
				} else if (point == 3) {
					point = 2;
				} else if (!(point == 2 || point == 4)) {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			default:
				if (point == 3) {
					point = 2;
				} else if (!(point == 2 || point == 4)) {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
			}
		}	
	}
	
	private JSONParseException createParseException(String message, ParserSource s) {
		return new JSONParseException("" + s.getLineNumber() + ": " + message + "\n" + s.toString() + " <- ?",
				s.getLineNumber(), s.getColumnNumber(), s.getOffset());
	}
	
	private String getMessage(String id, Object... args) {
		if (locale == null) locale = Locale.getDefault();
		ResourceBundle bundle = ResourceBundle.getBundle("net.arnx.jsonic.Messages", locale);
		return MessageFormat.format(bundle.getString(id), args);
	}
	
	public Object convert(Object value, Type type) throws JSONConvertException {
		Context context = new Context();
		
		Class<?> cls = getRawType(type);
		if (contextObject != null) context.scope = contextObject.getClass();
		if (context.scope == null) context.scope = cls.getEnclosingClass();
		if (context.scope == null) context.scope = cls;
		
		Object result = null;
		try {
			context.enter('$');
			result = convert(context, value, cls, type);
			context.exit();
		} catch (Exception e) {
			throw new JSONConvertException(getMessage("json.convert.ConversionError", 
					(value instanceof String) ? "\"" + value + "\"" : value, type, context), e);
		}
		return result;
	}
		
	/**
	 * Converts Map/List/Number/String/Boolean/null to other Java Objects. If you converts a lower level object in this method, 
	 * You should call convertChild method.
	 * 
	 * @param key property key object. Root node is '$'. When the parent is a array, the key is Integer, otherwise String. 
	 * @param value null or the instance of Map, List, Number, String or Boolean.
	 * @param c class for converting
	 * @param type generics type for converting. type equals to c if not generics.
	 * @return a converted object
	 * @throws Exception if conversion failed.
	 */
	protected <T> T convert(Context context, Object value, Class<? extends T> c, Type type) throws Exception {
		Object data = null;
		
		if (value == null) {
			if (c.isPrimitive()) {
				data = PRIMITIVE_MAP.get(c);
			}
		} else if (c.equals(type) && c.isAssignableFrom(value.getClass())) {
			data = value;
		} else if (value instanceof Map) {
			Map src = (Map)value;
			if (Map.class.isAssignableFrom(c)) {
				Map map = null;
				if (type instanceof ParameterizedType) {
					Type[] pts = ((ParameterizedType)type).getActualTypeArguments();
					Type pt0 = (pts != null && pts.length > 0) ? pts[0] : Object.class;
					Type pt1 = (pts != null && pts.length > 1) ? pts[1] : Object.class;
					Class<?> pc0 = getRawType(pt0);
					Class<?> pc1 = getRawType(pt1);
					
					if ((Object.class.equals(pc0) || String.class.equals(pc0))
							&& Object.class.equals(pc1)) {
						map = (Map)value;
					} else {
						map = (Map)create(context, c);
						for (Map.Entry entry : (Set<Map.Entry>)src.entrySet()) {
							context.enter('.');
							Object key = convert(context, entry.getKey(), pc0, pt0);
							context.exit();
							
							context.enter(entry.getKey());
							map.put(key, convert(context, entry.getValue(), pc1, pt1));
							context.exit();
						}
					}
				} else {
					map = (Map)create(context, c);
					map.putAll(src);
				}
				data = map;
			} else if (Collection.class.isAssignableFrom(c) || c.isArray()) {
				if (!(src instanceof SortedMap)) {
					src = new TreeMap(src);
				}
				data = convert(context, src.values(), c, type);
			} else if (c.isPrimitive() || c.isEnum()
					|| Number.class.isAssignableFrom(c)
					|| CharSequence.class.isAssignableFrom(c)
					|| Appendable.class.isAssignableFrom(c)
					|| Boolean.class.equals(c)
					|| Character.class.equals(c)
					|| Locale.class.equals(c)
					|| TimeZone.class.equals(c)
					|| Pattern.class.equals(c)
					|| File.class.equals(c)
					|| URL.class.equals(c)
					|| URI.class.equals(c)
					|| InetAddress.class.equals(c)
					|| Charset.class.equals(c)
					|| Class.class.equals(c)
				) {
				if (src.containsKey(null)) {
					Object target = src.get(null);
					if (target instanceof List) {
						List list = (List)target;
						target = (!list.isEmpty()) ? list.get(0) : null;
					}
					data = convert(context, target, c, type);
				}
			} else {
				Object o = create(context, c);
				if (o != null) {
					Map<String, AnnotatedElement> props = context.getSetProperties(c);
					for (Map.Entry entry : (Set<Map.Entry>)src.entrySet()) {
						String name = entry.getKey().toString();
						AnnotatedElement target = mapping(c, props, name);
						if (target == null) continue;
						
						context.enter(name, target.getAnnotation(JSONHint.class));
						if (target instanceof Method) {
							Method m = (Method)target;
							m.invoke(o, convert(context, entry.getValue(), m.getParameterTypes()[0], m.getGenericParameterTypes()[0]));
						} else {
							Field f = (Field)target;
							context.enter(name);
							f.set(o, convert(context, entry.getValue(), f.getType(), f.getGenericType()));
						}
						context.exit();
					}
					data = o;
				} else {
					throw new UnsupportedOperationException();
				}
			}
		} else if (value instanceof List) {
			List src = (List)value;
			if (Collection.class.isAssignableFrom(c)) {
				Collection collection = null;
				if (type instanceof ParameterizedType) {
					Type[] pts = ((ParameterizedType)type).getActualTypeArguments();
					Type pt = (pts != null && pts.length > 0) ? pts[0] : Object.class;
					Class<?> pc = getRawType(pt);
					
					if (Object.class.equals(pc)) {
						collection = src;
					} else {
						collection = (Collection)create(context, c);
						for (int i = 0; i < src.size(); i++) {
							context.enter(i);
							collection.add(convert(context, src.get(i), pc, pt));
							context.exit();
						}
					}
				} else {
					collection = (Collection)create(context, c);
					collection.addAll(src);
				}
				data = collection;
			} else if (c.isArray()) {
				Object array = Array.newInstance(c.getComponentType(), src.size());
				Class<?> pc = c.getComponentType();
				Type pt = (type instanceof GenericArrayType) ? 
						((GenericArrayType)type).getGenericComponentType() : pc;
				
				for (int i = 0; i < src.size(); i++) {
					context.enter(i);
					Array.set(array, i, convert(context, src.get(i), pc, pt));
					context.exit();
				}
				data = array;
			} else if (Map.class.isAssignableFrom(c)) {
				Map map = (Map)create(context, c);
				if (type instanceof ParameterizedType) {
					Type[] pts = ((ParameterizedType)type).getActualTypeArguments();
					Type pt0 = (pts != null && pts.length > 0) ? pts[0] : Object.class;
					Type pt1 = (pts != null && pts.length > 1) ? pts[1] : Object.class;
					Class<?> pc0 = getRawType(pt0);
					Class<?> pc1 = getRawType(pt1);

					for (int i = 0; i < src.size(); i++) {
						context.enter('.');
						Object key = convert(context, i, pc0, pt0);
						context.exit();
						
						context.enter(i);
						map.put(key, convert(context, src.get(i), pc1, pt1));
						context.exit();
					}
				} else {
					for (int i = 0; i < src.size(); i++) {
						map.put(i, src.get(i));
					}
				}
				data = map;
			} else if (Locale.class.equals(c)) {
				if (src.size() == 1) {
					data = new Locale(src.get(0).toString());
				} else if (src.size() == 2) {
					data = new Locale(src.get(0).toString(), src.get(1).toString());
				} else if (src.size() > 2) {
					data = new Locale(src.get(0).toString(), src.get(1).toString(), src.get(2).toString());
				}
			} else if (!src.isEmpty()) {
				data = convert(context, src.get(0), c, type);
			} else {
				throw new UnsupportedOperationException();
			}
		} else if (Number.class.isAssignableFrom((c.isPrimitive()) ? PRIMITIVE_MAP.get(c).getClass() : c)) {
			if (value instanceof String) {
				NumberFormat f = context.format(NumberFormat.class);
				if (f != null) value = f.parse((String)value);
			}
			
			if (byte.class.equals(c) || Byte.class.equals(c)) {
				if (value instanceof Boolean) {
					data = (((Boolean)value).booleanValue()) ? 1 : 0;
				} else if (value instanceof BigDecimal) {
					data = ((BigDecimal)value).byteValueExact();
				} else if (value instanceof Number) {
					data = ((Number)value).byteValue();
				} else {
					String str = value.toString().trim().toLowerCase();
					if (str.length() > 0) {
						int start = 0;
						if (str.charAt(0) == '+') {
							start++;
						}
						
						int num = 0;
						if (str.startsWith("0x", start)) {
							num = Integer.parseInt(str.substring(start+2), 16);
						} else {
							num = Integer.parseInt(str.substring(start));
						}
						
						data = (byte)((num > 127) ? num-256 : num);
					} else if (c.isPrimitive()) {
						data = (byte)0;
					}
				}
			} else if (short.class.equals(c) || Short.class.equals(c)) {
				if (value instanceof Boolean) {
					data = (((Boolean)value).booleanValue()) ? 1 : 0;
				} else if (value instanceof BigDecimal) {
					data = ((BigDecimal)value).shortValueExact();
				} else if (value instanceof Number) {
					data = ((Number)value).shortValue();
				} else {
					String str = value.toString().trim();
					if (str.length() > 0) {
						int start = 0;
						if (str.charAt(0) == '+') {
							start++;
						}
						
						if (str.startsWith("0x", start)) {
							data = (short)Integer.parseInt(str.substring(start+2), 16);
						} else {
							data = (short)Integer.parseInt(str.substring(start));
						}
					} else if (c.isPrimitive()) {
						data = (short)0;
					}
				}				
			} else if (int.class.equals(c) || Integer.class.equals(c)) {
				if (value instanceof Boolean) {
					data = (((Boolean)value).booleanValue()) ? 1 : 0;
				} else if (value instanceof BigDecimal) {
					data = ((BigDecimal)value).intValueExact();
				} else if (value instanceof Number) {
					data = ((Number)value).intValue();
				} else {
					String str = value.toString().trim();
					if (str.length() > 0) {
						int start = 0;
						if (str.charAt(0) == '+') {
							start++;
						}
						
						if (str.startsWith("0x", start)) {
							data = Integer.parseInt(str.substring(start+2), 16);
						} else {
							data = Integer.parseInt(str.substring(start));
						}
					} else if (c.isPrimitive()) {
						data = 0;
					}						
				}
			} else if (long.class.equals(c) || Long.class.equals(c)) {
				if (value instanceof Boolean) {
					data = (((Boolean)value).booleanValue()) ? 1l : 0l;
				} else if (value instanceof BigDecimal) {
					data = ((BigDecimal)value).longValueExact();
				} else if (value instanceof Number) {
					data = ((Number)value).longValue();
				} else {
					String str = value.toString().trim();
					if (str.length() > 0) {
						int start = 0;
						if (str.charAt(0) == '+') {
							start++;
						}
						
						if (str.startsWith("0x", start)) {
							data = Long.parseLong(str.substring(start+2), 16);
						} else {
							data = Long.parseLong(str.substring(start));
						}
					} else if (c.isPrimitive()) {
						data = 0l;
					}						
				}
			} else if (float.class.equals(c) || Float.class.equals(c)) {
				if (value instanceof Boolean) {
					data = (((Boolean)value).booleanValue()) ? 1.0f : Float.NaN;
				} else if (value instanceof Number) {
					data = ((Number)value).floatValue();
				} else {
					String str = value.toString().trim();
					if (str.length() > 0) {
						if (str.charAt(0) == '+') {
							data = Float.valueOf(str.substring(1));
						} else {
							data = Float.valueOf(str);
						}
					} else if (c.isPrimitive()) {
						data = 0.0f;
					}						
				}
			} else if (double.class.equals(c) || Double.class.equals(c)) {
				if (value instanceof Boolean) {
					data = (((Boolean)value).booleanValue()) ? 1.0 : Double.NaN;
				} else if (value instanceof Number) {
					data = ((Number)value).doubleValue();
				} else {
					String str = value.toString().trim();
					if (str.length() > 0) {
						if (str.charAt(0) == '+') {
							data = Double.valueOf(str.substring(1));
						} else {
							data = Double.valueOf(str);
						}
					} else if (c.isPrimitive()) {
						data = 0.0;
					}						
				}
			} else if (BigInteger.class.equals(c)) {				
				if (value instanceof Boolean) {
					data = (((Boolean)value).booleanValue()) ? BigInteger.ONE : BigInteger.ZERO;
				} else if (value instanceof BigDecimal) {
					data = ((BigDecimal)value).toBigIntegerExact();
				} else if (value instanceof Number) {
					data = BigInteger.valueOf(((Number)value).longValue());
				} else {
					String str = value.toString().trim();
					if (str.length() > 0) {
						int start = 0;
						if (str.charAt(0) == '+') {
							start++;
						}
						
						if (str.startsWith("0x", start)) {
							data = new BigInteger(str.substring(start+2), 16);
						} else {
							data = new BigInteger(str.substring(start));
						}
					}
				}
			} else if (BigDecimal.class.equals(c) || Number.class.equals(c)) {
				String str = value.toString().trim();
				if (str.length() > 0) {
					if (str.charAt(0) == '+') {
						data = new BigDecimal(str.substring(1));
					} else {
						data = new BigDecimal(str);
					}
				}
			} else {
				throw new UnsupportedOperationException();
			}
		} else {
			if (boolean.class.equals(c) || Boolean.class.equals(c)) {
				if (value instanceof Number) {
					data = !value.equals(0);
				} else {
					String s = value.toString().trim();
					if (s.length() == 0
						|| s.equalsIgnoreCase("f")
						|| s.equalsIgnoreCase("false")
						|| s.equalsIgnoreCase("no")
						|| s.equalsIgnoreCase("off")
						|| s.equals("NaN")) {
						data = false;
					} else {
						data = true;
					}
				}
			} else if (char.class.equals(c) || Character.class.equals(c)) {
				if (value instanceof Boolean) {
					data = (((Boolean)value).booleanValue()) ? '1' : '0';
				} else if (value instanceof Number) {
					data = (char)((Number)value).intValue();
				} else {
					String s = value.toString();
					if (s.length() > 0) {
						data = s.charAt(0);
					} else if (c.isPrimitive()) {
						data = '\0';
					}						
				}
			} else if (CharSequence.class.isAssignableFrom(c)) {
				data = value.toString();
			} else if (Appendable.class.isAssignableFrom(c)) {
				Appendable a = (Appendable)create(context, c);
				data = a.append(value.toString());
			} else if (Enum.class.isAssignableFrom(c)) {
				if (value instanceof Number) {
					data = c.getEnumConstants()[((Number)value).intValue()];
				} else if (value instanceof Boolean) {
					data = c.getEnumConstants()[((Boolean)value) ? 1 : 0];
				} else {
					String str = value.toString().trim();
					if (str.length() == 0) {
						data = null;
					} else if (Character.isDigit(str.charAt(0))) {
						data = c.getEnumConstants()[Integer.parseInt(str)];
					} else {
						data = Enum.valueOf((Class<? extends Enum>)c, str);
					}
				}
			} else if (Pattern.class.equals(c)) {
				data = Pattern.compile(value.toString());
			} else if (Date.class.isAssignableFrom(c)) {
				if (value instanceof Number) {
					Date date = (Date)create(context, c);
					date.setTime(((Number)value).longValue());
					data = date;
				} else {
					String str = value.toString().trim();
					if (str.length() > 0) {
						Date date = (Date)create(context, c);
						date.setTime(convertDate(str));
						data = date;
					}
				}
			} else if (Calendar.class.isAssignableFrom(c)) {
				if (value instanceof Number) {
					Calendar cal = (Calendar)create(context, c);
					cal.setTimeInMillis(((Number)value).longValue());
					data = cal;
				} else {
					String str = value.toString().trim();
					if (str.length() > 0) {
						Calendar cal = (Calendar)create(context, c);
						cal.setTimeInMillis(convertDate(str));
						data = cal;
					}
				}
			} else if (TimeZone.class.equals(c)) {
				data = TimeZone.getTimeZone(value.toString().trim());
			} else if (Locale.class.equals(c)) {
				String[] array = value.toString().split("\\p{Punct}");
				
				if (array.length == 1) {
					data = new Locale(array[0]);
				} else if (array.length == 2) {
					data = new Locale(array[0], array[1]);
				} else if (array.length > 2) {
					data = new Locale(array[0], array[1], array[2]);
				}
			} else if (File.class.equals(c)) {
				data = new File(value.toString().trim());
			} else if (URL.class.equals(c)) {
				if (value instanceof File) {
					data = ((File)value).toURI().toURL();
				} else if (value instanceof URI) {
					data = ((URI)value).toURL();
				} else {
					data = new URL(value.toString().trim());
				}
			} else if (URI.class.equals(c)) {
				if (value instanceof File) {
					data = ((File)value).toURI();
				} else if (value instanceof URL) {
					data = ((URL)value).toURI();
				} else {
					data = new URI(value.toString().trim());
				}
			} else if (InetAddress.class.equals(c)) {
				data = InetAddress.getByName(value.toString().trim());
			} else if (Charset.class.equals(c)) {
				data = Charset.forName(value.toString().trim());
			} else if (Class.class.equals(c)) {
				String s = value.toString().trim();
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
			} else if (Collection.class.isAssignableFrom(c)) {
				Collection collection = (Collection)create(context, c);
				if (type instanceof ParameterizedType) {
					Type[] pts = ((ParameterizedType)type).getActualTypeArguments();
					Type pt = (pts != null && pts.length > 0) ? pts[0] : Object.class;
					Class<?> pc = getRawType(pt);
					context.enter(0);
					collection.add(convert(context, value, pc, pt));
					context.exit();
				} else {
					collection.add(value);
				}
				data = collection;
			} else if (c.isArray()) {
				if (value instanceof String && byte.class.equals(c.getComponentType())) {
					data = Base64.decode((String)value);
				} else {
					Object array = Array.newInstance(c.getComponentType(), 1);
					Class<?> pc = c.getComponentType();
					Type pt = (type instanceof GenericArrayType) ? 
							((GenericArrayType)type).getGenericComponentType() : pc;
					context.enter(0);
					Array.set(array, 0, convert(context, value, pc, pt));
					context.exit();
					data = array;
				}
			} else {
				throw new UnsupportedOperationException();
			}
		}
		
		return (T)data;
	}
	
	protected boolean ignore(Class<?> target, Member member) {
		int modifiers = member.getModifiers();
		if (Modifier.isStatic(modifiers)) return true;
		if (Modifier.isTransient(modifiers)) return true;
		if (member.getDeclaringClass().equals(Object.class)) return true;
		return false;
	}
	
	protected Object create(Context context, Class<?> c) throws Exception {
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
		} else if ((c.isMemberClass() || c.isAnonymousClass()) && !Modifier.isStatic(c.getModifiers())) {
			Class eClass = c.getEnclosingClass();
			Constructor con = c.getDeclaredConstructor(eClass);
			if(context.tryAccess(c)) con.setAccessible(true);
			if (contextObject != null && eClass.isAssignableFrom(contextObject.getClass())) {
				instance = con.newInstance(contextObject);
			} else {
				instance = con.newInstance((Object)null);
			}
		} else {
			if (Date.class.isAssignableFrom(c)) {
				try {
					Constructor con = c.getDeclaredConstructor(long.class);
					if(context.tryAccess(c)) con.setAccessible(true);
					instance = con.newInstance(0l);
				} catch (NoSuchMethodException e) {
					// no handle
				}
			}
			
			if (instance == null) {
				Constructor con = c.getDeclaredConstructor();
				if(context.tryAccess(c)) con.setAccessible(true);
				instance = con.newInstance();
			}
		}
		
		return instance;
	}
	
	private AnnotatedElement mapping(Class c, Map<String, AnnotatedElement> props, String name) {
		AnnotatedElement target = props.get(name);
		if (target == null) {
			target = props.get(toLowerCamel(name));
			if (target == null) {
				target = props.get(name + "_");
			}
		}
		return target;
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
	
	private static Class<?> getRawType(Type t) {
		if (t instanceof Class) {
			return (Class<?>)t;
		}else if (t instanceof ParameterizedType) {
			return (Class<?>)((ParameterizedType)t).getRawType();
		} else if (t instanceof GenericArrayType) {
			Class cls = null;
			try {
				cls = Array.newInstance(getRawType(((GenericArrayType)t).getGenericComponentType()), 0).getClass();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return cls;
		} else if (t instanceof WildcardType) {
			Type[] types = ((WildcardType)t).getUpperBounds();
			return (types.length > 0) ? getRawType(types[0]) : Object.class;
		} else {
			return Object.class;
		}
	}
	
	private Long convertDate(String value) throws java.text.ParseException {
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
			} else if (locale != null) {
				format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale);
			} else {
				format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
			}
		} else if (locale != null) {
			format = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
		} else {
			format = DateFormat.getDateInstance(DateFormat.MEDIUM);
		}
		
		return format.parse(value).getTime();
	}
	
	public class Context {
		Class<?> scope;
		List<ContextState> path = new ArrayList(8);
		int level = -1;
		
		public int getLevel() {
			return level;
		}
		
		public Class<?> getScope() {
			return scope;
		}
		
		public Object getKey() {
			return path.get(getLevel()).key;
		}
		
		public Object getKey(int level) {
			if (level < 0) level = getLevel()+level; 
			return path.get(level).key;
		}
		
		void enter(Object key, JSONHint hint) {
			level++;
			if (level == path.size()) path.add(new ContextState());
			ContextState state = path.get(getLevel());
			state.key = key;
			state.hint = hint;
		}
		
		void enter(Object key) {
			enter(key, (level >= 0) ? path.get(level).hint : null);
		}
		
		void exit() {
			ContextState state = path.get(getLevel());
			state.key = null;
			state.hint = null;
			level--;
		}
		
		Map<String, AnnotatedElement> getGetProperties(Class<?> c) {
			Map<String, AnnotatedElement> props = new TreeMap<String, AnnotatedElement>();
			
			boolean access = tryAccess(c);
			
			for (Field f : c.getFields()) {
				if (ignore(c, f)) continue;
				
				String name = f.getName();
				if (f.isAnnotationPresent(JSONHint.class)) {
					JSONHint hint = f.getAnnotation(JSONHint.class);
					if (hint.ignore()) continue;
					if (hint.name().length() > 0) name = hint.name();
				}
				if (access) f.setAccessible(true);
				props.put(name, f);
			}
			
			for (Method m : c.getMethods()) {
				if (ignore(c, m)) continue;

				String name = m.getName();
				int start = 0;
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
				
				name = Introspector.decapitalize(name.substring(start));
				if (m.isAnnotationPresent(JSONHint.class)) {
					JSONHint hint = m.getAnnotation(JSONHint.class);
					if (hint.ignore()) continue;
					if (hint.name().length() > 0) name = hint.name();
				}
				if (access) m.setAccessible(true);
				props.put(name, m);
			}
			
			return props;
		}
		
		Map<String, AnnotatedElement> getSetProperties(Class<?> c) {
			Map<String, AnnotatedElement> props = new HashMap<String, AnnotatedElement>();
			
			boolean access = tryAccess(c);

			for (Field f : c.getFields()) {
				if (ignore(c, f)) continue;
				if (access) f.setAccessible(true);
				
				String name = f.getName();
				if (f.isAnnotationPresent(JSONHint.class)) {
					JSONHint hint = f.getAnnotation(JSONHint.class);
					if (hint.ignore()) continue;
					if (hint.name().length() > 0) name = hint.name();
				}
				props.put(name, f);
			}
			
			for (Method m : c.getMethods()) {
				if (ignore(c, m)) continue;

				String name = m.getName();
				int start = 0;
				if (name.startsWith("set") 
					&& name.length() > 3
					&& Character.isUpperCase(name.charAt(3))
					&& m.getParameterTypes().length == 1
					&& m.getReturnType().equals(void.class)) {
					start = 3;
				} else {
					continue;
				}
				
				if (access) m.setAccessible(true);
				name = Introspector.decapitalize(name.substring(start));
				if (m.isAnnotationPresent(JSONHint.class)) {
					JSONHint hint = m.getAnnotation(JSONHint.class);
					if (hint.ignore()) continue;
					if (hint.name().length() > 0) name = hint.name();
				}
				props.put(name, m);
			}
			
			return props;
		}
		
		boolean tryAccess(Class<?> c) {
			int modifier = c.getModifiers();
			if (scope != null && !Modifier.isPublic(modifier)) {
				if (Modifier.isPrivate(modifier)) {
					return scope.equals(c.getEnclosingClass());
				}
				int cpos = c.getName().lastIndexOf('.');
				int ppos = scope.getName().lastIndexOf('.');
				if (cpos == ppos 
					&& (cpos == -1 || c.getName().substring(0, cpos).equals(scope.getName().substring(0, ppos)))) {
					return true;
				}
			}
			return false;
		}
		
		<T extends Format> T format(Class<? extends T> c) {
			T format = null;
			JSONHint hint = path.get(getLevel()).hint;
			if (hint != null && hint.format().length() > 0) {
				if (NumberFormat.class.isAssignableFrom(c)) {
					if (locale != null) {
						format = (T)new DecimalFormat(hint.format(), new DecimalFormatSymbols(locale));
					} else {
						format = (T)new DecimalFormat(hint.format());
					}
				} else if (DateFormat.class.isAssignableFrom(c)) {
					if (locale != null) {
						format = (T)new SimpleDateFormat(hint.format(), locale);
					} else {
						format = (T)new SimpleDateFormat(hint.format());
					}
				}
			}
			return format;
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < path.size(); i++) {
				Object key = path.get(i).key;
				if (key instanceof Number) {
					sb.append('[').append(key).append(']');
				} else if (key instanceof Character) {
					sb.append(key);
				} else {
					String str = key.toString();
					boolean escape = false;
					for (int j = 0; j < str.length(); j++) {
						if (j == 0) {
							escape = !Character.isJavaIdentifierStart(str.charAt(j));
						} else {
							escape = !Character.isJavaIdentifierPart(str.charAt(j));
						}
						if (escape) break;
					}
					
					if (escape) {
						sb.append('[');
						try {
							formatString(str, sb);
						} catch (IOException e) {
							// no handle
						}
						sb.append(']');
					} else {
						sb.append('.').append(str);
					}
				}
			}
			return sb.toString();
		}
	}
	
	static class ContextState {
		Object key;
		JSONHint hint;
	}
}

interface ParserSource {
	int next() throws IOException;
	void back();
	long getLineNumber();
	long getColumnNumber();
	long getOffset();
	StringBuilder getCachedBuilder();
}

class CharSequenceParserSource implements ParserSource {
	private int lines = 1;
	private int columns = 1;
	private int offset = 0;
	
	private CharSequence cs;
	private StringBuilder cache = new StringBuilder(1000);
	
	public CharSequenceParserSource(CharSequence cs) {
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
	
	public StringBuilder getCachedBuilder() {
		cache.setLength(0);
		return cache;
	}
	
	public String toString() {
		return cs.subSequence(offset-columns+1, offset).toString();
	}
}

class ReaderParserSource implements ParserSource {
	private long lines = 1l;
	private long columns = 1l;
	private long offset = 0;

	private Reader reader;
	private char[] buf = new char[256];
	private int start = 0;
	private int end = 0;
	private StringBuilder cache = new StringBuilder(1000);
	
	public ReaderParserSource(InputStream in) throws IOException {
		if (!in.markSupported()) in = new BufferedInputStream(in);
		this.reader = new InputStreamReader(in, determineEncoding(in));
	}
	
	public ReaderParserSource(Reader reader) {
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
	
	public StringBuilder getCachedBuilder() {
		cache.setLength(0);
		return cache;
	}
	
	private String determineEncoding(InputStream in) throws IOException {
		String encoding = "UTF-8";

		in.mark(4);
		byte[] check = new byte[4];
		int size = in.read(check);
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
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		int maxlength = (columns-1 < buf.length) ? (int)columns-1 : buf.length-1;
		for (int i = maxlength; i >= 0; i--) {
			sb.append(buf[(start-2+buf.length-i) % (buf.length-1)]);
		}
		return sb.toString();
	}
}

class Base64 {
	private static final String BASE64_MAP = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
	
	public static String encode(byte[] data) {
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
	
	public static byte[] decode(CharSequence cs) {
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

