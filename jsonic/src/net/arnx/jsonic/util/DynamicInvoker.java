package net.arnx.jsonic.util;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class DynamicInvoker {
	private Class contextClass = null;
	private Object context = null;
	
	public void setContext(Object value) {
		if (value instanceof Class) {
			this.context = null;
			this.contextClass = (Class)value;
		} else {
			this.context = value;
			this.contextClass = (value != null) ? value.getClass() : null;
		}
	}
	
	public Object invoke(Object o, String methodName, List values) throws Exception {
		if (values == null) {
			values = Collections.EMPTY_LIST;
		}
		
		methodName = toJavaName(methodName);
		
		Class target = o.getClass();
		Method method = null;
		loop: do {
			for (Method m : target.getDeclaredMethods()) {
				if (methodName.equals(m.getName())
						&& !Modifier.isStatic(m.getModifiers())
						&& Modifier.isPublic(m.getModifiers())) {
					if (method == null && values.size() == m.getParameterTypes().length) {
						method = m;
						break loop;
					}
				}
			}
			
			target = target.getSuperclass();
		} while (method == null && target != null);
		
		if (method == null || limit(method)) {
			throw new NoSuchMethodException();
		}
		
		Class c = o.getClass();
		
		Class<?>[] paramTypes = method.getParameterTypes();
		Object[] params = new Object[paramTypes.length];
		for (int i = 0; i < params.length; i++) {
			params[i] = convert(values.get(i), paramTypes[i], paramTypes[i]);
		}
		
		boolean access = (!Modifier.isPublic(c.getModifiers()) 
				&& !Modifier.isPrivate(c.getModifiers())
				&& this.context != null
				&& c.getPackage().equals(this.context.getClass().getPackage()));
		
		if (access) method.setAccessible(true);
		return method.invoke(o, params);
	}
	
	public <T> T convert(Object value, Class<? extends T> c) throws Exception {
		return (T)convert(value, c, c);
	}

	public Object convert(Object value, Class c, Type type) throws Exception {
		Object data = null;
		
		try {
			if (c.isPrimitive()) {
				if (c.equals(boolean.class)) {
					if (value == null) {
						data = false;
					} else if (value instanceof Boolean) {
						data = value;
					} else if (value instanceof Number) {
						data = !value.equals(0);
					} else {
						String s = value.toString();
						if (s.length() == 0
							|| s.equalsIgnoreCase("false")
							|| s.equals("NaN")) {
							data = false;
						} else {
							data = true;
						}
					}
				} else if (c.equals(byte.class)) {
					if (value == null) {
						data = 0;
					} else if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1 : 0;
					} else if (value instanceof Number) {
						data = ((Number)value).byteValue();
					} else {
						data = Byte.valueOf(value.toString());
					}
				} else if (c.equals(short.class)) {
					if (value == null) {
						data = 0;
					} else if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1 : 0;
					} else if (value instanceof Number) {
						data = ((Number)value).shortValue();
					} else {
						data = Short.valueOf(value.toString());
					}
				} else if (c.equals(int.class)) {
					if (value == null) {
						data = 0;
					} else if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1 : 0;
					} else if (value instanceof Number) {
						data = ((Number)value).intValue();
					} else {
						data = Integer.valueOf(value.toString());
					}
				} else if (c.equals(long.class)) {
					if (value == null) {
						data = 0l;
					} else if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1l : 0l;
					} else if (value instanceof Number) {
						data = ((Number)value).longValue();
					} else {
						data = Long.valueOf(value.toString());
					}
				} else if (c.equals(float.class)) {
					if (value == null) {
						data = 0.0f;
					} else if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1.0f : Float.NaN;
					} else if (value instanceof Number) {
						data = ((Number)value).floatValue();
					} else {
						data = Float.valueOf(value.toString());
					}
				} else if (c.equals(double.class)) {
					if (value == null) {
						data = 0.0;
					} else if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1.0 : Double.NaN;
					} else if (value instanceof Number) {
						data = ((Number)value).doubleValue();
					} else {
						data = Double.valueOf(value.toString());
					}
				} else if (c.equals(char.class)) {
					if (value == null) {
						data = '\u0000';
					} else if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? '1' : '0';
					} else if (value instanceof Character) {
						data = value;
					} else if (value instanceof Number) {
						data = (char)((Number)value).intValue();
					} else {
						String s = value.toString();
						data = (s.length() > 0) ? s.charAt(0) : '\u0000';
					}
				}
			} else if (value != null) {
				if (c.isAssignableFrom(value.getClass()) && c.equals(type)) {
					data = value;
				} else if (Boolean.class.equals(c)) {
					if (value instanceof Number) {
						data = !value.equals(0);
					} else {
						String s = value.toString();
						if (s.length() == 0
							|| s.equalsIgnoreCase("false")
							|| s.equals("NaN")) {
							data = false;
						} else {
							data = true;
						}
					}
				} else if (Byte.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1 : 0;
					} else if (value instanceof Number) {
						data = ((Number)value).byteValue();
					} else {
						data = Byte.valueOf(value.toString());
					}
				} else if (Short.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1 : 0;
					} else if (value instanceof Number) {
						data = ((Number)value).shortValue();
					} else {
						data = Short.valueOf(value.toString());
					}				
				} else if (Integer.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1 : 0;
					} else if (value instanceof Number) {
						data = ((Number)value).intValue();
					} else {
						data = Integer.valueOf(value.toString());
					}
				} else if (Long.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1l : 0l;
					} else if (value instanceof Number) {
						data = ((Number)value).longValue();
					} else {
						data = Long.valueOf(value.toString());
					}
				} else if (Float.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1.0f : Float.NaN;
					} else if (value instanceof Number) {
						data = ((Number)value).floatValue();
					} else {
						data = Float.valueOf(value.toString());
					}
				} else if (Double.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? 1.0 : Double.NaN;
					} else if (value instanceof Number) {
						data = ((Number)value).doubleValue();
					} else {
						data = Double.valueOf(value.toString());
					}
				} else if (BigInteger.class.equals(c)) {				
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? BigInteger.ONE : BigInteger.ZERO;
					} else if (value instanceof BigDecimal) {
						data = ((BigDecimal)value).toBigInteger();
					} else {
						data = (new BigDecimal(value.toString())).toBigInteger();
					}
				} else if (BigDecimal.class.equals(c) || Number.class.equals(c)) {
					data = new BigDecimal(value.toString());
				} else if (Character.class.equals(c)) {
					if (value instanceof Boolean) {
						data = (((Boolean)value).booleanValue()) ? '1' : '0';
					} else if (value instanceof Number) {
						data = (char)((Number)value).intValue();
					} else {
						String s = value.toString();
						data = (s.length() > 0) ? s.charAt(0) : null;
					}
				} else if (CharSequence.class.isAssignableFrom(c)) {
					data = value.toString();
				} else if (Appendable.class.isAssignableFrom(c)) {
					Appendable a = (Appendable)create(c);
					data = a.append(value.toString());
				} else if (Date.class.isAssignableFrom(c)) {
					if (value instanceof Number) {
						Date date = (Date)create(c);
						date.setTime(((Number)value).longValue());
						data = date;
					} else {
						DateFormat format = DateFormat.getDateTimeInstance();
						Date date = (Date)create(c);
						date.setTime(format.parse(value.toString()).getTime());
						data = date;
					}
				} else if (Calendar.class.isAssignableFrom(c)) {
					Calendar cal = (Calendar)create(c);
					if (value instanceof Number) {
						cal.setTimeInMillis(((Number)value).longValue());
					} else {
						DateFormat format = DateFormat.getDateTimeInstance();
						try {
							cal.setTime(format.parse(value.toString()));
						} catch (Exception e) {
							data = null;
						}
					}
					data = cal;
				} else if (Collection.class.isAssignableFrom(c)) {
					if (value instanceof Collection) {
						Collection collection = (Collection)create(c);
						if (type instanceof ParameterizedType) {
							ParameterizedType pType = (ParameterizedType)type;
							Type[] cTypes = pType.getActualTypeArguments();
							Type cType = (cTypes != null && cTypes.length > 0) ? cTypes[0] : Object.class;
							Class<?> cClasses = null;
							if (cType instanceof ParameterizedType) {
								cClasses = (Class)((ParameterizedType)cType).getRawType();
							} else if (cType instanceof Class) {
								cClasses = (Class)cType;
							} else {
								cClasses = Object.class;
								cType = cClasses;
							}
							for (Object o : (Collection)value) {
								collection.add(convert(o, cClasses, cType));
							}
						} else {
							collection.addAll((Collection)value);
						}
						data = collection;
					}
				} else if (c.isArray()) {
					if (value instanceof Collection) {
						Object array = Array.newInstance(c.getComponentType(), ((Collection)value).size());
						int i = 0;
						Class<?> cClass = c.getComponentType();
						Type cType = (type instanceof GenericArrayType) ? 
								((GenericArrayType)type).getGenericComponentType() : cClass;
						
						for (Object o : (Collection)value) {
							Array.set(array, i++, convert(o, cClass, cType));
						}
						data = array;
					} else if (value instanceof CharSequence && byte.class.equals(c.getComponentType())) {
						data = Base64.decode((CharSequence)value);
					}
				} else if (Map.class.isAssignableFrom(c)) {
					if (value instanceof Map) {
						Map map = (Map)create(c);
						if (type instanceof ParameterizedType) {
							ParameterizedType pType = (ParameterizedType)type;
							Type[] cTypes = pType.getActualTypeArguments();
							Class<?>[] cClasses = new Class[2];
							for (int i = 0; i < cClasses.length; i++) {
								if (cTypes[i] instanceof ParameterizedType) {
									cClasses[i] = (Class)((ParameterizedType)cTypes[i]).getRawType();
								} else if (cTypes[i] instanceof Class) {
									cClasses[i] = (Class)cTypes[i];
								} else {
									cClasses[i] = Object.class;
									cTypes[i] = cClasses[i];
								}
							}
							for (Object key : ((Map)value).keySet()) {
								map.put(convert(key, cClasses[0], cTypes[0]),
										convert(((Map)value).get(key), cClasses[1], cTypes[1]));
							}
						} else {
							map.putAll((Map)value);
						}
						data = map;
					}
				} else if (Pattern.class.equals(c)) {
					data = Pattern.compile(value.toString());
				} else if (Locale.class.equals(c)) {
					String[] s = null;
					if (value instanceof Collection || value.getClass().isArray()) {
						s = (String[])convert(value, String[].class, String[].class);
					} else {
						s = value.toString().split("\\p{Punct}");
					}
					
					if (s.length == 0) {
						data = null;
					} else if (s.length == 1) {
						data = new Locale(s[0]);
					} else if (s.length == 2) {
						data = new Locale(s[0], s[1]);
					} else {
						data = new Locale(s[0], s[1], s[2]);
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
				} else if (value instanceof Map) {
					Object o = create(c);
					if (o != null) {
						Map<String, Object> props = getGetProperties(c);
						
						boolean access = tryAccess(c);
						
						Map map = (Map)value;
						for (Object key : map.keySet()) {
							Object target = props.get(toJavaName(key.toString()));
							if (target == null) {
								continue;
							} else if (target instanceof Method) {
								Method m = (Method)target;
								try {
									if (access) m.setAccessible(true);
									m.invoke(o, convert(map.get(key.toString()), m.getParameterTypes()[0], m.getGenericParameterTypes()[0]));
								} catch (Exception e) {
									handleConvertError(key.toString(), map.get(key), m.getParameterTypes()[0], m.getGenericParameterTypes()[0], e);
								}
							} else if (target instanceof Field) {
								Field f = (Field)target;
								try {
									if (access) f.setAccessible(true);
									f.set(o, convert(map.get(key.toString()), f.getType(), f.getGenericType()));
								} catch (Exception e) {
									handleConvertError(key.toString(), map.get(key), f.getType(), f.getGenericType(), e);
								}
							}
						}
						data = o;
					}
				}
			}
		} catch (Exception e) {
			handleConvertError(null, value, c, type, e);
		}
		
		return data;
	}
	
	protected Object create(Class c) throws Exception {
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
		} else if (c.isMemberClass()) {
			Class eClass = c.getEnclosingClass();
			Constructor con = c.getDeclaredConstructor(eClass);
			if (!Modifier.isPublic(con.getModifiers()) && tryAccess(c)) {
				con.setAccessible(true);
			}
			instance = con.newInstance((eClass.equals(this.contextClass)) ? this.context : null);
		} else {
			Constructor con = c.getDeclaredConstructor((Class[])null);
			if (!Modifier.isPublic(con.getModifiers()) && tryAccess(c)) {
				con.setAccessible(true);
			}
			instance = con.newInstance((Object[])null);
		}
		
		return instance;
	}
	
	protected void handleConvertError(String key, Object value, Class c, Type type, Exception e) throws Exception {
		// no handle
	}

	public boolean limit(Method method) {
		return method.getDeclaringClass().equals(Object.class);
	}
	
	public boolean tryAccess(Class c) {
		int modifier = c.getModifiers();
		if (this.contextClass == null || Modifier.isPublic(modifier)) return false;
		
		if (Modifier.isPrivate(modifier)) {
			return this.contextClass.equals(c.getEnclosingClass());
		}
		return c.getPackage().equals(this.contextClass.getPackage());
	}
	
	private static Map<String, Object> getGetProperties(Class c) {
		Map<String, Object> props = new HashMap<String, Object>();
		
		for (Field f : c.getFields()) {
			int modifiers = f.getModifiers();
			if (!Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers)) {
				props.put(f.getName(), f);
			}
		}
		
		for (Method m : c.getMethods()) {
			String name = m.getName();
			if (!Modifier.isStatic(m.getModifiers())
					&& name.startsWith("set")
					&& name.length() > 3
					&& Character.isUpperCase(name.charAt(3))
					&& m.getParameterTypes().length == 1
					&& m.getReturnType().equals(void.class)) {
				
				String key = null;
				if (!(name.length() > 4 && Character.isUpperCase(name.charAt(4)))) {
					char[] carray = name.toCharArray();
					carray[3] = Character.toLowerCase(carray[3]);
					key = new String(carray, 3, carray.length-3);
				} else {
					key = name.substring(3);
				}
				
				props.put(key, m);
			}
		}
		return props;
	}
	
	private static String toJavaName(String name) {
		StringBuilder sb = new StringBuilder(name.length());
		int i = 0;
		boolean toUpperCase = false;
		for (i = 0; i < name.length(); i++) {
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
}
