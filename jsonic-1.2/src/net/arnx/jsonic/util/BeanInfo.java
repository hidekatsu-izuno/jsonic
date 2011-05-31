package net.arnx.jsonic.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class BeanInfo {
	private static final Map<ClassLoader, Map<Class<?>, BeanInfo>> cache = 
		new WeakHashMap<ClassLoader, Map<Class<?>, BeanInfo>>();
	
	public static BeanInfo get(Class<?> cls) {
		return get(cls, true);
	}
	
	public static BeanInfo get(Class<?> cls, boolean create) {
		BeanInfo info = null;
		synchronized(cache) {
			Map<Class<?>, BeanInfo> map = cache.get(cls.getClassLoader());
			if (map == null) {
				map = new ConcurrentHashMap<Class<?>, BeanInfo>();
				cache.put(cls.getClassLoader(), map);
			} else {
				info = map.get(cls);
			}
			
			if (info == null && create) {
				info = new BeanInfo(cls);
				map.put(cls, info);
			}
		}
		return info;
	}
	
	public static void clear() {
		cache.clear();
	}
	
	private Class<?> type;
	private Type gtype;
	private Map<String, Property> props;
	
	private BeanInfo(Class<?> cls) {
		type = cls;
		gtype = cls;
		props = new LinkedHashMap<String, Property>();
		for (Field f : cls.getFields()) {
			if (Modifier.isStatic(f.getModifiers()) 
					|| f.isSynthetic()) {
				continue;
			}
			
			f.setAccessible(true);
			Property prop = new Property(this, f.getName());
			prop.reader = f;
			if (!Modifier.isFinal(f.getModifiers())) prop.writer = f;
			props.put(f.getName(), prop);
		}
		
		for (Method m : cls.getMethods()) {
			if (Modifier.isStatic(m.getModifiers())
					|| m.isSynthetic()
					|| m.isBridge()) {
				continue;
			}
			String name = m.getName();
			Class<?>[] paramTypes = m.getParameterTypes();
			Class<?> returnType = m.getReturnType();
			
			int type = -1;
			if (name.startsWith("get") 
					&& name.length() > 3 && !Character.isLowerCase(name.charAt(3))
					&& paramTypes.length == 0 && !returnType.equals(void.class)) {
				type = 1;
				name = name.substring(3);
			} else if (name.startsWith("is") 
					&& name.length() > 2 && !Character.isLowerCase(name.charAt(2))
					&& paramTypes.length == 0 && !returnType.equals(void.class)) {
				type = 1;
				name = name.substring(2);				
			} else if (name.startsWith("set") 
					&& name.length() > 3 && !Character.isLowerCase(name.charAt(3))
					&& paramTypes.length == 1 && !paramTypes[0].equals(void.class)) {
				type = 2;
				name = name.substring(3);
			} else {
				continue;
			}
			
			if (name.length() < 2 || !Character.isUpperCase(name.charAt(1))){
				char[] chars = name.toCharArray();
				chars[0] = Character.toLowerCase(chars[0]);
				name = new String(chars);
			}
			
			m.setAccessible(true);
			
			Property prop = props.get(name);
			if (prop == null) {
				prop = new Property(this, name);
				props.put(name, prop);
			}
			
			if (type == 1) {
				prop.reader = m;
			} else {
				prop.writer = m;
			}
		}
		
		for (Property prop : props.values()) {
			prop.init();
		}
	}
	
	public Class<?> getType() {
		return type;
	}
	
	public Type getGenericType() {
		return gtype;
	}
	
	public Property getProperty(String name) {
		return props.get(name);
	}
	
	public Collection<Property> getProperties() {
		return props.values();
	}

	@Override
	public String toString() {
		return "BeanInfo [props=" + props + "]";
	}
}
