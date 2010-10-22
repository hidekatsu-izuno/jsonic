package net.arnx.jsonic;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.WeakHashMap;

final class ClassUtil {
	static final WeakHashMap<ClassLoader, Map<String, ClassInfo>> cache = new WeakHashMap<ClassLoader, Map<String, ClassInfo>>();
	
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
		Map<String, ClassInfo> map;
		synchronized (cache) {
			map = cache.get(cl);
			
			if (map == null) {
				map = new LinkedHashMap<String, ClassInfo>(16, 0.75f, true) {
					protected boolean removeEldestEntry(Map.Entry<String, ClassInfo> eldest) {
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
				map.put(name, (target != null) ? new ClassInfo(target) : null);
			}
		}
		ClassInfo info = map.get(name);
		return (info != null) ? info.getTargetClass() : null;
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
	
	static class ClassInfo {
		private Class<?> cls;
		private LinkedList<Member> members;
		
		public ClassInfo(Class<?> cls) {
			this.cls = cls;
		}
		
		public Class<?> getTargetClass() {
			return cls;
		}
		
		public Field getField(String name) {
			init();
			return null;
		}
		
		public Method getGetMethod(String name) {
			init();
			return null;
		}
		
		public Method getSetMethod(String name) {
			init();
			return null;
		}
		
		public Method findMethod(String name, Object... params) {
			init();
			return null;
		}
		
		private void init() {
			synchronized(this) {
				if (members == null) {
					members = new LinkedList<Member>();
					for (Field field : cls.getFields()) {
						field.setAccessible(true);
					}
					for (Method method : cls.getMethods()) {
						method.setAccessible(true);
					}
				}
			}
		}
	}
}
