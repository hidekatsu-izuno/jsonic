package net.arnx.jsonic;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Map;
import java.util.HashMap;
import java.util.WeakHashMap;

final class ClassUtil {
	static final WeakHashMap<ClassLoader, Map<String, Class<?>>> cache = new WeakHashMap<ClassLoader, Map<String, Class<?>>>();
	
	static boolean accessible = true;
	
	private ClassUtil() {
	}
	
	public static Class<?> findClass(String name) {
		ClassLoader cl = null;
		try {
			if (accessible) {
				cl = Thread.currentThread().getContextClassLoader();
			}
		} catch (SecurityException e) {
			accessible = false;
		}
		Map<String, Class<?>> map;
		synchronized (cache) {
			map = cache.get(cl);
			
			if (map == null) {
				map = new HashMap<String, Class<?>>();
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
		if (sb.length() > 1 && Character.isUpperCase(sb.charAt(0)) && Character.isLowerCase(sb.charAt(1))) {
			sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
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
