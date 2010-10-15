package net.arnx.jsonic;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class ClassUtil {
	private static WeakHashMap<ClassLoader, Map<String, Class<?>>> cache = new WeakHashMap<ClassLoader, Map<String, Class<?>>>();
	
	private ClassUtil() {
	}
	
	public static Class<?> findClass(String name) {
		return findClass(name, Thread.currentThread().getContextClassLoader());
	}
	
	public static Class<?> findClass(String name, Class<?> cls) {
		return findClass(name, cls.getClassLoader());
	}
	
	public static Class<?> findClass(String name, ClassLoader cl) {
		Map<String, Class<?>> instance;
		synchronized (cache) {
			ClassLoader current = cl;
			if (current == null) current =  ClassLoader.getSystemClassLoader();
			do {
				instance = cache.get(current);
				if (instance != null && instance.containsKey(name)) {
					current = null;
				} else {
					current = current.getParent();
				}
			} while (current != null);
			
			if (instance == null) {
				ClassLoader loader;
				Class<?> target = null;
				try {
					target = cl.loadClass(name);
					loader = target.getClassLoader();
				} catch (ClassNotFoundException e) {
					target = null;
					loader = cl;
				}
				if (loader == null) loader = ClassLoader.getSystemClassLoader();
				instance = cache.get(loader);
				if (instance == null) {
					instance = new HashMap<String, Class<?>>();
					cache.put(loader, instance);
				}
				instance.put(name, target);
			}
		}
		return instance.get(name);
	}
	
	public static boolean equals(String name, Class<?> cls) {
		Class<?> target = findClass(name, cls);
		return (target != null) && target.equals(cls);		
	}
	
	public static boolean isAssignableFrom(String name, Class<?> cls) {
		Class<?> target = findClass(name, cls);
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
}
