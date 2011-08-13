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

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.arnx.jsonic.JSON;
import net.arnx.jsonic.util.ClassUtil;

public class Container {
	public Boolean debug;
	public String init = "init";
	public String destroy = "destroy";
	public String encoding;
	public Boolean expire;
	public boolean namingConversion = true;
	public Class<? extends JSON> processor;
	
	protected ServletConfig config;
	protected ServletContext context;
	protected HttpServlet servlet;
	protected HttpServletRequest request;
	protected HttpServletResponse response;
	
	public void init(HttpServlet servlet) throws ServletException {
		this.servlet = servlet;
		this.config = servlet.getServletConfig();
		this.context = servlet.getServletContext();
	}
	
	public void start(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.request = request;
		this.response = response;
		
		String encoding = this.encoding;
		Boolean expire = this.expire;
				
		GatewayFilter.Config gconfig = (GatewayFilter.Config)request.getAttribute(GatewayFilter.GATEWAY_KEY);
		if (gconfig != null) {
			if (encoding == null) encoding = gconfig.encoding;
			if (expire == null) expire = gconfig.expire;
		}
		
		if (encoding == null) encoding = "UTF-8";
		if (expire == null) expire = true;
		
		// set encoding
		if (encoding != null) {
			request.setCharacterEncoding(encoding);
			response.setCharacterEncoding(encoding);
		}
		
		// set expiration
		if (expire != null && expire) {
			response.setHeader("Cache-Control", "no-cache");
			response.setHeader("Pragma", "no-cache");
			response.setHeader("Expires", "Tue, 29 Feb 2000 12:00:00 GMT");
		}
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
		
		if (namingConversion) methodName = ClassUtil.toLowerCamel(methodName);
		
		if (methodName.equals(init) || methodName.equals(destroy)) {
			debug("Method name is same init or destroy method name.");
			return null;
		}
		
		Class<?> c = component.getClass();
		
		Method method = null;
		int length = -1;
		
		Method vmethod = null;
		int vlength = -1;
		
		loop:for (Method cmethod : c.getMethods()) {
			if (Modifier.isStatic(cmethod.getModifiers())
					|| cmethod.isSynthetic() || cmethod.isBridge()) {
				continue loop;
			}
			
			if (cmethod.getName().equals(methodName)) {
				Method tmethod = method;
				int tlength = length;
				int clength = cmethod.getParameterTypes().length;
				
				if (cmethod.isVarArgs()) {
					tmethod = vmethod;
					tlength = vlength;
					clength--;
				}
				
				if (clength > params.size() || clength < tlength) {
					continue loop;
				}
				
				if (clength > tlength) {
					if (cmethod.isVarArgs()) {
						vmethod = cmethod;
						vlength = clength;
					} else {
						method = cmethod;
						length = clength;
					}
					continue loop;
				}
				
				int tpoint = calcurateDistance(tmethod, params);
				int cpoint = calcurateDistance(cmethod, params);
				if (cpoint > tpoint) {
					if (cmethod.isVarArgs()) {
						vmethod = cmethod;
						vlength = clength;
					} else {
						method = cmethod;
						length = clength;
					}
				} else if (tpoint == cpoint) {
					if (cmethod.isVarArgs()) {
						vmethod = null;
					} else {
						method = null; 
					}
				}
				continue loop;
			}
		}
		
		if (vmethod != null) {
			if (method == null) {
				method = vmethod;
			} else {
				int point = calcurateDistance(method, params);
				int vpoint = calcurateDistance(vmethod, params);
				if (vpoint > point) {
					method = vmethod;
				}
			}
		}
		
		if (method == null || limit(c, method)) {
			debug("method missing: " + toPrintString(c, methodName, params));
			return null;
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
			if (i == args.length-1 && method.isVarArgs()) {
				args[i] = json.convert(params.subList((i < params.size()) ? i : params.size(), params.size()), argTypes[i]);
			} else {
				args[i] = json.convert((i < params.size()) ? params.get(i) : null, argTypes[i]);
			}
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
	
	public void end(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
			context.log("[DEBUG] " + message, e);
		} else {
			context.log("[DEBUG] " + message);
		}
	}
	
	public void warn(String message) {
		warn(message, null);
	}
	
	public void warn(String message, Throwable e) {
		if (!isDebugMode()) return;
		
		if (e != null) {
			context.log("[WARNING] " + message, e);
		} else {
			context.log("[WARNING] " + message);
		}
	}
	
	public void error(String message, Throwable e) {
		if (e != null) {
			context.log("[ERROR] " + message, e);
		} else {
			context.log("[ERROR] " + message);
		}
	}
	
	JSON createJSON(Locale locale) throws ServletException  {
		try {
			JSON json = (processor != null) ? processor.newInstance() : new JSON() {
				@Override
				protected boolean ignore(Context context, Class<?> target, Member member) {
					return member.getDeclaringClass().equals(Throwable.class)
						|| super.ignore(context, target, member);
				}
			};
			json.setLocale(locale);
			return json;
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}
	
	static boolean isJSONType(String contentType) {
		if (contentType != null) {
			contentType = contentType.toLowerCase();
			return (contentType.equals("application/json") || contentType.startsWith("application/json;"));
		}
		return false;
	}
	
	static int calcurateDistance(Method m, List<?> params) {
		Class<?>[] types = m.getParameterTypes();
		
		if (m.isVarArgs()) {
			Class<?> vtype = types[types.length-1].getComponentType();
			Class<?>[] vtypes = new Class<?>[params.size()];
			System.arraycopy(types, 0, vtypes, 0, types.length-1);
			for (int i = types.length-1; i < vtypes.length; i++) {
				vtypes[i] = vtype;
			}
			types = vtypes;
		}
		
		int point = 0;
		for (int i = 0; i < types.length; i++) {
			Object param = params.get(i);
			if (param == null) {
				continue;
			} else if (param instanceof String) {
				if (String.class.equals(types[i])) {
					point += 10;
				} else if (types[i].isAssignableFrom(String.class)) {
					point += 9;
				} else if (types[i].isPrimitive()
						|| Boolean.class.equals(types[i])
						|| CharSequence.class.isAssignableFrom(types[i])
						|| Character.class.isAssignableFrom(types[i])
						|| Number.class.isAssignableFrom(types[i])
						|| Date.class.isAssignableFrom(types[i])
						|| Calendar.class.isAssignableFrom(types[i])
						|| Locale.class.equals(types[i])
						|| TimeZone.class.equals(types[i])
						|| Pattern.class.equals(types[i])
						|| Charset.class.equals(types[i])
						|| URI.class.equals(types[i])
						|| URL.class.equals(types[i])
						|| UUID.class.equals(types[i])) {
					point += 8;
				} else if (Object.class.equals(types[i])) {
					point += 1;
				}
			} else if (param instanceof Number) {
				if (byte.class.equals(types[i])
						|| short.class.equals(types[i])
						|| int.class.equals(types[i])
						|| long.class.equals(types[i])
						|| double.class.equals(types[i])
						|| Number.class.isAssignableFrom(types[i])) {
					point += 10;
				} else if (types[i].isAssignableFrom(BigDecimal.class)) {
					point += 9;
				} else if (types[i].isPrimitive()
						|| CharSequence.class.isAssignableFrom(types[i])
						|| Character.class.equals(types[i])
						|| Boolean.class.equals(types[i])
						|| Date.class.isAssignableFrom(types[i])
						|| Calendar.class.isAssignableFrom(types[i])) {
					point += 8;
				} else if (Object.class.equals(types[i])) {
					point += 1;
				}
			} else if (param instanceof Boolean) {
				if (boolean.class.equals(types[i])
						|| Boolean.class.equals(types[i])
						) {
					point += 10;
				} else if (types[i].isAssignableFrom(Boolean.class)) {
					point += 9;
				} else if (types[i].isPrimitive()
						|| Number.class.isAssignableFrom(types[i])
						|| CharSequence.class.isAssignableFrom(types[i])
						|| Character.class.equals(types[i])) {
					point += 8;
				} else if (Object.class.equals(types[i])) {
					point += 1;
				}
			} else if (param instanceof List<?>) {
				if (Collection.class.isAssignableFrom(types[i])
						|| types[i].isArray()) {
					point += 10;
				} else if (types[i].isAssignableFrom(ArrayList.class)) {
					point += 9;
				} else if (Map.class.isAssignableFrom(types[i])) {
					point += 8;
				} else if (Object.class.equals(types[i])) {
					point += 1;
				}
			} else if (param instanceof Map<?, ?>) {
				if (Map.class.isAssignableFrom(types[i])) {
					point += 10;
				} else if (types[i].isAssignableFrom(LinkedHashMap.class)) {
					point += 9;
				} else if (List.class.isAssignableFrom(types[i])
						|| types[i].isArray()) {
					point += 8;
				} else if (!(types[i].isPrimitive()
						|| Boolean.class.equals(types[i])
						|| CharSequence.class.isAssignableFrom(types[i])
						|| Character.class.isAssignableFrom(types[i])
						|| Number.class.isAssignableFrom(types[i])
						|| Date.class.isAssignableFrom(types[i])
						|| Calendar.class.isAssignableFrom(types[i])
						|| Locale.class.equals(types[i])
						|| TimeZone.class.equals(types[i])
						|| Pattern.class.equals(types[i])
						|| Charset.class.equals(types[i])
						|| URI.class.equals(types[i])
						|| URL.class.equals(types[i])
						|| UUID.class.equals(types[i]))) {
					point += 8;
				} else if (Object.class.equals(types[i])) {
					point += 1;
				}
			}
		}
		return point;
	}
	
	static String toPrintString(Class<?> c, String methodName, List<?> args) {
		StringBuilder sb = new StringBuilder(c.getName());
		sb.append('#').append(methodName).append('(');
		if (args != null) {
			String str = JSON.encode(args);
			sb.append(str, 1, str.length()-1);
		}
		sb.append(')');
		return sb.toString();
	}
	
	
	@SuppressWarnings("unchecked")
	static <T> T cast(Object o) {
		return (T)o;
	}
}