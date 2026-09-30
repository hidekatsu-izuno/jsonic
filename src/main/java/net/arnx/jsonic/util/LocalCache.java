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

	private ResourceBundle resources;
	private Locale locale;
	private TimeZone timeZone;

	private StringBuilder builderCache;
	private int stringCacheCount = 0;
	private String[] stringCache;
	private Map<Class<?>, Map<Object, Object>> formatCache;

	public LocalCache(String bundle, Locale locale, TimeZone timeZone) {
		this.resources = ResourceBundle.getBundle(bundle, locale);
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
			formatCache = new HashMap<Class<?>, Map<Object, Object>>();
		} else {
			map = formatCache.get(cls);
		}
		if (map == null) {
			map = new HashMap<Object, Object>();
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
