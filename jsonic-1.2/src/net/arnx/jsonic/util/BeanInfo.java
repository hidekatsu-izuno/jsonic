package net.arnx.jsonic.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class BeanInfo {
	private static final Map<ClassLoader, Map<Class<?>, BeanInfo>> cache = 
		new WeakHashMap<ClassLoader, Map<Class<?>, BeanInfo>>();
	
	public static BeanInfo get(Class<?> cls) {
		BeanInfo info = null;
		synchronized(cache) {
			Map<Class<?>, BeanInfo> map = cache.get(cls.getClassLoader());
			if (map == null) {
				map = new ConcurrentHashMap<Class<?>, BeanInfo>();
				cache.put(cls.getClassLoader(), map);
			} else {
				info = map.get(cls);
			}
			
			if (info == null) {
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
	private Map<String, Property> props = new LinkedHashMap<String, Property>();
	
	private BeanInfo(Class<?> cls) {
		type = cls;
		for (Field f : cls.getFields()) {
			if (Modifier.isStatic(f.getModifiers()) 
					|| f.isSynthetic()) {
				continue;
			}
			
			f.setAccessible(true);
			props.put(f.getName(), new Property(cls, f.getName(), f, null, null));
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
			
			Property prop = props.get(name);
			if (prop == null) {
				prop = new Property(cls, name, null, null, null);
				props.put(name, prop);
			}
			
			m.setAccessible(true);
			if (type == 1) {
				prop.readMethod = m;
			} else {
				prop.writeMethod = m;
			}
		}
	}
	
	public Class<?> getType() {
		return type;
	}
	
	public Property getProperty(String name) {
		return props.get(name);
	}
	
	public Collection<Property> getProperties() {
		return props.values();
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
	
	public static int hashCode(Object target) {
		if (target == null) return 0;
		final int prime = 31;
		int result = 1;
		
		Class<?> current = target.getClass();
		do {
			for (Field f : current.getDeclaredFields()) {
				if (Modifier.isStatic(f.getModifiers()) 
						|| Modifier.isTransient(f.getModifiers())
						|| f.isSynthetic()) {
					continue;
				}
				
				Object self;
				try {
					f.setAccessible(true);
					self = f.get(target);
				} catch (IllegalAccessException e) {
					throw new IllegalStateException(e);
				}
				if (self == null) {
					result = prime * result + 0;
				} else if (current.isArray()) {
					if (current.equals(boolean[].class)) {
						result = prime * result + Arrays.hashCode((boolean[])self);
					} else if (current.equals(char[].class)) {
						result = prime * result + Arrays.hashCode((char[])self);
					} else if (current.equals(byte[].class)) {
						result = prime * result + Arrays.hashCode((byte[])self);
					} else if (current.equals(short[].class)) {
						result = prime * result + Arrays.hashCode((short[])self);
					} else if (current.equals(int[].class)) {
						result = prime * result + Arrays.hashCode((int[])self);
					} else if (current.equals(long[].class)) {
						result = prime * result + Arrays.hashCode((long[])self);
					} else if (current.equals(float[].class)) {
						result = prime * result + Arrays.hashCode((float[])self);
					} else if (current.equals(double[].class)) {
						result = prime * result + Arrays.hashCode((double[])self);
					} else {
						result = prime * result + Arrays.hashCode((Object[])self);
					}
				} else {
					result = prime * result + self.hashCode();;
				}
			}
			current = current.getSuperclass();
		} while (!Object.class.equals(current));
		
		return result;
	}
	
	public static boolean equals(Object target, Object o) {
		if (target == o) return true;
		if (target == null || o == null) return false;
		if (!target.getClass().equals(o.getClass())) return false;
		
		Class<?> current = target.getClass();
		do {
			for (Field f : current.getDeclaredFields()) {
				if (Modifier.isStatic(f.getModifiers()) 
						|| Modifier.isTransient(f.getModifiers())
						|| f.isSynthetic()) {
					continue;
				}
				
				Object self;
				Object other;
				try {
					f.setAccessible(true);
					self = f.get(target);
					other = f.get(o);
				} catch (IllegalAccessException e) {
					throw new IllegalStateException(e);
				}
				if (self == null) {
					if (other != null) return false;
				} else if (current.isArray()) {
					if (current.equals(boolean[].class)) {
						if (!Arrays.equals((boolean[])self, (boolean[])other)) return false;
					} else if (current.equals(char[].class)) {
						if (!Arrays.equals((char[])self, (char[])other)) return false;
					} else if (current.equals(byte[].class)) {
						if (!Arrays.equals((byte[])self, (byte[])other)) return false;
					} else if (current.equals(short[].class)) {
						if (!Arrays.equals((short[])self, (short[])other)) return false;
					} else if (current.equals(int[].class)) {
						if (!Arrays.equals((int[])self, (int[])other)) return false;
					} else if (current.equals(long[].class)) {
						if (!Arrays.equals((long[])self, (long[])other)) return false;
					} else if (current.equals(float[].class)) {
						if (!Arrays.equals((float[])self, (float[])other)) return false;
					} else if (current.equals(double[].class)) {
						if (!Arrays.equals((double[])self, (double[])other)) return false;
					} else {
						if (!Arrays.equals((Object[])self, (Object[])other)) return false;
					}
				} else if (!self.equals(other)) {
					return false;
				}
			}
			current = current.getSuperclass();
		} while (!Object.class.equals(current));
		
		return true;
	}
	
	public static String toString(Object target) {
		if (target == null) return "null";
		
		BeanInfo info = BeanInfo.get(target.getClass());
		
		StringBuilder sb = new StringBuilder(10 * info.getProperties().size() + 20);
		sb.append(target.getClass().getSimpleName()).append(" [");
		boolean first = true;
		for (Property prop : info.getProperties()) {
			if (!prop.isReadable() || prop.getName().equals("class")) continue;
			
			if (!first) {
				sb.append(", ");
			} else {
				first = false;
			}
			sb.append(prop.getName()).append("=");
			try {
				Object value = prop.get(target);
				if (value.getClass().isArray()) {
					if (value instanceof boolean[]) {
						Arrays.toString((boolean[])value);
					} else if (value instanceof char[]) {
						Arrays.toString((char[])value);
					} else if (value instanceof byte[]) {
						Arrays.toString((byte[])value);
					} else if (value instanceof short[]) {
						Arrays.toString((short[])value);
					} else if (value instanceof int[]) {
						Arrays.toString((int[])value);
					} else if (value instanceof long[]) {
						Arrays.toString((long[])value);
					} else if (value instanceof float[]) {
						Arrays.toString((float[])value);
					} else if (value instanceof double[]) {
						Arrays.toString((double[])value);
					} else {
						Arrays.toString((Object[])value);
					}
				} else {
					sb.append(value);
				}
			} catch (Exception e) {
				sb.append("?");
			}
		}
		sb.append("]");
		
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return (type == null) ? 0 : type.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BeanInfo other = (BeanInfo) obj;
		if (props == null) {
			if (other.props != null)
				return false;
		} else if (!props.equals(other.props))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "BeanInfo [props=" + props + "]";
	}
}
