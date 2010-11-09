package net.arnx.jsonic;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

final class ClassUtil {
	static final WeakHashMap<ClassLoader, Map<String, Class<?>>> cache = new WeakHashMap<ClassLoader, Map<String, Class<?>>>();
	
	static boolean accessible = true;
	
	private ClassUtil() {
	}
	
	public static ClassLoader getContextClassLoader() {
		ClassLoader cl = null;
		if (accessible) {
			try {
				cl = Thread.currentThread().getContextClassLoader();
			} catch (SecurityException e) {
				accessible = false;
			}
		}
		return cl;
	}
	
	public static Class<?> findClass(String name) {
		return findClass(name, true);
	}
	
	public static Class<?> findClass(String name, boolean useCache) {
		ClassLoader cl = getContextClassLoader();
		
		if (useCache) {
			Map<String, Class<?>> map;
			synchronized (cache) {
				map = cache.get(cl);
				
				if (map == null) {
					map = new LinkedHashMap<String, Class<?>>(16, 0.75f, true) {
						protected boolean removeEldestEntry(Map.Entry<String, Class<?>> eldest) {
							return size() > 1024;
						};
					};
					cache.put(cl, map);
				}
			}
			synchronized (map) {
				if (!map.containsKey(name)) {
					Class<?> target;
					try {
						if (cl != null) {
							target = cl.loadClass(name);
						} else {
							target = Class.forName(name);
						}
					} catch (ClassNotFoundException e) {
						target = null;
					}
					map.put(name, target);
				}
			}
			return map.get(name);
		} else {
			Class<?> target;
			try {
				if (cl != null) {
					target = cl.loadClass(name);
				} else {
					target = Class.forName(name);
				}
			} catch (ClassNotFoundException e) {
				target = null;
			}
			return target;
		}
	}
	
	public static boolean equals(String name, Class<?> cls) {
		Class<?> target = findClass(name);
		return (target != null) && target.equals(cls);		
	}
	
	public static boolean isAssignableFrom(String name, Class<?> cls) {
		Class<?> target = findClass(name);
		return (target != null) && target.isAssignableFrom(cls);		
	}
	
	public static Class<?> getRawType(Type t) {
		if (t instanceof Class<?>) {
			return (Class<?>)t;
		} else if (t instanceof ParameterizedType) {
			return (Class<?>)((ParameterizedType)t).getRawType();
		} else if (t instanceof GenericArrayType) {
			Class<?> cls = null;
			try {
				cls = Array.newInstance(getRawType(((GenericArrayType)t).getGenericComponentType()), 0).getClass();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			return cls;
		} else if (t instanceof WildcardType) {
			Type[] types = ((WildcardType)t).getUpperBounds();
			return (types.length > 0) ? getRawType(types[0]) : Object.class;
		} else {
			return Object.class;
		}
	}
	
	public static Type resolveTypeVariable(TypeVariable<?> type, ParameterizedType parent) {
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
	
	public static String toUpperCamel(String name) {
		StringBuilder sb = new StringBuilder(name.length());
		boolean toUpperCase = true;
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
		return sb.toString();
	}
	
	public static String toLowerCamel(String name) {
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
	
	public static void clear() {
		synchronized (cache) {
			cache.clear();
		}
	}
}

interface Property extends Comparable<Property> {
	public String getName();
	public JSONHint getHint();
	public Object get(Object o) throws Exception;
	public void set(Object o, Object value) throws Exception;
	public Class<?> getType(Type type);
	public Type getGenericType(Type type);
}

class FieldProperty implements Property {
	String name;
	Field field;
	JSONHint hint;
	Type gtype;
	Class<?> type;
	
	public FieldProperty(String name, Field field, JSONHint hint) {
		this.name = name;
		this.field = field;
		this.hint = hint;
		this.field.setAccessible(true);
	}
	
	public String getName() {
		return name;
	}
	
	public JSONHint getHint() {
		return hint;
	}
	
	public Object get(Object o) throws Exception {
		return field.get(o);
	}
	
	public void set(Object o, Object value) throws Exception {
		field.set(o, value);
	}
	
	public Type getGenericType(Type target) {
		init(target);
		return gtype;
	}
	
	public Class<?> getType(Type target) {
		init(target);
		return type;
	}
	
	private void init(Type target) {
		gtype = field.getGenericType();
		type =  field.getType();
		if (gtype instanceof TypeVariable<?> && target instanceof ParameterizedType) {
			gtype = ClassUtil.resolveTypeVariable((TypeVariable<?>)gtype, (ParameterizedType)target);
			type = ClassUtil.getRawType(gtype);
		}
	}
	
	@Override
	public int compareTo(Property prop) {
		return name.compareTo(prop.getName());
	}
}

class MethodProperty implements Property {
	String name;
	Method method;
	JSONHint hint;
	Type gtype;
	Class<?> type;
	
	public MethodProperty(String name, Method method, JSONHint hint) {
		this.name = name;
		this.method = method;
		this.hint = hint;
		this.method.setAccessible(true);
	}
	
	public String getName() {
		return name;
	}
	
	public JSONHint getHint() {
		return hint;
	}
	
	public Object get(Object o) throws Exception {
		return method.invoke(o);
	}
	
	public void set(Object o, Object value) throws Exception {
		method.invoke(o, value);
	}
	
	public Type getGenericType(Type target) {
		init(target);
		return gtype;
	}
	
	public Class<?> getType(Type target) {
		init(target);
		return type;
	}
	
	private void init(Type target) {
		gtype = method.getGenericParameterTypes()[0];
		type = method.getParameterTypes()[0];
		if (gtype instanceof TypeVariable<?> && target instanceof ParameterizedType) {
			gtype = ClassUtil.resolveTypeVariable((TypeVariable<?>)gtype, (ParameterizedType)target);
			type = ClassUtil.getRawType(gtype);
		}
	}
	
	@Override
	public int compareTo(Property prop) {
		return name.compareTo(prop.getName());
	}
}