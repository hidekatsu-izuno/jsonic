package net.arnx.jsonic.internal.converter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;

import net.arnx.jsonic.JSON.Context;

public class StringSerializableConverter implements Converter {
	public static final StringSerializableConverter INSTANCE = new StringSerializableConverter();

	public Object convert(Context context, Object value, Class<?> c, Type t)
			throws Exception {
		if (value instanceof String) {
			try {
				Constructor<?> con = c.getConstructor(String.class);
				con.setAccessible(true);
				return con.newInstance(value.toString());
			} catch (NoSuchMethodException e) {
				return null;
			}
		} else if (value != null) {
			throw new UnsupportedOperationException("Cannot convert "
					+ value.getClass() + " to " + t);
		}
		return null;
	}
}
