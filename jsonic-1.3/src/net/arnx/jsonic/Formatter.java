/* 
 * Copyright 2014 Hidekatsu Izuno
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.arnx.jsonic;

import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.Struct;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.RandomAccess;
import java.util.TimeZone;

import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaProperty;
import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.JSON.Mode;
import net.arnx.jsonic.io.OutputSource;
import net.arnx.jsonic.util.Base64;
import net.arnx.jsonic.util.BeanInfo;
import net.arnx.jsonic.util.ClassUtil;
import net.arnx.jsonic.util.PropertyInfo;

interface Formatter {
	boolean accept(Object o);
	boolean format(Context context, Object src, Object o, OutputSource out) throws Exception;
}

final class NullFormatter implements Formatter {
	public static final NullFormatter INSTANCE = new NullFormatter();
	
	@Override
	public boolean accept(Object o) {
		return o == null;
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		out.append("null");
		return false;
	}
}

final class PlainFormatter implements Formatter {
	public static final PlainFormatter INSTANCE = new PlainFormatter();
	
	@Override
	public boolean accept(Object o) {
		return o != null;
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		out.append(o.toString());
		return false;
	}
}

final class StringFormatter implements Formatter {
	public static final StringFormatter INSTANCE = new StringFormatter();

	private static final int[] ESCAPE_CHARS = new int[128];

	static {
		for (int i = 0; i < 32; i++) {
			ESCAPE_CHARS[i] = -1;
		}
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
	}

	@Override
	public boolean accept(Object o) {
		return o != null;
	}

	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		serialize(context, o.toString(), out);
		return false;
	}

	static void serialize(final Context context, final String s, final OutputSource out) throws IOException {
		out.append('"');
		int start = 0;
		final int length = s.length();
		for (int i = 0; i < length; i++) {
			int c = s.charAt(i);
			if (c < ESCAPE_CHARS.length) {
				int x = ESCAPE_CHARS[c];
				if (x == 0) {
					// no handle
				} else if (x > 0) {
					if (start < i) out.append(s, start, i);
					out.append('\\');
					out.append((char)x);
					start = i + 1;
				} else if (x == -1 || (x == -2 && context.getMode() != Mode.STRICT)) {
					if (start < i) out.append(s, start, i);
					out.append("\\u00");
					out.append("0123456789ABCDEF".charAt(c / 16));
					out.append("0123456789ABCDEF".charAt(c % 16));
					start = i + 1;
				}
			} else if (c == '\u2028') {
				if (start < i) out.append(s, start, i);
				out.append("\\u2028");
				start = i + 1;
			} else if (c == '\u2029') {
				if (start < i) out.append(s, start, i);
				out.append("\\u2029");
				start = i + 1;
			}
		}
		if (start < length) out.append(s, start, length);
		out.append('"');
	}
}

final class StringableFormmatter implements Formatter {
	public static final StringableFormmatter INSTANCE = new StringableFormmatter();

	@Override
	public boolean accept(Object o) {
		return o instanceof CharSequence || o instanceof Type || o instanceof Member || o instanceof File;
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		return StringFormatter.INSTANCE.format(context, src, o, out);
	}	
}

final class TimeZoneFormatter implements Formatter {
	public static final TimeZoneFormatter INSTANCE = new TimeZoneFormatter();

	@Override
	public boolean accept(Object o) {
		return o instanceof TimeZone;
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		return StringFormatter.INSTANCE.format(context, src, ((TimeZone)o).getID(), out);
	}
}

final class CharsetFormatter implements Formatter {
	public static final CharsetFormatter INSTANCE = new CharsetFormatter();

	@Override
	public boolean accept(Object o) {
		return o instanceof Charset;
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		return StringFormatter.INSTANCE.format(context, src, ((Charset)o).name(), out);
	}
}

final class InetAddressFormatter implements Formatter {
	public static final InetAddressFormatter INSTANCE = new InetAddressFormatter();
	private static final Class<?> target = InetAddress.class;

	@Override
	public boolean accept(Object o) {
		return o != null && target.isAssignableFrom(o.getClass());
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		return StringFormatter.INSTANCE.format(context, src, ((InetAddress)o).getHostAddress(), out);
	}
}

final class NumberFormatter implements Formatter {
	public static final NumberFormatter INSTANCE = new NumberFormatter();

	public boolean accept(Object o) {
		return o instanceof Number;
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		NumberFormat f = context.getNumberFormat();
		if (f != null) {
			StringFormatter.serialize(context, f.format(o), out);
		} else {
			out.append(o.toString());
		}
		return false;
	}
}

final class EnumFormatter implements Formatter {
	public static final EnumFormatter INSTANCE = new EnumFormatter();
	
	public boolean accept(Object o) {
		return o != null && o.getClass().isEnum();
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		if (context.getEnumStyle() != null) {
			return StringFormatter.INSTANCE.format(context, src, context.getEnumStyle().to(((Enum<?>)o).name()), out);
		} else {
			return NumberFormatter.INSTANCE.format(context, src, ((Enum<?>)o).ordinal(), out);
		}
	}
}

final class FloatFormatter implements Formatter {
	public static final FloatFormatter INSTANCE = new FloatFormatter();
	
	public boolean accept(Object o) {
		return o instanceof Float || o instanceof Double;
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		NumberFormat f = context.getNumberFormat();
		if (f != null) {
			StringFormatter.serialize(context, f.format(o), out);
		} else {
			double d = ((Number) o).doubleValue();
			if (Double.isNaN(d) || Double.isInfinite(d)) {
				if (context.getMode() != Mode.SCRIPT) {
					out.append('"');
					out.append(o.toString());
					out.append('"');
				} else if (Double.isNaN(d)) {
					out.append("Number.NaN");
				} else {
					out.append("Number.");
					out.append((d > 0) ? "POSITIVE" : "NEGATIVE");
					out.append("_INFINITY");
				}
			} else {
				out.append(o.toString());
			}
		}
		return false;
	}
}

final class DateFormatter implements Formatter {
	public static final DateFormatter INSTANCE = new DateFormatter();
	
	public boolean accept(Object o) {
		return o instanceof Date;
	}

	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		Date date = (Date) o;
		DateFormat f = context.getDateFormat();
		if (f != null) {
			StringFormatter.serialize(context, f.format(o), out);
		} else if (context.getMode() == Mode.SCRIPT) {
			out.append("new Date(");
			out.append(Long.toString(date.getTime()));
			out.append(")");
		} else {
			out.append(Long.toString(date.getTime()));
		}
		return false;
	}
}

final class CalendarFormatter implements Formatter {
	public static final CalendarFormatter INSTANCE = new CalendarFormatter();
	
	public boolean accept(Object o) {
		return o instanceof Calendar;
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		return DateFormatter.INSTANCE.format(context, src, ((Calendar)o).getTime(), out);
	}	
}

final class BooleanArrayFormatter implements Formatter {
	public static final BooleanArrayFormatter INSTANCE = new BooleanArrayFormatter();
	
	public boolean accept(Object o) {
		return o instanceof boolean[];
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		out.append('[');
		boolean[] array = (boolean[]) o;
		for (int i = 0; i < array.length; i++) {
			out.append(String.valueOf(array[i]));
			if (i != array.length - 1) {
				out.append(',');
				if (context.isPrettyPrint()) {
					out.append(' ');
				}
			}
		}
		out.append(']');
		return true;
	}
}

final class ByteArrayFormatter implements Formatter {
	public static final ByteArrayFormatter INSTANCE = new ByteArrayFormatter();
	
	public boolean accept(Object o) {
		return o instanceof byte[];
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		StringFormatter.serialize(context, Base64.encode((byte[]) o), out);
		return false;
	}
}

final class SerializableFormatter implements Formatter {
	public static final SerializableFormatter INSTANCE = new SerializableFormatter();
	
	public boolean accept(Object o) {
		return o instanceof Serializable;
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		return StringFormatter.INSTANCE.format(context, src, Base64.encode(ClassUtil.serialize(o)), out);
	}
}

final class RowIdFormatter implements Formatter {
	public static final RowIdFormatter INSTANCE = new RowIdFormatter();
	private static final Class<?> target = RowId.class;
	
	public boolean accept(Object o) {
		return o != null && target.isAssignableFrom(o.getClass());
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		return SerializableFormatter.INSTANCE.format(context, src, o, out);
	}
}

final class ShortArrayFormatter implements Formatter {
	public static final ShortArrayFormatter INSTANCE = new ShortArrayFormatter();
	
	public boolean accept(Object o) {
		return o instanceof short[];
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		NumberFormat f = context.getNumberFormat();
		short[] array = (short[]) o;
		out.append('[');
		for (int i = 0; i < array.length; i++) {
			if (f != null) {
				StringFormatter.serialize(context, f.format(array[i]), out);
			} else {
				out.append(String.valueOf(array[i]));
			}
			if (i != array.length - 1) {
				out.append(',');
				if (context.isPrettyPrint()) {
					out.append(' ');
				}
			}
		}
		out.append(']');
		return true;
	}
}

final class IntArrayFormatter implements Formatter {
	public static final IntArrayFormatter INSTANCE = new IntArrayFormatter();
	
	public boolean accept(Object o) {
		return o instanceof int[];
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		NumberFormat f = context.getNumberFormat();
		int[] array = (int[]) o;
		out.append('[');
		for (int i = 0; i < array.length; i++) {
			if (f != null) {
				StringFormatter.serialize(context, f.format(array[i]), out);
			} else {
				out.append(String.valueOf(array[i]));
			}
			if (i != array.length - 1) {
				out.append(',');
				if (context.isPrettyPrint()) {
					out.append(' ');
				}
			}
		}
		out.append(']');
		return true;
	}
}

final class LongArrayFormatter implements Formatter {
	public static final LongArrayFormatter INSTANCE = new LongArrayFormatter();
	
	public boolean accept(Object o) {
		return o instanceof long[];
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		NumberFormat f = context.getNumberFormat();
		long[] array = (long[]) o;
		out.append('[');
		for (int i = 0; i < array.length; i++) {
			if (f != null) {
				StringFormatter.serialize(context, f.format(array[i]), out);
			} else {
				out.append(String.valueOf(array[i]));
			}
			if (i != array.length - 1) {
				out.append(',');
				if (context.isPrettyPrint()) {
					out.append(' ');
				}
			}
		}
		out.append(']');
		return true;
	}
}

final class FloatArrayFormatter implements Formatter {
	public static final FloatArrayFormatter INSTANCE = new FloatArrayFormatter();
	
	public boolean accept(Object o) {
		return o instanceof float[];
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		NumberFormat f = context.getNumberFormat();
		float[] array = (float[]) o;
		out.append('[');
		for (int i = 0; i < array.length; i++) {
			if (Float.isNaN(array[i]) || Float.isInfinite(array[i])) {
				if (context.getMode() != Mode.SCRIPT) {
					out.append('"');
					out.append(Float.toString(array[i]));
					out.append('"');
				} else if (Double.isNaN(array[i])) {
					out.append("Number.NaN");
				} else {
					out.append("Number.");
					out.append((array[i] > 0) ? "POSITIVE" : "NEGATIVE");
					out.append("_INFINITY");
				}
			} else if (f != null) {
				StringFormatter.serialize(context, f.format(array[i]), out);
			} else {
				out.append(String.valueOf(array[i]));
			}
			if (i != array.length - 1) {
				out.append(',');
				if (context.isPrettyPrint()) {
					out.append(' ');
				}
			}
		}
		out.append(']');
		return true;
	}
}

final class DoubleArrayFormatter implements Formatter {
	public static final DoubleArrayFormatter INSTANCE = new DoubleArrayFormatter();
	
	public boolean accept(Object o) {
		return o instanceof double[];
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		NumberFormat f = context.getNumberFormat();
		double[] array = (double[]) o;
		out.append('[');
		for (int i = 0; i < array.length; i++) {
			if (Double.isNaN(array[i]) || Double.isInfinite(array[i])) {
				if (context.getMode() != Mode.SCRIPT) {
					out.append('"');
					out.append(Double.toString(array[i]));
					out.append('"');
				} else if (Double.isNaN(array[i])) {
					out.append("Number.NaN");
				} else {
					out.append("Number.");
					out.append((array[i] > 0) ? "POSITIVE" : "NEGATIVE");
					out.append("_INFINITY");
				}
			} else if (f != null) {
				StringFormatter.serialize(context, f.format(array[i]), out);
			} else {
				out.append(String.valueOf(array[i]));
			}
			if (i != array.length - 1) {
				out.append(',');
				if (context.isPrettyPrint()) {
					out.append(' ');
				}
			}
		}
		out.append(']');
		return true;
	}
}

final class ObjectArrayFormatter implements Formatter {
	public static final ObjectArrayFormatter INSTANCE = new ObjectArrayFormatter();
	
	public boolean accept(Object o) {
		return o instanceof Object[];
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		final Object[] array = (Object[]) o;
		final JSONHint hint = context.getHint();
		
		Class<?> lastClass = null;
		Formatter lastFormatter = null;

		out.append('[');
		int i = 0;
		for (; i < array.length; i++) {
			Object item = array[i];
			if (item == src)
				item = null;

			if (i != 0) out.append(',');
			if (context.isPrettyPrint()) {
				out.append('\n');
				context.appendIndent(out, context.getDepth() + 1);
			}
			context.enter(i, hint);
			item = context.preformatInternal(item);
			if (item == null) {
				NullFormatter.INSTANCE.format(context, src, item, out);
			} else if (hint == null) {
				if (item.getClass() == lastClass) {
					lastFormatter.format(context, src, item, out);
				} else {
					lastFormatter = context.formatInternal(item, out);
					lastClass = item.getClass();
				}
			} else {
				context.formatInternal(item, out);
			}
			context.exit();
		}
		if (context.isPrettyPrint() && i > 0) {
			out.append('\n');
			context.appendIndent(out, context.getDepth());
		}
		out.append(']');
		return true;
	}
}

final class SQLArrayFormatter implements Formatter {
	public static final SQLArrayFormatter INSTANCE = new SQLArrayFormatter();
	private static final Class<?> target = java.sql.Array.class;
	
	public boolean accept(Object o) {
		return o != null && target.isAssignableFrom(o.getClass());
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		Object array;
		try {
			array = ((java.sql.Array)o).getArray();
		} catch (SQLException e) {
			array = null;
		}
		if (array == null) array = new Object[0];
		return ObjectArrayFormatter.INSTANCE.format(context, src, array, out);
	}
}

final class StructFormmatter implements Formatter {
	public static final StructFormmatter INSTANCE = new StructFormmatter();
	private static final Class<?> target = Struct.class;
	
	public boolean accept(Object o) {
		return o != null && target.isAssignableFrom(o.getClass());
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		Object value;
		try {
			value = ((Struct)o).getAttributes();
		} catch (SQLException e) {
			value = null;
		}
		if (value == null) value = new Object[0];
		return ObjectArrayFormatter.INSTANCE.format(context, src, o, out);
	}
}

final class ByteFormatter implements Formatter {
	public static final ByteFormatter INSTANCE = new ByteFormatter();
	
	public boolean accept(Object o) {
		return o instanceof Byte;
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		out.append(Integer.toString(((Byte)o).byteValue() & 0xFF));
		return false;
	}
}

final class ClassFormatter implements Formatter {
	public static final ClassFormatter INSTANCE = new ClassFormatter();
	
	public boolean accept(Object o) {
		return o instanceof Class;
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		return StringFormatter.INSTANCE.format(context, src, ((Class<?>)o).getName(), out);
	}
}

final class LocaleFormatter implements Formatter {
	public static final LocaleFormatter INSTANCE = new LocaleFormatter();
	
	public boolean accept(Object o) {
		return o instanceof Locale;
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		return StringFormatter.INSTANCE.format(context, src, ((Locale)o).toString().replace('_', '-'), out);
	}
}

final class CharArrayFormatter implements Formatter {
	public static final CharArrayFormatter INSTANCE = new CharArrayFormatter();
	
	public boolean accept(Object o) {
		return o instanceof char[];
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		return StringFormatter.INSTANCE.format(context, src, String.valueOf((char[])o), out);
	}
}

final class ListFormatter implements Formatter {
	public static final ListFormatter INSTANCE = new ListFormatter();
	
	public boolean accept(Object o) {
		return o instanceof List && o instanceof RandomAccess;
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		final List<?> list = (List<?>)o;
		final JSONHint hint = context.getHint();
		final int length = list.size();
		
		Class<?> lastClass = null;
		Formatter lastFormatter = null;
		
		out.append('[');
		int count = 0;
		while (count < length) {
			Object item = list.get(count);
			if (item == src) item = null;
			
			if (count != 0) out.append(',');
			if (context.isPrettyPrint()) {
				out.append('\n');
				context.appendIndent(out, context.getDepth() + 1);
			}
			context.enter(count, hint);
			item = context.preformatInternal(item);
			if (item == null) {
				NullFormatter.INSTANCE.format(context, src, item, out);
			} else if (hint == null) {
				if (item.getClass() == lastClass) {
					lastFormatter.format(context, src, item, out);
				} else {
					lastFormatter = context.formatInternal(item, out);
					lastClass = item.getClass();
				}
			} else {
				context.formatInternal(item, out);
			}
			context.exit();
			count++;
		}
		if (context.isPrettyPrint() && count > 0) {
			out.append('\n');
			context.appendIndent(out, context.getDepth());
		}
		out.append(']');
		return true;
	}
}

final class IteratorFormatter implements Formatter {
	public static final IteratorFormatter INSTANCE = new IteratorFormatter();
	
	public boolean accept(Object o) {
		return o instanceof Iterator;
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		final Iterator<?> t = (Iterator<?>)o;
		final JSONHint hint = context.getHint();
		
		Class<?> lastClass = null;
		Formatter lastFormatter = null;
		
		out.append('[');
		int count = 0;
		while(t.hasNext()) {
			Object item = t.next();
			if (item == src)
				item = null;

			if (count != 0) out.append(',');
			if (context.isPrettyPrint()) {
				out.append('\n');
				context.appendIndent(out, context.getDepth() + 1);
			}
			context.enter(count, hint);
			item = context.preformatInternal(item);
			if (item == null) {
				NullFormatter.INSTANCE.format(context, src, item, out);
			} else if (hint == null) {
				if (item.getClass() == lastClass) {
					lastFormatter.format(context, src, item, out);
				} else {
					lastFormatter = context.formatInternal(item, out);
					lastClass = item.getClass();
				}
			} else {
				context.formatInternal(item, out);
			}
			context.exit();
			count++;
		}
		if (context.isPrettyPrint() && count > 0) {
			out.append('\n');
			context.appendIndent(out, context.getDepth());
		}
		out.append(']');
		return true;
	}
}

final class IterableFormatter implements Formatter {
	public static final IterableFormatter INSTANCE = new IterableFormatter();
	
	public boolean accept(Object o) {
		return o instanceof Iterable;
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		return IteratorFormatter.INSTANCE.format(context, src, ((Iterable<?>) o).iterator(), out);
	}
}

final class EnumerationFormatter implements Formatter {
	public static final EnumerationFormatter INSTANCE = new EnumerationFormatter();
	
	public boolean accept(Object o) {
		return o instanceof Enumeration;
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		final Enumeration<?> e = (Enumeration<?>)o;
		final JSONHint hint = context.getHint();
		
		out.append('[');
		int count = 0;
		
		Class<?> lastClass = null;
		Formatter lastFormatter = null;
		while (e.hasMoreElements()) {
			Object item = e.nextElement();
			if (item == src) item = null;

			if (count != 0) out.append(',');
			if (context.isPrettyPrint()) {
				out.append('\n');
				context.appendIndent(out, context.getDepth() + 1);
			}
			context.enter(count, hint);
			item = context.preformatInternal(item);
			if (item == null) {
				NullFormatter.INSTANCE.format(context, src, item, out);
			} else if (hint == null) {
				if (item.getClass() == lastClass) {
					lastFormatter.format(context, src, item, out);
				} else {
					lastFormatter = context.formatInternal(item, out);
					lastClass = item.getClass();
				}
			} else {
				context.formatInternal(item, out);
			}
			context.exit();
			count++;
		}
		if (context.isPrettyPrint() && count > 0) {
			out.append('\n');
			context.appendIndent(out, context.getDepth());
		}
		out.append(']');
		return true;
	}
}

final class MapFormatter implements Formatter {
	public static final MapFormatter INSTANCE = new MapFormatter();

	public boolean accept(Object o) {
		return o instanceof Map;
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		final Map<?, ?> map = (Map<?, ?>)o;
		final JSONHint hint = context.getHint();

		Class<?> lastClass = null;
		Formatter lastFormatter = null;

		out.append('{');
		int count = 0;
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			Object key = entry.getKey();
			if (key == null) continue;

			Object value = entry.getValue();
			if (value == src) continue;

			if (count != 0) out.append(',');
			if (context.isPrettyPrint()) {
				out.append('\n');
				context.appendIndent(out, context.getDepth() + 1);
			}
			StringFormatter.serialize(context, key.toString(), out);
			out.append(':');
			if (context.isPrettyPrint()) out.append(' ');
			context.enter(key, hint);
			value = context.preformatInternal(value);
			if (value == null) {
				NullFormatter.INSTANCE.format(context, src, value, out);
			} else if (hint == null) {
				if (value.getClass() == lastClass) {
					lastFormatter.format(context, src, value, out);
				} else {
					lastFormatter = context.formatInternal(value, out);
					lastClass = value.getClass();
				}
			} else {
				context.formatInternal(value, out);
			}
			context.exit();
			count++;
		}
		if (context.isPrettyPrint() && count > 0) {
			out.append('\n');
			context.appendIndent(out, context.getDepth());
		}
		out.append('}');
		return true;
	}
}

final class ObjectFormatter implements Formatter {
	private Class<?> cls;
	private transient PropertyInfo[] props;
	
	public  ObjectFormatter(Class<?> cls) {
		this.cls = cls;
	}

	public boolean accept(Object o) {
		return o != null && !o.getClass().isPrimitive();
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		if (props == null) props = getGetProperties(context, cls);
		
		out.append('{');
		int count = 0;
		
		String key = null;
		try {
			Class<?> lastClass = null;
			Formatter lastFormatter = null;
			
			for (PropertyInfo prop : props) {
				key = prop.getName();
				
				Object value = prop.get(o);
				if (value == src || (context.isSuppressNull() && value == null)) {
					continue;
				}

				if (count != 0) out.append(',');
				if (context.isPrettyPrint()) {
					out.append('\n');
					context.appendIndent(out, context.getDepth() + 1);
				}
				StringFormatter.serialize(context, key.toString(), out);
				out.append(':');
				if (context.isPrettyPrint()) out.append(' ');
				JSONHint hint = context.getLocalCache().getHint((AnnotatedElement)prop.getReadMember());
				context.enter(key, hint);
				key = null;
				
				value = context.preformatInternal(value);
				if (value == null) {
					NullFormatter.INSTANCE.format(context, src, value, out);
				} else if (hint == null) {
					if (value.getClass() == lastClass) {
						lastFormatter.format(context, src, value, out);
					} else {
						lastFormatter = context.formatInternal(value, out);
						lastClass = value.getClass();
					}
				} else {
					context.formatInternal(value, out);
				}
				context.exit();
				count++;
			}
		} catch (Exception e) {
			if (key != null) {
				context.enter(key, null);
			}
			throw e;
		}
		if (context.isPrettyPrint() && count > 0) {
			out.append('\n');
			context.appendIndent(out, context.getDepth() + 1);
		}
		out.append('}');
		return true;
	}
	
	static PropertyInfo[] getGetProperties(Context context, Class<?> c) {
		Map<String, PropertyInfo> props = new HashMap<String, PropertyInfo>();
		
		// Field
		for (PropertyInfo prop : BeanInfo.get(c).getProperties()) {
			Field f = prop.getField();
			if (f == null || context.ignoreInternal(c, f)) continue;
			
			JSONHint hint = f.getAnnotation(JSONHint.class);
			String name = null;
			int ordinal = prop.getOrdinal();
			if (hint != null) {
				if (hint.ignore()) continue;
				ordinal = hint.ordinal();
				if (hint.name().length() != 0) name = hint.name();
			}
			
			if (name == null) {
				name = context.normalizeInternal(prop.getName());
				if (context.getPropertyStyle() != null) {
					name = context.getPropertyStyle().to(name);
				}
			}
			
			if (!name.equals(prop.getName()) || ordinal != prop.getOrdinal() || f != prop.getReadMember()) {
				props.put(name, new PropertyInfo(prop.getBeanClass(), name, 
					prop.getField(), null, null, prop.isStatic(), ordinal));
			} else {
				props.put(name, prop);
			}
		}
		
		// Method
		for (PropertyInfo prop : BeanInfo.get(c).getProperties()) {
			Method m = prop.getReadMethod();
			if (m == null || context.ignoreInternal(c, m)) continue;
			
			JSONHint hint = m.getAnnotation(JSONHint.class);
			String name = null;
			int ordinal = prop.getOrdinal();
			if (hint != null) {
				if (hint.ignore()) continue;
				ordinal = hint.ordinal();
				if (hint.name().length() != 0) name = hint.name();
			}
			
			if (name == null) {
				name = context.normalizeInternal(prop.getName());
				if (context.getPropertyStyle() != null) {
					name = context.getPropertyStyle().to(name);
				}
			}
			
			if (!name.equals(prop.getName()) || ordinal != prop.getOrdinal()) {
				props.put(name, new PropertyInfo(prop.getBeanClass(), name, 
					null, prop.getReadMethod(), null, prop.isStatic(), ordinal));
			} else {
				props.put(name, prop);
			}
		}
		
		Collection<PropertyInfo> values = props.values();
		PropertyInfo[] list = new PropertyInfo[values.size()];
		int i = 0;
		for (PropertyInfo pi : values) {
			list[i++] = pi;
		}
		Arrays.sort(list);
		return list;
	}
}

final class DynaBeanFormatter implements Formatter {
	public static final DynaBeanFormatter INSTANCE = new DynaBeanFormatter();
	private static final Class<?> target = DynaBean.class;

	public boolean accept(Object o) {
		return o != null && target.isAssignableFrom(o.getClass());
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		out.append('{');
		int count = 0;

		String key = null;
		try {
			DynaBean bean = (DynaBean)o;
			DynaProperty[] props = bean.getDynaClass().getDynaProperties();
			if (props == null) props = new DynaProperty[0];
			
			JSONHint hint = context.getHint();

			for (DynaProperty dp : props) {
				key = dp.getName();
				if (key == null) continue;

				Object value = bean.get(key);	
				if (value == src || (context.isSuppressNull() && value == null)) {
					continue;
				}

				if (count != 0) out.append(',');
				if (context.isPrettyPrint()) {
					out.append('\n');
					context.appendIndent(out, context.getDepth() + 1);
				}
				StringFormatter.serialize(context, key, out);
				out.append(':');
				if (context.isPrettyPrint()) out.append(' ');
				context.enter(key, hint);
				key = null;
				value = context.preformatInternal(value);
				context.formatInternal(value, out);
				context.exit();
				count++;
			}
		} catch (Exception e) {
			if (key != null) {
				context.enter(key, null);
			}
			throw e;
		}
		if (context.isPrettyPrint() && count > 0) {
			out.append('\n');
			context.appendIndent(out, context.getDepth());
		}
		out.append('}');
		return true;
	}
}

final class DOMNodeFormatter implements Formatter {
	public static final DOMNodeFormatter INSTANCE = new DOMNodeFormatter();
	private static final Class<?> target = Node.class;

	@Override
	public boolean accept(Object o) {
		return o != null && target.isAssignableFrom(o.getClass());
	}
	
	public boolean format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		Node node = (Node)o;
		switch (node.getNodeType()) {
		case Node.DOCUMENT_NODE:
			return formatElement(context, src, ((Document)node).getDocumentElement(), out);
		case Node.ELEMENT_NODE:
			return formatElement(context, src, node, out);
		case Node.CDATA_SECTION_NODE:
		case Node.TEXT_NODE:
			return StringFormatter.INSTANCE.format(context, src, ((CharacterData)o).getData(), out);
		default:
			return false;
		}
	}
	
	public boolean formatElement(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		Element elem = (Element)o;
		out.append('[');
		StringFormatter.serialize(context, elem.getTagName(), out);

		out.append(',');
		if (context.isPrettyPrint()) {
			out.append('\n');
			context.appendIndent(out, context.getDepth() + 1);
		}
		out.append('{');
		if (elem.hasAttributes()) {
			NamedNodeMap names = elem.getAttributes();
			for (int i = 0; i < names.getLength(); i++) {
				if (i != 0) {
					out.append(',');
				}
				if (context.isPrettyPrint() && names.getLength() > 1) {
					out.append('\n');
					for (int j = 0; j < context.getDepth() + 2; j++)
						out.append('\t');
				}
				Node node = names.item(i);
				if (node instanceof Attr) {
					StringFormatter.serialize(context, node.getNodeName(), out);
					out.append(':');
					if (context.isPrettyPrint())
						out.append(' ');
					StringFormatter.serialize(context, node.getNodeValue(), out);
				}
			}
			if (context.isPrettyPrint() && names.getLength() > 1) {
				out.append('\n');
				context.appendIndent(out, context.getDepth() + 1);
			}
		}
		out.append('}');
		if (elem.hasChildNodes()) {
			NodeList nodes = elem.getChildNodes();
			JSONHint hint = context.getHint();
			for (int i = 0; i < nodes.getLength(); i++) {
				Object value = nodes.item(i);
				if ((value instanceof Element)
						|| (value instanceof CharacterData && !(value instanceof Comment))) {
					out.append(',');
					if (context.isPrettyPrint()) {
						out.append('\n');
						context.appendIndent(out, context.getDepth() + 1);
					}
					context.enter(i + 2, hint);
					value = context.preformatInternal(value);
					context.formatInternal(value, out);
					context.exit();
					if (out instanceof Flushable)
						((Flushable) out).flush();
				}
			}
		}
		if (context.isPrettyPrint()) {
			out.append('\n');
			context.appendIndent(out, context.getDepth());
		}
		out.append(']');
		return true;
	}
}
