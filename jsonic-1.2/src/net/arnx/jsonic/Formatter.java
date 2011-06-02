package net.arnx.jsonic;

import java.io.Flushable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.JSON.Mode;
import net.arnx.jsonic.util.ClassUtil;
import net.arnx.jsonic.util.Property;

interface Formatter {
	boolean format(JSON json, Context context, Object src, Object o, InputSource in) throws Exception;
}

final class NullFormatter implements Formatter {
	public static final NullFormatter INSTANCE = new NullFormatter();

	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		in.append("null");
		return false;
	}
}

final class PlainFormatter implements Formatter {
	public static final PlainFormatter INSTANCE = new PlainFormatter();

	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		in.append(o.toString());
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


	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		serialize(context, o.toString(), in);
		return false;
	}

	static void serialize(final Context context, final String s, final InputSource in) throws Exception {
		in.append('"');
		int start = 0;
		final int length = s.length();
		for (int i = 0; i < length; i++) {
			int c = s.charAt(i);
			if (c < ESCAPE_CHARS.length) {
				int x = ESCAPE_CHARS[c];
				if (x == 0) {
					// no handle
				} else if (x > 0) {
					if (start < i) in.append(s, start, i);
					in.append('\\');
					in.append((char) x);
					start = i + 1;
				} else if (x == -1 || (x == -2 && context.getMode() != Mode.STRICT)) {
					if (start < i) in.append(s, start, i);
					in.append("\\u00");
					in.append("0123456789ABCDEF".charAt(c / 16));
					in.append("0123456789ABCDEF".charAt(c % 16));
					start = i + 1;
				}
			} else if (c == '\u2028') {
				in.append("\\u2028");
				start = i + 1;
			} else if (c == '\u2029') {
				in.append("\\u2029");
				start = i + 1;
			}
		}
		if (start < length) in.append(s, start, length);
		in.append('"');
	}
}

final class NumberFormatter implements Formatter {
	public static final NumberFormatter INSTANCE = new NumberFormatter();

	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		NumberFormat f = context.format(NumberFormat.class);
		if (f != null) {
			StringFormatter.serialize(context, f.format(o), in);
		} else {
			in.append(o.toString());
		}
		return false;
	}
}

final class FloatFormatter implements Formatter {
	public static final FloatFormatter INSTANCE = new FloatFormatter();

	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		NumberFormat f = context.format(NumberFormat.class);
		if (f != null) {
			StringFormatter.serialize(context, f.format(o), in);
		} else {
			double d = ((Number) o).doubleValue();
			if (Double.isNaN(d) || Double.isInfinite(d)) {
				if (context.getMode() != Mode.SCRIPT) {
					in.append('"');
					in.append(o.toString());
					in.append('"');
				} else if (Double.isNaN(d)) {
					in.append("Number.NaN");
				} else {
					in.append("Number.");
					in.append((d > 0) ? "POSITIVE" : "NEGATIVE");
					in.append("_INFINITY");
				}
			} else {
				in.append(o.toString());
			}
		}
		return false;
	}
}

final class DateFormatter implements Formatter {
	public static final DateFormatter INSTANCE = new DateFormatter();

	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		Date date = (Date) o;
		DateFormat f = context.format(DateFormat.class);
		if (f != null) {
			StringFormatter.serialize(context, f.format(o), in);
		} else if (context.getMode() == Mode.SCRIPT) {
			in.append("new Date(");
			in.append(Long.toString(date.getTime()));
			in.append(")");
		} else {
			in.append(Long.toString(date.getTime()));
		}
		return false;
	}
}

final class BooleanArrayFormatter implements Formatter {
	public static final BooleanArrayFormatter INSTANCE = new BooleanArrayFormatter();

	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		in.append('[');
		boolean[] array = (boolean[]) o;
		for (int i = 0; i < array.length; i++) {
			in.append(String.valueOf(array[i]));
			if (i != array.length - 1) {
				in.append(',');
				if (context.isPrettyPrint())
					in.append(' ');
			}
		}
		in.append(']');
		return true;
	}
}

final class ByteArrayFormatter implements Formatter {
	public static final ByteArrayFormatter INSTANCE = new ByteArrayFormatter();

	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		StringFormatter.serialize(context, Base64.encode((byte[]) o), in);
		return false;
	}
}

final class SerializableFormatter implements Formatter {
	public static final SerializableFormatter INSTANCE = new SerializableFormatter();
	
	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		return StringFormatter.INSTANCE.format(json, context, src, Base64.encode(ClassUtil.serialize(o)), in);
	}
}

final class ShortArrayFormatter implements Formatter {
	public static final ShortArrayFormatter INSTANCE = new ShortArrayFormatter();
	
	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		NumberFormat f = context.format(NumberFormat.class);
		short[] array = (short[]) o;
		in.append('[');
		for (int i = 0; i < array.length; i++) {
			if (f != null) {
				StringFormatter.serialize(context, f.format(array[i]), in);
			} else {
				in.append(String.valueOf(array[i]));
			}
			if (i != array.length - 1) {
				in.append(',');
				if (context.isPrettyPrint())
					in.append(' ');
			}
		}
		in.append(']');
		return true;
	}
}

final class IntArrayFormatter implements Formatter {
	public static final IntArrayFormatter INSTANCE = new IntArrayFormatter();

	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		NumberFormat f = context.format(NumberFormat.class);
		int[] array = (int[]) o;
		in.append('[');
		for (int i = 0; i < array.length; i++) {
			if (f != null) {
				StringFormatter.serialize(context, f.format(array[i]), in);
			} else {
				in.append(String.valueOf(array[i]));
			}
			if (i != array.length - 1) {
				in.append(',');
				if (context.isPrettyPrint())
					in.append(' ');
			}
		}
		in.append(']');
		return true;
	}
}

final class LongArrayFormatter implements Formatter {
	public static final LongArrayFormatter INSTANCE = new LongArrayFormatter();
	
	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		NumberFormat f = context.format(NumberFormat.class);
		long[] array = (long[]) o;
		in.append('[');
		for (int i = 0; i < array.length; i++) {
			if (f != null) {
				StringFormatter.serialize(context, f.format(array[i]), in);
			} else {
				in.append(String.valueOf(array[i]));
			}
			if (i != array.length - 1) {
				in.append(',');
				if (context.isPrettyPrint()) in.append(' ');
			}
		}
		in.append(']');
		return true;
	}
}

final class FloatArrayFormatter implements Formatter {
	public static final FloatArrayFormatter INSTANCE = new FloatArrayFormatter();
	
	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		NumberFormat f = context.format(NumberFormat.class);
		float[] array = (float[]) o;
		in.append('[');
		for (int i = 0; i < array.length; i++) {
			if (Float.isNaN(array[i]) || Float.isInfinite(array[i])) {
				if (context.getMode() != Mode.SCRIPT) {
					in.append('"');
					in.append(Float.toString(array[i]));
					in.append('"');
				} else if (Double.isNaN(array[i])) {
					in.append("Number.NaN");
				} else {
					in.append("Number.");
					in.append((array[i] > 0) ? "POSITIVE" : "NEGATIVE");
					in.append("_INFINITY");
				}
			} else if (f != null) {
				StringFormatter.serialize(context, f.format(array[i]), in);
			} else {
				in.append(String.valueOf(array[i]));
			}
			if (i != array.length - 1) {
				in.append(',');
				if (context.isPrettyPrint())
					in.append(' ');
			}
		}
		in.append(']');
		return true;
	}
}

final class DoubleArrayFormatter implements Formatter {
	public static final DoubleArrayFormatter INSTANCE = new DoubleArrayFormatter();
	
	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		NumberFormat f = context.format(NumberFormat.class);
		double[] array = (double[]) o;
		in.append('[');
		for (int i = 0; i < array.length; i++) {
			if (Double.isNaN(array[i]) || Double.isInfinite(array[i])) {
				if (context.getMode() != Mode.SCRIPT) {
					in.append('"');
					in.append(Double.toString(array[i]));
					in.append('"');
				} else if (Double.isNaN(array[i])) {
					in.append("Number.NaN");
				} else {
					in.append("Number.");
					in.append((array[i] > 0) ? "POSITIVE" : "NEGATIVE");
					in.append("_INFINITY");
				}
			} else if (f != null) {
				StringFormatter.serialize(context, f.format(array[i]), in);
			} else {
				in.append(String.valueOf(array[i]));
			}
			if (i != array.length - 1) {
				in.append(',');
				if (context.isPrettyPrint()) in.append(' ');
			}
		}
		in.append(']');
		return true;
	}
}

final class ObjectArrayFormatter implements Formatter {
	public static final ObjectArrayFormatter INSTANCE = new ObjectArrayFormatter();
	
	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		Object[] array = (Object[]) o;
		in.append('[');
		int i = 0;
		for (; i < array.length; i++) {
			Object item = array[i];
			if (item == src)
				item = null;

			if (i != 0)
				in.append(',');
			if (context.isPrettyPrint()) {
				in.append('\n');
				for (int j = 0; j < context.getLevel() + 1; j++)
					in.append('\t');
			}
			context.enter(i);
			json.format(context, item, in);
			context.exit();
		}
		if (context.isPrettyPrint() && i > 0) {
			in.append('\n');
			for (int j = 0; j < context.getLevel(); j++)
				in.append('\t');
		}
		in.append(']');
		return true;
	}
}

final class ByteFormatter implements Formatter {
	public static final ByteFormatter INSTANCE = new ByteFormatter();
	
	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		in.append(Integer.toString(((Byte)o).byteValue() & 0xFF));
		return false;
	}
}

final class ClassFormatter implements Formatter {
	public static final ClassFormatter INSTANCE = new ClassFormatter();
	
	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		return StringFormatter.INSTANCE.format(json, context, src, ((Class<?>)o).getName(), in);
	}
}

final class LocaleFormatter implements Formatter {
	public static final LocaleFormatter INSTANCE = new LocaleFormatter();
	
	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		return StringFormatter.INSTANCE.format(json, context, src, ((Locale)o).toString().replace('_', '-'), in);
	}
}

final class CharArrayFormatter implements Formatter {
	public static final CharArrayFormatter INSTANCE = new CharArrayFormatter();
	
	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		return StringFormatter.INSTANCE.format(json, context, src, new String((char[]) o), in);
	}
}

final class ListFormatter implements Formatter {
	public static final ListFormatter INSTANCE = new ListFormatter();
	
	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		final List<?> list = (List<?>)o;
		final JSONHint hint = context.getHint();
		final int length = list.size();
		
		Class<?> lastClass = null;
		Formatter lastFormatter = null;
		
		in.append('[');
		int count = 0;
		while (count < length) {
			Object item = list.get(count);
			if (item == src) item = null;
			
			if (count != 0) in.append(',');
			if (context.isPrettyPrint()) {
				in.append('\n');
				for (int j = 0; j < context.getLevel() + 1; j++) in.append('\t');
			}
			context.enter(count, hint);
			if (item == null) {
				NullFormatter.INSTANCE.format(json, context, src, item, in);
			} else if (hint == null) {
				if (item.getClass().equals(lastClass)) {
					lastFormatter.format(json, context, src, item, in);
				} else {
					lastFormatter = json.format(context, item, in);
					lastClass = (lastFormatter != null) ? item.getClass() : null;
				}
			} else {
				json.format(context, item, in);
			}
			context.exit();
			count++;
		}
		if (context.isPrettyPrint() && count > 0) {
			in.append('\n');
			for (int j = 0; j < context.getLevel(); j++) in.append('\t');
		}
		in.append(']');
		return true;
	}
}

final class IteratorFormatter implements Formatter {
	public static final IteratorFormatter INSTANCE = new IteratorFormatter();
	
	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		final Iterator<?> t = (Iterator<?>)o;
		final JSONHint hint = context.getHint();
		
		Class<?> lastClass = null;
		Formatter lastFormatter = null;
		
		in.append('[');
		int count = 0;
		while(t.hasNext()) {
			Object item = t.next();
			if (item == src)
				item = null;

			if (count != 0) in.append(',');
			if (context.isPrettyPrint()) {
				in.append('\n');
				for (int j = 0; j < context.getLevel() + 1; j++) in.append('\t');
			}
			context.enter(count, hint);
			if (item == null) {
				NullFormatter.INSTANCE.format(json, context, src, item, in);
			} else if (hint == null) {
				if (item.getClass().equals(lastClass)) {
					lastFormatter.format(json, context, src, item, in);
				} else {
					lastFormatter = json.format(context, item, in);
					lastClass = (lastFormatter != null) ? item.getClass() : null;
				}
			} else {
				json.format(context, item, in);
			}
			context.exit();
			count++;
		}
		if (context.isPrettyPrint() && count > 0) {
			in.append('\n');
			for (int j = 0; j < context.getLevel(); j++) in.append('\t');
		}
		in.append(']');
		return true;
	}
}

final class IterableFormatter implements Formatter {
	public static final IterableFormatter INSTANCE = new IterableFormatter();
	
	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		return IteratorFormatter.INSTANCE.format(json, context, src, ((Iterable<?>) o).iterator(), in);
	}
}

final class EnumerationFormatter implements Formatter {
	public static final EnumerationFormatter INSTANCE = new EnumerationFormatter();
	
	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		final Enumeration<?> e = (Enumeration<?>)o;
		final JSONHint hint = context.getHint();
		
		in.append('[');
		int count = 0;
		
		Class<?> lastClass = null;
		Formatter lastFormatter = null;
		while (e.hasMoreElements()) {
			Object item = e.nextElement();
			if (item == src) item = null;

			if (count != 0) in.append(',');
			if (context.isPrettyPrint()) {
				in.append('\n');
				for (int j = 0; j < context.getLevel() + 1; j++)
					in.append('\t');
			}
			context.enter(count, hint);
			if (item == null) {
				NullFormatter.INSTANCE.format(json, context, src, item, in);
			} else if (hint == null) {
				if (item.getClass().equals(lastClass)) {
					lastFormatter.format(json, context, src, item, in);
				} else {
					lastFormatter = json.format(context, item, in);
					lastClass = (lastFormatter != null) ? item.getClass() : null;
				}
			} else {
				json.format(context, item, in);
			}
			context.exit();
			count++;
		}
		if (context.isPrettyPrint() && count > 0) {
			in.append('\n');
			for (int j = 0; j < context.getLevel(); j++)
				in.append('\t');
		}
		in.append(']');
		return true;
	}
}

final class MapFormatter implements Formatter {
	public static final MapFormatter INSTANCE = new MapFormatter();
	
	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		final Map<?, ?> map = (Map<?, ?>)o;
		final JSONHint hint = context.getHint();

		Class<?> lastClass = null;
		Formatter lastFormatter = null;

		in.append('{');
		int count = 0;
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			Object key = entry.getKey();
			if (key == null) continue;

			Object value = entry.getValue();
			if (value == src || (context.isSuppressNull() && value == null)) continue;

			if (count != 0) in.append(',');
			if (context.isPrettyPrint()) {
				in.append('\n');
				for (int j = 0; j < context.getLevel() + 1; j++) in.append('\t');
			}
			StringFormatter.serialize(context, key.toString(), in);
			in.append(':');
			if (context.isPrettyPrint()) in.append(' ');
			context.enter(key, hint);
			if (value == null) {
				NullFormatter.INSTANCE.format(json, context, src, value, in);
			} else if (hint == null) {
				if (value.getClass().equals(lastClass)) {
					lastFormatter.format(json, context, src, value, in);
				} else {
					lastFormatter = json.format(context, value, in);
					lastClass = (lastFormatter != null) ? value.getClass() : null;
				}
			} else {
				json.format(context, value, in);
			}
			context.exit();
			count++;
		}
		if (context.isPrettyPrint() && count > 0) {
			in.append('\n');
			for (int j = 0; j < context.getLevel(); j++)
				in.append('\t');
		}
		in.append('}');
		return true;
	}
}

final class ObjectFormatter implements Formatter {
	public static final ObjectFormatter INSTANCE = new ObjectFormatter();
	
	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		List<Property> props = context.getGetProperties(o.getClass());

		in.append('{');
		int count = 0;
		final int length = props.size();
		for (int p = 0; p < length; p++) {
			Property prop = props.get(p);
			Object value = null;
			Exception cause = null;

			try {
				value = prop.get(o);
				if (value == src || (context.isSuppressNull() && value == null))
					continue;

				if (count != 0) in.append(',');
				if (context.isPrettyPrint()) {
					in.append('\n');
					for (int j = 0; j < context.getLevel() + 1; j++)
						in.append('\t');
				}
			} catch (Exception e) {
				cause = e;
			}

			StringFormatter.serialize(context, prop.getName(), in);
			in.append(':');
			if (context.isPrettyPrint()) in.append(' ');
			context.enter(prop.getName(), prop.getReadAnnotation(JSONHint.class));
			if (cause != null) throw cause;
			
			json.format(context, value, in);
			context.exit();
			count++;
		}
		if (context.isPrettyPrint() && count > 0) {
			in.append('\n');
			for (int j = 0; j < context.getLevel(); j++)
				in.append('\t');
		}
		in.append('}');
		return true;
	}
}

final class DynaBeanFormatter implements Formatter {
	public static final DynaBeanFormatter INSTANCE = new DynaBeanFormatter();
	
	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		in.append('{');
		int count = 0;
		try {
			Class<?> dynaBeanClass = ClassUtil.findClass("org.apache.commons.beanutils.DynaBean");

			Object dynaClass = dynaBeanClass.getMethod("getDynaClass")
					.invoke(o);
			Object[] dynaProperties = (Object[]) dynaClass.getClass()
					.getMethod("getDynaProperties").invoke(dynaClass);

			if (dynaProperties != null && dynaProperties.length > 0) {
				Method getName = dynaProperties[0].getClass().getMethod(
						"getName");
				Method get = dynaBeanClass.getMethod("get", String.class);

				for (Object dp : dynaProperties) {
					Object name = null;
					try {
						name = getName.invoke(dp);
					} catch (InvocationTargetException e) {
						throw e;
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

					if (value == src || (cause == null && context.isSuppressNull() && value == null)) {
						continue;
					}

					if (count != 0) in.append(',');
					if (context.isPrettyPrint()) {
						in.append('\n');
						for (int j = 0; j < context.getLevel() + 1; j++)
							in.append('\t');
					}
					StringFormatter.serialize(context, name.toString(), in);
					in.append(':');
					if (context.isPrettyPrint()) in.append(' ');
					context.enter(name);
					if (cause != null) throw cause;
					json.format(context, value, in);
					context.exit();
					count++;
				}
			}
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof Error) {
				throw (Error)e.getCause();
			} else if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException)e.getCause();
			} else {
				throw (Exception)e.getCause();
			}
		} catch (Exception e) {
			// no handle
		}
		if (context.isPrettyPrint() && count > 0) {
			in.append('\n');
			for (int j = 0; j < context.getLevel(); j++)
				in.append('\t');
		}
		in.append('}');
		return true;
	}
}

final class DOMElementFormatter implements Formatter {
	public static final DOMElementFormatter INSTANCE = new DOMElementFormatter();
	
	public boolean format(final JSON json, final Context context, final Object src, final Object o, final InputSource in) throws Exception {
		Element elem = (Element)o;
		in.append('[');
		StringFormatter.serialize(context, elem.getTagName(), in);

		in.append(',');
		if (context.isPrettyPrint()) {
			in.append('\n');
			for (int j = 0; j < context.getLevel() + 1; j++)
				in.append('\t');
		}
		in.append('{');
		if (elem.hasAttributes()) {
			NamedNodeMap names = elem.getAttributes();
			for (int i = 0; i < names.getLength(); i++) {
				if (i != 0) {
					in.append(',');
				}
				if (context.isPrettyPrint() && names.getLength() > 1) {
					in.append('\n');
					for (int j = 0; j < context.getLevel() + 2; j++)
						in.append('\t');
				}
				Node node = names.item(i);
				if (node instanceof Attr) {
					StringFormatter.serialize(context, node.getNodeName(), in);
					in.append(':');
					if (context.isPrettyPrint())
						in.append(' ');
					StringFormatter.serialize(context, node.getNodeValue(), in);
				}
			}
			if (context.isPrettyPrint() && names.getLength() > 1) {
				in.append('\n');
				for (int j = 0; j < context.getLevel() + 1; j++)
					in.append('\t');
			}
		}
		in.append('}');
		if (elem.hasChildNodes()) {
			NodeList nodes = elem.getChildNodes();
			for (int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(i);
				if ((node instanceof Element)
						|| (node instanceof CharacterData && !(node instanceof Comment))) {
					in.append(',');
					if (context.isPrettyPrint()) {
						in.append('\n');
						for (int j = 0; j < context.getLevel() + 1; j++)
							in.append('\t');
					}
					context.enter(i + 2);
					json.format(context, node, in);
					context.exit();
					if (in instanceof Flushable)
						((Flushable) in).flush();
				}
			}
		}
		if (context.isPrettyPrint()) {
			in.append('\n');
			for (int j = 0; j < context.getLevel(); j++)
				in.append('\t');
		}
		in.append(']');
		return true;
	}
}
