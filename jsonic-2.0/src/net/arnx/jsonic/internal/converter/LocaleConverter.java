package net.arnx.jsonic.internal.converter;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.arnx.jsonic.JSON.Context;

public class LocaleConverter implements Converter {
	public static final LocaleConverter INSTANCE = new LocaleConverter();

	public Object convert(Context context, Object value, Class<?> c, Type t)
			throws Exception {
		if (value instanceof List<?>) {
			List<?> src = (List<?>) value;
			if (src.size() == 1) {
				return new Locale(src.get(0).toString());
			} else if (src.size() == 2) {
				return new Locale(src.get(0).toString(), src.get(1).toString());
			} else if (src.size() > 2) {
				return new Locale(src.get(0).toString(), src.get(1).toString(),
						src.get(2).toString());
			} else {
				return null;
			}
		} else {
			if (value instanceof Map<?, ?>) {
				value = ((Map<?, ?>) value).get(null);
			}

			if (value instanceof String) {
				String[] array = value.toString().split("\\p{Punct}");

				if (array.length == 1) {
					return new Locale(array[0]);
				} else if (array.length == 2) {
					return new Locale(array[0], array[1]);
				} else if (array.length > 2) {
					return new Locale(array[0], array[1], array[2]);
				} else {
					return null;
				}
			} else if (value != null) {
				throw new UnsupportedOperationException("Cannot convert "
						+ value.getClass() + " to " + t);
			}
		}
		return null;
	}
}
