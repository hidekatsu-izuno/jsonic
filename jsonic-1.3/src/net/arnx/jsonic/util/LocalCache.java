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
	private Map<String, DateFormat> dateFormatCache;
	private Map<String, NumberFormat> numberFormatCache;
	private Map<ParameterTypeKey, Type> parameterTypeCache;

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

	public NumberFormat getNumberFormat(String format) {
		NumberFormat nformat = null;
		if (numberFormatCache == null) {
			numberFormatCache = new HashMap<String, NumberFormat>();
		} else {
			nformat = numberFormatCache.get(format);
		}
		if (nformat == null) {
			nformat = new DecimalFormat(format, new DecimalFormatSymbols(locale));
			numberFormatCache.put(format, nformat);
		}
		return nformat;
	}

	public DateFormat getDateFormat(String format) {
		DateFormat dformat = null;
		if (dateFormatCache == null) {
			dateFormatCache = new HashMap<String, DateFormat>();
		} else {
			dformat = dateFormatCache.get(format);
		}
		if (dformat == null) {
			dformat = new ExtendedDateFormat(format, locale);
			dformat.setTimeZone(timeZone);
			dateFormatCache.put(format, dformat);
		}
		return dformat;
	}

	public Type getParameterType(Type t, Class<?> cls, int pos) {
		ParameterTypeKey key = new ParameterTypeKey(t, cls, pos);
		Type result = null;
		if (parameterTypeCache == null) {
			parameterTypeCache = new HashMap<ParameterTypeKey, Type>();
		} else {
			result = parameterTypeCache.get(key);
		}
		if (result == null) {
			result = ClassUtil.getParameterType(t, cls, pos);
			parameterTypeCache.put(key, result);
		}
		return result;
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
		private Type t;
		private Class<?> cls;
		private int pos;

		public ParameterTypeKey(Type t, Class<?> cls, int pos) {
			this.t = t;
			this.cls = cls;
			this.pos = pos;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((cls == null) ? 0 : cls.hashCode());
			result = prime * result + pos;
			result = prime * result + ((t == null) ? 0 : t.hashCode());
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
			if (cls == null) {
				if (other.cls != null)
					return false;
			} else if (!cls.equals(other.cls))
				return false;
			if (pos != other.pos)
				return false;
			if (t == null) {
				if (other.t != null)
					return false;
			} else if (!t.equals(other.t))
				return false;
			return true;
		}
	}
}
