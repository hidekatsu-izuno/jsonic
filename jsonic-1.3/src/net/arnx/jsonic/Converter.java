/*
 * Copyright 2014 Hidekatsu Izuno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.arnx.jsonic;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Struct;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.io.StringBuilderOutputSource;
import net.arnx.jsonic.util.Base64;
import net.arnx.jsonic.util.BeanInfo;
import net.arnx.jsonic.util.ClassUtil;
import net.arnx.jsonic.util.PropertyInfo;

interface Converter {
	public boolean accept(Class<?> cls);
	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception;
}

final class NullableConverter implements Converter {
	private static final Class<?>[] targets = new Class<?>[] {
		java.sql.Array.class,
		Struct.class
	};

	public NullableConverter() {
	}

	@Override
	public boolean accept(Class<?> cls) {
		for (Class<?> target : targets) {
			if (target.isAssignableFrom(cls)) {
				return true;
			}
		}
		return false;
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) {
		return null;
	}
}

final class PlainConverter implements Converter {
	public static final PlainConverter INSTANCE = new PlainConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return true;
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) {
		return value;
	}
}

final class FormatConverter implements Converter {
	public static final FormatConverter INSTANCE = new FormatConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return true;
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		Context context2 = context.copy();
		context2.skipHint = context.getHint();
		value = context2.preformatInternal((value != null) ? value.getClass() : Object.class, value);
		StringBuilderOutputSource out = new StringBuilderOutputSource(200);
		try {
			context2.formatInternal(value, out);
		} catch (IOException e) {
			// no handle
		}
		out.flush();

		context.skipHint = context2.skipHint;
		Object ret =  context.postparseInternal(out.toString(), c, t);
		context.skipHint = null;

		return ret;
	}
}

final class StringSerializableConverter implements Converter {
	public static final StringSerializableConverter INSTANCE = new StringSerializableConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return true;
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (c.isEnum() || Enum.class.isAssignableFrom(c)) {
			return EnumConverter.INSTANCE.convert(context, value, c, t);
		} else if (value instanceof String) {
			if (c == String.class) {
				return value.toString();
			} else {
				try {
					Constructor<?> con = c.getConstructor(String.class);
					con.setAccessible(true);
					return con.newInstance(value.toString());
				} catch (NoSuchMethodException e) {
					return null;
				}
			}
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class SerializableConverter implements Converter {
	public static final SerializableConverter INSTANCE = new SerializableConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return Serializable.class.isAssignableFrom(cls);
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof String) {
			return ClassUtil.deserialize(Base64.decode((String)value));
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class BooleanConverter implements Converter {
	public static final BooleanConverter INSTANCE = new BooleanConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return boolean.class == cls || Boolean.class == cls;
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return (c == boolean.class) ? false : null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return (c == boolean.class) ? false : null;
		} else if (value instanceof Boolean) {
			return value;
		} else if (value instanceof BigDecimal) {
			return !value.equals(BigDecimal.ZERO);
		} else if (value instanceof BigInteger) {
			return !value.equals(BigInteger.ZERO);
		} else if (value instanceof Number) {
			return ((Number)value).doubleValue() != 0;
		} else {
			String s = value.toString().trim();
			if (s.length() == 0
				|| s.equalsIgnoreCase("0")
				|| s.equalsIgnoreCase("f")
				|| s.equalsIgnoreCase("false")
				|| s.equalsIgnoreCase("no")
				|| s.equalsIgnoreCase("off")
				|| s.equals("NaN")) {
				return false;
			} else {
				return true;
			}
		}
	}
}

final class CharacterConverter implements Converter {
	public static final CharacterConverter INSTANCE = new CharacterConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return char.class == cls || Character.class == cls;
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return (c == char.class) ? '\0' : null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return (c == char.class) ? '\0' : null;
		} else if (value instanceof Boolean) {
			return (((Boolean)value).booleanValue()) ? '1' : '0';
		} else if (value instanceof BigDecimal) {
			return (char)((BigDecimal)value).intValueExact();
		} else if (value instanceof String) {
			String s = value.toString();
			if (s.length() > 0) {
				return s.charAt(0);
			} else {
				return (c == char.class) ? '\0' : null;
			}
		} else if (value instanceof Number) {
			return (char)((Number)value).intValue();
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class ByteConverter implements Converter {
	public static final ByteConverter INSTANCE = new ByteConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return byte.class == cls || Byte.class == cls;
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return (c == byte.class) ? (byte)0 : null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return (c == byte.class) ? (byte)0 : null;
		} else if (value instanceof BigDecimal) {
			return ((BigDecimal)value).byteValueExact();
		} else if (value instanceof String) {
			NumberFormat f = context.getNumberFormat();
			if (f != null) value = f.parse((String)value);

			String str = value.toString().trim().toLowerCase();
			if (str.length() > 0) {
				int start = 0;
				if (str.charAt(0) == '+') {
					start++;
				}

				int num = 0;
				if (str.startsWith("0x", start)) {
					num = Integer.parseInt(str.substring(start+2), 16);
				} else {
					num = Integer.parseInt(str.substring(start));
				}

				return (byte)((num > 127) ? num-256 : num);
			} else {
				return (c == byte.class) ? (byte)0 : null;
			}
		} else if (value instanceof Boolean) {
			return (((Boolean)value).booleanValue()) ? 1 : 0;
		} else if (value instanceof Number) {
			return ((Number)value).byteValue();
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class ShortConverter implements Converter {
	public static final ShortConverter INSTANCE = new ShortConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return short.class == cls || Short.class == cls;
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return (c == short.class) ? (short)0 : null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return (c == short.class) ? (short)0 : null;
		} else if (value instanceof BigDecimal) {
			return ((BigDecimal)value).shortValueExact();
		} else  if (value instanceof String) {
			NumberFormat f = context.getNumberFormat();
			if (f != null) value = f.parse((String)value);

			String str = value.toString().trim();
			if (str.length() > 0) {
				int start = 0;
				if (str.charAt(0) == '+') {
					start++;
				}

				if (str.startsWith("0x", start)) {
					return (short)Integer.parseInt(str.substring(start+2), 16);
				} else {
					return (short)Integer.parseInt(str.substring(start));
				}
			} else {
				return (c == short.class) ? (short)0 : null;
			}
		} else if (value instanceof Boolean) {
			return (((Boolean)value).booleanValue()) ? 1 : 0;
		} else if (value instanceof Number) {
			return ((Number)value).shortValue();
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class IntegerConverter  implements Converter {
	public static final IntegerConverter INSTANCE = new IntegerConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return int.class == cls || Integer.class == cls;
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return (c == int.class) ? 0 : null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return (c == int.class) ? 0 : null;
		} else if (value instanceof BigDecimal) {
			return ((BigDecimal)value).intValueExact();
		} else  if (value instanceof String) {
			NumberFormat f = context.getNumberFormat();
			if (f != null) value = f.parse((String)value);

			String str = value.toString().trim();
			if (str.length() > 0) {
				int start = 0;
				if (str.charAt(0) == '+') {
					start++;
				}

				if (str.startsWith("0x", start)) {
					return Integer.parseInt(str.substring(start+2), 16);
				} else {
					return Integer.parseInt(str.substring(start));
				}
			} else {
				return (c == int.class) ? 0 : null;
			}
		} else if (value instanceof Boolean) {
			return (((Boolean)value).booleanValue()) ? 1 : 0;
		} else if (value instanceof Number) {
			return ((Number)value).intValue();
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class LongConverter implements Converter {
	public static final LongConverter INSTANCE = new LongConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return long.class == cls || Long.class == cls;
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return (c == long.class) ? 0L : null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return (c == long.class) ? 0L : null;
		} else if (value instanceof BigDecimal) {
			return ((BigDecimal)value).longValueExact();
		} else if (value instanceof String) {
			NumberFormat f = context.getNumberFormat();
			if (f != null) value = f.parse((String)value);

			String str = value.toString().trim();
			if (str.length() > 0) {
				int start = 0;
				if (str.charAt(0) == '+') {
					start++;
				}

				if (str.startsWith("0x", start)) {
					return Long.parseLong(str.substring(start+2), 16);
				} else {
					return Long.parseLong(str.substring(start));
				}
			} else {
				return (c == long.class) ? 0L : null;
			}
		} else if (value instanceof Boolean) {
			return (((Boolean)value).booleanValue()) ? 1l : 0l;
		} else if (value instanceof Number) {
			return ((Number)value).longValue();
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class FloatConverter  implements Converter {
	public static final FloatConverter INSTANCE = new FloatConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return float.class == cls || Float.class == cls;
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return (c == float.class) ? 0.0F : null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return (c == float.class) ? 0.0F : null;
		} else if (value instanceof String) {
			NumberFormat f = context.getNumberFormat();
			if (f != null) value = f.parse((String)value);

			String str = value.toString().trim();
			if (str.length() > 0) {
				return Float.valueOf(str);
			} else {
				return (c == float.class) ? 0.0F : null;
			}
		} else if (value instanceof Boolean) {
			return (((Boolean)value).booleanValue()) ? 1.0f : Float.NaN;
		} else if (value instanceof Number) {
			return ((Number)value).floatValue();
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class DoubleConverter  implements Converter {
	public static final DoubleConverter INSTANCE = new DoubleConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return double.class == cls || Double.class == cls;
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return (c == double.class) ? 0.0 : null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return (c == double.class) ? 0.0 : null;
		} else if (value instanceof BigDecimal) {
			return ((BigDecimal)value).doubleValue();
		} else if (value instanceof String) {
			NumberFormat f = context.getNumberFormat();
			if (f != null) value = f.parse((String)value);

			String str = value.toString().trim();
			if (str.length() > 0) {
				return Double.valueOf(str);
			} else {
				return (c == double.class) ? 0.0 : null;
			}
		} else if (value instanceof Boolean) {
			return (((Boolean)value).booleanValue()) ? 1.0 : Double.NaN;
		} else if (value instanceof Number) {
			return ((Number)value).doubleValue();
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class BigIntegerConverter  implements Converter {
	public static final BigIntegerConverter INSTANCE = new BigIntegerConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return BigInteger.class == cls;
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof BigDecimal) {
			return ((BigDecimal)value).toBigIntegerExact();
		} else if (value instanceof BigInteger) {
			return value;
		} else if (value instanceof String) {
			NumberFormat f = context.getNumberFormat();
			if (f != null) value = f.parse((String)value);

			String str = value.toString().trim();
			if (str.length() > 0) {
				int start = 0;
				if (str.charAt(0) == '+') {
					start++;
				}

				if (str.startsWith("0x", start)) {
					return new BigInteger(str.substring(start+2), 16);
				} else {
					return new BigInteger(str.substring(start));
				}
			}
			return null;
		} else if (value instanceof Boolean) {
			return (((Boolean)value).booleanValue()) ? BigInteger.ONE : BigInteger.ZERO;
		} else if (value instanceof Number) {
			return BigInteger.valueOf(((Number)value).longValue());
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class BigDecimalConverter  implements Converter {
	public static final BigDecimalConverter INSTANCE = new BigDecimalConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return BigDecimal.class == cls;
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof BigDecimal) {
			return value;
		} else if (value instanceof String) {
			NumberFormat f = context.getNumberFormat();
			if (f != null) value = f.parse((String)value);

			String str = value.toString().trim();
			if (str.length() > 0) {
				if (str.charAt(0) == '+') {
					return new BigDecimal(str.substring(1));
				} else {
					return new BigDecimal(str);
				}
			}
			return null;
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class PatternConverter implements Converter {
	public static final PatternConverter INSTANCE = new PatternConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return Pattern.class.equals(cls);
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof String) {
			return Pattern.compile(value.toString());
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class TimeZoneConverter implements Converter {
	public static final TimeZoneConverter INSTANCE = new TimeZoneConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return TimeZone.class.equals(cls);
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof String) {
			return TimeZone.getTimeZone(value.toString().trim());
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class LocaleConverter implements Converter {
	public static final LocaleConverter INSTANCE = new LocaleConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return Locale.class.equals(cls);
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			if (src.size() == 1) {
				return new Locale(src.get(0).toString());
			} else if (src.size() == 2) {
				return new Locale(src.get(0).toString(), src.get(1).toString());
			} else if (src.size() > 2) {
				return new Locale(src.get(0).toString(), src.get(1).toString(), src.get(2).toString());
			} else {
				return null;
			}
		} else {
			if (value instanceof Map<?, ?>) {
				value = ((Map<?,?>)value).get(null);
			}

			if (value instanceof String) {
				String[] array = value.toString().split("\\p{Punct}");

				if (array.length == 1) {
					return new Locale(array[0]);
				} else if (array.length == 2) {
					return new Locale(array[0], array[1]);
				} else if (array.length > 2) {
					return new Locale(array[0], array[1], array[2]);
				} else {
					return null;
				}
			} else {
				throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
			}
		}
	}
}

final class FileConverter implements Converter {
	public static final FileConverter INSTANCE = new FileConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return File.class.equals(cls);
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof String) {
			return new File(value.toString().trim());
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class URLConverter implements Converter {
	public static final URLConverter INSTANCE = new URLConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return URL.class.equals(cls);
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof String) {
			if (value instanceof File) {
				return ((File)value).toURI().toURL();
			} else if (value instanceof URI) {
				return ((URI)value).toURL();
			} else {
				return new URL(value.toString().trim());
			}
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class URIConverter implements Converter {
	public static final URIConverter INSTANCE = new URIConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return URI.class.equals(cls);
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof String) {
			if (value instanceof File) {
				return ((File)value).toURI();
			} else if (value instanceof URL) {
				return ((URL)value).toURI();
			} else {
				return new URI(value.toString().trim());
			}
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class UUIDConverter implements Converter {
	public static final UUIDConverter INSTANCE = new UUIDConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return UUID.class.equals(cls);
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof String) {
			return UUID.fromString(value.toString().trim());
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class CharsetConverter implements Converter {
	public static final CharsetConverter INSTANCE = new CharsetConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return Charset.class.equals(cls);
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof String) {
			return Charset.forName(value.toString().trim());
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class ClassConverter implements Converter {
	public static final ClassConverter INSTANCE = new ClassConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return Class.class.equals(cls);
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof String) {
			String s = value.toString().trim();
			if (s.equals("boolean")) {
				return boolean.class;
			} else if (s.equals("byte")) {
				return byte.class;
			} else if (s.equals("short")) {
				return short.class;
			} else if (s.equals("int")) {
				return int.class;
			} else if (s.equals("long")) {
				return long.class;
			} else if (s.equals("float")) {
				return float.class;
			} else if (s.equals("double")) {
				return double.class;
			} else {
				try {
					ClassLoader cl = Thread.currentThread().getContextClassLoader();
					return cl.loadClass(value.toString());
				} catch (ClassNotFoundException e) {
					return null;
				}
			}
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class CharSequenceConverter implements Converter {
	public static final CharSequenceConverter INSTANCE = new CharSequenceConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return CharSequence.class.isAssignableFrom(cls);
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else {
			return value.toString();
		}
	}
}

final class AppendableConverter implements Converter {
	public static final AppendableConverter INSTANCE = new AppendableConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return Appendable.class.isAssignableFrom(cls);
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else {
			Appendable a = (Appendable)context.createInternal(c);
			return a.append(value.toString());
		}
	}
}

final class PathConverter implements Converter {
	public PathConverter() {
	}

	@Override
	public boolean accept(Class<?> cls) {
		return Path.class.isAssignableFrom(cls);
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else {
			return Paths.get(value.toString());
		}
	}
}

final class EnumConverter implements Converter {
	public static final EnumConverter INSTANCE = new EnumConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return cls.isEnum() || Enum.class.isAssignableFrom(cls);
	}

	@SuppressWarnings({ "rawtypes" })
	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		}

		Enum[] enums = (Enum[])c.getEnumConstants();
		if (value instanceof Number) {
			return enums[((Number)value).intValue()];
		} else if (value instanceof Boolean) {
			return enums[((Boolean)value) ? 1 : 0];
		} else {
			String str = value.toString().trim();
			if (str.length() == 0) {
				return null;
			} else if (Character.isDigit(str.charAt(0))) {
				return enums[Integer.parseInt(str)];
			} else {
				for (Enum e : enums) {
					if (str.equals(e.name())) return e;
				}
				if (context.getEnumStyle() != null) {
					for (Enum e : enums) {
						if (str.equals(context.getEnumStyle().to(e.name()))) return e;
					}
				}
				throw new IllegalArgumentException(str + " is not " + c);
			}
		}
	}
}

final class DateConverter implements Converter {
	public static final DateConverter INSTANCE = new DateConverter();
	private static final Pattern TIMEZONE_PATTERN = Pattern.compile("(?:GMT|UTC)([+-][0-9]{2})([0-9]{2})");

	@Override
	public boolean accept(Class<?> cls) {
		return Date.class.isAssignableFrom(cls);
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		}

		Date date = null;
		if (value instanceof Number) {
			date = (Date)context.createInternal(c);
			date.setTime(((Number)value).longValue());
		} else if (value != null) {
			String str = value.toString().trim();
			if (str.length() > 0) {
				DateFormat format = context.getDateFormat();
				if (format != null) {
					date = format.parse(str);
				} else {
					date = convertDate(context, str);
				}

				if (date != null && !c.isAssignableFrom(date.getClass())) {
					long time = date.getTime();
					date = (Date)context.createInternal(c);
					date.setTime(time);
				}
			}
		}

		if (date instanceof java.sql.Date) {
			Calendar cal = Calendar.getInstance(context.getTimeZone(), context.getLocale());
			cal.setTimeInMillis(date.getTime());
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			date.setTime(cal.getTimeInMillis());
		} else if (date instanceof java.sql.Time) {
			Calendar cal = Calendar.getInstance(context.getTimeZone(), context.getLocale());
			cal.setTimeInMillis(date.getTime());
			cal.set(Calendar.YEAR, 1970);
			cal.set(Calendar.MONTH, Calendar.JANUARY);
			cal.set(Calendar.DATE, 1);
			date.setTime(cal.getTimeInMillis());
		}

		return date;
	}

	static Date convertDate(Context context, String value) throws ParseException {
		value = value.trim();
		if (value.length() == 0) {
			return null;
		}
		value = TIMEZONE_PATTERN.matcher(value).replaceFirst("GMT$1:$2");

		DateFormat format = null;
		if (Character.isDigit(value.charAt(0))) {
			StringBuilder sb = context.getLocalCache().getCachedBuffer();

			String types = "yMdHmsSZ";
			// 0: year, 1:month, 2: day, 3: hour, 4: minute, 5: sec, 6:msec, 7: timezone
			int pos = (value.length() > 2 && value.charAt(2) == ':') ? 3 : 0;
			boolean before = true;
			int count = 0;
			for (int i = 0; i < value.length(); i++) {
				char c = value.charAt(i);
				if ((pos == 4 || pos == 5 || pos == 6)
						&& (c == '+' || c == '-')
						&& (i + 1 < value.length())
						&& (Character.isDigit(value.charAt(i+1)))) {

					if (!before) sb.append('\'');
					pos = 7;
					count = 0;
					before = true;
					continue;
				} else if (pos == 7 && c == ':'
						&& (i + 1 < value.length())
						&& (Character.isDigit(value.charAt(i+1)))) {
					value = value.substring(0, i) + value.substring(i+1);
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
					if (count == ((type == 'y' || type == 'Z') ? 4 : (type == 'S') ? 3 : 2)) {
						count = 0;
						pos++;
						type = types.charAt(pos);
					}
					if (type != 'Z' || count == 0) sb.append(type);
					count++;
				} else {
					sb.append((c == '\'') ? "''" : c);
				}
				before = digit;
			}
			if (!before) sb.append('\'');

			format = new SimpleDateFormat(sb.toString(), Locale.ENGLISH);
		} else if (value.length() > 18) {
			if (value.charAt(3) == ',') {
				String pattern = "EEE, dd MMM yyyy HH:mm:ss Z";
				format = new SimpleDateFormat(
						(value.length() < pattern.length()) ? pattern.substring(0, value.length()) : pattern, Locale.ENGLISH);
			} else if (value.charAt(13) == ':') {
				format = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);
			} else if (value.charAt(18) == ':') {
				String pattern = "EEE MMM dd yyyy HH:mm:ss Z";
				format = new SimpleDateFormat(
						(value.length() < pattern.length()) ? pattern.substring(0, value.length()) : pattern, Locale.ENGLISH);
			} else  {
				format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, context.getLocale());
			}
		} else {
			format = DateFormat.getDateInstance(DateFormat.MEDIUM, context.getLocale());
		}
		format.setLenient(false);
		format.setTimeZone(context.getTimeZone());

		return format.parse(value);
	}
}

final class CalendarConverter implements Converter {
	public static final CalendarConverter INSTANCE = new CalendarConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return Calendar.class.isAssignableFrom(cls);
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof Number) {
			Calendar cal = (Calendar)context.createInternal(c);
			cal.setTimeInMillis(((Number)value).longValue());
			return cal;
		} else {
			String str = value.toString().trim();
			if (str.length() > 0) {
				Calendar cal = (Calendar)context.createInternal(c);

				DateFormat format = context.getDateFormat();
				if (format != null) {
					cal.setTime(format.parse(str));
				} else {
					cal.setTime(DateConverter.convertDate(context, str));
				}
				return  cal;
			} else {
				return null;
			}
		}
	}
}

final class InetAddressConverter implements Converter {
	public InetAddressConverter() {
	}

	@Override
	public boolean accept(Class<?> cls) {
		return InetAddress.class.isAssignableFrom(cls);
	}

	@Override
	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else  {
			return InetAddress.getByName(value.toString().trim());
		}
	}
}

final class ArrayConverter implements Converter {
	public static final ArrayConverter INSTANCE = new ArrayConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return cls.isArray();
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			Map<?, ?> src = (Map<?, ?>)value;
			if (!(src instanceof SortedMap<?, ?>)) {
				src = new TreeMap<Object, Object>(src);
			}
			value = src.values();
		}

		if (value == null) {
			return null;
		} else if (value instanceof Collection) {
			Collection<?> src = (Collection<?>)value;
			Object array = Array.newInstance(c.getComponentType(), src.size());
			Class<?> pc = c.getComponentType();
			Type pt = (t instanceof GenericArrayType) ?
					((GenericArrayType)t).getGenericComponentType() : pc;

			Iterator<?> it = src.iterator();
			JSONHint hint = context.getHint();
			for (int i = 0; it.hasNext(); i++) {
				context.enter(i, hint);
				Array.set(array, i, context.postparseInternal(it.next(), pc, pt));
				context.exit();
			}
			return array;
		} else {
			Class<?> ctype = c.getComponentType();
			if (value instanceof String) {
				if (byte.class.equals(ctype)) {
					return Base64.decode((String)value);
				} else if (char.class.equals(ctype)) {
					return ((String)value).toCharArray();
				}
			}
			Object array = Array.newInstance(ctype, 1);
			Class<?> pc = ctype;
			Type pt = (t instanceof GenericArrayType) ?
					((GenericArrayType)t).getGenericComponentType() : pc;
			context.enter(0, context.getHint());
			Array.set(array, 0, context.postparseInternal(value, pc, pt));
			context.exit();
			return array;
		}
	}
}

final class CollectionConverter implements Converter {
	public static final CollectionConverter INSTANCE = new CollectionConverter();

	private static final TypeVariable<?> TYPE_VARIABLE;

	static {
		TypeVariable<?>[] params = Collection.class.getTypeParameters();
		TYPE_VARIABLE = params[0];
	}

	@Override
	public boolean accept(Class<?> cls) {
		return Collection.class.isAssignableFrom(cls);
	}

	@SuppressWarnings("unchecked")
	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		}

		Type pt = context.getResolvedType(t, c, TYPE_VARIABLE);
		Class<?> pc = ClassUtil.getRawType(pt);
		JSONHint hint = context.getHint();

		Collection<Object> collection = null;

		if (value instanceof List) {
			List<?> src = (List<?>)value;

			context.createSizeHint = src.size();
			collection = (Collection<Object>)context.createInternal(c);
			context.createSizeHint = -1;

			if (Object.class.equals(pc)) {
				collection.addAll(src);
			} else {
				for (int i = 0; i < src.size(); i++) {
					context.enter(i, hint);
					collection.add(context.postparseInternal(src.get(i), pc, pt));
					context.exit();
				}
			}
		} else if (value instanceof Map) {
			Map<?, ?> map = (Map<?, ?>)value;
			if (!(map instanceof SortedMap<?, ?>)) {
				map = new TreeMap<Object, Object>(map);
			}

			Collection<?> src = map.values();
			context.createSizeHint = src.size();
			collection = (Collection<Object>)context.createInternal(c);
			context.createSizeHint = -1;

			if (Object.class.equals(pc)) {
				collection.addAll(src);
			} else {
				Iterator<?> it = src.iterator();
				for (int i = 0; it.hasNext(); i++) {
					context.enter(i, hint);
					collection.add(context.postparseInternal(it.next(), pc, pt));
					context.exit();
				}
			}
		} else if (value instanceof Collection) {
			Collection<?> src = (Collection<?>)value;
			context.createSizeHint = src.size();
			collection = (Collection<Object>)context.createInternal(c);
			context.createSizeHint = -1;

			if (Object.class.equals(pc)) {
				collection.addAll(src);
			} else {
				Iterator<?> it = src.iterator();
				for (int i = 0; it.hasNext(); i++) {
					context.enter(i, hint);
					collection.add(context.postparseInternal(it.next(), pc, pt));
					context.exit();
				}
			}
		} else {
			context.createSizeHint = 1;
			collection = (Collection<Object>)context.createInternal(c);
			context.createSizeHint = -1;

			if (Object.class.equals(pc)) {
				collection.add(value);
			} else {
				context.enter(0, hint);
				collection.add(context.postparseInternal(value, pc, pt));
				context.exit();
			}
		}

		return collection;
	}
}

final class PropertiesConverter implements Converter {
	public static final PropertiesConverter INSTANCE = new PropertiesConverter();

	@Override
	public boolean accept(Class<?> cls) {
		return Properties.class.isAssignableFrom(cls);
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		}

		Properties prop = (Properties)context.createInternal(c);
		if (value instanceof Map<?, ?> || value instanceof List<?>) {
			flattenProperties(context.getLocalCache().getCachedBuffer(), value, prop);
		} else {
			prop.setProperty(value.toString(), null);
		}
		return prop;
	}

	private static void flattenProperties(StringBuilder key, Object value, Properties props) {
		if (value instanceof Map<?,?>) {
			for (Map.Entry<?, ?> entry : ((Map<?, ?>)value).entrySet()) {
				int pos = key.length();
				if (pos > 0) key.append('.');
				key.append(entry.getKey());
				flattenProperties(key, entry.getValue(), props);
				key.setLength(pos);
			}
		} else if (value instanceof List<?>) {
			List<?> list = (List<?>)value;
			for (int i = 0; i < list.size(); i++) {
				int pos = key.length();
				if (pos > 0) key.append('.');
				key.append(i);
				flattenProperties(key, list.get(i), props);
				key.setLength(pos);
			}
		} else {
			props.setProperty(key.toString(), value.toString());
		}
	}
}

final class MapConverter implements Converter {
	public static final MapConverter INSTANCE = new MapConverter();

	private static final TypeVariable<?> TYPE_VARIABLE_KEY;
	private static final TypeVariable<?> TYPE_VARIABLE_VALUE;

	static {
		TypeVariable<?>[] params = Map.class.getTypeParameters();
		TYPE_VARIABLE_KEY = params[0];
		TYPE_VARIABLE_VALUE = params[1];
	}

	@Override
	public boolean accept(Class<?> cls) {
		return Map.class.isAssignableFrom(cls);
	}

	@SuppressWarnings("unchecked")
	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		}

		Type pt0 = context.getResolvedType(t, c, TYPE_VARIABLE_KEY);
		Type pt1 = context.getResolvedType(t, c, TYPE_VARIABLE_VALUE);
		Class<?> pc0 = ClassUtil.getRawType(pt0);
		Class<?> pc1 = ClassUtil.getRawType(pt1);

		Map<Object, Object> map;

		if (value instanceof Map<?, ?>) {
			Map<?, ?> src = (Map<?,?>)value;
			context.createSizeHint = src.size();
			map = (Map<Object, Object>)context.createInternal(c);
			context.createSizeHint = -1;

			if (Object.class.equals(pc0) && Object.class.equals(pc1)) {
				map.putAll(src);
			} else {
				JSONHint hint = context.getHint();
				for (Map.Entry<?, ?> entry : src.entrySet()) {
					Object key = context.postparseInternal(entry.getKey(), pc0, pt0);
					context.enter(entry.getKey(), hint);
					map.put(key, context.postparseInternal(entry.getValue(), pc1, pt1));
					context.exit();
				}
			}
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			context.createSizeHint = src.size();
			map = (Map<Object, Object>)context.createInternal(c);
			context.createSizeHint = -1;

			if (Object.class.equals(pc0) && Object.class.equals(pc1)) {
				for (int i = 0; i < src.size(); i++) {
					map.put(i, src.get(i));
				}
			} else {
				JSONHint hint = context.getHint();
				for (int i = 0; i < src.size(); i++) {
					Object key = context.postparseInternal(i, pc0, pt0);
					context.enter(i, hint);
					map.put(key, context.postparseInternal(src.get(i), pc1, pt1));
					context.exit();
				}
			}
		} else {
			context.createSizeHint = 1;
			map = (Map<Object, Object>)context.createInternal(c);
			context.createSizeHint = -1;

			JSONHint hint = context.getHint();
			Object key = (hint != null && hint.anonym().length() > 0) ? hint.anonym() : null;
			if (Object.class.equals(pc0) && Object.class.equals(pc1)) {
				map.put(value, null);
			} else {
				key = context.postparseInternal(key, pc0, pt0);
				context.enter(key, hint);
				map.put(key, context.postparseInternal(value, pc1, pt1));
				context.exit();
			}
		}
		return map;
	}
}

final class ObjectConverter implements Converter {
	private Class<?> cls;
	private transient Map<String, PropertyInfo> props;

	public ObjectConverter(Class<?> cls) {
		this.cls = cls;
	}

	@Override
	public boolean accept(Class<?> cls) {
		return !cls.isPrimitive();
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		}

		if (props == null) props = getSetProperties(context, cls);

		if (value instanceof Map<?, ?>) {
			Object o = context.createInternal(c);
			if (o == null) return null;
			for (Map.Entry<?, ?> entry : ((Map<?, ?>)value).entrySet()) {
				String name = entry.getKey().toString();
				PropertyInfo target = props.get(name);
				if (target == null) target = props.get(toLowerCamel(context, name));
				if (target == null) continue;

				JSONHint hint = target.getWriteAnnotation(JSONHint.class);
				context.enter(name, hint);
				Type ttype = target.getWriteGenericType();
				Class<?> tcls = target.getWriteType();
				if (ttype != tcls && t instanceof ParameterizedType) {
					ttype = context.getResolvedType(t, c, ttype);
					tcls = ClassUtil.getRawType(ttype);
				}
				target.set(o, context.postparseInternal(entry.getValue(), tcls, ttype));
				context.exit();
			}
			return o;
		} else if (value instanceof List<?>) {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		} else {
			JSONHint hint = context.getHint();
			if (hint != null && hint.anonym().length() > 0) {
				PropertyInfo target = props.get(hint.anonym());
				if (target == null) return null;
				Object o = context.createInternal(c);
				if (o == null) return null;

				JSONHint hint2 = target.getWriteAnnotation(JSONHint.class);
				context.enter(hint.anonym(), hint2);
				Class<?> cls = target.getWriteType();
				Type gtype = target.getWriteGenericType();
				if (gtype instanceof TypeVariable<?> && t instanceof ParameterizedType) {
					gtype = resolveTypeVariable((TypeVariable<?>)gtype, (ParameterizedType)t);
					cls = ClassUtil.getRawType(gtype);
				}
				target.set(o, context.postparseInternal(value, cls, gtype));
				context.exit();
				return o;
			} else {
				throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
			}
		}
	}

	private static Map<String, PropertyInfo> getSetProperties(Context context, Class<?> c) {
		Map<String, PropertyInfo> props = new HashMap<String, PropertyInfo>();

		// Field
		for (PropertyInfo prop : BeanInfo.get(c).getProperties()) {
			Field f = prop.getField();
			if (f == null || Modifier.isFinal(f.getModifiers()) || context.ignoreInternal(c, f)) continue;

			JSONHint hint = f.getAnnotation(JSONHint.class);
			String name = null;
			int ordinal = prop.getOrdinal();
			if (hint != null) {
				if (hint.ignore()) continue;
				ordinal = hint.ordinal();
				if (hint.name().length() != 0) name = hint.name();
			}

			if (name == null) {
				name = context.normalizeInternal(prop.getName());
				if (context.getPropertyStyle() != null) {
					name = context.getPropertyStyle().to(name);
				}
			}

			if (!name.equals(prop.getName()) || ordinal != prop.getOrdinal() || f != prop.getWriteMember()) {
				props.put(name, new PropertyInfo(prop.getBeanClass(), name,
					prop.getField(), null, null, prop.isStatic(), ordinal));
			} else {
				props.put(name, prop);
			}
		}

		// Method
		for (PropertyInfo prop : BeanInfo.get(c).getProperties()) {
			Method m = prop.getWriteMethod();
			if (m == null || context.ignoreInternal(c, m)) continue;

			JSONHint hint = m.getAnnotation(JSONHint.class);
			String name = null;
			int ordinal = prop.getOrdinal();
			if (hint != null) {
				if (hint.ignore()) continue;
				ordinal = hint.ordinal();
				if (hint.name().length() != 0) name = hint.name();
			}

			if (name == null) {
				name = context.normalizeInternal(prop.getName());
				if (context.getPropertyStyle() != null) {
					name = context.getPropertyStyle().to(name);
				}
			}

			if (!name.equals(prop.getName()) || ordinal != prop.getOrdinal()) {
				prop = new PropertyInfo(prop.getBeanClass(), name,
					null, null, prop.getWriteMethod(), prop.isStatic(), ordinal);
			}

			if (prop.getWriteMethod() != null) {
				prop.getWriteMethod().setAccessible(true);
			} else if (prop.getField() != null) {
				prop.getField().setAccessible(true);
			}

			props.put(name, prop);
		}
		return props;
	}

	private static String toLowerCamel(Context context, String name) {
		StringBuilder sb = context.getLocalCache().getCachedBuffer();
		boolean toUpperCase = false;
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (c == ' ' || c == '_' || c == '-') {
				toUpperCase = true;
			} else if (toUpperCase) {
				sb.append(Character.toUpperCase(c));
				toUpperCase = false;
			} else {
				sb.append(c);
			}
		}
		if (sb.length() > 1 && Character.isUpperCase(sb.charAt(0)) && !Character.isUpperCase(sb.charAt(1))) {
			sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
		}
		return context.getLocalCache().getString(sb);
	}

	private static Type resolveTypeVariable(TypeVariable<?> type, ParameterizedType parent) {
		Class<?> rawType = ClassUtil.getRawType(parent);
		if (rawType.equals(type.getGenericDeclaration())) {
			String tvName = type.getName();
			TypeVariable<?>[] rtypes = ((Class<?>)rawType).getTypeParameters();
			Type[] atypes = parent.getActualTypeArguments();

			for (int i = 0; i < rtypes.length; i++) {
				if (tvName.equals(rtypes[i].getName())) return atypes[i];
			}
		}

		return type.getBounds()[0];
	}
}

final class DurationConverter implements Converter {
	public DurationConverter() {
	}

	@Override
	public boolean accept(Class<?> cls) {
		return Duration.class == cls;
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof String) {
			return Duration.parse(((String)value));
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class InstantConverter implements Converter {
	public InstantConverter() {
	}

	@Override
	public boolean accept(Class<?> cls) {
		return Instant.class == cls;
	}

	@Override
	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof Number) {
			return Instant.ofEpochMilli(((Number)value).longValue());
		} else if (value instanceof String) {
			String format = context.getDateFormatText();
			if (format != null) {
				TemporalAccessor temp = context.getLocalCache()
					.get(DateTimeFormatter.class, format, DateTimeFormatterProvider.INSTANCE)
					.parseBest((String)value,
							new TemporalQuery<ZonedDateTime>() {
								@Override
								public ZonedDateTime queryFrom(TemporalAccessor temporal) {
									return ZonedDateTime.from(temporal);
								}
							},
							new TemporalQuery<OffsetDateTime>() {
								@Override
								public OffsetDateTime queryFrom(TemporalAccessor temporal) {
									return OffsetDateTime.from(temporal);
								}
							},
							new TemporalQuery<LocalDateTime>() {
								@Override
								public LocalDateTime queryFrom(TemporalAccessor temporal) {
									return LocalDateTime.from(temporal);
								}
							},
							new TemporalQuery<LocalDate>() {
								@Override
								public LocalDate queryFrom(TemporalAccessor temporal) {
									return LocalDate.from(temporal);
								}
							});

				if (temp instanceof ZonedDateTime) {
					return ((ZonedDateTime)temp).toInstant();
				} else if (temp instanceof OffsetDateTime) {
					return ((OffsetDateTime)temp).toInstant();
				} else if (temp instanceof LocalDateTime) {
					return ((LocalDateTime)temp).atZone(context.getTimeZone().toZoneId()).toInstant();
				} else if (temp instanceof LocalDate) {
					return ((LocalDate)temp).atStartOfDay(context.getTimeZone().toZoneId()).toInstant();
				} else {
					return (Instant)temp;
				}
			} else {
				return Instant.parse(((String)value));
			}
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class LocalDateConverter implements Converter {
	public LocalDateConverter() {
	}

	@Override
	public boolean accept(Class<?> cls) {
		return LocalDate.class == cls;
	}

	@Override
	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof String) {
			String format = context.getDateFormatText();
			if (format != null) {
				return LocalDate.parse(((String)value), context.getLocalCache()
						.get(DateTimeFormatter.class, format, DateTimeFormatterProvider.INSTANCE));
			} else {
				return LocalDate.parse(((String)value));
			}
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class LocalDateTimeConverter implements Converter {
	public LocalDateTimeConverter() {
	}

	@Override
	public boolean accept(Class<?> cls) {
		return LocalDateTime.class == cls;
	}

	@Override
	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof String) {
			String format = context.getDateFormatText();
			if (format != null) {
				return LocalDateTime.parse(((String)value), context.getLocalCache()
						.get(DateTimeFormatter.class, format, DateTimeFormatterProvider.INSTANCE));
			} else {
				return LocalDateTime.parse(((String)value));
			}
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class LocalTimeConverter implements Converter {
	public LocalTimeConverter() {
	}

	@Override
	public boolean accept(Class<?> cls) {
		return LocalTime.class == cls;
	}

	@Override
	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof String) {
			String format = context.getDateFormatText();
			if (format != null) {
				return LocalTime.parse(((String)value), context.getLocalCache()
						.get(DateTimeFormatter.class, format, DateTimeFormatterProvider.INSTANCE));
			} else {
				return LocalTime.parse(((String)value));
			}
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class MonthDayConverter implements Converter {
	public MonthDayConverter() {
	}

	@Override
	public boolean accept(Class<?> cls) {
		return MonthDay.class == cls;
	}

	@Override
	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof String) {
			String format = context.getDateFormatText();
			if (format != null) {
				return MonthDay.parse(((String)value), context.getLocalCache()
						.get(DateTimeFormatter.class, format, DateTimeFormatterProvider.INSTANCE));
			} else {
				return MonthDay.parse(((String)value));
			}
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class OffsetDateTimeConverter implements Converter {
	public OffsetDateTimeConverter() {
	}

	@Override
	public boolean accept(Class<?> cls) {
		return OffsetDateTime.class == cls;
	}

	@Override
	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof String) {
			String format = context.getDateFormatText();
			if (format != null) {
				return OffsetDateTime.parse(((String)value), context.getLocalCache()
						.get(DateTimeFormatter.class, format, DateTimeFormatterProvider.INSTANCE));
			} else {
				return OffsetDateTime.parse(((String)value));
			}
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class OffsetTimeConverter implements Converter {
	public OffsetTimeConverter() {
	}

	@Override
	public boolean accept(Class<?> cls) {
		return OffsetTime.class == cls;
	}

	@Override
	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof String) {
			String format = context.getDateFormatText();
			if (format != null) {
				return OffsetTime.parse(((String)value), context.getLocalCache()
						.get(DateTimeFormatter.class, format, DateTimeFormatterProvider.INSTANCE));
			} else {
				return OffsetTime.parse(((String)value));
			}
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class PeriodConverter implements Converter {
	public PeriodConverter() {
	}

	@Override
	public boolean accept(Class<?> cls) {
		return Period.class == cls;
	}

	@Override
	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof String) {
			return Period.parse(((String)value));
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class YearConverter implements Converter {
	public YearConverter() {
	}

	@Override
	public boolean accept(Class<?> cls) {
		return Year.class == cls;
	}

	@Override
	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof Number) {
			return Year.of(((Number)value).intValue());
		} else if (value instanceof String) {
			String format = context.getDateFormatText();
			if (format != null) {
				return Year.parse(((String)value), context.getLocalCache()
						.get(DateTimeFormatter.class, format, DateTimeFormatterProvider.INSTANCE));
			} else {
				return Year.parse(((String)value));
			}
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class YearMonthConverter implements Converter {
	public YearMonthConverter() {
	}

	@Override
	public boolean accept(Class<?> cls) {
		return YearMonth.class == cls;
	}

	@Override
	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof String) {
			String format = context.getDateFormatText();
			if (format != null) {
				return YearMonth.parse(((String)value), context.getLocalCache()
						.get(DateTimeFormatter.class, format, DateTimeFormatterProvider.INSTANCE));
			} else {
				return YearMonth.parse(((String)value));
			}
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class ZonedDateTimeConverter implements Converter {
	public ZonedDateTimeConverter() {
	}

	@Override
	public boolean accept(Class<?> cls) {
		return ZonedDateTime.class == cls;
	}

	@Override
	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof String) {
			String format = context.getDateFormatText();
			if (format != null) {
				return ZonedDateTime.parse(((String)value), context.getLocalCache()
						.get(DateTimeFormatter.class, format, DateTimeFormatterProvider.INSTANCE));
			} else {
				return ZonedDateTime.parse(((String)value));
			}
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class ZoneIdConverter implements Converter {
	public ZoneIdConverter() {
	}

	@Override
	public boolean accept(Class<?> cls) {
		return ZoneId.class == cls;
	}

	@Override
	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof String) {
			return ZoneId.of(((String)value));
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class ZoneOffsetConverter implements Converter {
	public ZoneOffsetConverter() {
	}

	@Override
	public boolean accept(Class<?> cls) {
		return ZoneOffset.class == cls;
	}

	@Override
	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof String) {
			return ZoneOffset.of(((String)value));
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class DayOfWeekConverter implements Converter {
	public DayOfWeekConverter() {
	}

	@Override
	public boolean accept(Class<?> cls) {
		return DayOfWeek.class == cls;
	}

	@Override
	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof Number) {
			return DayOfWeek.of(((Number)value).intValue());
		} else if (value instanceof String) {
			String text = (String)value;
			String format = context.getDateFormatText();
			if (format != null) {
				return context.getLocalCache()
						.get(DateTimeFormatter.class, format, DateTimeFormatterProvider.INSTANCE)
						.parse(text, new TemporalQuery<DayOfWeek>() {
								@Override
								public DayOfWeek queryFrom(TemporalAccessor temporal) {
									return DayOfWeek.from(temporal);
								}
							});
			} else if (text.length() > 0 && text.charAt(0) >= '0' && text.charAt(0) <= '9') {
				return DayOfWeek.of(Integer.parseInt(text));
			} else {
				return DayOfWeek.valueOf(text);
			}
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class MonthConverter implements Converter {
	public MonthConverter() {
	}

	@Override
	public boolean accept(Class<?> cls) {
		return Month.class == cls;
	}

	@Override
	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return null;
		} else if (value instanceof Map<?, ?>) {
			value = ((Map<?,?>)value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>)value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value == null) {
			return null;
		} else if (value instanceof Number) {
			return Month.of(((Number)value).intValue());
		} else if (value instanceof String) {
			String text = (String)value;
			String format = context.getDateFormatText();
			if (format != null) {
				return context.getLocalCache()
						.get(DateTimeFormatter.class, format, DateTimeFormatterProvider.INSTANCE)
						.parse(text, new TemporalQuery<Month>() {
								@Override
								public Month queryFrom(TemporalAccessor temporal) {
									return Month.from(temporal);
								}
							});
			} else if (text.length() > 0 && text.charAt(0) >= '0' && text.charAt(0) <= '9') {
				return Month.of(Integer.parseInt(text));
			} else {
				return Month.valueOf(text);
			}
		} else {
			throw new UnsupportedOperationException("Cannot convert " + value.getClass() + " to " + t);
		}
	}
}

final class OptionalIntConverter implements Converter {
	public OptionalIntConverter() {
	}

	@Override
	public boolean accept(Class<?> cls) {
		return OptionalInt.class.equals(cls);
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return OptionalInt.empty();
		}

		value = IntegerConverter.INSTANCE.convert(context, value, c, t);
		if (value instanceof Integer) {
			return OptionalInt.of((Integer)value);
		} else {
			return OptionalInt.empty();
		}
	}
}

final class OptionalLongConverter implements Converter {
	public OptionalLongConverter() {
	}

	@Override
	public boolean accept(Class<?> cls) {
		return OptionalLong.class.equals(cls);
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return OptionalLong.empty();
		}

		value = LongConverter.INSTANCE.convert(context, value, c, t);
		if (value instanceof Long) {
			return OptionalLong.of((Long)value);
		} else {
			return OptionalLong.empty();
		}
	}
}

final class OptionalDoubleConverter implements Converter {
	public OptionalDoubleConverter() {
	}

	@Override
	public boolean accept(Class<?> cls) {
		return OptionalDouble.class.equals(cls);
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return OptionalDouble.empty();
		}

		value = DoubleConverter.INSTANCE.convert(context, value, c, t);
		if (value instanceof Double) {
			return OptionalDouble.of((Double)value);
		} else {
			return OptionalDouble.empty();
		}
	}
}

final class OptionalConverter implements Converter {
	private static final TypeVariable<?> GENERICS_TYPE = Optional.class.getTypeParameters()[0];

	public OptionalConverter() {
	}

	@Override
	public boolean accept(Class<?> cls) {
		return Optional.class.isAssignableFrom(cls);
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) throws Exception {
		if (value == null) {
			return Optional.empty();
		}

		t = ClassUtil.getResolvedType(t, c, GENERICS_TYPE);
		value = context.convertInternal(value, ClassUtil.getRawType(t), t);
		if (value != null) {
			return Optional.of(value);
		} else {
			return Optional.empty();
		}
	}
}