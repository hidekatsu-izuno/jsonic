package net.arnx.jsonic.internal.converter;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import net.arnx.jsonic.JSON.Context;

public class AppendableConverter implements Converter {
	public static final AppendableConverter INSTANCE = new AppendableConverter();

	public Object convert(Context context, Object value, Class<?> c, Type t)
			throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?, ?>) value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>) value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value != null) {
			Appendable a = (Appendable) context.create(c);
			return a.append(value.toString());
		}
		return null;
	}
}
