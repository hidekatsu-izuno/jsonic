/*
 * Copyright 2007-2008 Hidekatsu Izuno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package net.arnx.jsonic;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

public abstract class Converter {
	private static final Map<Class, Object> PRIMITIVE_MAP = new IdentityHashMap<Class, Object>();
	
	static {
		PRIMITIVE_MAP.put(boolean.class, false);
		PRIMITIVE_MAP.put(byte.class, (byte)0);
		PRIMITIVE_MAP.put(short.class, (short)0);
		PRIMITIVE_MAP.put(int.class, 0);
		PRIMITIVE_MAP.put(long.class, 0l);
		PRIMITIVE_MAP.put(float.class, 0.0f);
		PRIMITIVE_MAP.put(double.class, 0.0);
		PRIMITIVE_MAP.put(char.class, '\0');
	}
	
	private Class<?> contextClass = null;
	private Object context = null;
	
	/**
	 * Sets context for inner class.
	 * 
	 * @param value context object or class
	 */
	public void setContext(Object value) {
		if (value instanceof Class<?>) {
			this.context = null;
			this.contextClass = (Class<?>)value;
		} else {
			this.context = value;
			this.contextClass = (value != null) ? value.getClass() : null;
		}
	}

	Locale locale;
	
	/**
	 * Sets locale for conversion or message.
	 * 
	 * @param locale
	 */
	public void setLocale(Locale locale) {
		if (locale == null) {
			throw new NullPointerException();
		}
		this.locale = locale;
	}
	
	protected final <T> T convertChild(Object key, Object value, Class<? extends T> c, Type type) throws ConvertException {
		T result = null;
		Class cast = (c.isPrimitive()) ? PRIMITIVE_MAP.get(c).getClass() : c;
		
		try {
			result = (T)cast.cast(convert(key, value, c, type));
		} catch (ConvertException e) {
			e.add(key);
			throw e;
		} catch (Exception e) {
			ConvertException ce = new ConvertException(getMessage("converter.convert.ConversionError", 
					(value instanceof String) ? "\"" + value + "\"" : value, type), e);
			ce.add(key);
			throw ce;
		}

		return result;
	}
	
	/**
	 * Converts Map/List/Number/String/Boolean/null to other Java Objects. If you converts a lower level object in this method, 
	 * You should call convertChild method.
	 * 
	 * @param key property key object. If the parent is a array, it is Integer. otherwise it is String. 
	 * @param value null or the instance of Map, List, Number, String or Boolean.
	 * @param c class for converting
	 * @param type generics type for converting. type equals to c if not generics.
	 * @return a converted object
	 * @throws Exception if conversion failed.
	 */
	protected <T> T convert(Object key, Object value, Class<? extends T> c, Type type) throws Exception {
		Object data = null;
		
		try {
			if (value == null) {
				if (c.isPrimitive()) {
					data = PRIMITIVE_MAP.get(c);
				}
			} else if (c.equals(type) && c.isAssignableFrom(value.getClass())) {
				data = value;
			} else if (value instanceof Map) {
				Map src = (Map)value;
				if (Map.class.isAssignableFrom(c)) {
					Map map = null;
					if (type instanceof ParameterizedType) {
						Type[] pts = ((ParameterizedType)type).getActualTypeArguments();
						Type pt0 = (pts != null && pts.length > 0) ? pts[0] : Object.class;
						Type pt1 = (pts != null && pts.length > 1) ? pts[1] : Object.class;
						Class<?> pc0 = getRawType(pt0);
						Class<?> pc1 = getRawType(pt1);
						
						if ((Object.class.equals(pc0) || String.class.equals(pc0))
								&& Object.class.equals(pc1)) {
							map = (Map)value;
						} else {
							map = (Map)create(c);
							for (Map.Entry entry : (Set<Map.Entry>)src.entrySet()) {
								map.put(convertChild(key, entry.getKey(), pc0, pt0), 
										convertChild(entry.getKey(), entry.getValue(), pc1, pt1));
							}
						}
					} else {
						map = (Map)create(c);
						map.putAll(src);
					}
					data = map;
				} else {
					Object o = create(c);
					if (o != null) {
						Map<String, Member> props = getSetProperties(c);
						for (Map.Entry entry : (Set<Map.Entry>)src.entrySet()) {
							String name = entry.getKey().toString();
							Member target = mapping(c, props, name);
							if (target == null) continue;
							
							if (target instanceof Method) {
								Method m = (Method)target;
								m.invoke(o, convertChild(name, entry.getValue(), m.getParameterTypes()[0], m.getGenericParameterTypes()[0]));
							} else {
								Field f = (Field)target;
								f.set(o, convertChild(name, entry.getValue(), f.getType(), f.getGenericType()));
							}
						}
						data = o;
					} else {
						throw new UnsupportedOperationException();
					}
				}
			} else if (value instanceof List) {
				List src = (List)value;
				if (Collection.class.isAssignableFrom(c)) {
					Collection collection = null;
					if (type instanceof ParameterizedType) {
						Type[] pts = ((ParameterizedType)type).getActualTypeArguments();
						Type pt = (pts != null && pts.length > 0) ? pts[0] : Object.class;
						Class<?> pc = getRawType(pt);
						
						if (Object.class.equals(pc)) {
							collection = src;
						} else {
							collection = (Collection)create(c);
							for (int i = 0; i < src.size(); i++) {
								collection.add(convertChild(i, src.get(i), pc, pt));
							}
						}
					} else {
						collection = (Collection)create(c);
						collection.addAll(src);
					}
					data = collection;
				} else if (c.isArray()) {
					Object array = Array.newInstance(c.getComponentType(), src.size());
					Class<?> pc = c.getComponentType();
					Type pt = (type instanceof GenericArrayType) ? 
							((GenericArrayType)type).getGenericComponentType() : pc;
					
					for (int i = 0; i < src.size(); i++) {
						Array.set(array, i, convertChild(i, src.get(i), pc, pt));
					}
					data = array;
				} else if (Map.class.isAssignableFrom(c)) {
					Map map = (Map)create(c);
					if (type instanceof ParameterizedType) {
						Type[] pts = ((ParameterizedType)type).getActualTypeArguments();
						Type pt0 = (pts != null && pts.length > 0) ? pts[0] : Object.class;
						Type pt1 = (pts != null && pts.length > 1) ? pts[1] : Object.class;
						Class<?> pc0 = getRawType(pt0);
						Class<?> pc1 = getRawType(pt1);

						for (int i = 0; i < src.size(); i++) {
							map.put(convertChild(key, i, pc0, pt0), convertChild(i, src.get(i), pc1, pt1));
						}
					} else {
						for (int i = 0; i < src.size(); i++) {
							map.put(i, src.get(i));
						}
					}
					data = map;
				} else if (Locale.class.equals(c)) {
					if (src.size() == 1) {
						data = new Locale(src.get(0).toString());
					} else if (src.size() == 2) {
						data = new Locale(src.get(0).toString(), src.get(1).toString());
					} else if (src.size() > 2) {
						data = new Locale(src.get(0).toString(), src.get(1).toString(), src.get(2).toString());
					}
				} else {
					throw new UnsupportedOperationException();
				}
			} else {
				if (boolean.class.equals(c) || Boolean.class.equals(c)) {
					if (value instanceof Number) {
						data = !value.equals(0);
					} else {
						String s = value.toString();
						if (s.length() == 0
							|| s.equalsIgnoreCase("f")
							|| s.equalsIgnoreCase("false")
							|| s.equalsIgnoreCase("no")
							|| s.equalsIgnoreCase("off")
							|| s.equals("NaN")) {
							data = false;
						} else {
							data = true;
						}
					}
				} else if (byte.class.equals(c) || Byte.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1 : 0;
					} else if (value instanceof Number) {
						data = ((Number)value).byteValue();
					} else {
						String str = value.toString().trim();
						if (str.length() > 0) {
							data = Byte.valueOf(str);
						} else if (c.isPrimitive()) {
							data = (byte)0;
						}
					}
				} else if (short.class.equals(c) || Short.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1 : 0;
					} else if (value instanceof Number) {
						data = ((Number)value).shortValue();
					} else {
						String str = value.toString().trim();
						if (str.length() > 0) {
							data = Short.valueOf(str);
						} else if (c.isPrimitive()) {
							data = (short)0;
						}
					}				
				} else if (int.class.equals(c) || Integer.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1 : 0;
					} else if (value instanceof Number) {
						data = ((Number)value).intValue();
					} else {
						String str = value.toString().trim();
						if (str.length() > 0) {
							data = Integer.valueOf(str);
						} else if (c.isPrimitive()) {
							data = 0;
						}						
					}
				} else if (long.class.equals(c) || Long.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1l : 0l;
					} else if (value instanceof Number) {
						data = ((Number)value).longValue();
					} else {
						String str = value.toString().trim();
						if (str.length() > 0) {
							data = Long.valueOf(str);
						} else if (c.isPrimitive()) {
							data = 0l;
						}						
					}
				} else if (float.class.equals(c) || Float.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1.0f : Float.NaN;
					} else if (value instanceof Number) {
						data = ((Number)value).floatValue();
					} else {
						String str = value.toString().trim();
						if (str.length() > 0) {
							data = Float.valueOf(str);
						} else if (c.isPrimitive()) {
							data = 0.0f;
						}						
					}
				} else if (double.class.equals(c) || Double.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1.0 : Double.NaN;
					} else if (value instanceof Number) {
						data = ((Number)value).doubleValue();
					} else {
						String str = value.toString().trim();
						if (str.length() > 0) {
							data = Double.valueOf(str);
						} else if (c.isPrimitive()) {
							data = 0.0;
						}						
					}
				} else if (BigInteger.class.equals(c)) {				
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? BigInteger.ONE : BigInteger.ZERO;
					} else if (value instanceof BigDecimal) {
						data = ((BigDecimal)value).toBigInteger();
					} else {
						String str = value.toString().trim();
						if (str.length() > 0) data = (new BigDecimal(str)).toBigInteger();
					}
				} else if (BigDecimal.class.equals(c) || Number.class.equals(c)) {
					String str = value.toString().trim();
					if (str.length() > 0) data = new BigDecimal(str);
				} else if (char.class.equals(c) || Character.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? '1' : '0';
					} else if (value instanceof Number) {
						data = (char)((Number)value).intValue();
					} else {
						String s = value.toString();
						if (s.length() > 0) {
							data = s.charAt(0);
						} else if (c.isPrimitive()) {
							data = '\0';
						}						
					}
				} else if (CharSequence.class.isAssignableFrom(c)) {
					data = value.toString();
				} else if (Appendable.class.isAssignableFrom(c)) {
					Appendable a = (Appendable)create(c);
					data = a.append(value.toString());
				} else if (Enum.class.isAssignableFrom(c)) {
					if (value instanceof Number) {
						data = c.getEnumConstants()[((Number)value).intValue()];
					} else {
						data = Enum.valueOf((Class<? extends Enum>)c, value.toString());
					}
				} else if (Pattern.class.equals(c)) {
					data = Pattern.compile(value.toString());
				} else if (Date.class.isAssignableFrom(c)) {
					if (value instanceof Number) {
						Date date = (Date)create(c);
						date.setTime(((Number)value).longValue());
						data = date;
					} else {
						String str = value.toString().trim();
						if (str.length() > 0) {
							Date date = (Date)create(c);
							date.setTime(convertDate(str));
							data = date;
						}
					}
				} else if (Calendar.class.isAssignableFrom(c)) {
					if (value instanceof Number) {
						Calendar cal = (Calendar)create(c);
						cal.setTimeInMillis(((Number)value).longValue());
						data = cal;
					} else {
						String str = value.toString().trim();
						if (str.length() > 0) {
							Calendar cal = (Calendar)create(c);
							cal.setTimeInMillis(convertDate(str));
							data = cal;
						}
					}
				} else if (TimeZone.class.equals(c)) {
					data = TimeZone.getTimeZone(value.toString().trim());
				} else if (Locale.class.equals(c)) {
					String[] array = value.toString().split("\\p{Punct}");
					
					if (array.length == 1) {
						data = new Locale(array[0]);
					} else if (array.length == 2) {
						data = new Locale(array[0], array[1]);
					} else if (array.length > 2) {
						data = new Locale(array[0], array[1], array[2]);
					}
				} else if (Class.class.equals(c)) {
					String s = value.toString();
					if (s.equals("boolean")) {
						data = boolean.class;
					} else if (s.equals("byte")) {
						data = byte.class;
					} else if (s.equals("short")) {
						data = short.class;
					} else if (s.equals("int")) {
						data = int.class;
					} else if (s.equals("long")) {
						data = long.class;
					} else if (s.equals("float")) {
						data = float.class;
					} else if (s.equals("double")) {
						data = double.class;
					} else {
						data = Class.forName(value.toString());
					}
				} else if (Collection.class.isAssignableFrom(c)) {
					Collection collection = (Collection)create(c);
					if (type instanceof ParameterizedType) {
						Type[] pts = ((ParameterizedType)type).getActualTypeArguments();
						Type pt = (pts != null && pts.length > 0) ? pts[0] : Object.class;
						Class<?> pc = getRawType(pt);

						collection.add(convertChild(0, value, pc, pt));
					} else {
						collection.add(value);
					}
					data = collection;
				} else if (c.isArray()) {
					if (value instanceof String && byte.class.equals(c.getComponentType())) {
						data = Base64.decode((String)value);
					} else {
						Object array = Array.newInstance(c.getComponentType(), 1);
						Class<?> pc = c.getComponentType();
						Type pt = (type instanceof GenericArrayType) ? 
								((GenericArrayType)type).getGenericComponentType() : pc;
								
						Array.set(array, 0, convertChild(0, value, pc, pt));
						data = array;
					}
				} else {
					throw new UnsupportedOperationException();
				}
			}
		} catch (ConvertException e) {
			throw e;
		}
		
		return (T)data;
	}
	
	protected boolean ignore(Class<?> target, Member member) {
		int modifiers = member.getModifiers();
		if (Modifier.isStatic(modifiers)) return true;
		if (Modifier.isTransient(modifiers)) return true;
		if (member.getDeclaringClass().equals(Object.class)) return true;
		return false;
	}
	
	protected Object create(Class<?> c) throws Exception {
		Object instance = null;
		
		if (c.isInterface()) {
			if (SortedMap.class.equals(c)) {
				instance = new TreeMap();
			} else if (Map.class.equals(c)) {
				instance = new LinkedHashMap();
			} else if (SortedSet.class.equals(c)) {
				instance = new TreeSet();
			} else if (Set.class.equals(c)) {
				instance = new LinkedHashSet();
			} else if (List.class.equals(c)) {
				instance = new ArrayList();
			} else if (Collection.class.equals(c)) {
				instance = new ArrayList();
			} else if (Appendable.class.equals(c)) {
				instance = new StringBuilder();
			}
		} else if (Modifier.isAbstract(c.getModifiers())) {
			if (Calendar.class.equals(c)) {
				instance = Calendar.getInstance();
			}
		} else if (c.isMemberClass() || c.isAnonymousClass()) {
			Class eClass = c.getEnclosingClass();
			Constructor con = c.getDeclaredConstructor(eClass);
			if(tryAccess(c)) con.setAccessible(true);
			instance = con.newInstance((eClass.equals(this.contextClass)) ? this.context : null);
		} else {
			if (Date.class.isAssignableFrom(c)) {
				try {
					Constructor con = c.getDeclaredConstructor(long.class);
					if(tryAccess(c)) con.setAccessible(true);
					instance = con.newInstance(0l);
				} catch (NoSuchMethodException e) {
					// no handle
				}
			}
			
			if (instance == null) {
				Constructor con = c.getDeclaredConstructor();
				if(tryAccess(c)) con.setAccessible(true);
				instance = con.newInstance();
			}
		}
		
		return instance;
	}
	
	private Member mapping(Class c, Map<String, Member> props, String name) {
		Member target = props.get(name);
		if (target == null) {
			target = props.get(toLowerCamel(name));
			if (target == null) {
				target = props.get(name + "_");
			}
		}
		return target;
	}
	
	private boolean tryAccess(Class<?> c) {
		int modifier = c.getModifiers();
		if (this.contextClass != null && !Modifier.isPublic(modifier)) {
			if (Modifier.isPrivate(modifier)) {
				return this.contextClass.equals(c.getEnclosingClass());
			}
			return c.getPackage().equals(this.contextClass.getPackage());
		}
		return false;
	}
	
	private static String toLowerCamel(String name) {
		StringBuilder sb = new StringBuilder(name.length());
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
		if (sb.length() > 1 && Character.isUpperCase(sb.charAt(0)) && Character.isLowerCase(sb.charAt(1))) {
			sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
		}
		return sb.toString();
	}
	
	Map<String, Member> getGetProperties(Class<?> c) {
		Map<String, Member> props = new HashMap<String, Member>();
		
		boolean access = tryAccess(c);

		for (Field f : c.getFields()) {
			if (ignore(c, f)) continue;
			if (access) f.setAccessible(true);
			props.put(f.getName(), f);
		}
		
		for (Method m : c.getMethods()) {
			if (ignore(c, m)) continue;

			String name = m.getName();
			int start = 0;
			if (name.startsWith("get")
				&& name.length() > 3
				&& Character.isUpperCase(name.charAt(3))
				&& m.getParameterTypes().length == 0
				&& !m.getReturnType().equals(void.class)) {
				start = 3;
			} else if (name.startsWith("is")
				&& name.length() > 2
				&& Character.isUpperCase(name.charAt(2))
				&& m.getParameterTypes().length == 0
				&& m.getReturnType().equals(boolean.class)) {
				start = 2;
			} else {
				continue;
			}
			
			char[] cs = name.toCharArray();
			if (cs.length < start+2 || Character.isLowerCase(cs[start+1])) {
				cs[start] = Character.toLowerCase(cs[start]);
			}
			if (access) m.setAccessible(true);
			props.put(new String(cs, start, cs.length-start), m);
		}
		
		return props;
	}
	
	Map<String, Member> getSetProperties(Class<?> c) {
		Map<String, Member> props = new HashMap<String, Member>();
		
		boolean access = tryAccess(c);

		for (Field f : c.getFields()) {
			if (ignore(c, f)) continue;
			if (access) f.setAccessible(true);
			props.put(f.getName(), f);
		}
		
		for (Method m : c.getMethods()) {
			if (ignore(c, m)) continue;

			String name = m.getName();
			int start = 0;
			if (name.startsWith("set") 
				&& name.length() > 3
				&& Character.isUpperCase(name.charAt(3))
				&& m.getParameterTypes().length == 1
				&& m.getReturnType().equals(void.class)) {
				start = 3;
			} else {
				continue;
			}
			
			char[] cs = name.toCharArray();
			if (cs.length < start+2 || Character.isLowerCase(cs[start+1])) {
				cs[start] = Character.toLowerCase(cs[start]);
			}
			if (access) m.setAccessible(true);
			props.put(new String(cs, start, cs.length-start), m);
		}
		
		return props;
	}
	
	private Class<?> getRawType(Type t) {
		if (t instanceof ParameterizedType) {
			return (Class<?>)((ParameterizedType)t).getRawType();
		} else if (t instanceof Class) {
			return (Class<?>)t;
		} else {
			return Object.class;
		}		
	}
	
	private Long convertDate(String value) throws java.text.ParseException {
		value = value.trim();
		if (value.length() == 0) {
			return null;
		}
		value = Pattern.compile("(?:GMT|UTC)([+-][0-9]{2})([0-9]{2})")
			.matcher(value)
			.replaceFirst("GMT$1:$2");
		
		DateFormat format = null;
		if (Character.isDigit(value.charAt(0))) {
			StringBuilder sb = new StringBuilder(value.length() * 2);

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
			} else {
				format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale);
			}
		} else {
			format = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
		}
		
		return format.parse(value).getTime();
	}
	
	String getMessage(String id, Object... args) {
		if (locale == null) locale = Locale.getDefault();
		ResourceBundle bundle = ResourceBundle.getBundle("net.arnx.jsonic.Messages", locale);
		return MessageFormat.format(bundle.getString(id), args);
	}
}

class Base64 {
	private static final String BASE64_MAP = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
	
	public static String encode(byte[] data) {
		if (data == null) return null;
		
		char[] buffer = new char[data.length / 3 * 4 + ((data.length % 3 == 0) ? 0 : 4)];
		
		int buf = 0;
		for (int i = 0; i < data.length; i++) {
			switch (i % 3) {
				case 0 :
					buffer[i / 3 * 4] = BASE64_MAP.charAt((data[i] & 0xFC) >> 2);
					buf = (data[i] & 0x03) << 4;
					if (i + 1 == data.length) {
						buffer[i / 3 * 4 + 1] = BASE64_MAP.charAt(buf);
						buffer[i / 3 * 4 + 2] = '=';
						buffer[i / 3 * 4 + 3] = '=';
					}
					break;
				case 1 :
					buf += (data[i] & 0xF0) >> 4;
					buffer[i / 3 * 4 + 1] = BASE64_MAP.charAt(buf);
					buf = (data[i] & 0x0F) << 2;
					if (i + 1 == data.length) {
						buffer[i / 3 * 4 + 2] = BASE64_MAP.charAt(buf);
						buffer[i / 3 * 4 + 3] = '=';
					}
					break;
				case 2 :
					buf += (data[i] & 0xC0) >> 6;
					buffer[i / 3 * 4 + 2] = BASE64_MAP.charAt(buf);
					buffer[i / 3 * 4 + 3] = BASE64_MAP.charAt(data[i] & 0x3F);
					break;
			}
		}

		return new String(buffer);
	}
	
	public static byte[] decode(CharSequence cs) {
		int addsize = 0;
		int bufsize = 0;
		
		for (int i = 0; i < cs.length(); i++) {
			char c = cs.charAt(i);
			if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '+' || c == '/') {
				bufsize++;
			} else if (c == '=') {
				if (i+1 < cs.length() && cs.charAt(i+1) == '=' && (bufsize+2) % 4 == 0) {
					bufsize += 2;
					addsize = -2;
				} else if ((bufsize+1) % 4 == 0) {
					bufsize += 1;
					addsize = -1;
				} else {
					return null;
				}
				break;
			}
		}
		
		byte[] buffer = new byte[bufsize / 4 * 3 + addsize];
		
		int pos = 0;
		for (int i = 0; i < cs.length(); i++) {
			char c = cs.charAt(i);
			
			int data = 0;
			if (c >= 'A' && c <= 'Z') {
				data = c - 65;
			} else if (c >= 'a' && c <= 'z') {
				data = c - 97 + 26;
			} else if (c >= '0' && c <= '9') {
				data = c - 48 + 52;
			} else if (c == '+') {
				data = 62;
			} else if (c == '/') {
				data = 63;
			} else if (c == '=') {
				break;
			} else {
				continue;
			}
			
			switch (pos % 4) {
			case 0:
				buffer[pos / 4 * 3] = (byte)(data << 2);
				break;
			case 1:
				buffer[pos / 4 * 3] += (byte)(data >> 4);
				if (pos / 4 * 3 + 1 < buffer.length) {
					buffer[pos / 4 * 3 + 1] = (byte)((data & 0xF) << 4);
				}
				break;
			case 2:
				buffer[pos / 4 * 3 + 1] += (byte)(data >> 2);
				if (pos / 4 * 3 + 2 < buffer.length) {
					buffer[pos / 4 * 3 + 2] = (byte)((data & 0x3) << 6);
				}
				break;
			case 3:
				buffer[pos / 4 * 3 + 2] += (byte)data;
				break;
			}
			pos++;
		}
		
		return buffer;
	}
}



