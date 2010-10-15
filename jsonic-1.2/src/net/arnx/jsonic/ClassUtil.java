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
	private static WeakHashMap<ClassLoader, ClassUtil> cache = new WeakHashMap<ClassLoader, ClassUtil>();
	
	private Map<String, Class<?>> map = new HashMap<String, Class<?>>();
	
	private ClassUtil() {
	}
	
	public static Class<?> findClass(String name) {
		return findClass(name, Thread.currentThread().getContextClassLoader());
	}
	
	public static Class<?> findClass(String name, Class<?> cls) {
		return findClass(name, cls.getClassLoader());
	}
	
	public static Class<?> findClass(String name, ClassLoader cl) {
		if (cl == null) cl = ClassLoader.getSystemClassLoader();

		ClassUtil cc;
		synchronized (cl) {
			cc = cache.get(cl);
			if (cc == null) {
				cc = new ClassUtil();
				cache.put(cl, cc);
			}
		}
		
		Class<?> target;
		synchronized (cc) {
			if (!cc.map.containsKey(name)) {
				try {
					target = cl.loadClass(name);
				} catch (ClassNotFoundException e) {
					target = null;
				}
				cc.map.put(name, target);
			} else {
				target = cc.map.get(name);
			}
		}
		return target;
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
