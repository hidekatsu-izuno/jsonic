package net.arnx.jsonic.internal.converter;

import java.io.File;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import net.arnx.jsonic.JSON.Context;

public class FileConverter implements Converter {
	public static final FileConverter INSTANCE = new FileConverter();

	public Object convert(Context context, Object value, Class<?> c, Type t)
			throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?, ?>) value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>) value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		if (value instanceof String) {
			return new File(value.toString().trim());
		} else if (value != null) {
			throw new UnsupportedOperationException("Cannot convert "
					+ value.getClass() + " to " + t);
		}
		return null;
	}
}
