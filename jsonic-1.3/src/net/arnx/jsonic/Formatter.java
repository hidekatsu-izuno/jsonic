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
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.Struct;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
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
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.RandomAccess;
import java.util.TimeZone;

import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaProperty;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.JSON.Mode;
import net.arnx.jsonic.io.OutputSource;
import net.arnx.jsonic.util.Base64;
import net.arnx.jsonic.util.BeanInfo;
import net.arnx.jsonic.util.ClassUtil;
import net.arnx.jsonic.util.PropertyInfo;

interface Formatter {
	boolean accept(Object o);
	boolean isStruct();
	void format(Context context, Object src, Object o, OutputSource out) throws Exception;
}

final class NullFormatter implements Formatter {
	public static final NullFormatter INSTANCE = new NullFormatter();

	@Override
	public boolean accept(Object o) {
		return o == null;
	}

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		out.append("null");
	}
}

final class PlainFormatter implements Formatter {
	public static final PlainFormatter INSTANCE = new PlainFormatter();

	@Override
	public boolean accept(Object o) {
		return o != null;
	}

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		out.append(o.toString());
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

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		serialize(context, o.toString(), out);
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

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		StringFormatter.serialize(context, o.toString(), out);
	}
}

final class TimeZoneFormatter implements Formatter {
	public static final TimeZoneFormatter INSTANCE = new TimeZoneFormatter();

	@Override
	public boolean accept(Object o) {
		return o instanceof TimeZone;
	}

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		StringFormatter.serialize(context, ((TimeZone)o).getID(), out);
	}
}

final class CharsetFormatter implements Formatter {
	public static final CharsetFormatter INSTANCE = new CharsetFormatter();

	@Override
	public boolean accept(Object o) {
		return o instanceof Charset;
	}

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		StringFormatter.serialize(context, ((Charset)o).name(), out);
	}
}

final class InetAddressFormatter implements Formatter {
	public InetAddressFormatter() {
	}

	@Override
	public boolean accept(Object o) {
		return o != null && InetAddress.class.isAssignableFrom(o.getClass());
	}

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		StringFormatter.serialize(context, ((InetAddress)o).getHostAddress(), out);
	}
}

final class NumberFormatter implements Formatter {
	public static final NumberFormatter INSTANCE = new NumberFormatter();

	public boolean accept(Object o) {
		return o instanceof Number;
	}

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		NumberFormat f = context.getNumberFormat();
		if (f != null) {
			StringFormatter.serialize(context, f.format(o), out);
		} else {
			out.append(o.toString());
		}
	}

	public void format(final Context context, final int num, final OutputSource out) throws Exception {
		NumberFormat f = context.getNumberFormat();
		if (f != null) {
			StringFormatter.serialize(context, f.format(num), out);
		} else {
			out.append(Integer.toString(num));
		}
	}

	public void format(final Context context, final long num, final OutputSource out) throws Exception {
		NumberFormat f = context.getNumberFormat();
		if (f != null) {
			StringFormatter.serialize(context, f.format(num), out);
		} else {
			out.append(Long.toString(num));
		}
	}
}

final class EnumFormatter implements Formatter {
	public static final EnumFormatter INSTANCE = new EnumFormatter();

	public boolean accept(Object o) {
		return o != null && (o.getClass().isEnum() || o instanceof Enum);
	}

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		if (context.getEnumStyle() != null) {
			StringFormatter.serialize(context, context.getEnumStyle().to(((Enum<?>)o).name()), out);
		} else {
			out.append(Integer.toString(((Enum<?>)o).ordinal()));
		}
	}
}

final class FloatFormatter implements Formatter {
	public static final FloatFormatter INSTANCE = new FloatFormatter();

	public boolean accept(Object o) {
		return o instanceof Float || o instanceof Double;
	}

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		format(context, ((Number)o).doubleValue(), out);
	}

	public void format(final Context context, final double d, final OutputSource out) throws Exception {
		NumberFormat f = context.getNumberFormat();
		if (f != null) {
			StringFormatter.serialize(context, f.format(d), out);
		} else {
			if (Double.isNaN(d) || Double.isInfinite(d)) {
				if (context.getMode() != Mode.SCRIPT) {
					out.append('"');
					out.append(Double.toString(d));
					out.append('"');
				} else if (Double.isNaN(d)) {
					out.append("Number.NaN");
				} else {
					out.append("Number.");
					out.append((d > 0) ? "POSITIVE" : "NEGATIVE");
					out.append("_INFINITY");
				}
			} else {
				out.append(Double.toString(d));
			}
		}
	}
}

final class DateFormatter implements Formatter {
	public static final DateFormatter INSTANCE = new DateFormatter();

	public boolean accept(Object o) {
		return o instanceof Date;
	}

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
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
	}
}

final class CalendarFormatter implements Formatter {
	public static final CalendarFormatter INSTANCE = new CalendarFormatter();

	public boolean accept(Object o) {
		return o instanceof Calendar;
	}

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		DateFormatter.INSTANCE.format(context, src, ((Calendar)o).getTime(), out);
	}
}

final class BooleanArrayFormatter implements Formatter {
	public static final BooleanArrayFormatter INSTANCE = new BooleanArrayFormatter();

	@Override
	public boolean accept(Object o) {
		return o instanceof boolean[];
	}

	@Override
	public boolean isStruct() {
		return true;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
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
	}
}

final class ByteArrayFormatter implements Formatter {
	public static final ByteArrayFormatter INSTANCE = new ByteArrayFormatter();

	@Override
	public boolean accept(Object o) {
		return o instanceof byte[];
	}

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		StringFormatter.serialize(context, Base64.encode((byte[]) o), out);
	}
}

final class SerializableFormatter implements Formatter {
	public static final SerializableFormatter INSTANCE = new SerializableFormatter();

	@Override
	public boolean accept(Object o) {
		return o instanceof Serializable;
	}

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		StringFormatter.serialize(context, Base64.encode(ClassUtil.serialize(o)), out);
	}
}

final class PathFormatter implements Formatter {
	public PathFormatter() {
	}

	@Override
	public boolean accept(Object o) {
		return o instanceof Path;
	}

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		StringFormatter.INSTANCE.format(context, src, o, out);
	}
}


final class RowIdFormatter implements Formatter {
	public RowIdFormatter() {
	}

	@Override
	public boolean accept(Object o) {
		return o instanceof RowId;
	}

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		SerializableFormatter.INSTANCE.format(context, src, o, out);
	}
}

final class ShortArrayFormatter implements Formatter {
	public static final ShortArrayFormatter INSTANCE = new ShortArrayFormatter();

	public boolean accept(Object o) {
		return o instanceof short[];
	}

	@Override
	public boolean isStruct() {
		return true;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
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
	}
}

final class IntArrayFormatter implements Formatter {
	public static final IntArrayFormatter INSTANCE = new IntArrayFormatter();

	@Override
	public boolean accept(Object o) {
		return o instanceof int[];
	}

	@Override
	public boolean isStruct() {
		return true;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
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
	}
}

final class LongArrayFormatter implements Formatter {
	public static final LongArrayFormatter INSTANCE = new LongArrayFormatter();

	@Override
	public boolean accept(Object o) {
		return o instanceof long[];
	}

	@Override
	public boolean isStruct() {
		return true;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
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
	}
}

final class FloatArrayFormatter implements Formatter {
	public static final FloatArrayFormatter INSTANCE = new FloatArrayFormatter();

	public boolean accept(Object o) {
		return o instanceof float[];
	}

	@Override
	public boolean isStruct() {
		return true;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
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
	}
}

final class DoubleArrayFormatter implements Formatter {
	public static final DoubleArrayFormatter INSTANCE = new DoubleArrayFormatter();

	public boolean accept(Object o) {
		return o instanceof double[];
	}

	@Override
	public boolean isStruct() {
		return true;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
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
	}
}

final class ObjectArrayFormatter implements Formatter {
	public static final ObjectArrayFormatter INSTANCE = new ObjectArrayFormatter();

	public boolean accept(Object o) {
		return o instanceof Object[];
	}

	@Override
	public boolean isStruct() {
		return true;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		final Object[] array = (Object[]) o;
		final JSONHint hint = context.getHint();

		Class<?> lastClass = null;
		Formatter lastFormatter = null;
		Class<?> cType = array.getClass().getComponentType();

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
			item = context.preformatInternal(cType, item);
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
	}
}

final class SQLArrayFormatter implements Formatter {
	public SQLArrayFormatter() {
	}

	@Override
	public boolean accept(Object o) {
		return o != null && java.sql.Array.class.isAssignableFrom(o.getClass());
	}

	@Override
	public boolean isStruct() {
		return true;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		Object array;
		try {
			array = ((java.sql.Array)o).getArray();
		} catch (SQLException e) {
			array = null;
		}
		if (array == null) array = new Object[0];
		ObjectArrayFormatter.INSTANCE.format(context, src, array, out);
	}
}

final class StructFormmatter implements Formatter {
	public StructFormmatter() {
	}

	@Override
	public boolean accept(Object o) {
		return o != null && Struct.class.isAssignableFrom(o.getClass());
	}

	@Override
	public boolean isStruct() {
		return true;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		Object value;
		try {
			value = ((Struct)o).getAttributes();
		} catch (SQLException e) {
			value = null;
		}
		if (value == null) value = new Object[0];
		ObjectArrayFormatter.INSTANCE.format(context, src, o, out);
	}
}

final class ByteFormatter implements Formatter {
	public static final ByteFormatter INSTANCE = new ByteFormatter();

	public boolean accept(Object o) {
		return o instanceof Byte;
	}

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		out.append(Integer.toString(((Byte)o).byteValue() & 0xFF));
	}
}

final class ClassFormatter implements Formatter {
	public static final ClassFormatter INSTANCE = new ClassFormatter();

	@Override
	public boolean accept(Object o) {
		return o instanceof Class;
	}

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		StringFormatter.serialize(context, ((Class<?>)o).getName(), out);
	}
}

final class LocaleFormatter implements Formatter {
	public static final LocaleFormatter INSTANCE = new LocaleFormatter();

	@Override
	public boolean accept(Object o) {
		return o instanceof Locale;
	}

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		StringFormatter.serialize(context, ((Locale)o).toString().replace('_', '-'), out);
	}
}

final class CharArrayFormatter implements Formatter {
	public static final CharArrayFormatter INSTANCE = new CharArrayFormatter();

	@Override
	public boolean accept(Object o) {
		return o instanceof char[];
	}

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		StringFormatter.serialize(context, String.valueOf((char[])o), out);
	}
}

final class ListFormatter implements Formatter {
	public static final ListFormatter INSTANCE = new ListFormatter();

	public boolean accept(Object o) {
		return o instanceof List && o instanceof RandomAccess;
	}

	@Override
	public boolean isStruct() {
		return true;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
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
			item = context.preformatInternal((item != null) ? item.getClass() : Object.class, item);
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
	}
}

final class IteratorFormatter implements Formatter {
	public static final IteratorFormatter INSTANCE = new IteratorFormatter();

	public boolean accept(Object o) {
		return o instanceof Iterator;
	}

	@Override
	public boolean isStruct() {
		return true;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
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
			item = context.preformatInternal((item != null) ? item.getClass() : Object.class, item);
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
	}
}

final class IterableFormatter implements Formatter {
	public static final IterableFormatter INSTANCE = new IterableFormatter();

	public boolean accept(Object o) {
		return o instanceof Iterable;
	}

	@Override
	public boolean isStruct() {
		return true;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		IteratorFormatter.INSTANCE.format(context, src, ((Iterable<?>) o).iterator(), out);
	}
}

final class EnumerationFormatter implements Formatter {
	public static final EnumerationFormatter INSTANCE = new EnumerationFormatter();

	public boolean accept(Object o) {
		return o instanceof Enumeration;
	}

	@Override
	public boolean isStruct() {
		return true;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
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
			item = context.preformatInternal((item != null) ? item.getClass() : Object.class, item);
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
	}
}

final class MapFormatter implements Formatter {
	public static final MapFormatter INSTANCE = new MapFormatter();

	public boolean accept(Object o) {
		return o instanceof Map;
	}

	@Override
	public boolean isStruct() {
		return true;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
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
			value = context.preformatInternal((value != null) ? value.getClass() : Object.class, value);
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

	@Override
	public boolean isStruct() {
		return true;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
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
				JSONHint hint = prop.getReadAnnotation(JSONHint.class);
				context.enter(key, hint);
				key = null;

				value = context.preformatInternal(prop.getReadGenericType(), value);
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
			context.appendIndent(out, context.getDepth());
		}
		out.append('}');
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
				prop = new PropertyInfo(prop.getBeanClass(), name,
					prop.getField(), null, null, prop.isStatic(), ordinal);
			}

			if (prop.getReadMethod() != null) {
				prop.getReadMethod().setAccessible(true);
			} else if (prop.getField() != null) {
				prop.getField().setAccessible(true);
			}

			props.put(name, prop);
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
	public DynaBeanFormatter() {
	}

	@Override
	public boolean accept(Object o) {
		return o != null && DynaBean.class.isAssignableFrom(o.getClass());
	}

	@Override
	public boolean isStruct() {
		return true;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
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
				value = context.preformatInternal(dp.getType(), value);
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
	}
}

final class ElementNodeFormatter implements Formatter {
	private static final Class<?>[] targets = new Class<?>[] {
		Document.class,
		Element.class
	};

	public ElementNodeFormatter() {
	}

	@Override
	public boolean accept(Object o) {
		for (Class<?> target : targets) {
			if (o != null && target.isAssignableFrom(o.getClass())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isStruct() {
		return true;
	}

	@Override
	public void format(final Context context, final Object src, final Object o, final OutputSource out) throws Exception {
		if (o instanceof Document) {
			formatElement(context, src, ((Document)o).getDocumentElement(), out);
		} else {
			formatElement(context, src, (Element)o, out);
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
					value = context.preformatInternal(value.getClass(), value);
					context.formatInternal(value, out);
					context.exit();
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

final class TextNodeFormatter implements Formatter {
	private static final Class<?>[] targets = {
		CDATASection.class,
		Text.class
	};

	public TextNodeFormatter() {
	}

	@Override
	public boolean accept(Object o) {
		for (Class<?> target : targets) {
			if (o != null && target.isAssignableFrom(o.getClass())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(Context context, Object src, Object o, OutputSource out) throws Exception {
		StringFormatter.serialize(context, ((CharacterData)o).getData(), out);
	}
}

final class TemporalFormatter implements Formatter {
	private static final Class<?>[] targets = {
		TemporalAccessor.class,
		TemporalAmount.class,
		ZoneId.class
	};

	public TemporalFormatter() {
	}

	@Override
	public boolean accept(Object o) {
		for (Class<?> target : targets) {
			if (o != null && target.isAssignableFrom(o.getClass())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(Context context, Object src, Object o, OutputSource out) throws Exception {
		StringFormatter.serialize(context, o.toString(), out);
	}
}


final class OptionalIntFormatter implements Formatter {
	public static final OptionalIntFormatter INSTNACE = new OptionalIntFormatter();

	public OptionalIntFormatter() {
	}

	@Override
	public boolean accept(Object o) {
		return o != null && OptionalInt.class.equals(o.getClass());
	}

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(Context context, Object src, Object o, OutputSource out) throws Exception {
		OptionalInt optional = (OptionalInt)o;
		if (optional.isPresent()) {
			NumberFormatter.INSTANCE.format(context, optional.getAsInt(), out);
		} else {
			NullFormatter.INSTANCE.format(context, src, o, out);
		}
	}
}

final class OptionalLongFormatter implements Formatter {
	public static final OptionalLongFormatter INSTNACE = new OptionalLongFormatter();

	public OptionalLongFormatter() {
	}

	@Override
	public boolean accept(Object o) {
		return o != null && OptionalLong.class.equals(o.getClass());
	}

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(Context context, Object src, Object o, OutputSource out) throws Exception {
		OptionalLong optional = (OptionalLong)o;
		if (optional.isPresent()) {
			NumberFormatter.INSTANCE.format(context, optional.getAsLong(), out);
		} else {
			NullFormatter.INSTANCE.format(context, src, o, out);
		}
	}
}

final class OptionalDoubleFormatter implements Formatter {
	public static final OptionalDoubleFormatter INSTNACE = new OptionalDoubleFormatter();

	public OptionalDoubleFormatter() {
	}

	@Override
	public boolean accept(Object o) {
		return o != null && OptionalDouble.class.equals(o.getClass());
	}

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(Context context, Object src, Object o, OutputSource out) throws Exception {
		OptionalDouble optional = (OptionalDouble)o;
		if (optional.isPresent()) {
			FloatFormatter.INSTANCE.format(context, optional.getAsDouble(), out);
		} else {
			NullFormatter.INSTANCE.format(context, src, o, out);
		}
	}
}

final class OptionalFormatter implements Formatter {
	public static final OptionalFormatter INSTNACE = new OptionalFormatter();

	public OptionalFormatter() {
	}

	@Override
	public boolean accept(Object o) {
		return o != null && Optional.class.isAssignableFrom(o.getClass());
	}

	@Override
	public boolean isStruct() {
		return false;
	}

	@Override
	public void format(Context context, Object src, Object o, OutputSource out) throws Exception {
		Optional<?> optional = (Optional<?>)o;
		if (optional.isPresent()) {
			context.formatInternal(optional.get(), out);
		} else {
			NullFormatter.INSTANCE.format(context, src, o, out);
		}
	}
}
