package net.arnx.jsonic.internal.converter;

import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import net.arnx.jsonic.JSON.Context;

public class CalendarConverter implements Converter {
	public static final CalendarConverter INSTANCE = new CalendarConverter();

	public Object convert(Context context, Object value, Class<?> c, Type t)
			throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?, ?>) value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>) value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value instanceof Number) {
			Calendar cal = (Calendar) context.create(c);
			cal.setTimeInMillis(((Number) value).longValue());
			return cal;
		} else if (value != null) {
			String str = value.toString().trim();
			if (str.length() > 0) {
				Calendar cal = (Calendar) context.create(c);
				cal.setTimeInMillis(DateConverter.convertDate(str, context));
				return cal;
			}
		}
		return null;
	}
}