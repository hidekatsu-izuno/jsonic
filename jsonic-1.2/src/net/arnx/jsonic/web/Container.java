/*
 * Copyright 2007-2008 Hidekatsu Izuno
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
package net.arnx.jsonic.web;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.arnx.jsonic.JSON;

public class Container {
	public Boolean debug;
	public String init = "init";
	public String destroy = "destroy";
	public boolean namingConversion = true;
	
	protected ServletConfig config;
	protected ServletContext context;
	protected HttpServletRequest request;
	protected HttpServletResponse response;
	
	public void init(ServletConfig config) {
		this.config = config;
		this.context = config.getServletContext();
	}
	
	public void start(HttpServletRequest request, HttpServletResponse response) {
		this.request = request;
		this.response = response;
	}
	
	public Object getComponent(String className) throws Exception {
		Object o = findClass(className).newInstance();
		
		for (Field field : o.getClass().getFields()) {
			Class<?> c = field.getType();
			if (ServletContext.class.equals(c) && "application".equals(field.getName())) {
				field.set(o, context);
			} else if (ServletConfig.class.equals(c) && "config".equals(field.getName())) {
				field.set(o, config);
			} else if (HttpServletRequest.class.equals(c) && "request".equals(field.getName())) {
				field.set(o, request);
			} else if (HttpServletResponse.class.equals(c)	&& "response".equals(field.getName())) {
				field.set(o, response);
			} else if (HttpSession.class.equals(c) && "session".equals(field.getName())) {
				field.set(o, request.getSession(true));
			}
		}
		
		return o;
	}
	
	public Method getMethod(Object component, String methodName, List<?> params) throws NoSuchMethodException {
		if (params == null) params = Collections.emptyList();
		
		if (namingConversion) methodName = toLowerCamel(methodName);
		
		Class<?> c = component.getClass();
		
		Method method = null;
		Type[] lastPType = null;
		
		for (Method m : c.getMethods()) {
			if (Modifier.isStatic(m.getModifiers()) || m.isSynthetic() || m.isBridge()) {
				continue;
			}
			
			if (m.getName().equals(methodName)) {
				Type[] pTypes = m.getGenericParameterTypes();
				if (pTypes.length <= params.size()) {
					if (method == null || (params.size() - pTypes.length) < (params.size() - lastPType.length)) {
						method = m;
						lastPType = pTypes;
					} else if (pTypes.length == lastPType.length) {
						throw new IllegalStateException("too many methods found: " + toPrintString(c, method.getName(), params));
					}
				}
			}
		}
		
		if (method == null || limit(c, method)) {
			throw new NoSuchMethodException("method missing: " + toPrintString(c, methodName, params));
		}
		
		return method;
	}
	
	/**
	 * Called before invoking the target method.
	 * 
	 * @param component The target instance.
	 * @param method The invoking method.
	 * @param params The parameters before processing of the target method.
	 * @return The parameters before processing.
	 */
	public Object[] preinvoke(Object component, Method method, Object... params) throws Exception {
		return params;
	}
	
	public Object execute(JSON json, Object component, Method method, List<?> params) throws Exception {
		Object result = null;
		
		Method init = null;
		Method destroy = null;
		
		if (this.init != null || this.destroy != null) {
			boolean illegalInit = false;
			boolean illegalDestroy = false;
			
			for (Method m : component.getClass().getMethods()) {
				if (Modifier.isStatic(m.getModifiers())
						|| m.isSynthetic()
						|| m.isBridge()) {
					continue;
				}
				
				if (m.getName().equals(this.init)) {
					if (m.getReturnType().equals(void.class) && m.getParameterTypes().length == 0) {
						init = m;
					} else {
						illegalInit = true;
					}
					continue;
				}
				if (m.getName().equals(this.destroy)) {
					if (m.getReturnType().equals(void.class) && m.getParameterTypes().length == 0) {
						destroy = m;
					} else {
						illegalDestroy = true;
					}
					continue;
				}
			}
	
			if (illegalInit) this.debug("Notice: init method must have no arguments.");		
			if (illegalDestroy) this.debug("Notice: destroy method must have no arguments.");
		}
		
		Type[] argTypes = method.getGenericParameterTypes();
		Object[] args = new Object[argTypes.length];
		for (int i = 0; i < args.length; i++) {
			args[i] = json.convert((i < params.size()) ? params.get(i) : null, argTypes[i]);
		}
		if (this.isDebugMode()) {
			this.debug("Execute: " + toPrintString(component.getClass(), method.getName(), Arrays.asList(args)));
		}
		
		if (init != null) {
			if (this.isDebugMode()) {
				this.debug("Execute: " + toPrintString(component.getClass(), init.getName(), null));
			}
			init.invoke(component);
		}
		
		args = this.preinvoke(component, method, args);
		result = method.invoke(component, args);
		result = this.postinvoke(component, method, result);
		
		if (destroy != null) {
			if (this.isDebugMode()) {
				this.debug("Execute: " + toPrintString(component.getClass(), destroy.getName(), null));
			}
			destroy.invoke(component);
		}
		
		return result;
	}
	
	/**
	 * Called after invoked the target method.
	 * 
	 * @param component The target instance.
	 * @param method The invoked method.
	 * @param result The returned value of the target method call.
	 * @return The returned value after processed.
	 */
	public Object postinvoke(Object component, Method method, Object result) throws Exception {
		return result;
	}
	
	public void end(HttpServletRequest request, HttpServletResponse response) {
		request = null;
		response = null;
	}

	public void destory() {
	}
	
	
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		Class<?> c = null;
		try {
			c = Class.forName(name, true, Thread.currentThread().getContextClassLoader());
		} catch (ClassNotFoundException e) {
			try {
				c = Class.forName(name, true, this.getClass().getClassLoader());
			} catch (ClassNotFoundException e2) {
				c = Class.forName(name);				
			}
		}
		
		return c;
	}
	
	protected boolean limit(Class<?> c, Method method) {
		return method.getDeclaringClass().equals(Object.class);
	}

	public boolean isDebugMode() {
		return (debug != null) ? debug : false;
	}
	
	public void debug(String message) {
		debug(message, null);
	}
	
	public void debug(String message, Throwable e) {
		if (!isDebugMode()) return;
		
		if (e != null) {
			context.log(message, e);
		} else {
			context.log(message);
		}
	}
	
	public void error(String message, Throwable e) {
		if (e != null) {
			context.log(message, e);
		} else {
			context.log(message);
		}
	}
	
	
	private static String toPrintString(Class<?> c, String methodName, List<?> args) {
		StringBuilder sb = new StringBuilder(c.getName());
		sb.append('#').append(methodName).append('(');
		if (args != null) {
			String str = JSON.encode(args);
			sb.append(str, 1, str.length()-1);
		}
		sb.append(')');
		return sb.toString();
	}
	
	private static String toLowerCamel(String name) {
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