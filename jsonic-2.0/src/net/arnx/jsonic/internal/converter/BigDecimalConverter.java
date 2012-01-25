package net.arnx.jsonic.internal.converter;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;

import net.arnx.jsonic.JSON.Context;

public class BigDecimalConverter implements Converter {
	public static final BigDecimalConverter INSTANCE = new BigDecimalConverter();

	public Object convert(Context context, Object value, Class<?> c, Type t)
			throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?, ?>) value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>) value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value instanceof BigDecimal) {
			return value;
		} else if (value instanceof String) {
			NumberFormat f = context.getNumberFormat();
			if (f != null)
				value = f.parse((String) value);

			String str = value.toString().trim();
			if (str.length() > 0) {
				if (str.charAt(0) == '+') {
					return new BigDecimal(str.substring(1));
				} else {
					return new BigDecimal(str);
				}
			}
			return null;
		} else if (value != null) {
			throw new UnsupportedOperationException("Cannot convert "
					+ value.getClass() + " to " + t);
		}
		return null;
	}
}
