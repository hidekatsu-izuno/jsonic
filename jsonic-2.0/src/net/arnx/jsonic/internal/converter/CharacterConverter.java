package net.arnx.jsonic.internal.converter;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import net.arnx.jsonic.JSON.Context;

public class CharacterConverter implements Converter {
	public static final CharacterConverter INSTANCE = new CharacterConverter();

	public Object convert(Context context, Object value, Class<?> c, Type t)
			throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?, ?>) value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>) value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value instanceof Boolean) {
			return (((Boolean) value).booleanValue()) ? '1' : '0';
		} else if (value instanceof BigDecimal) {
			return (char) ((BigDecimal) value).intValueExact();
		} else if (value instanceof String) {
			String s = value.toString();
			if (s.length() > 0) {
				return s.charAt(0);
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
