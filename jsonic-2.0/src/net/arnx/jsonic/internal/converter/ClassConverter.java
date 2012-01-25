package net.arnx.jsonic.internal.converter;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import net.arnx.jsonic.JSON.Context;

public class ClassConverter implements Converter {
	public static final ClassConverter INSTANCE = new ClassConverter();

	public Object convert(Context context, Object value, Class<?> c, Type t)
			throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?, ?>) value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>) value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		if (value instanceof String) {
			String s = value.toString().trim();
			if (s.equals("boolean")) {
				return boolean.class;
			} else if (s.equals("byte")) {
				return byte.class;
			} else if (s.equals("short")) {
				return short.class;
			} else if (s.equals("int")) {
				return int.class;
			} else if (s.equals("long")) {
				return long.class;
			} else if (s.equals("float")) {
				return float.class;
			} else if (s.equals("double")) {
				return double.class;
			} else {
				try {
					ClassLoader cl = Thread.currentThread()
							.getContextClassLoader();
					return cl.loadClass(value.toString());
				} catch (ClassNotFoundException e) {
					return null;
				}
			}
		} else if (value != null) {
			throw new UnsupportedOperationException("Cannot convert "
					+ value.getClass() + " to " + t);
		}
		return null;
	}
}
