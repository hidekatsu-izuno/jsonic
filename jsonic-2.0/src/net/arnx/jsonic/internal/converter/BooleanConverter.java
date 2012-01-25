package net.arnx.jsonic.internal.converter;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import net.arnx.jsonic.JSON.Context;

public class BooleanConverter implements Converter {
	public static final BooleanConverter INSTANCE = new BooleanConverter();

	public Object convert(Context context, Object value, Class<?> c, Type t)
			throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?, ?>) value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>) value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value instanceof Boolean) {
			return value;
		} else if (value instanceof BigDecimal) {
			return !value.equals(BigDecimal.ZERO);
		} else if (value instanceof BigInteger) {
			return !value.equals(BigInteger.ZERO);
		} else if (value instanceof Number) {
			return ((Number) value).doubleValue() != 0;
		} else if (value != null) {
			String s = value.toString().trim();
			if (s.length() == 0 || s.equalsIgnoreCase("0")
					|| s.equalsIgnoreCase("f") || s.equalsIgnoreCase("false")
					|| s.equalsIgnoreCase("no") || s.equalsIgnoreCase("off")
					|| s.equals("NaN")) {
				return false;
			} else {
				return true;
			}
		}
		return PlainConverter.getDefaultValue(c);
	}
}
