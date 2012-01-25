package net.arnx.jsonic.internal.converter;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;

import net.arnx.jsonic.JSON.Context;

public class ByteConverter implements Converter {
	public static final ByteConverter INSTANCE = new ByteConverter();

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
			return (((Boolean) value).booleanValue()) ? 1 : 0;
		} else if (value instanceof BigDecimal) {
			return ((BigDecimal) value).byteValueExact();
		} else if (value instanceof Number) {
			return ((Number) value).byteValue();
		} else if (value instanceof String) {
			String str = value.toString().trim().toLowerCase();
			if (str.length() > 0) {
				int start = 0;
				if (str.charAt(0) == '+') {
					start++;
				}

				int num = 0;
				if (str.startsWith("0x", start)) {
					num = Integer.parseInt(str.substring(start + 2), 16);
				} else {
					num = Integer.parseInt(str.substring(start));
				}

				return (byte) ((num > 127) ? num - 256 : num);
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
