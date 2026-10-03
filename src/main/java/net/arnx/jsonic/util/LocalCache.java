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
package net.arnx.jsonic.util;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TimeZone;

public class LocalCache {
	private static final int CACHE_SIZE = 256;

	private final String bundle;
	private ResourceBundle resources;
	private Locale locale;
	private TimeZone timeZone;

	private StringBuilder builderCache;
	private int stringCacheCount = 0;
	private String[] stringCache;
	private Map<Class<?>, Map<Object, Object>> formatCache;

	public LocalCache(String bundle, Locale locale, TimeZone timeZone) {
		this.bundle = bundle;
		this.locale = locale;
		this.timeZone = timeZone;
	}

	public StringBuilder getCachedBuffer() {
		if (builderCache == null) {
			builderCache = new StringBuilder();
		} else {
			builderCache.setLength(0);
		}
		return builderCache;
	}

	public String getString(CharSequence cs) {
		if (cs.length() == 0) return "";

		if (cs.length() < 32 && stringCacheCount++ > 16) {
			int index = getCacheIndex(cs);
			if (index < 0) {
				return cs.toString();
			}

			if (stringCache == null) stringCache = new String[CACHE_SIZE];

			String str = stringCache[index];
			if (str == null || str.length() != cs.length()) {
				str = cs.toString();
				stringCache[index] = str;
				return str;
			}

			for (int i = 0; i < cs.length(); i++) {
				if (str.charAt(i) != cs.charAt(i)) {
					str = cs.toString();
					stringCache[index] = str;
					return str;
				}
			}
			return str;
		}

		return cs.toString();
	}

	/** Interns a slice without copying it through a temporary builder. */
	public String getString(CharSequence cs, int start, int end) {
		int length = end - start;
		if (length == 0) return "";
		if (length < 32 && stringCacheCount++ > 16) {
			int hash = 0;
			for (int i = start, limit = start + Math.min(16, length); i < limit; i++) {
				hash = hash * 31 + cs.charAt(i);
			}
			int index = hash & (CACHE_SIZE - 1);
			if (stringCache == null) stringCache = new String[CACHE_SIZE];
			String value = stringCache[index];
			if (value != null && value.length() == length) {
				int i = 0;
				while (i < length && value.charAt(i) == cs.charAt(start + i)) i++;
				if (i == length) return value;
			}
			return stringCache[index] = cs.subSequence(start, end).toString();
		}
		return cs.subSequence(start, end).toString();
	}

	/** Interns a buffered character slice without a temporary StringBuilder. */
	public String getBufferedString(char[] chars, int start, int end) {
		int length = end - start;
		if (length == 0) return "";
		if (length < 32 && stringCacheCount++ > 16) {
			int hash = 0;
			for (int i = start, limit = start + Math.min(16, length); i < limit; i++) {
				hash = hash * 31 + chars[i];
			}
			int index = hash & (CACHE_SIZE - 1);
			if (stringCache == null) stringCache = new String[CACHE_SIZE];
			String value = stringCache[index];
			if (value != null && value.length() == length) {
				int i = 0;
				while (i < length && value.charAt(i) == chars[start + i]) i++;
				if (i == length) return value;
			}
			return stringCache[index] = new String(chars, start, length);
		}
		return new String(chars, start, length);
	}

	private int getCacheIndex(CharSequence cs) {
		int h = 0;
		int max = Math.min(16, cs.length());
		for (int i = 0; i < max; i++) {
			h = h * 31 + cs.charAt(i);
		}
		return h & (CACHE_SIZE-1);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> cls, Object key, Provider<T> provider) {
		Map<Object, Object> map = null;
		if (formatCache == null) {
			formatCache = new HashMap<>();
		} else {
			map = formatCache.get(cls);
		}
		if (map == null) {
			map = new HashMap<>();
			formatCache.put(cls, map);
		}
		Object f = map.get(key);
		if (f == null) {
			f = provider.get(key, locale, timeZone);
			map.put(key, f);
		}
		return (T)f;
	}

	public NumberFormat getNumberFormat(String format) {
		return get(NumberFormat.class, format, NumberFormatProvider.INSTANCE);
	}

	public DateFormat getDateFormat(String format) {
		return get(DateFormat.class, format, DateFormatProvider.INSTANCE);
	}

	public Type getResolvedType(Type ptype, Class<?> pcls, Type type) {
		return get(Type.class, new ParameterTypeKey(ptype, pcls, type), ResolvedTypeProvider.INSTANCE);
	}

	public String getMessage(String id) {
		return getMessage(id, (Object[])null);
	}

	public String getMessage(String id, Object... args) {
		// Successful conversions do not need error messages. ResourceBundle lookup
		// otherwise allocates lookup keys on every new conversion context.
		if (resources == null) resources = ResourceBundle.getBundle(bundle, locale);
		if (args != null && args.length > 0) {
			return MessageFormat.format(resources.getString(id), args);
		} else {
			return resources.getString(id);
		}
	}

	private static class ParameterTypeKey {
		private Type ptype;
		private Class<?> pcls;
		private Type type;

		public ParameterTypeKey(Type ptype, Class<?> pcls, Type type) {
			this.ptype = ptype;
			this.pcls = pcls;
			this.type = type;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((ptype == null) ? 0 : ptype.hashCode());
			result = prime * result + ((pcls == null) ? 0 : pcls.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ParameterTypeKey other = (ParameterTypeKey) obj;
			if (ptype == null) {
				if (other.ptype != null)
					return false;
			} else if (!ptype.equals(other.ptype))
				return false;
			if (pcls == null) {
				if (other.pcls != null)
					return false;
			} else if (!pcls.equals(other.pcls))
				return false;
			if (type == null) {
				if (other.type != null)
					return false;
			} else if (!type.equals(other.type))
				return false;
			return true;
		}
	}

	public static interface Provider<T> {
		public T get(Object key, Locale locale, TimeZone timeZone);
	}

	private static class NumberFormatProvider implements Provider<NumberFormat> {
		public static final NumberFormatProvider INSTANCE = new NumberFormatProvider();

		@Override
		public NumberFormat get(Object format, Locale locale, TimeZone timeZone) {
			return new DecimalFormat((String)format, new DecimalFormatSymbols(locale));
		}
	}

	private static class DateFormatProvider implements Provider<DateFormat> {
		public static final DateFormatProvider INSTANCE = new DateFormatProvider();

		@Override
		public DateFormat get(Object format, Locale locale, TimeZone timeZone) {
			ExtendedDateFormat dformat = new ExtendedDateFormat((String)format, locale);
			dformat.setTimeZone(timeZone);
			return dformat;
		}
	}

	private static class ResolvedTypeProvider implements Provider<Type> {
		public static final ResolvedTypeProvider INSTANCE = new ResolvedTypeProvider();

		@Override
		public Type get(Object o, Locale locale, TimeZone timeZone) {
			ParameterTypeKey key = (ParameterTypeKey)o;
			return ClassUtil.getResolvedType(key.ptype, key.pcls, key.type);
		}
	}
}
