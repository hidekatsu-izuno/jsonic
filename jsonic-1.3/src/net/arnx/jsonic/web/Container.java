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
package net.arnx.jsonic.web;

import java.io.IOException;
import java.lang.reflect.Field;
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
import net.arnx.jsonic.JSONHint;
import net.arnx.jsonic.NamingStyle;
import net.arnx.jsonic.util.BeanInfo;
import net.arnx.jsonic.util.ClassUtil;
import net.arnx.jsonic.util.PropertyInfo;

public class Container {
	public Boolean debug;
	public String init = "init";
	public String destroy = "destroy";
	public String encoding;
	public Boolean expire;
	public boolean namingConversion = true;
	
	@JSONHint(anonym = "class")
	public ProcessorConfig processor;
	
	protected ServletConfig config;
	protected ServletContext context;
	protected HttpServlet servlet;
		
	public void init(HttpServlet servlet) throws ServletException {
		this.servlet = servlet;
		this.config = servlet.getServletConfig();
		this.context = servlet.getServletContext();
	}
	
	public void start(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
			Class<?> cls = field.getType();
			if ("config".equals(field.getName()) && ServletConfig.class.equals(cls)) {
				field.set(o, ExternalContext.getConfig());
			} else if ("application".equals(field.getName()) && ServletContext.class.equals(cls)) {
				field.set(o, ExternalContext.getApplication());
			} else if ("request".equals(field.getName()) && HttpServletRequest.class.equals(cls)) {
				field.set(o, ExternalContext.getRequest());
			} else if ("response".equals(field.getName()) && HttpServletResponse.class.equals(cls)) {
				field.set(o, ExternalContext.getResponse());
			} else if ("session".equals(field.getName()) && HttpSession.class.equals(cls)) {
				field.set(o, ExternalContext.getSession());
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
		Class<?>[] types = null;
		
		Method vmethod = null;
		Class<?>[] vtypes = null;
		
		for (Method cmethod : c.getMethods()) {
			if (Modifier.isStatic(cmethod.getModifiers())
					|| cmethod.isSynthetic()
					|| cmethod.isBridge()
					|| !cmethod.getName().equals(methodName)) {
				continue;
			}
			
			Class<?>[] ctypes = cmethod.getParameterTypes();
			
			if (cmethod.isVarArgs()) {
				if (ctypes.length-1 > params.size()) {
					continue;
				}
				
				Class<?> vtype = ctypes[ctypes.length-1].getComponentType();
				Class<?>[] tmp = new Class<?>[params.size()];
				System.arraycopy(tmp, 0, ctypes, 0, ctypes.length-1);
				for (int i = ctypes.length-1; i < tmp.length; i++) {
					tmp[i] = vtype;
				}
				ctypes = tmp;
				
				if (vmethod == null || ctypes.length > vtypes.length) {
					vmethod = cmethod;
					vtypes = ctypes;
				} else {
					int vpoint = calcurateDistance(vtypes, params);
					int cpoint = calcurateDistance(ctypes, params);
					if (cpoint > vpoint) {
						vmethod = cmethod;
						vtypes = ctypes;
					} else if (cpoint == vpoint) {
						vmethod = null;
					}
				}
			} else {
				if (ctypes.length > params.size()
						|| (types != null && ctypes.length < types.length)) {
					continue;
				}
				
				if (method == null || ctypes.length > types.length) {
					method = cmethod;
					types = ctypes;
				} else {
					int point = calcurateDistance(types, params);
					int cpoint = calcurateDistance(ctypes, params);
					if (cpoint > point) {
						method = cmethod;
						types = ctypes;
					} else if (cpoint == point) {
						method = null;
					}
				}
			}
		}
		
		if (vmethod != null) {
			if (method == null) {
				method = vmethod;
			} else {
				int point = calcurateDistance(types, params);
				int vpoint = calcurateDistance(vtypes, params);
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
			if (!init.isAccessible()) init.setAccessible(true);
			init.invoke(component);
		}
		
		args = this.preinvoke(component, method, args);
		
		if (!method.isAccessible()) method.setAccessible(true);
		result = method.invoke(component, args);
		
		result = this.postinvoke(component, method, result);
		
		if (destroy != null) {
			if (this.isDebugMode()) {
				this.debug("Execute: " + toPrintString(component.getClass(), destroy.getName(), null));
			}
			if (!destroy.isAccessible()) destroy.setAccessible(true);
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
	
	public void exception(Exception e, HttpServletRequest request, HttpServletResponse response) throws ServletException {
	}
	
	public void end(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
	
	public Object getErrorData(Throwable cause) {
		Map<String, Object> data = null;
		for (PropertyInfo pi : BeanInfo.get(cause.getClass()).getProperties()) {
			if (pi.getReadMember().getDeclaringClass().equals(Throwable.class)
					|| pi.getReadMember().getDeclaringClass().equals(Object.class)) {
				continue;
			}
			
			Object value = pi.get(cause);
			if (value == cause) {
				continue;
			}
			
			if (data == null) data = new LinkedHashMap<String, Object>();
			data.put(pi.getName(), value);
		}
		return (data != null) ? data : Collections.emptyMap();
	}
	
	JSON createJSON(Locale locale) throws ServletException  {
		JSON json;
		if (processor != null) {
			try {
				json = processor.type.newInstance();
				if (processor.locale != null) {
					json.setLocale(processor.locale);
				} else {
					json.setLocale(locale);
				}
				if (processor.mode != null) json.setMode(processor.mode);
				if (processor.timeZone != null) json.setTimeZone(processor.timeZone);
				if (processor.maxDepth != null) json.setMaxDepth(processor.maxDepth);
				if (processor.prettyPrint != null) json.setPrettyPrint(processor.prettyPrint);
				if (processor.initialIndent != null) json.setInitialIndent(processor.initialIndent);
				if (processor.indentText != null) json.setIndentText(processor.indentText);
				if (processor.suppressNull != null) json.setSuppressNull(processor.suppressNull);
				if (processor.dateFormat != null) json.setDateFormat(processor.dateFormat);
				if (processor.numberFormat != null) json.setNumberFormat(processor.numberFormat);
				if (processor.propertyStyle != null) json.setPropertyStyle(processor.propertyStyle);
				if (processor.enumStyle != null) json.setEnumStyle(processor.enumStyle);
			} catch (Exception e) {
				throw new ServletException(e);
			}
		} else {
			json = new JSON();
			json.setLocale(locale);
		}
		return json;
	}
	
	static boolean isJSONType(String contentType) {
		if (contentType != null) {
			contentType = contentType.toLowerCase();
			return (contentType.equals("application/json") || contentType.startsWith("application/json;"));
		}
		return false;
	}
	
	static int calcurateDistance(Class<?>[] types, List<?> params) {
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
	
	static class ProcessorConfig {
		@JSONHint(name = "class")
		public Class<? extends JSON> type = JSON.class;
		
		public JSON.Mode mode;
		public Locale locale;
		public TimeZone timeZone;
		public Integer maxDepth;
		public Boolean prettyPrint;
		public Integer initialIndent;
		public String indentText;
		public Boolean suppressNull;
		public String dateFormat;
		public String numberFormat;
		public NamingStyle propertyStyle;
		public NamingStyle enumStyle;
	}
}