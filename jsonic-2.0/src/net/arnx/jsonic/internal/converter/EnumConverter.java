package net.arnx.jsonic.internal.converter;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import net.arnx.jsonic.JSON.Context;

public class EnumConverter implements Converter {
	public static final EnumConverter INSTANCE = new EnumConverter();

	@SuppressWarnings({ "rawtypes" })
	public Object convert(Context context, Object value, Class<?> c, Type t)
			throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?, ?>) value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>) value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		Enum[] enums = (Enum[]) c.getEnumConstants();
		if (value instanceof Number) {
			return enums[((Number) value).intValue()];
		} else if (value instanceof Boolean) {
			return enums[((Boolean) value) ? 1 : 0];
		} else if (value != null) {
			String str = value.toString().trim();
			if (str.length() == 0) {
				return null;
			} else if (Character.isDigit(str.charAt(0))) {
				return enums[Integer.parseInt(str)];
			} else {
				for (Enum e : enums) {
					if (str.equals(e.name()))
						return e;
				}
				if (context.getEnumCaseStyle() != null) {
					for (Enum e : enums) {
						if (str.equals(context.getEnumCaseStyle().to(e.name())))
							return e;
					}
				}
				throw new IllegalArgumentException(str + " is not " + c);
			}
		}
		return null;
	}
}