package net.arnx.jsonic.internal.converter;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import net.arnx.jsonic.JSON.Context;

public class CharsetConverter implements Converter {
	public static final CharsetConverter INSTANCE = new CharsetConverter();

	public Object convert(Context context, Object value, Class<?> c, Type t)
			throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?, ?>) value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>) value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		if (value instanceof String) {
			return Charset.forName(value.toString().trim());
		} else if (value != null) {
			throw new UnsupportedOperationException("Cannot convert "
					+ value.getClass() + " to " + t);
		}
		return null;
	}
}
