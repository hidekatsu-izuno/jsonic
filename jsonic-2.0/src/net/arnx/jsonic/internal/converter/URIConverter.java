package net.arnx.jsonic.internal.converter;

import java.io.File;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

import net.arnx.jsonic.JSON.Context;

public class URIConverter implements Converter {
	public static final URIConverter INSTANCE = new URIConverter();

	public Object convert(Context context, Object value, Class<?> c, Type t)
			throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?, ?>) value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>) value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}
		if (value instanceof String) {
			if (value instanceof File) {
				return ((File) value).toURI();
			} else if (value instanceof URL) {
				return ((URL) value).toURI();
			} else {
				return new URI(value.toString().trim());
			}
		} else if (value != null) {
			throw new UnsupportedOperationException("Cannot convert "
					+ value.getClass() + " to " + t);
		}
		return null;
	}
}
