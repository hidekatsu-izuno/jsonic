package net.arnx.jsonic;

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
		ClassLoader cl = getContextClassLoader();
		
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
	}
	
	public static boolean equals(String name, Class<?> cls) {
		Class<?> target = findClass(name);
		return (target != null) && target.equals(cls);		
	}
	
	public static boolean isAssignableFrom(String name, Class<?> cls) {
		Class<?> target = findClass(name);
		return (target != null) && target.isAssignableFrom(cls);		
	}
	
	public static void clear() {
		synchronized (cache) {
			cache.clear();
		}
	}
}
