/*
 * Copyright 2007-2011 Hidekatsu Izuno
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
package net.arnx.jsonic.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class BeanInfo {
	private static final Map<ClassLoader, Map<Class<?>, BeanInfo>> cache = 
		new WeakHashMap<ClassLoader, Map<Class<?>, BeanInfo>>();
	
	public static BeanInfo get(Class<?> cls) {
		BeanInfo info = null;
		synchronized(cache) {
			Map<Class<?>, BeanInfo> map = cache.get(cls.getClassLoader());
			if (map == null) {
				map = new LinkedHashMap<Class<?>, BeanInfo>(16, 0.75f, true) {
					protected boolean removeEldestEntry(Map.Entry<Class<?>, BeanInfo> eldest) {
						return size() > 1024;
					};
				};
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
	private Map<String, PropertyInfo> sprops = new LinkedHashMap<String, PropertyInfo>();
	private Map<String, MethodInfo> smethods = new LinkedHashMap<String, MethodInfo>();
	private Map<String, PropertyInfo> props = new LinkedHashMap<String, PropertyInfo>();
	private Map<String, MethodInfo> methods = new LinkedHashMap<String, MethodInfo>();
	
	private BeanInfo(Class<?> cls) {
		type = cls;
		
		String name = cls.getSimpleName();
		for (Constructor<?> con : cls.getConstructors()) {
			if (con.isSynthetic()) {
				continue;
			}
			
			MethodInfo mi = smethods.get(name);
			if (mi == null) {
				mi = new MethodInfo(cls, name, null);
				smethods.put(name, mi);
			}
			mi.members.add(con);			
		}
		
		for (Field f : cls.getFields()) {
			if (f.isSynthetic()) {
				continue;
			}
			
			Map<String, PropertyInfo> cprops = Modifier.isStatic(f.getModifiers()) ? sprops : props;
			
			name = f.getName();
			f.setAccessible(true);
			cprops.put(name, new PropertyInfo(cls, name, f, null, null));
		}
		
		for (Method m : cls.getMethods()) {
			if (m.isSynthetic() || m.isBridge()) {
				continue;
			}
			
			name = m.getName();
			Class<?>[] paramTypes = m.getParameterTypes();
			Class<?> returnType = m.getReturnType();
			
			Map<String, MethodInfo> cmethods = Modifier.isStatic(m.getModifiers()) ? smethods : methods;
			
			MethodInfo mi = cmethods.get(name);
			if (mi == null) {
				mi = new MethodInfo(cls, name, null);
				cmethods.put(name, mi);
			}
			mi.members.add(m);
			
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
			
			Map<String, PropertyInfo> cprops = Modifier.isStatic(m.getModifiers()) ? sprops : props;
			
			PropertyInfo prop = cprops.get(name);
			if (prop == null) {
				prop = new PropertyInfo(cls, name, null, null, null);
				cprops.put(name, prop);
			}
			
			m.setAccessible(true);
			if (type == 1) {
				prop.readMethod = m;
			} else {
				prop.writeMethod = m;
			}
		}
	}
	
	public Object newInstance() {
		try {
			Constructor<?> target = type.getConstructor();
			target.setAccessible(true);
			return target.newInstance();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	public Class<?> getType() {
		return type;
	}
	
	public PropertyInfo getStaticProperty(String name) {
		return sprops.get(name);
	}
	
	public PropertyInfo getProperty(String name) {
		return props.get(name);
	}
	
	public MethodInfo getStaticMethod(String name) {
		return smethods.get(name);
	}
	
	public MethodInfo getMethod(String name) {
		return methods.get(name);
	}
	
	public Collection<PropertyInfo> getProperties() {
		return props.values();
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
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "BeanInfo [staticProperties = " + sprops 
			+ ", properties = " + props
			+ ", staticMethods = " + smethods
			+ ", methods = " + methods
			+ "]";
	}
}
