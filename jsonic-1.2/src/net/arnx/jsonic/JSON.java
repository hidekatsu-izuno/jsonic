/*
 * Copyright 2007-2009 Hidekatsu Izuno
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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.sql.Struct;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.RandomAccess;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;

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
 * @version 1.2.4
 * @see <a href="http://www.rfc-editor.org/rfc/rfc4627.txt">RFC 4627</a>
 * @see <a href="http://www.apache.org/licenses/LICENSE-2.0">the Apache License, Version 2.0</a>
 */
public class JSON {
	/**
	 * JSON processing mode
	 */
	public enum Mode {
		TRADITIONAL,
		STRICT,
		SCRIPT
	}
	
	/**
	 * Setup your custom class for using static method. default: net.arnx.jsonic.JSON
	 */
	public static Class<? extends JSON> prototype = JSON.class;
	
	private static final int TYPE_UNKNOWN = 0;
	private static final int TYPE_PLAIN = 1;
	private static final int TYPE_STRING = 2;
	private static final int TYPE_NUMBER = 3;
	private static final int TYPE_FLOAT = 4;
	private static final int TYPE_DATE = 5;
	private static final int TYPE_BOOLEAN_ARRAY = 6;
	private static final int TYPE_BYTE_ARRAY = 7;
	private static final int TYPE_SHORT_ARRAY = 8;
	private static final int TYPE_INT_ARRAY = 9;
	private static final int TYPE_LONG_ARRAY = 10;
	private static final int TYPE_FLOAT_ARRAY = 11;
	private static final int TYPE_DOUBLE_ARRAY = 12;
	private static final int TYPE_OBJECT_ARRAY = 13;
	private static final int TYPE_LIST = 14;
	private static final int TYPE_MAP = 15;
	private static final int TYPE_OBJECT = 16;

	private static final int TYPE_BYTE = 17;
	private static final int TYPE_CLASS = 18;
	private static final int TYPE_LOCALE = 19;
	private static final int TYPE_CHAR_ARRAY = 20;
	private static final int TYPE_SERIALIZE = 21;
	private static final int TYPE_ITERABLE = 22;
	private static final int TYPE_ITERATOR = 23;
	private static final int TYPE_ENUMERATION = 24;
	private static final int TYPE_DYNA_BEAN = 25;
	private static final int TYPE_DOM_ELEMENT = 26;
	
	private static final Map<Class<?>, Object> PRIMITIVE_MAP = new HashMap<Class<?>, Object>(8);
	private static final Map<Class<?>, Integer> FORMAT_MAP = new HashMap<Class<?>, Integer>(48);
	
	private static final int[] ESCAPE_CHARS = new int[128];
	
	static {
		for (int i = 0; i < 32; i++) ESCAPE_CHARS[i] = -1;
		ESCAPE_CHARS['\b'] = 'b';
		ESCAPE_CHARS['\t'] = 't';
		ESCAPE_CHARS['\n'] = 'n';
		ESCAPE_CHARS['\f'] = 'f';
		ESCAPE_CHARS['\r'] = 'r';
		ESCAPE_CHARS['"'] = '"';
		ESCAPE_CHARS['\\'] = '\\';
		ESCAPE_CHARS['<'] = -2;
		ESCAPE_CHARS['>'] = -2;
		ESCAPE_CHARS[0x7F] = -1;
		
		PRIMITIVE_MAP.put(boolean.class, false);
		PRIMITIVE_MAP.put(byte.class, (byte)0);
		PRIMITIVE_MAP.put(short.class, (short)0);
		PRIMITIVE_MAP.put(int.class, 0);
		PRIMITIVE_MAP.put(long.class, 0l);
		PRIMITIVE_MAP.put(float.class, 0.0f);
		PRIMITIVE_MAP.put(double.class, 0.0);
		PRIMITIVE_MAP.put(char.class, '\0');
		
		FORMAT_MAP.put(boolean.class, TYPE_PLAIN);
		FORMAT_MAP.put(byte.class, TYPE_BYTE);
		FORMAT_MAP.put(short.class, TYPE_NUMBER);
		FORMAT_MAP.put(int.class, TYPE_NUMBER);
		FORMAT_MAP.put(long.class, TYPE_NUMBER);
		FORMAT_MAP.put(float.class, TYPE_FLOAT);
		FORMAT_MAP.put(double.class, TYPE_FLOAT);
		FORMAT_MAP.put(char.class, TYPE_STRING);
		
		FORMAT_MAP.put(boolean[].class, TYPE_BOOLEAN_ARRAY);
		FORMAT_MAP.put(byte[].class, TYPE_BYTE_ARRAY);
		FORMAT_MAP.put(short[].class, TYPE_SHORT_ARRAY);
		FORMAT_MAP.put(int[].class, TYPE_INT_ARRAY);
		FORMAT_MAP.put(long[].class, TYPE_LONG_ARRAY);
		FORMAT_MAP.put(float[].class, TYPE_FLOAT_ARRAY);
		FORMAT_MAP.put(double[].class, TYPE_DOUBLE_ARRAY);
		FORMAT_MAP.put(char[].class, TYPE_CHAR_ARRAY);
		FORMAT_MAP.put(Object[].class, TYPE_OBJECT_ARRAY);
		
		FORMAT_MAP.put(Boolean.class, TYPE_PLAIN);
		FORMAT_MAP.put(Byte.class, TYPE_BYTE);
		FORMAT_MAP.put(Short.class, TYPE_NUMBER);
		FORMAT_MAP.put(Integer.class, TYPE_NUMBER);
		FORMAT_MAP.put(Long.class, TYPE_NUMBER);
		FORMAT_MAP.put(Float.class, TYPE_FLOAT);
		FORMAT_MAP.put(Double.class, TYPE_FLOAT);
		FORMAT_MAP.put(Character.class, TYPE_STRING);
		
		FORMAT_MAP.put(BigInteger.class, TYPE_NUMBER);
		FORMAT_MAP.put(BigDecimal.class, TYPE_NUMBER);
		FORMAT_MAP.put(String.class, TYPE_STRING);
		FORMAT_MAP.put(Date.class, TYPE_DATE);
		FORMAT_MAP.put(java.sql.Date.class, TYPE_DATE);
		FORMAT_MAP.put(java.sql.Time.class, TYPE_DATE);
		FORMAT_MAP.put(java.sql.Timestamp.class, TYPE_DATE);
		FORMAT_MAP.put(URI.class, TYPE_STRING);
		FORMAT_MAP.put(URL.class, TYPE_STRING);
		FORMAT_MAP.put(UUID.class, TYPE_STRING);
		FORMAT_MAP.put(Pattern.class, TYPE_STRING);
		FORMAT_MAP.put(Class.class, TYPE_CLASS);
		FORMAT_MAP.put(Locale.class, TYPE_LOCALE);
		
		FORMAT_MAP.put(ArrayList.class, TYPE_LIST);
		FORMAT_MAP.put(LinkedList.class, TYPE_ITERABLE);
		FORMAT_MAP.put(HashSet.class, TYPE_ITERABLE);
		FORMAT_MAP.put(TreeSet.class, TYPE_ITERABLE);
		FORMAT_MAP.put(LinkedHashSet.class, TYPE_ITERABLE);
		
		FORMAT_MAP.put(HashMap.class, TYPE_MAP);
		FORMAT_MAP.put(IdentityHashMap.class, TYPE_MAP);
		FORMAT_MAP.put(Properties.class, TYPE_MAP);
		FORMAT_MAP.put(TreeMap.class, TYPE_MAP);
		FORMAT_MAP.put(LinkedHashMap.class, TYPE_MAP);
	}
	
	static JSON newInstance() {
		JSON instance = null;
		try {
			instance = prototype.newInstance();
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
	 * @throws JSONException if error occurred when formating.
	 */
	public static String encode(Object source) throws JSONException {
		return encode(source, false);
	}
	
	/**
	 * Encodes a object into a json string.
	 * 
	 * @param source a object to encode.
	 * @param prettyPrint output a json string with indent, space or break.
	 * @return a json string
	 * @throws JSONException if error occurred when formating.
	 */
	public static String encode(Object source, boolean prettyPrint) throws JSONException {		
		JSON json = JSON.newInstance();
		json.setPrettyPrint(prettyPrint);		
		return json.format(source);
	}

	/**
	 * Encodes a object into a json string.
	 * 
	 * @param source a object to encode.
	 * @param out a destination to output a json string.
	 * @throws IOException if I/O Error occurred.
	 * @throws JSONException if error occurred when formating.
	 */
	public static void encode(Object source, OutputStream out) throws IOException, JSONException {
		JSON.newInstance().format(source, new OutputStreamWriter(out, "UTF-8"));
	}

	/**
	 * Encodes a object into a json string.
	 * 
	 * @param source a object to encode.
	 * @param out a destination to output a json string.
	 * @param prettyPrint output a json string with indent, space or break.
	 * @throws IOException if I/O Error occurred.
	 * @throws JSONException if error occurred when formating.
	 */
	public static void encode(Object source, OutputStream out, boolean prettyPrint) throws IOException, JSONException {
		JSON json = JSON.newInstance();
		json.setPrettyPrint(prettyPrint);		
		json.format(source, new OutputStreamWriter(out, "UTF-8"));
	}

	/**
	 * Encodes a object into a json string.
	 * 
	 * @param source a object to encode.
	 * @param appendable a destination to output a json string.
	 * @throws IOException if I/O Error occurred.
	 * @throws JSONException if error occurred when formating.
	 */
	public static void encode(Object source, Appendable appendable) throws IOException, JSONException {
		JSON.newInstance().format(source, appendable);
	}

	/**
	 * Encodes a object into a json string.
	 * 
	 * @param source a object to encode.
	 * @param appendable a destination to output a json string.
	 * @param prettyPrint output a json string with indent, space or break.
	 * @throws IOException if I/O Error occurred.
	 * @throws JSONException if error occurred when formating.
	 */
	public static void encode(Object source, Appendable appendable, boolean prettyPrint) throws IOException, JSONException {
		JSON json = JSON.newInstance();
		json.setPrettyPrint(prettyPrint);		
		json.format(source, appendable);
	}
	
	/**
	 * Escapes a object into JavaScript format.
	 * 
	 * @param source a object to encode.
	 * @return a escaped object
	 * @throws JSONException if error occurred when formating.
	 */
	public static String escapeJS(Object source) throws JSONException {
		JSON json = JSON.newInstance();
		json.setMode(JSON.Mode.SCRIPT);
		return json.format(source);
	}
	
	/**
	 * Decodes a json string into a object.
	 * 
	 * @param source a json string to decode
	 * @return a decoded object
	 * @throws JSONException if error occurred when parsing.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T decode(String source) throws JSONException {
		return (T)JSON.newInstance().parse(source);
	}
	
	/**
	 * Decodes a json string into a typed object.
	 * 
	 * @param source a json string to decode
	 * @param cls class for converting
	 * @return a decoded object
	 * @throws JSONException if error occurred when parsing.
	 */
	public static <T> T decode(String source, Class<? extends T> cls) throws JSONException {
		return JSON.newInstance().parse(source, cls);
	}
	
	/**
	 * Decodes a json string into a typed object.
	 * 
	 * @param source a json string to decode
	 * @param type type for converting
	 * @return a decoded object
	 * @throws JSONException if error occurred when parsing.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T decode(String source, Type type) throws JSONException {
		return (T)JSON.newInstance().parse(source, type);
	}

	/**
	 * Decodes a json stream into a object. (character encoding should be Unicode)
	 * 
	 * @param in a json stream to decode
	 * @return a decoded object
	 * @throws IOException if I/O error occurred.
	 * @throws JSONException if error occurred when parsing.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T decode(InputStream in) throws IOException, JSONException {
		return (T)JSON.newInstance().parse(in);
	}

	/**
	 * Decodes a json stream into a object. (character encoding should be Unicode)
	 * 
	 * @param in a json stream to decode
	 * @param cls class for converting
	 * @return a decoded object
	 * @throws IOException if I/O error occurred.
	 * @throws JSONException if error occurred when parsing.
	 */
	public static <T> T decode(InputStream in, Class<? extends T> cls) throws IOException, JSONException {
		return JSON.newInstance().parse(in, cls);
	}

	/**
	 * Decodes a json stream into a object. (character encoding should be Unicode)
	 * 
	 * @param in a json stream to decode
	 * @param type type for converting
	 * @return a decoded object
	 * @throws IOException if I/O error occurred.
	 * @throws JSONException if error occurred when parsing.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T decode(InputStream in, Type type) throws IOException, JSONException {
		return (T)JSON.newInstance().parse(in, type);
	}
	
	/**
	 * Decodes a json stream into a object.
	 * 
	 * @param reader a json stream to decode
	 * @return a decoded object
	 * @throws IOException if I/O error occurred.
	 * @throws JSONException if error occurred when parsing.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T decode(Reader reader) throws IOException, JSONException {
		return (T)JSON.newInstance().parse(reader);
	}

	/**
	 * Decodes a json stream into a object.
	 * 
	 * @param reader a json stream to decode
	 * @param cls class for converting
	 * @return a decoded object
	 * @throws IOException if I/O error occurred.
	 * @throws JSONException if error occurred when parsing.
	 */
	public static <T> T decode(Reader reader, Class<? extends T> cls) throws IOException, JSONException {
		return JSON.newInstance().parse(reader, cls);
	}

	/**
	 * Decodes a json stream into a object.
	 * 
	 * @param reader a json stream to decode
	 * @param type type for converting
	 * @return a decoded object
	 * @throws IOException if I/O error occurred.
	 * @throws JSONException if error occurred when parsing.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T decode(Reader reader, Type type) throws IOException, JSONException {
		return (T)JSON.newInstance().parse(reader, type);
	}
	
	/**
	 * Validates a json text
	 * 
	 * @param cs source a json string to decode
	 * @throws JSONException if error occurred when parsing.
	 */
	public static void validate(CharSequence cs) throws JSONException {
		JSON json = JSON.newInstance();
		json.setMode(Mode.STRICT);
		json.setMaxDepth(0);
		json.parse(cs);
	}
	
	/**
	 * Validates a json stream
	 * 
	 * @param in source a json string to decode
	 * @throws IOException if I/O error occurred.
	 * @throws JSONException if error occurred when parsing.
	 */
	public static void validate(InputStream in) throws IOException, JSONException {
		JSON json = JSON.newInstance();
		json.setMode(Mode.STRICT);
		json.setMaxDepth(0);
		json.parse(in);
	}
	
	/**
	 * Validates a json stream
	 * 
	 * @param reader source a json string to decode
	 * @throws IOException if I/O error occurred.
	 * @throws JSONException if error occurred when parsing.
	 */
	public static void validate(Reader reader) throws IOException, JSONException {
		JSON json = JSON.newInstance();
		json.setMode(Mode.STRICT);
		json.setMaxDepth(0);
		json.parse(reader);
	}
	
	Object contextObject;
	Locale locale;
	boolean prettyPrint = false;
	int maxDepth = 32;
	boolean suppressNull = false;
	Mode mode = Mode.TRADITIONAL;

	public JSON() {
	}
	
	public JSON(int maxDepth) {
		setMaxDepth(maxDepth);
	}
	
	public JSON(Mode mode) {
		setMode(mode);
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
		if (value < 0) {
			throw new IllegalArgumentException(getMessage("json.TooSmallArgumentError", "maxDepth", 0));
		}
		this.maxDepth = value;
	}
	
	/**
	 * Gets maximum depth for the nest level.
	 * 
	 * @return a maximum depth
	 */
	public int getMaxDepth() {
		return this.maxDepth;
	}
	
	/**
	 * If this property is true, the member of null value in JSON object is ignored.
	 * default value is false.
	 * 
	 * @param value true to ignore the member of null value in JSON object.
	 */
	public void setSuppressNull(boolean value) {
		this.suppressNull = value;
	}
	
	/**
	 * Sets JSON interpreter mode.
	 * 
	 * @param mode JSON interpreter mode
	 */
	public void setMode(Mode mode) {
		if (mode == null) {
			throw new NullPointerException();
		}
		this.mode = mode;
	}
	
	/**
	 * Gets JSON interpreter mode.
	 * 
	 * @return JSON interpreter mode
	 */
	public Mode getMode() {
		return mode;
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
	
	/**
	 * Format a object into a json string.
	 * 
	 * @param source a object to encode.
	 * @param out a destination to output a json string.
	 * @return a reference to 'out' object in parameters
	 */
	public OutputStream format(Object source, OutputStream out) throws IOException {
		format(source, new BufferedWriter(new OutputStreamWriter(out, "UTF-8")));
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
		
		FormatSource fs;
		if (ap instanceof StringWriter) {
			fs = new WriterFormatSource((Writer)ap);
		} else if (ap instanceof Writer) {
			fs = new BufferedWriterFormatSource((Writer)ap);
		} else if (ap instanceof StringBuilder) {
			fs = new StringBuilderFormatSource((StringBuilder)ap);
		} else if (ap instanceof StringBuilder) {
			fs = new StringBuilderFormatSource((StringBuilder)ap);
		} else {
			fs = new AppendableFormatSource(ap);
		}
		
		context.enter('$');
		format(context, source, fs);
		context.exit();
		fs.flush();
		return ap;
	}
	
	/**
	 * Converts Any Java Object to JSON recognizable Java object before format.
	 * 
	 * @param context current context.
	 * @param value source a object to format.
	 * @return null or the instance of Map, Iterator(or Array, Enumerator), Number, CharSequence or Boolean.
	 * @throws Exception if conversion failed.
	 */
	protected Object preformat(Context context, Object value) throws Exception {
		return value;
	}
	
	void format(Context context, Object src, FormatSource ap) throws IOException {
		Object o = src;
		if (context.getLevel() > this.maxDepth) {
			o = null;
		} else {
			try {
				o = preformat(context, o);
			} catch (Exception e) {
				throw new JSONException(getMessage("json.format.ConversionError", o, context),
					JSONException.PREFORMAT_ERROR, e);
			}
		}
		
		if (o == null) {
			checkRoot(context);
			ap.append("null");
			return;
		}

		int type = TYPE_UNKNOWN;
		
		JSONHint hint = context.getHint();
		if (hint != null) {
			if (hint.serialized()) {
				type = TYPE_PLAIN;
			} else if (String.class.equals(hint.type())) {
				type = TYPE_STRING;
			} else if (Serializable.class.equals(hint.type())) {
				type = TYPE_SERIALIZE;
			}
		}
		
		if (type == TYPE_UNKNOWN) {
			Integer result = FORMAT_MAP.get(o.getClass());
			if (result != null) type = result;
		}
		
		if (type == TYPE_UNKNOWN) {
			if (context.hasMemberCache(o.getClass())) {
				type = TYPE_OBJECT;
			} else if (o instanceof Map<?, ?>) {
				type = TYPE_MAP;
			} else if (o instanceof Iterable<?>) {
				if (o instanceof RandomAccess && o instanceof List<?>) {
					type = TYPE_LIST;
				} else {
					type = TYPE_ITERABLE;
				}
			} else if (o instanceof Object[]) {
				type = TYPE_OBJECT_ARRAY;
			} else if (o instanceof Enum<?>) {
				o = ((Enum<?>)o).ordinal();
				type = TYPE_NUMBER;
			} else if (o instanceof CharSequence) {
				type = TYPE_STRING;
			} else if (o instanceof Date) {
				type = TYPE_DATE;
			} else if (o instanceof Calendar) {
				o = ((Calendar)o).getTime();
				type = TYPE_DATE;
			} else if (o instanceof Number) {
				type = TYPE_NUMBER;
			} else if (o instanceof Iterator<?>) {
				type = TYPE_ITERATOR;
			} else if (o instanceof Enumeration) {
				type = TYPE_ENUMERATION;
			} else if (o instanceof Type || o instanceof Member || o instanceof File) {
				type = TYPE_STRING;
			} else if (o instanceof TimeZone) {
				o = ((TimeZone)o).getID();
				type = TYPE_STRING;
			} else if (o instanceof Charset) {
				o = ((Charset)o).name();
				type = TYPE_STRING;
			} else if (o instanceof java.sql.Array) {
				try {
					o = ((java.sql.Array)o).getArray();
				} catch (SQLException e) {
					o = null;
				}
				if (o == null) o = new Object[0];
				type = FORMAT_MAP.get(o.getClass());
			} else if (o instanceof Struct) {
				try {
					o = ((Struct)o).getAttributes();
				} catch (SQLException e) {
					o = null;
				}
				if (o == null) o = new Object[0];
				type = TYPE_OBJECT_ARRAY;
			} else if (o instanceof Node) {
				if (o instanceof CharacterData && !(o instanceof Comment)) {
					o = ((CharacterData)o).getData();
					type = TYPE_STRING;
				} else if (o instanceof Document) {
					o = ((Document)o).getDocumentElement();
					type = TYPE_DOM_ELEMENT;
				} else if (o instanceof Element) {
					type = TYPE_DOM_ELEMENT;
				}
			} else if (ClassUtil.isAssignableFrom("java.sql.RowId", o.getClass())) {
				type = TYPE_SERIALIZE;
			} else if (ClassUtil.isAssignableFrom("java.net.InetAddress", o.getClass())) {
				Class<?> inetAddressClass = ClassUtil.findClass("java.net.InetAddress");
				try {
					o = (String)inetAddressClass.getMethod("getHostAddress").invoke(o);
				} catch (Exception e) {
					o = null;
				}
				if (o == null) {
					o = "null";
					type = TYPE_PLAIN;
				} else {
					type = TYPE_STRING;
				}
			} else if (ClassUtil.isAssignableFrom("org.apache.commons.beanutils.DynaBean", o.getClass())) {
				type = TYPE_DYNA_BEAN;
			} else {
				type = TYPE_OBJECT;
			}
		}

		switch (type) {
		case TYPE_PLAIN: {
			checkRoot(context);
			ap.append(o.toString());
			return;
		}
		case TYPE_STRING:
			checkRoot(context);
			formatString(context, o.toString(), ap);
			return;
		case TYPE_NUMBER: {
			checkRoot(context);
			NumberFormat f = context.format(NumberFormat.class);
			if (f != null) {
				formatString(context, f.format(o), ap);
			} else {
				ap.append(o.toString());
			}
			return;
		}
		case TYPE_FLOAT: {
			checkRoot(context);
			NumberFormat f = context.format(NumberFormat.class);
			if (f != null) {
				formatString(context, f.format(o), ap);
			} else {
				double d = ((Number)o).doubleValue();
				if (Double.isNaN(d) || Double.isInfinite(d)) {
					if (mode != Mode.SCRIPT) {
						ap.append('"');
						ap.append(o.toString());
						ap.append('"');
					} else if (Double.isNaN(d)) {
						ap.append("Number.NaN");
					} else {
						ap.append("Number.");
						ap.append((d > 0) ? "POSITIVE" : "NEGATIVE");
						ap.append("_INFINITY");
					}
				} else {
					ap.append(o.toString());
				}
			}
			return;
		}
		case TYPE_DATE: {
			checkRoot(context);
			Date date = (Date)o;
			DateFormat f = context.format(DateFormat.class);
			if (f != null) {
				formatString(context, f.format(date), ap);
			} else if (mode == Mode.SCRIPT) {
				ap.append("new Date(");
				ap.append(Long.toString(date.getTime()));
				ap.append(")");
			} else {
				ap.append(Long.toString(date.getTime()));
			}
			return;
		}
		case TYPE_BOOLEAN_ARRAY: {
			ap.append('[');
			boolean[] array = (boolean[])o;
			for (int i = 0; i < array.length; i++) {
				ap.append(String.valueOf(array[i]));
				if (i != array.length-1) {
					ap.append(',');
					if (context.isPrettyPrint()) ap.append(' ');
				}
			}
			ap.append(']');
			return;
		}
		case TYPE_BYTE_ARRAY: {
			checkRoot(context);
			formatString(context, Base64.encode((byte[])o), ap);
			return;
		}
		case TYPE_SHORT_ARRAY: {
			NumberFormat f = context.format(NumberFormat.class);
			short[] array = (short[])o;
			ap.append('[');
			for (int i = 0; i < array.length; i++) {
				if (f != null) {
					formatString(context, f.format(array[i]), ap);
				} else {
					ap.append(String.valueOf(array[i]));
				}
				if (i != array.length-1) {
					ap.append(',');
					if (context.isPrettyPrint()) ap.append(' ');
				}
			}
			ap.append(']');
			return;
		}
		case TYPE_INT_ARRAY: {
			NumberFormat f = context.format(NumberFormat.class);
			int[] array = (int[])o;
			ap.append('[');
			for (int i = 0; i < array.length; i++) {
				if (f != null) {
					formatString(context, f.format(array[i]), ap);
				} else {
					ap.append(String.valueOf(array[i]));
				}
				if (i != array.length-1) {
					ap.append(',');
					if (context.isPrettyPrint()) ap.append(' ');
				}
			}
			ap.append(']');
			return;
		}
		case TYPE_LONG_ARRAY: {
			NumberFormat f = context.format(NumberFormat.class);
			long[] array = (long[])o;
			ap.append('[');
			for (int i = 0; i < array.length; i++) {
				if (f != null) {
					formatString(context, f.format(array[i]), ap);
				} else {
					ap.append(String.valueOf(array[i]));
				}
				if (i != array.length-1) {
					ap.append(',');
					if (context.isPrettyPrint()) ap.append(' ');
				}
			}
			ap.append(']');
			return;
		}
		case TYPE_FLOAT_ARRAY: {
			NumberFormat f = context.format(NumberFormat.class);
			float[] array = (float[])o;
			ap.append('[');
			for (int i = 0; i < array.length; i++) {
				if (Float.isNaN(array[i]) || Float.isInfinite(array[i])) {
					if (mode != Mode.SCRIPT) {
						ap.append('"');
						ap.append(Float.toString(array[i]));
						ap.append('"');
					} else if (Double.isNaN(array[i])) {
						ap.append("Number.NaN");
					} else {
						ap.append("Number.");
						ap.append((array[i] > 0) ? "POSITIVE" : "NEGATIVE");
						ap.append("_INFINITY");
					}
				} else if (f != null) {
					formatString(context, f.format(array[i]), ap);
				} else {
					ap.append(String.valueOf(array[i]));
				}
				if (i != array.length-1) {
					ap.append(',');
					if (context.isPrettyPrint()) ap.append(' ');
				}
			}
			ap.append(']');
			return;
		}
		case TYPE_DOUBLE_ARRAY: {
			NumberFormat f = context.format(NumberFormat.class);
			double[] array = (double[])o;
			ap.append('[');
			for (int i = 0; i < array.length; i++) {
				if (Double.isNaN(array[i]) || Double.isInfinite(array[i])) {
					if (mode != Mode.SCRIPT) {
						ap.append('"');
						ap.append(Double.toString(array[i]));
						ap.append('"');
					} else if (Double.isNaN(array[i])) {
						ap.append("Number.NaN");
					} else {
						ap.append("Number.");
						ap.append((array[i] > 0) ? "POSITIVE" : "NEGATIVE");
						ap.append("_INFINITY");
					}
				} else if (f != null) {
					formatString(context, f.format(array[i]), ap);
				} else {
					ap.append(String.valueOf(array[i]));
				}
				if (i != array.length-1) {
					ap.append(',');
					if (context.isPrettyPrint()) ap.append(' ');
				}
			}
			ap.append(']');
			return;
		}
		case TYPE_OBJECT_ARRAY: {
			Object[] array = (Object[])o;
			ap.append('[');
			int i = 0;
			for (; i < array.length; i++) {
				Object item = array[i];
				if (item == src) item = null;
				
				if (i != 0) ap.append(',');
				if (context.isPrettyPrint()) {
					ap.append('\n');
					for (int j = 0; j < context.getLevel()+1; j++) ap.append('\t');
				}
				context.enter(i);
				format(context, item, ap);
				context.exit();
			}
			if (context.isPrettyPrint() && i > 0) {
				ap.append('\n');
				for (int j = 0; j < context.getLevel(); j++) ap.append('\t');
			}
			ap.append(']');
			return;
		}
		case TYPE_LIST: {
			List<?> list = (List<?>)o;
			ap.append('[');
			int length = list.size();
			int i = 0;
			for (; i < length; i++) {
				Object item = list.get(i);
				if (item == src) item = null;
				
				if (i != 0) ap.append(',');
				if (context.isPrettyPrint()) {
					ap.append('\n');
					for (int j = 0; j < context.getLevel()+1; j++) ap.append('\t');
				}
				context.enter(i);
				format(context, item, ap);
				context.exit();
			}
			if (context.isPrettyPrint() && i > 0) {
				ap.append('\n');
				for (int j = 0; j < context.getLevel(); j++) ap.append('\t');
			}
			ap.append(']');
			return;
		}
		case TYPE_MAP: {
			Map<?, ?> map = (Map<?, ?>)o;
			
			ap.append('{');
			int i = 0;
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				Object key = entry.getKey();
				if (key == null) continue;
				
				Object value = entry.getValue();
				if (value == src || (this.suppressNull && value == null)) continue; 
				
				if (i > 0) ap.append(',');
				if (context.isPrettyPrint()) {
					ap.append('\n');
					for (int j = 0; j < context.getLevel()+1; j++) ap.append('\t');
				}
				formatString(context, key.toString(), ap);
				ap.append(':');
				if (context.isPrettyPrint()) ap.append(' ');
				context.enter(key);
				format(context, value, ap);
				context.exit();
				i++;
			}
			if (context.isPrettyPrint() && i > 0) {
				ap.append('\n');
				for (int j = 0; j < context.getLevel(); j++) ap.append('\t');
			}
			ap.append('}');
			return;
		}
		case TYPE_OBJECT: {
			List<Property> props = context.getGetProperties(o.getClass());
			
			ap.append('{');
			int i = 0;
			for (int p = 0; p < props.size(); p++) {
				Property prop = props.get(p);
				Object value = null;
				Exception cause = null; 

				try {
					value = prop.get(o);
					if (value == src || (this.suppressNull && value == null)) continue;
					
					if (i > 0) ap.append(',');
					if (context.isPrettyPrint()) {
						ap.append('\n');
						for (int j = 0; j < context.getLevel()+1; j++) ap.append('\t');
					}
				} catch (Exception e) {
					cause = e;
				}
				
				formatString(context, prop.getName(), ap);
				ap.append(':');
				if (context.isPrettyPrint()) ap.append(' ');
				context.enter(prop.getName(), prop.getHint());
				if (cause != null) {
					throw new JSONException(getMessage("json.format.ConversionError",
							(src instanceof CharSequence) ? "\"" + src + "\"" : src, context),
							JSONException.FORMAT_ERROR, cause);					
				}
				format(context, value, ap);
				context.exit();
				i++;
			}
			if (context.isPrettyPrint() && i > 0) {
				ap.append('\n');
				for (int j = 0; j < context.getLevel(); j++) ap.append('\t');
			}
			ap.append('}');
			return;
		}
		}
		
		switch (type) {
		case TYPE_BYTE: {
			checkRoot(context);
			ap.append(Integer.toString(((Byte)o).byteValue() & 0xFF));
			return;
		}
		case TYPE_CLASS: {
			checkRoot(context);
			formatString(context, ((Class<?>)o).getName(), ap);
			return;
		}
		case TYPE_LOCALE: {
			checkRoot(context);
			formatString(context, ((Locale)o).toString().replace('_', '-'), ap);
			return;
		}
		case TYPE_CHAR_ARRAY: {
			checkRoot(context);
			formatString(context, new String((char[])o), ap);
			return;
		}
		case TYPE_SERIALIZE: {
			checkRoot(context);
			formatString(context, Base64.encode(serialize(o)), ap);
			return;
		}
		case TYPE_ITERABLE:
			o = ((Iterable<?>)o).iterator();			
		case TYPE_ITERATOR: {
			Iterator<?> t = (Iterator<?>)o;
			ap.append('[');
			int i = 0;
			for (; t.hasNext(); i++) {
				Object item = t.next();
				if (item == src) item = null;

				if (i != 0) ap.append(',');
				if (context.isPrettyPrint()) {
					ap.append('\n');
					for (int j = 0; j < context.getLevel()+1; j++) ap.append('\t');
				}
				context.enter(i);
				format(context, item, ap);
				context.exit();
			}
			if (context.isPrettyPrint() && i > 0) {
				ap.append('\n');
				for (int j = 0; j < context.getLevel(); j++) ap.append('\t');
			}
			ap.append(']');
			return;
		}
		case TYPE_ENUMERATION: {
			Enumeration<?> e = (Enumeration<?>)o;
			ap.append('[');
			int i = 0;
			for (; e.hasMoreElements(); i++) {
				Object item = e.nextElement();
				if (item == src) item = null;

				if (i != 0) ap.append(',');
				if (context.isPrettyPrint()) {
					ap.append('\n');
					for (int j = 0; j < context.getLevel()+1; j++) ap.append('\t');
				}
				context.enter(i);
				format(context, item, ap);
				context.exit();
			}
			if (context.isPrettyPrint() && i > 0) {
				ap.append('\n');
				for (int j = 0; j < context.getLevel(); j++) ap.append('\t');
			}
			ap.append(']');
			return;
		}
		case TYPE_DYNA_BEAN: {
			ap.append('{');
			int i = 0;
			try {
				Class<?> dynaBeanClass = ClassUtil.findClass("org.apache.commons.beanutils.DynaBean");
				
				Object dynaClass = dynaBeanClass.getMethod("getDynaClass").invoke(o);
				Object[] dynaProperties = (Object[])dynaClass.getClass().getMethod("getDynaProperties").invoke(dynaClass);
				
				if (dynaProperties != null && dynaProperties.length > 0) {
					Method getName = dynaProperties[0].getClass().getMethod("getName");
					Method get = dynaBeanClass.getMethod("get", String.class);
					
					for (Object dp : dynaProperties) {
						Object name = null;
						try {
							name = getName.invoke(dp);
						} catch (Exception e) {
						}
						if (name == null) continue;
						
						Object value = null;
						Exception cause = null;
						
						try {
							value = get.invoke(o, name);
						} catch (Exception e) {
							cause = e;
						}
						
						if (value == src || (cause == null && this.suppressNull && value == null)) continue; 
						
						if (i > 0) ap.append(',');
						if (context.isPrettyPrint()) {
							ap.append('\n');
							for (int j = 0; j < context.getLevel()+1; j++) ap.append('\t');
						}
						formatString(context, name.toString(), ap);
						ap.append(':');
						if (context.isPrettyPrint()) ap.append(' ');
						context.enter(name);
						if (cause != null) {
							throw new JSONException(getMessage("json.format.ConversionError",
									(src instanceof CharSequence) ? "\"" + src + "\"" : src, context),
									JSONException.FORMAT_ERROR, cause);
						}
						format(context, value, ap);
						context.exit();
						i++;
					}
				}
			} catch (Exception e) {
				// no handle
			}
			if (context.isPrettyPrint() && i > 0) {
				ap.append('\n');
				for (int j = 0; j < context.getLevel(); j++) ap.append('\t');
			}
			ap.append('}');
			return;
		}
		case TYPE_DOM_ELEMENT: {
			Element elem = (Element)o;
			ap.append('[');
			formatString(context, elem.getTagName(), ap);
			
			ap.append(',');
			if (context.isPrettyPrint()) {
				ap.append('\n');
				for (int j = 0; j < context.getLevel()+1; j++) ap.append('\t');
			}
			ap.append('{');
			if (elem.hasAttributes()) {
				NamedNodeMap names = elem.getAttributes();
				for (int i = 0; i < names.getLength(); i++) {
					if (i != 0) {
						ap.append(',');
					}
					if (context.isPrettyPrint() && names.getLength() > 1) {
						ap.append('\n');
						for (int j = 0; j < context.getLevel()+2; j++) ap.append('\t');
					}
					Node node = names.item(i);
					if (node instanceof Attr) {
						formatString(context, node.getNodeName(), ap);
						ap.append(':');
						if (context.isPrettyPrint()) ap.append(' ');
						formatString(context, node.getNodeValue(), ap);
					}
				}
				if (context.isPrettyPrint() && names.getLength() > 1) {
					ap.append('\n');
					for (int j = 0; j < context.getLevel()+1; j++) ap.append('\t');
				}
			}
			ap.append('}');
			if (elem.hasChildNodes()) {
				NodeList nodes = elem.getChildNodes();
				for (int i = 0; i < nodes.getLength(); i++) {
					Node node = nodes.item(i);
					if ((node instanceof Element) || (node instanceof CharacterData && !(node instanceof Comment))) {
						ap.append(',');
						if (context.isPrettyPrint()) {
							ap.append('\n');
							for (int j = 0; j < context.getLevel()+1; j++) ap.append('\t');
						}
						context.enter(i+2);
						format(context, node, ap);
						context.exit();
						if (ap instanceof Flushable) ((Flushable)ap).flush();
					}
				}
			}
			if (context.isPrettyPrint()) {
				ap.append('\n');
				for (int j = 0; j < context.getLevel(); j++) ap.append('\t');
			}
			ap.append(']');
			return;
		}
		}
		
		throw new IllegalStateException();
	}
	
	void checkRoot(Context context) {
		if (context.getLevel() == 0 && mode != Mode.SCRIPT) {
			throw new JSONException(getMessage("json.format.IllegalRootTypeError"), JSONException.FORMAT_ERROR);
		}
	}
	
	void formatString(Context context, String s, FormatSource ap) throws IOException {
		ap.append('"');
		int start = 0;
		int length = s.length();
		for (int i = 0; i < length; i++) {
			int c = s.charAt(i);
			if (c < ESCAPE_CHARS.length && ESCAPE_CHARS[c] != 0) { 
				int x = ESCAPE_CHARS[c];
				if (x > 0) {
					if (start < i) ap.append(s, start, i);
					ap.append('\\');
					ap.append((char)x);					
					start = i+1;
				} else if (x == -1 || (x == -2 && mode == Mode.SCRIPT)) {
					if (start < i) ap.append(s, start, i);
					ap.append("\\u00");
					ap.append("0123456789ABCDEF".charAt(c/16));
					ap.append("0123456789ABCDEF".charAt(c%16));					
					start = i+1;
				}
			}
		}
		if (start < length) ap.append(s, start, length);
		ap.append('"');
	}
	
	@SuppressWarnings("unchecked")
	public <T> T parse(CharSequence cs) throws JSONException {
		Object value = null;
		try {
			value = parse(new Context(), new CharSequenceParserSource(cs));
		} catch (IOException e) {
			// never occur
		}
		return (T)value; 
	}
	
	@SuppressWarnings("unchecked")
	public <T> T parse(CharSequence s, Class<? extends T> cls) throws JSONException {
		return (T)parse(s, (Type)cls);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T parse(CharSequence s, Type type) throws JSONException {
		T value = null;
		try {
			Context context = new Context();
			value = (T)convert(context, parse(context, new CharSequenceParserSource(s)), type);
		} catch (IOException e) {
			// never occur
		}
		return value;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T parse(InputStream in) throws IOException, JSONException {
		return (T)parse(new Context(), new ReaderParserSource(in));
	}
	
	@SuppressWarnings("unchecked")
	public <T> T parse(InputStream in, Class<? extends T> cls) throws IOException, JSONException {
		return (T)parse(in, (Type)cls);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T parse(InputStream in, Type type) throws IOException, JSONException {
		Context context = new Context();
		return (T)convert(context, parse(context, new ReaderParserSource(in)), type);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T parse(Reader reader) throws IOException, JSONException {
		return (T)parse(new Context(), new ReaderParserSource(reader));
	}
	
	@SuppressWarnings("unchecked")
	public <T> T parse(Reader reader, Class<? extends T> cls) throws IOException, JSONException {
		return (T)parse(reader, (Type)cls);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T parse(Reader reader, Type type) throws IOException, JSONException {
		Context context = new Context();
		return (T)convert(context, parse(context, new ReaderParserSource(reader)), type);
	}
	
	Object parse(Context context, ParserSource s) throws IOException, JSONException {
		boolean isEmpty = true;
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
				continue;
			case '{':
				if (isEmpty) {
					s.back();
					o = parseObject(context, s, 1);
					isEmpty = false;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				continue;
			case '[':
				if (isEmpty) {
					s.back();
					o = parseArray(context, s, 1);
					isEmpty = false;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				continue;
			case '/':
			case '#':
				if (mode == Mode.TRADITIONAL || (mode == Mode.SCRIPT && c == '/')) {
					s.back();
					skipComment(s);
					continue;
				}
			case '\'':
			case '"':
				if (mode == Mode.SCRIPT) {
					if (isEmpty) {
						s.back();
						o = parseString(context, s, 1);
						isEmpty = false;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					continue;
				}
			default:
				if (mode == Mode.SCRIPT) {
					if (isEmpty) {
						s.back();
						o = ((c == '-') || (c >= '0' && c <= '9')) ? parseNumber(context, s, 1) : parseLiteral(context, s, 1, false);
						isEmpty = false;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					continue;
				}
			}
			
			if (mode == Mode.TRADITIONAL && isEmpty) {
				s.back();
				o = parseObject(context, s, 1);
				isEmpty = false;
			} else {
				throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
			}
		}
		
		if (isEmpty) {
			if (mode == Mode.TRADITIONAL) {
				o = new LinkedHashMap<String, Object>();
			} else {
				throw createParseException(getMessage("json.parse.EmptyInputError"), s);
			}
		}
		
		return o;
	}
	
	Map<Object, Object> parseObject(Context context, ParserSource s, int level) throws IOException, JSONException {
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
				if (mode == Mode.TRADITIONAL && point == 5) {
					point = 6;
				}
				continue;
			case ' ':
			case '\t':
			case 0xFEFF: // BOM
				continue;
			case '{':
				if (point == 0) {
					start = '{';
					point = 1;
				} else if (point == 2 || point == 3){
					s.back();
					Object value = parseObject(context, s, level+1);
					if (level < this.maxDepth) map.put(key, value);
					point = 5;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				continue;
			case ':':
				if (point == 2) {
					point = 3;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				continue;
			case ',':
				if (point == 5 || point == 6 || (mode == Mode.TRADITIONAL && point == 3)) {
					if (point == 3 && level < this.maxDepth && !this.suppressNull) {
						map.put(key, null);
					}
					point = 1;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				continue;
			case '}':
				if (start == '{' && (point == 1 || point == 5 || point == 6 || (mode == Mode.TRADITIONAL && point == 3))) {
					if (point == 3 && level < this.maxDepth && !this.suppressNull) {
						map.put(key, null);
					}
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break loop;
			case '[':
				if (point == 3) {
					s.back();
					List<Object> value = parseArray(context, s, level+1);
					if (level < this.maxDepth) map.put(key, value);
					point = 5;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				continue;
			case '\'':
				if (mode == Mode.STRICT) {
					break;
				}
			case '"':
				if (point == 0) {
					s.back();
					point = 1;
				} else if (point == 1 || point == 6) {
					s.back();
					key = parseString(context, s, level+1);
					point = 2;
				} else if (point == 3) {
					s.back();
					String value = parseString(context, s, level+1);
					if (level < this.maxDepth) map.put(key, value);
					point = 5;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				continue;
			case '/':
			case '#':
				if (mode == Mode.TRADITIONAL || (mode == Mode.SCRIPT && c == '/')) {
					s.back();
					skipComment(s);
					if (point == 5) {
						point = 6;
					}
					continue;
				}
			}
			
			if (point == 0) {
				s.back();
				point = 1;
			} else if (point == 1 || point == 6) {
				s.back();
				key = ((c == '-') || (c >= '0' && c <= '9')) ? parseNumber(context, s, level+1) : parseLiteral(context, s, level+1, mode != Mode.STRICT);
				point = 2;
			} else if (point == 3) {
				s.back();
				Object value = ((c == '-') || (c >= '0' && c <= '9')) ? parseNumber(context, s, level+1) : parseLiteral(context, s, level+1, mode == Mode.TRADITIONAL);
				if (level < this.maxDepth && (value != null || !this.suppressNull)) {
					map.put(key, value);
				}
				point = 5;
			} else {
				throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
			}
		}
		
		if (n == -1) {
			if (point == 3 || point == 4) {
				if (level < this.maxDepth && !this.suppressNull) map.put(key, null);
			} else if (point == 2) {
				throw createParseException(getMessage("json.parse.ObjectNotClosedError"), s);
			}
		}
		
		if ((n == -1) ? (start != '\0') : (n != '}')) {
			throw createParseException(getMessage("json.parse.ObjectNotClosedError"), s);
		}
		return map;
	}
	
	List<Object> parseArray(Context context, ParserSource s, int level) throws IOException, JSONException {
		int point = 0; // 0 '[' 1 'value' 2 '\n'? 3 ',' 4  ... ']' E
		List<Object> list = (level <= this.maxDepth) ? new ArrayList<Object>() : null;
		
		int n = -1;
		loop:while ((n = s.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case '\r':
			case '\n':
				if (mode == Mode.TRADITIONAL && point == 2) {
					point = 3;
				}
				continue;
			case ' ':
			case '\t':
			case 0xFEFF: // BOM
				continue;
			case '[':
				if (point == 0) {
					point = 1;
				} else if (point == 1 || point == 3 || point == 4) {
					s.back();
					List<Object> value = parseArray(context, s, level+1);
					if (level < this.maxDepth) list.add(value);
					point = 2;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				continue;
			case ',':
				if (mode == Mode.TRADITIONAL && (point == 1 || point == 4)) {
					if (level < this.maxDepth) list.add(null);
				} else if (point == 2 || point == 3) {
					point = 4;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				continue;
			case ']':
				if (point == 1 || point == 2 || point == 3) {
					// nothing
				} else if (mode == Mode.TRADITIONAL && point == 4) {
					if (level < this.maxDepth) list.add(null);
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break loop;	
			case '{':
				if (point == 1 || point == 3 || point == 4){
					s.back();
					Map<Object, Object> value = parseObject(context, s, level+1);
					if (level < this.maxDepth) list.add(value);
					point = 2;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				continue;
			case '\'':
				if (mode == Mode.STRICT) {
					break;
				}
			case '"':
				if (point == 1 || point == 3 || point == 4) {
					s.back();
					String value = parseString(context, s, level+1);
					if (level < this.maxDepth) list.add(value);
					point = 2;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				continue;
			case '/':
			case '#':
				if (mode == Mode.TRADITIONAL || (mode == Mode.SCRIPT && c == '/')) {
					s.back();
					skipComment(s);
					if (point == 2) {
						point = 3;
					}
					continue;
				}
			}
			
			if (point == 1 || point == 3 || point == 4) {
				s.back();
				Object value = ((c == '-') || (c >= '0' && c <= '9')) ? parseNumber(context, s, level+1) : parseLiteral(context, s, level+1, mode == Mode.TRADITIONAL);
				if (level < this.maxDepth) list.add(value);
				point = 2;
			} else {
				throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
			}
		}
		
		if (n != ']') {
			throw createParseException(getMessage("json.parse.ArrayNotClosedError"), s);
		}
		return list;
	}
	
	String parseString(Context context, ParserSource s, int level) throws IOException, JSONException {
		int point = 0; // 0 '"|'' 1 'c' ... '"|'' E
		StringBuilderFormatSource sb = (level <= this.maxDepth) ? context.getCachedBuffer() : null;
		char start = '\0';
		
		int n = -1;
		loop:while ((n = s.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case 0xFEFF: // BOM
				continue;
			case '\\':
				if (point == 1) {
					if (mode != Mode.TRADITIONAL || start == '"') {
						s.back();
						c = parseEscape(s);
						if (sb != null) sb.append(c);
					} else {
						if (sb != null) sb.append(c);
					}
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				continue;
			case '\'':
				if (mode == Mode.STRICT) {
					continue;
				}
			case '"':
				if (point == 0) {
					start = c;
					point = 1;
					continue;
				} else if (point == 1) {
					if (start == c) {
						break loop;						
					} else {
						if (sb != null) sb.append(c);
					}
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				continue;
			}
			if (point == 1 && (mode != Mode.STRICT  || c >= 0x20)) {
				if (sb != null) sb.append(c);
				continue;
			}
			throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
		}
		
		if (n != start) {
			throw createParseException(getMessage("json.parse.StringNotClosedError"), s);
		}
		return (sb != null) ? sb.toString() : null;
	}
	
	Object parseLiteral(Context context, ParserSource s, int level, boolean any) throws IOException, JSONException {
		int point = 0; // 0 'IdStart' 1 'IdPart' ... !'IdPart' E
		StringBuilderFormatSource sb = context.getCachedBuffer();

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
			} else if (point == 1 && (Character.isJavaIdentifierPart(c) || c == '.')) {
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
		
		if (!any) {
			throw createParseException(getMessage("json.parse.UnrecognizedLiteral", str), s);
		}
		
		return str;
	}	
	
	Number parseNumber(Context context, ParserSource s, int level) throws IOException, JSONException {
		int point = 0; // 0 '(-)' 1 '0' | ('[1-9]' 2 '[0-9]*') 3 '(.)' 4 '[0-9]' 5 '[0-9]*' 6 'e|E' 7 '[+|-]' 8 '[0-9]' 9 '[0-9]*' E
		StringBuilderFormatSource sb = (level <= this.maxDepth) ? context.getCachedBuffer() : null;
		
		int n = -1;
		loop:while ((n = s.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case 0xFEFF: // BOM
				break;
			case '+':
				if (point == 7) {
					if (sb != null) sb.append(c);
					point = 8;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '-':
				if (point == 0) {
					if (sb != null) sb.append(c);
					point = 1;
				} else if (point == 7) {
					if (sb != null) sb.append(c);
					point = 8;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '.':
				if (point == 2 || point == 3) {
					if (sb != null) sb.append(c);
					point = 4;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case 'e':
			case 'E':
				if (point == 2 || point == 3 || point == 5 || point == 6) {
					if (sb != null) sb.append(c);
					point = 7;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			default:
				if (c >= '0' && c <= '9') {
					if (point == 0 || point == 1) {
						if (sb != null) sb.append(c);
						point = (c == '0') ? 3 : 2;
					} else if (point == 2 || point == 5 || point == 9) {
						if (sb != null) sb.append(c);
					} else if (point == 4) {
						if (sb != null) sb.append(c);
						point = 5;
					} else if (point == 7 || point == 8) {
						if (sb != null) sb.append(c);
						point = 9;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
				} else if (point == 2 || point == 3 || point == 5 || point == 6 || point == 9) {
					s.back();
					break loop;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
			}
		}
		
		return (sb != null) ? new BigDecimal(sb.toString()) : null;
	}
	
	char parseEscape(ParserSource s) throws IOException, JSONException {
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
	
	void skipComment(ParserSource s) throws IOException, JSONException {
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
				if (mode == Mode.TRADITIONAL) {
					if (point == 0) {
						point = 4;
					} else if (point == 3) {
						point = 2;
					} else if (!(point == 2 || point == 4)) {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				}
			default:
				if (point == 3) {
					point = 2;
				} else if (!(point == 2 || point == 4)) {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
			}
		}	
	}
	
	JSONException createParseException(String message, ParserSource s) {
		return new JSONException("" + s.getLineNumber() + ": " + message + "\n" + s.toString() + " <- ?",
				JSONException.PARSE_ERROR, s.getLineNumber(), s.getColumnNumber(), s.getOffset());
	}
	
	String getMessage(String id, Object... args) {
		if (locale == null) locale = Locale.getDefault();
		ResourceBundle bundle = ResourceBundle.getBundle("net.arnx.jsonic.Messages", locale);
		return MessageFormat.format(bundle.getString(id), args);
	}
	
	public Object convert(Object value, Type type)  throws JSONException {
		return convert(new Context(), value, type);
	}
	
	@SuppressWarnings("unchecked")
	<T> T convert(Context context, Object value, Type type) throws JSONException {
		Class<?> cls = ClassUtil.getRawType(type);
		
		Object result = null;
		try {
			context.enter('$');
			result = postparse(context, value, cls, type);
			context.exit();
		} catch (Exception e) {
			throw new JSONException(getMessage("json.parse.ConversionError",
					(value instanceof String) ? "\"" + value + "\"" : value, type, context),
					JSONException.POSTPARSE_ERROR, e);
		}
		return (T)result;
	}
		
	/**
	 * Converts Map, List, Number, String, Boolean or null to other Java Objects after parsing. 
	 * 
	 * @param context current context.
	 * @param value null or the instance of Map, List, Number, String or Boolean.
	 * @param cls class for converting
	 * @param type generics type for converting. type equals to c if not generics.
	 * @return a converted object
	 * @throws Exception if conversion failed.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected <T> T postparse(Context context, Object value, Class<? extends T> cls, Type type) throws Exception {
		Object data = null;
		
		JSONHint hint = context.getHint();
		Class<?> c = cls;
		
		if (c.isPrimitive()) {
			data = PRIMITIVE_MAP.get(c);
			c = data.getClass();
		}
		
		if (value == null) {
			// no handle
		} else if (hint != null && hint.serialized()) {
			data = format(value);
		} else if ((hint != null && Serializable.class.equals(hint.type())) || ClassUtil.isAssignableFrom("java.sql.RowId", c)) {
			try {
				data = deserialize(Base64.decode((String)value));
			} catch (Exception e) {
				throw e;
			}
		} else if (hint != null && String.class.equals(hint.type())) {
			try {
				Constructor<?> con = c.getConstructor(String.class);
				data = con.newInstance(value.toString());
			} catch (NoSuchMethodException e) {
				data = null; // ignored
			}
		} else if (c.equals(type) && c.isAssignableFrom(value.getClass())) {
			data = value;
		} else if (value instanceof Map) {
			Map<?, ?> src = (Map<?, ?>)value;
			if (Map.class.isAssignableFrom(c)) {
				Map<Object, Object> map = null;
				if (Properties.class.isAssignableFrom(c)) {
					map = (Map<Object, Object>)create(context, c);
					flattenProperties(new StringBuilder(32), (Map<Object, Object>)value, (Properties)map);
				} else if (type instanceof ParameterizedType) {
					Type[] pts = ((ParameterizedType)type).getActualTypeArguments();
					Type pt0 = (pts != null && pts.length > 0) ? pts[0] : Object.class;
					Type pt1 = (pts != null && pts.length > 1) ? pts[1] : Object.class;
					Class<?> pc0 = ClassUtil.getRawType(pt0);
					Class<?> pc1 = ClassUtil.getRawType(pt1);
					
					if ((Object.class.equals(pc0) || String.class.equals(pc0))
							&& Object.class.equals(pc1)) {
						map = (Map<Object, Object>)value;
					} else {
						map = (Map<Object, Object>)create(context, c);
						for (Map.Entry<?, ?> entry : src.entrySet()) {
							context.enter('.');
							Object key = postparse(context, entry.getKey(), pc0, pt0);
							context.exit();
							
							context.enter(entry.getKey());
							map.put(key, postparse(context, entry.getValue(), pc1, pt1));
							context.exit();
						}
					}
				} else {
					map = (Map<Object, Object>)create(context, c);
					map.putAll(src);
				}
				data = map;
			} else if (Collection.class.isAssignableFrom(c) || c.isArray()) {
				if (!(src instanceof SortedMap)) {
					src = new TreeMap<Object, Object>(src);
				}
				data = postparse(context, src.values(), c, type);
			} else if (c.isEnum()
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
					|| ClassUtil.equals("java.net.InetAddress", c)
					|| Charset.class.equals(c)
					|| Class.class.equals(c)
				) {
				if (src.containsKey(null)) {
					Object target = src.get(null);
					if (target instanceof List) {
						List<?> list = (List<?>)target;
						target = (!list.isEmpty()) ? list.get(0) : null;
					}
					data = postparse(context, target, c, type);
				}
			} else {
				Object o = create(context, c);
				if (o != null) {
					Map<String, Property> props = context.getSetProperties(c);
					for (Map.Entry<?, ?> entry : src.entrySet()) {
						String name = entry.getKey().toString();
						Property target = props.get(name);
						if (target == null) target = props.get(ClassUtil.toLowerCamel(name));
						if (target == null) continue;
						
						context.enter(name, target.getHint());
						target.set(o, postparse(context, value, target.getType(type), target.getGenericType(type)));
						context.exit();
					}
					data = o;
				} else {
					throw new UnsupportedOperationException();
				}
			}
		} else if (value instanceof List) {
			List<?> src = (List<?>)value;
			if (Collection.class.isAssignableFrom(c)) {
				Collection<Object> collection = null;
				if (type instanceof ParameterizedType) {
					Type[] pts = ((ParameterizedType)type).getActualTypeArguments();
					Type pt = (pts != null && pts.length > 0) ? pts[0] : Object.class;
					Class<?> pc = ClassUtil.getRawType(pt);
					
					if (Object.class.equals(pc)) {
						collection = (Collection<Object>)src;
					} else {
						collection = (Collection<Object>)create(context, c);
						for (int i = 0; i < src.size(); i++) {
							context.enter(i);
							collection.add(postparse(context, src.get(i), pc, pt));
							context.exit();
						}
					}
				} else {
					collection = (Collection<Object>)create(context, c);
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
					Array.set(array, i, postparse(context, src.get(i), pc, pt));
					context.exit();
				}
				data = array;
			} else if (Map.class.isAssignableFrom(c)) {
				Map<Object, Object> map = (Map<Object, Object>)create(context, c);
				if (Properties.class.isAssignableFrom(c)) {
					flattenProperties(new StringBuilder(32), (List<Object>)value, (Properties)map);
				} else if (type instanceof ParameterizedType) {
					Type[] pts = ((ParameterizedType)type).getActualTypeArguments();
					Type pt0 = (pts != null && pts.length > 0) ? pts[0] : Object.class;
					Type pt1 = (pts != null && pts.length > 1) ? pts[1] : Object.class;
					Class<?> pc0 = ClassUtil.getRawType(pt0);
					Class<?> pc1 = ClassUtil.getRawType(pt1);

					for (int i = 0; i < src.size(); i++) {
						context.enter('.');
						Object key = postparse(context, i, pc0, pt0);
						context.exit();
						
						context.enter(i);
						map.put(key, postparse(context, src.get(i), pc1, pt1));
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
				data = postparse(context, src.get(0), c, type);
			} else {
				throw new UnsupportedOperationException();
			}
		} else if (Number.class.isAssignableFrom(c)) {
			if (value instanceof String) {
				NumberFormat f = context.format(NumberFormat.class);
				if (f != null) value = f.parse((String)value);
			}
			
			if (Byte.class.equals(c)) {
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
					}
				}
			} else if (Short.class.equals(c)) {
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
					}				
				}
			} else if (Long.class.equals(c)) {
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
					}					
				}
			} else if (Float.class.equals(c)) {
				if (value instanceof Boolean) {
					data = (((Boolean)value).booleanValue()) ? 1.0f : Float.NaN;
				} else if (value instanceof Number) {
					data = ((Number)value).floatValue();
				} else {
					String str = value.toString().trim();
					if (str.length() > 0) {
						data = Float.valueOf(str);
					}					
				}
			} else if (Double.class.equals(c)) {
				if (value instanceof Boolean) {
					data = (((Boolean)value).booleanValue()) ? 1.0 : Double.NaN;
				} else if (value instanceof Number) {
					data = ((Number)value).doubleValue();
				} else {
					String str = value.toString().trim();
					if (str.length() > 0) {
						data = Double.valueOf(str);
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
			} else if (Character.class.equals(c)) {
				if (value instanceof Boolean) {
					data = (((Boolean)value).booleanValue()) ? '1' : '0';
				} else if (value instanceof Number) {
					data = (char)((Number)value).intValue();
				} else {
					String s = value.toString();
					if (s.length() > 0) {
						data = s.charAt(0);
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
				Date date = null;
				long millis = -1;
				if (value instanceof Number) {
					millis = ((Number)value).longValue();
					date = (Date)create(context, c);
				} else {
					String str = value.toString().trim();
					if (str.length() > 0) {
						millis = convertDate(str, locale);
						date = (Date)create(context, c);						
					}
				}
				
				if (date != null) {
					if (date instanceof java.sql.Date) {
						Calendar cal = Calendar.getInstance();
						cal.setTimeInMillis(millis);
						cal.set(Calendar.HOUR_OF_DAY, 0);
						cal.set(Calendar.MINUTE, 0);
						cal.set(Calendar.SECOND, 0);
						cal.set(Calendar.MILLISECOND, 0);
						date.setTime(cal.getTimeInMillis());
					} else if (date instanceof java.sql.Time) {
						Calendar cal = Calendar.getInstance();
						cal.setTimeInMillis(millis);
						cal.set(Calendar.YEAR, 1970);
						cal.set(Calendar.MONTH, Calendar.JANUARY);
						cal.set(Calendar.DATE, 1);
						date.setTime(cal.getTimeInMillis());
					} else {
						date.setTime(millis);
					}
				}
				
				data = date;
			} else if (Calendar.class.isAssignableFrom(c)) {
				if (value instanceof Number) {
					Calendar cal = (Calendar)create(context, c);
					cal.setTimeInMillis(((Number)value).longValue());
					data = cal;
				} else {
					String str = value.toString().trim();
					if (str.length() > 0) {
						Calendar cal = (Calendar)create(context, c);
						cal.setTimeInMillis(convertDate(str, locale));
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
			} else if (UUID.class.equals(c)) {
				data = UUID.fromString(value.toString().trim());
			} else if (ClassUtil.equals("java.net.InetAddress", c)) {
				Class<?> inetAddressClass = ClassUtil.findClass("java.net.InetAddress");
				data = inetAddressClass.getMethod("getByName", String.class).invoke(null, value.toString().trim());
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
					try {
						ClassLoader cl = Thread.currentThread().getContextClassLoader();
						data = cl.loadClass(value.toString());
					} catch (ClassNotFoundException e) {
					}
				}
			} else if (Collection.class.isAssignableFrom(c)) {
				Collection<Object> collection = (Collection<Object>)create(context, c);
				if (type instanceof ParameterizedType) {
					Type[] pts = ((ParameterizedType)type).getActualTypeArguments();
					Type pt = (pts != null && pts.length > 0) ? pts[0] : Object.class;
					Class<?> pc = ClassUtil.getRawType(pt);
					context.enter(0);
					collection.add(postparse(context, value, pc, pt));
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
					Array.set(array, 0, postparse(context, value, pc, pt));
					context.exit();
					data = array;
				}
			} else if (java.sql.Array.class.isAssignableFrom(c)
					|| Struct.class.isAssignableFrom(c)) {
				data = null; // ignored
			} else if (Map.class.isAssignableFrom(c)) {
				Map<Object, Object> map = (Map)create(context, c);
				Object key = (hint != null && hint.anonym().length() > 0) ? hint.anonym() : null;
				if (type instanceof ParameterizedType) {
					Type[] pts = ((ParameterizedType)type).getActualTypeArguments();
					Type pt0 = (pts != null && pts.length > 0) ? pts[0] : Object.class;
					Type pt1 = (pts != null && pts.length > 1) ? pts[1] : Object.class;
					Class<?> pc0 = ClassUtil.getRawType(pt0);
					Class<?> pc1 = ClassUtil.getRawType(pt1);
					
					context.enter('.');
					key = postparse(context, key, pc0, pt0);
					context.exit();
					
					context.enter(key);
					map.put(key, postparse(context, value, pc1, pt1));
					context.exit();
				} else {
					map.put(value, null);
				}
				data = map;
			} else if (hint != null && hint.anonym().length() > 0) {
				Map<String, Property> props = context.getSetProperties(c);
				Property target = props.get(hint.anonym());
				if (target != null) {
					Object o = create(context, c);
					if (o != null) {
						context.enter(hint.anonym(), target.getHint());
						target.set(o, postparse(context, value, target.getType(type), target.getGenericType(type)));
						context.exit();
					}
					data = o;
				}
			} else {
				throw new UnsupportedOperationException();
			}
		}
		
		return (T)data;
	}
	
	protected String normalize(String name) {
		return name;
	}
	
	/**
	 * Ignore this property. A default behavior is to ignore transient or declaring method in java.lang.Object.
	 * You can override this method if you have to change default behavior.
	 * 
	 * @param context current context
	 * @param target target class
	 * @param member target member
	 * @return true if this member must be ignored.
	 */
	protected boolean ignore(Context context, Class<?> target, Member member) {
		if (Modifier.isTransient(member.getModifiers())) return true;
		if (member.getDeclaringClass().equals(Object.class)) return true;
		return false;
	}
	
	protected <T> T create(Context context, Class<? extends T> c) throws Exception {
		Object instance = null;
		
		JSONHint hint = context.getHint();
		if (hint != null && hint.type() != Object.class) c = hint.type().asSubclass(c);
		
		if (c.isInterface()) {
			if (SortedMap.class.equals(c)) {
				instance = new TreeMap<Object, Object>();
			} else if (Map.class.equals(c)) {
				instance = new LinkedHashMap<Object, Object>();
			} else if (SortedSet.class.equals(c)) {
				instance = new TreeSet<Object>();
			} else if (Set.class.equals(c)) {
				instance = new LinkedHashSet<Object>();
			} else if (List.class.equals(c)) {
				instance = new ArrayList<Object>();
			} else if (Collection.class.equals(c)) {
				instance = new ArrayList<Object>();
			} else if (Appendable.class.equals(c)) {
				instance = new StringBuilder();
			}
		} else if (Modifier.isAbstract(c.getModifiers())) {
			if (Calendar.class.equals(c)) {
				instance = Calendar.getInstance();
			}
		} else if ((c.isMemberClass() || c.isAnonymousClass()) && !Modifier.isStatic(c.getModifiers())) {
			Class<?> eClass = c.getEnclosingClass();
			Constructor<?> con = c.getDeclaredConstructor(eClass);
			con.setAccessible(true);
			if (contextObject != null && eClass.isAssignableFrom(contextObject.getClass())) {
				instance = con.newInstance(contextObject);
			} else {
				instance = con.newInstance((Object)null);
			}
		} else {
			if (Date.class.isAssignableFrom(c)) {
				try {
					Constructor<?> con = c.getDeclaredConstructor(long.class);
					con.setAccessible(true);
					instance = con.newInstance(0l);
				} catch (NoSuchMethodException e) {
					// no handle
				}
			}
			
			if (instance == null) {
				Constructor<?> con = c.getDeclaredConstructor();
				con.setAccessible(true);
				instance = con.newInstance();
			}
		}
		
		return c.cast(instance);
	}
	
	static void flattenProperties(StringBuilder key, Object value, Properties props) {
		if (value instanceof Map<?,?>) {
			for (Map.Entry<?, ?> entry : ((Map<?, ?>)value).entrySet()) {
				int pos = key.length();
				if (pos > 0) key.append('.');
				key.append(entry.getKey());
				flattenProperties(key, entry.getValue(), props);
				key.setLength(pos);
			}
		} else if (value instanceof List<?>) {
			List<?> list = (List<?>)value;
			for (int i = 0; i < list.size(); i++) {
				int pos = key.length();
				if (pos > 0) key.append('.');
				key.append(i);
				flattenProperties(key, list.get(i), props);
				key.setLength(pos);
			}
		} else {
			props.setProperty(key.toString(), value.toString());
		}
	}
	
	static byte[] serialize(Object data) throws IOException {
		ByteArrayOutputStream array = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(array);
		out.writeObject(data);
		out.close();
		return array.toByteArray();
	}	
	
	static Object deserialize(byte[] array) throws IOException, ClassNotFoundException {
		Object ret = null;
		ObjectInputStream in = null;
		try {
			in = new ObjectInputStream(new ByteArrayInputStream(array));
			ret = in.readObject();
		} finally {
			if (in != null) in.close();
		}
		return ret;
	}
	
	static Long convertDate(String value, Locale locale) throws java.text.ParseException {
		value = value.trim();
		if (value.length() == 0) {
			return null;
		}
		if (locale == null) locale = Locale.getDefault();
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
			} else  {
				format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale);
			}
		} else {
			format = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
		}
		
		return format.parse(value).getTime();
	}
	
	public class Context {
		final boolean prettyPrint;
		
		Object[] path;
		int level = -1;
		Map<Class<?>, Object> memberCache;
		StringBuilderFormatSource builderCache;
		
		public Context() {
			prettyPrint = JSON.this.prettyPrint;
		}
		
		public StringBuilderFormatSource getCachedBuffer() {
			if (builderCache == null) {
				builderCache = new StringBuilderFormatSource();
			} else {
				builderCache.clear();
			}
			return builderCache;
		}
		
		public boolean isPrettyPrint() {
			return prettyPrint;
		}
		
		/**
		 * Returns the current level.
		 * 
		 * @return level number. 0 is root node.
		 */
		public int getLevel() {
			return level;
		}
		
		/**
		 * Returns the current key object.
		 * 
		 * @return Root node is '$'. When the parent is a array, the key is Integer, otherwise String. 
		 */
		public Object getKey() {
			return path[level*2];
		}
		
		/**
		 * Returns the key object in any level. the negative value means relative to current level.
		 * 
		 * @return Root node is '$'. When the parent is a array, the key is Integer, otherwise String. 
		 */
		public Object getKey(int level) {
			if (level < 0) level = getLevel()+level;
			return path[level*2];
		}
		
		/**
		 * Returns the current hint annotation.
		 * 
		 * @return the current annotation if present on this context, else null.
		 */
		public JSONHint getHint() {
			return (JSONHint)path[level*2+1];
		}
		
		@SuppressWarnings("unchecked")
		public <T> T convert(Object key, Object value, Class<? extends T> c) throws Exception {
			enter(key);
			T o = JSON.this.postparse(this, value, c, c);
			exit();
			return (T)((c.isPrimitive()) ? PRIMITIVE_MAP.get(c).getClass() : c).cast(o);
		}
		
		public Object convert(Object key, Object value, Type t) throws Exception {
			Class<?> c = ClassUtil.getRawType(t);
			enter(key);
			Object o = JSON.this.postparse(this, value, c, t);
			exit();
			return ((c.isPrimitive()) ? PRIMITIVE_MAP.get(c).getClass() : c).cast(o);
		}
		
		void enter(Object key, JSONHint hint) {
			level++;
			if (path == null) path = new Object[8];
			if (path.length < level*2+2) {
				Object[] newPath = new Object[Math.max(path.length*2, level*2+2)];
				System.arraycopy(path, 0, newPath, 0, path.length);
				path = newPath;
			}
			path[level*2] = key;
			path[level*2+1] = hint;
		}
		
		void enter(Object key) {
			enter(key, (JSONHint)((level != -1) ? path[level*2+1] : null));
		}
		
		void exit() {
			level--;
		}
		
		boolean hasMemberCache(Class<?> c) {
			return memberCache != null && memberCache.containsKey(c);
		}
		
		@SuppressWarnings("unchecked")
		List<Property> getGetProperties(Class<?> c) {
			if (memberCache == null) memberCache = new HashMap<Class<?>, Object>();
			
			List<Property> list = (List<Property>)memberCache.get(c);
			if (list != null) return list;
			
			Map<String, Property> props = new HashMap<String, Property>();
			
			for (Field f : c.getFields()) {
				if (Modifier.isStatic(f.getModifiers())
						|| f.isSynthetic()
						|| ignore(this, c, f)) {
					continue;
				}
				
				String name = normalize(f.getName());
				JSONHint hint = f.getAnnotation(JSONHint.class);
				if (hint != null) {
					if (hint.ignore()) continue;
					if (hint.name().length() > 0) name = hint.name();
				}
				props.put(name, new FieldProperty(name, f, hint));
			}
			
			for (Method m : c.getMethods()) {
				if (Modifier.isStatic(m.getModifiers())
						|| m.isSynthetic()
						|| m.isBridge()
						|| ignore(this, c, m)) {
					continue;
				}

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
				
				name = name.substring(start);
				if (name.length() < 2 || !Character.isUpperCase(name.charAt(1)) ){
					char[] chars = name.toCharArray();
					chars[0] = Character.toLowerCase(chars[0]);
					name = new String(chars);
				}
				name = normalize(name);
				
				JSONHint hint = m.getAnnotation(JSONHint.class);
				if (hint != null) {
					if (hint.ignore()) continue;
					if (hint.name().length() > 0) name = hint.name();
				}
				props.put(name, new MethodProperty(name, m, hint));
			}
			
			list = new ArrayList<Property>(props.values());
			Collections.sort(list);
			
			memberCache.put(c, list);
			return list;
		}
		
		@SuppressWarnings("unchecked")
		Map<String, Property> getSetProperties(Class<?> c) {
			if (memberCache == null) memberCache = new HashMap<Class<?>, Object>();
			
			Map<String, Property> props = (Map<String, Property>)memberCache.get(c);
			if (props != null) return props;
			props = new HashMap<String, Property>();

			for (Field f : c.getFields()) {
				if (Modifier.isStatic(f.getModifiers())
						|| f.isSynthetic()
						|| ignore(this, c, f)) {
					continue;
				}
				
				String name = normalize(f.getName());
				JSONHint hint = f.getAnnotation(JSONHint.class);
				if (hint != null) {
					if (hint.ignore()) continue;
					if (hint.name().length() > 0) name = hint.name();
				}
				props.put(name, new FieldProperty(name, f, hint));
			}
			
			for (Method m : c.getMethods()) {
				if (Modifier.isStatic(m.getModifiers())
						|| m.isSynthetic()
						|| m.isBridge()
						|| ignore(this, c, m)) {
					continue;
				}

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
				
				name = name.substring(start);
				if (name.length() < 2 || !Character.isUpperCase(name.charAt(1)) ){
					char[] chars = name.toCharArray();
					chars[0] = Character.toLowerCase(chars[0]);
					name = new String(chars);
				}
				name = normalize(name);
				
				JSONHint hint = m.getAnnotation(JSONHint.class);
				if (hint != null) {
					if (hint.ignore()) continue;
					if (hint.name().length() > 0) name = hint.name();
				}
				props.put(name, new MethodProperty(name, m, hint));
			}
			
			memberCache.put(c, props);
			return props;
		}
		
		<T extends Format> T format(Class<? extends T> c) {
			T format = null;
			JSONHint hint = getHint();
			if (hint != null && hint.format().length() > 0) {
				if (NumberFormat.class.isAssignableFrom(c)) {
					if (locale != null) {
						format = c.cast(new DecimalFormat(hint.format(), new DecimalFormatSymbols(locale)));
					} else {
						format = c.cast(new DecimalFormat(hint.format()));
					}
				} else if (DateFormat.class.isAssignableFrom(c)) {
					if (locale != null) {
						format = c.cast(new ComplexDateFormat(hint.format(), locale));
					} else {
						format = c.cast(new ComplexDateFormat(hint.format()));
					}
				}
			}
			return format;
		}
		
		public String toString() {
			StringBuilderFormatSource sb = getCachedBuffer();
			for (int i = 0; i < path.length; i+=2) {
				Object key = path[i];
				if (key == null) {
					sb.append("[null]");
				} else if (key instanceof Number) {
					sb.append('[');
					sb.append(key.toString());
					sb.append(']');
				} else if (key instanceof Character) {
					sb.append(key.toString());
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
							formatString(this, str, sb);
						} catch (IOException e) {
							// no handle
						}
						sb.append(']');
					} else {
						sb.append('.');
						sb.append(str);
					}
				}
			}
			return sb.toString();
		}
	}
}
