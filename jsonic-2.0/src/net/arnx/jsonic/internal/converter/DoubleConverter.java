package net.arnx.jsonic.internal.converter;

import java.lang.reflect.Type;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;

import net.arnx.jsonic.JSON.Context;

public class DoubleConverter implements Converter {
	public static final DoubleConverter INSTANCE = new DoubleConverter();

	public Object convert(Context context, Object value, Class<?> c, Type t)
			throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?, ?>) value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>) value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value instanceof String) {
			NumberFormat f = context.getNumberFormat();
			if (f != null)
				value = f.parse((String) value);
		}

		if (value instanceof Boolean) {
			return (((Boolean) value).booleanValue()) ? 1.0 : Double.NaN;
		} else if (value instanceof Number) {
			return ((Number) value).doubleValue();
		} else if (value instanceof String) {
			String str = value.toString().trim();
			if (str.length() > 0) {
				return Double.valueOf(str);
			} else {
				return PlainConverter.getDefaultValue(c);
			}
		} else if (value != null) {
			throw new UnsupportedOperationException("Cannot convert "
					+ value.getClass() + " to " + t);
		}
		return PlainConverter.getDefaultValue(c);
	}
}
