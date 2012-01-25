package net.arnx.jsonic.internal.converter;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import net.arnx.jsonic.JSON.Context;

public class DateConverter implements Converter {
	public static final DateConverter INSTANCE = new DateConverter();
	private static final Pattern TIMEZONE_PATTERN = Pattern
			.compile("(?:GMT|UTC)([+-][0-9]{2})([0-9]{2})");

	public Object convert(Context context, Object value, Class<?> c, Type t)
			throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?, ?>) value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>) value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		Date date = null;
		long millis = -1;
		if (value instanceof Number) {
			millis = ((Number) value).longValue();
			date = (Date) context.create(c);
		} else if (value != null) {
			String str = value.toString().trim();
			if (str.length() > 0) {
				millis = convertDate(str, context);
				date = (Date) context.create(c);
			}
		}

		if (date != null) {
			if (date instanceof java.sql.Date) {
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(millis);
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);
				date.setTime(cal.getTimeInMillis());
			} else if (date instanceof java.sql.Time) {
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(millis);
				cal.set(Calendar.YEAR, 1970);
				cal.set(Calendar.MONTH, Calendar.JANUARY);
				cal.set(Calendar.DATE, 1);
				date.setTime(cal.getTimeInMillis());
			} else {
				date.setTime(millis);
			}
		}

		return date;
	}

	static Long convertDate(String value, Context context)
			throws ParseException {
		value = value.trim();
		if (value.length() == 0) {
			return null;
		}
		value = TIMEZONE_PATTERN.matcher(value).replaceFirst("GMT$1:$2");

		DateFormat format = null;
		if (Character.isDigit(value.charAt(0))) {
			StringBuilder sb = new StringBuilder(value.length() * 2);

			String types = "yMdHmsSZ";
			// 0: year, 1:month, 2: day, 3: hour, 4: minute, 5: sec, 6:msec, 7:
			// timezone
			int pos = (value.length() > 2 && value.charAt(2) == ':') ? 3 : 0;
			boolean before = true;
			int count = 0;
			for (int i = 0; i < value.length(); i++) {
				char c = value.charAt(i);
				if ((pos == 4 || pos == 5 || pos == 6)
						&& (c == '+' || c == '-') && (i + 1 < value.length())
						&& (Character.isDigit(value.charAt(i + 1)))) {

					if (!before)
						sb.append('\'');
					pos = 7;
					count = 0;
					before = true;
					continue;
				} else if (pos == 7 && c == ':' && (i + 1 < value.length())
						&& (Character.isDigit(value.charAt(i + 1)))) {
					value = value.substring(0, i) + value.substring(i + 1);
					continue;
				}

				boolean digit = (Character.isDigit(c) && pos < 8);
				if (before != digit) {
					sb.append('\'');
					if (digit) {
						count = 0;
						pos++;
					}
				}

				if (digit) {
					char type = types.charAt(pos);
					if (count == ((type == 'y' || type == 'Z') ? 4
							: (type == 'S') ? 3 : 2)) {
						count = 0;
						pos++;
						type = types.charAt(pos);
					}
					if (type != 'Z' || count == 0)
						sb.append(type);
					count++;
				} else {
					sb.append((c == '\'') ? "''" : c);
				}
				before = digit;
			}
			if (!before)
				sb.append('\'');

			format = new SimpleDateFormat(sb.toString(), Locale.ENGLISH);
		} else if (value.length() > 18) {
			if (value.charAt(3) == ',') {
				String pattern = "EEE, dd MMM yyyy HH:mm:ss Z";
				format = new SimpleDateFormat(
						(value.length() < pattern.length()) ? pattern.substring(
								0, value.length())
								: pattern, Locale.ENGLISH);
			} else if (value.charAt(13) == ':') {
				format = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy",
						Locale.ENGLISH);
			} else if (value.charAt(18) == ':') {
				String pattern = "EEE MMM dd yyyy HH:mm:ss Z";
				format = new SimpleDateFormat(
						(value.length() < pattern.length()) ? pattern.substring(
								0, value.length())
								: pattern, Locale.ENGLISH);
			} else {
				format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
						DateFormat.MEDIUM, context.getLocale());
			}
		} else {
			format = DateFormat.getDateInstance(DateFormat.MEDIUM,
					context.getLocale());
		}
		format.setLenient(false);
		format.setTimeZone(context.getTimeZone());

		return format.parse(value).getTime();
	}
}