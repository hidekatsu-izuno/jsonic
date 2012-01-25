package net.arnx.jsonic.internal.converter;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;

import net.arnx.jsonic.JSON.Context;

public class BigIntegerConverter implements Converter {
	public static final BigIntegerConverter INSTANCE = new BigIntegerConverter();

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
			return (((Boolean) value).booleanValue()) ? BigInteger.ONE
					: BigInteger.ZERO;
		} else if (value instanceof BigDecimal) {
			return ((BigDecimal) value).toBigIntegerExact();
		} else if (value instanceof BigInteger) {
			return value;
		} else if (value instanceof Number) {
			return BigInteger.valueOf(((Number) value).longValue());
		} else if (value instanceof String) {
			String str = value.toString().trim();
			if (str.length() > 0) {
				int start = 0;
				if (str.charAt(0) == '+') {
					start++;
				}

				if (str.startsWith("0x", start)) {
					return new BigInteger(str.substring(start + 2), 16);
				} else {
					return new BigInteger(str.substring(start));
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
