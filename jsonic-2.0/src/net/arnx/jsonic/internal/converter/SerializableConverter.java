package net.arnx.jsonic.internal.converter;

import java.lang.reflect.Type;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.util.Base64;
import net.arnx.jsonic.internal.util.ClassUtil;

public class SerializableConverter implements Converter {
	public static final SerializableConverter INSTANCE = new SerializableConverter();

	public Object convert(Context context, Object value, Class<?> c, Type t)
			throws Exception {
		if (value instanceof String) {
			return ClassUtil.deserialize(Base64.decode((String) value));
		} else if (value != null) {
			throw new UnsupportedOperationException("Cannot convert "
					+ value.getClass() + " to " + t);
		}
		return null;
	}
}
