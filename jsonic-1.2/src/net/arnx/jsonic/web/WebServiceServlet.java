/*
 * Copyright 2007-2009 Hidekatsu Izuno
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.arnx.jsonic.JSONException;

import static javax.servlet.http.HttpServletResponse.*;
import static net.arnx.jsonic.web.Container.*;

@Deprecated
public class WebServiceServlet extends HttpServlet {
	private static final long serialVersionUID = -63348112220078595L;
	
	protected class Config {
		public String container;
		public Class<? extends net.arnx.jsonic.JSON> processor;
		public Map<String, String> mappings;
		public Map<String, Pattern> definitions;
	}
	
	private Container container;
	private Config config;
	
	private List<RouteMapping> mappings = new ArrayList<RouteMapping>();
	
	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
		
		String configText = servletConfig.getInitParameter("config");
		if (configText == null) configText = "";
		 
		net.arnx.jsonic.JSON json = new net.arnx.jsonic.JSON();
		
		try {
			config = json.parse(configText, getConfigClass());
			Class<?> containerClass =  Container.class;
			if (config.container != null) {
				config.container = config.container.replaceFirst("^(\\Qnet.arnx.jsonic.web.\\E)(.+Container)", "$1extension.$2");
				containerClass = findClass(config.container);
			}
			
			container = (Container)json.parse(configText, containerClass);
			container.init(this);
		} catch (Exception e) {
			throw new ServletException(e);
		}
		
		if (config.processor == null) config.processor = WebServiceServlet.JSON.class;
		
		if (config.definitions == null) config.definitions = new HashMap<String, Pattern>();
		if (!config.definitions.containsKey("package")) config.definitions.put("package", Pattern.compile(".+"));
		
		if (config.mappings != null) {
			for (Map.Entry<String, String> entry : config.mappings.entrySet()) {
				mappings.add(new RouteMapping(entry.getKey(), entry.getValue(), config.definitions));
			}
		}
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
	protected Class<? extends Config> getConfigClass() {
		return Config.class;
	}

	protected Route preprocess(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		String uri = (request.getContextPath().equals("/")) ?
				request.getRequestURI() : 
				request.getRequestURI().substring(request.getContextPath().length());
		
		String path = getServletContext().getRealPath(uri);
		File file = (path != null) ? new File(path) : null;
		if (file != null && file.exists() && file.isFile()) {
			OutputStream out = response.getOutputStream();
			InputStream in = new FileInputStream(file);
			try {
				byte[] buffer = new byte[1024];
				int count = 0;
				while ((count = in.read(buffer)) > 0) {
					out.write(buffer, 0, count);
				}
			} finally {
				if (in != null) in.close();
			}
			return null;
		}
		
		Route route = null;
		for (RouteMapping m : mappings) {
			if ((route = m.matches(request, uri)) != null) {
				break;
			}
		}
		
		if (route == null) {
			response.sendError(SC_NOT_FOUND, "Not Found");
			return null;
		}
		
		String method = route.getMethod();
		if (!method.equals("GET") && !method.equals("POST") && !method.equals("PUT") && !method.equals("DELETE")) {
			response.sendError(SC_FORBIDDEN, "Method Not Allowed");
			return null;
		}
		
		container.debug("Route found: " + request.getMethod() + " " + uri);
		return route;
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		container.start(request, response);
		try {
			Route route = preprocess(request, response);
			if (route == null) return;
			
			if (route.isRpcMode()) {
				response.addHeader("Allow", "POST");
				response.sendError(SC_METHOD_NOT_ALLOWED, "Method Not Allowd");
			} else {
				doREST(route, request, response);
			}
		} finally {
			container.end(request, response);
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		container.start(request, response);
		try {
			Route route = preprocess(request, response);
			if (route == null) return;
	
			if (route.isRpcMode()) {
				doRPC(route, request, response);
			} else {
				doREST(route, request, response);
			}
		} finally {
			container.end(request, response);
		}
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		container.start(request, response);
		try {
			Route route = preprocess(request, response);
			if (route == null) return;
			
			if (route.isRpcMode()) {
				response.addHeader("Allow", "POST");
				response.sendError(SC_METHOD_NOT_ALLOWED, "Method Not Allowed");
			} else {
				doREST(route, request, response);
			}
		} finally {
			container.end(request, response);
		}
	}
	
	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
		throws ServletException, IOException {
		container.start(request, response);
		try {
			Route route = preprocess(request, response);
			if (route == null) return;
			
			if (route.isRpcMode()) {
				response.addHeader("Allow", "POST");
				response.sendError(SC_METHOD_NOT_ALLOWED, "Method Not Allowed");
			} else {
				doREST(route, request, response);
			}
		} finally {
			container.end(request, response);
		}
	}
	
	class RpcRequest {
		public String method;
		public List<Object> params;
		public Object id;
	}
	
	protected void doRPC(Route route, HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
				
		net.arnx.jsonic.JSON json = null;
		try {
			json = (net.arnx.jsonic.JSON)config.processor.newInstance();
		} catch (Exception e) {
			throw new ServletException(e);
		}
		json.setLocale(request.getLocale());
		
		// request processing
		RpcRequest req = null;
		Object result = null;
		
		int errorCode = 0;
		String errorMessage = null;
		Throwable throwable = null;
		
		try {
			req = json.parse(request.getReader(), RpcRequest.class);
			if (req == null || req.method == null || req.params == null) {
				throwable = new IllegalArgumentException("Request is empty.");
				errorCode = -32600;
				errorMessage = "Invalid Request.";
			} else {
				int delimiter = req.method.lastIndexOf('.');
				if (delimiter <= 0 && delimiter+1 == req.method.length()) {
					throw new NoSuchMethodException(req.method);
				} else {
					Object component = container.getComponent(route.getComponentClass(req.method.substring(0, delimiter)));
					if (component == null) {
						throw new NoSuchMethodException("Method not found: " + req.method);
					}
					
					json.setContext(component);
					result = invoke(json, component, req.method.substring(delimiter+1), req.params);
				}
			}
		} catch (ClassNotFoundException e) {
			container.debug("Class Not Found.", e);
			throwable = e;
			errorCode = -32601;
			errorMessage = "Method not found.";
		} catch (NoSuchMethodException e) {
			container.debug("Method Not Found.", e);
			throwable = e;
			errorCode = -32601;
			errorMessage = "Method not found.";
		} catch (JSONException e) {
			container.debug("Fails to parse JSON.", e);
			throwable = e;
			if (e.getErrorCode() == JSONException.POSTPARSE_ERROR) {
				errorCode = -32602;
				errorMessage = "Invalid params.";
			} else  {
				errorCode = -32700;
				errorMessage = "Parse error.";
			}
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			container.debug("Fails to invoke method.", e);
			throwable = cause;
			if (cause instanceof IllegalStateException
				|| cause instanceof UnsupportedOperationException) {
				errorCode = -32601;
				errorMessage = "Method not found.";
			} else if (cause instanceof IllegalArgumentException) {
				errorCode = -32602;
				errorMessage = "Invalid params.";
			} else {
				errorCode = -32603;
				errorMessage = cause.getMessage();
			}
		} catch (Exception e) {
			container.error("Internal error occurred.", e);
			throwable = e;
			errorCode = -32603;
			errorMessage = "Internal error.";
		}
		
		// it's notification when id was null
		if (req != null && req.method != null && req.params != null && req.id == null) {
			response.setStatus(SC_ACCEPTED);
			return;
		}

		// response processing
		response.setContentType("application/json");
		
		Map<String, Object> res = new LinkedHashMap<String, Object>();
		res.put("result", result);
		if (errorCode == 0) {
			res.put("error", null);
		} else {
			Map<String, Object> error = new LinkedHashMap<String, Object>();
			error.put("code", errorCode);
			error.put("message", errorMessage);
			error.put("data", throwable);
			res.put("error", error);
		}
		res.put("id", (req != null) ? req.id : null);

		Writer writer = response.getWriter();
		
		json.setContext(result);
		json.setPrettyPrint(container.isDebugMode());
		json.format(res, writer);
	}
	
	@SuppressWarnings("unchecked")
	protected void doREST(Route route, HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		
		String methodName = route.getMethod().toLowerCase();
		int status = SC_OK;
		String callback = null;
		
		if ("GET".equals(route.getMethod())) {
			methodName = "find";
			callback = route.getParameter("callback");
		} else if ("POST".equals(route.getMethod())) {
			methodName = "create";
			status = SC_CREATED;
		} else if ("PUT".equals(route.getMethod())) {
			methodName = "update";
		} else if ("DELETE".equals(route.getMethod())) {
			methodName = "delete";
		}
		
		// request processing
		net.arnx.jsonic.JSON json = null;
		try {
			json = (net.arnx.jsonic.JSON)config.processor.newInstance();
		} catch (Exception e) {
			throw new ServletException(e);
		}
		json.setLocale(request.getLocale());
		
		Object res = null;
		try {
			Object component = container.getComponent(route.getComponentClass(null));
			if (component == null) {
				response.sendError(SC_NOT_FOUND, "Not Found");
				return;
			}
			
			List<Object> params = null;
			if (!route.hasJSONContent()) {
				params = new ArrayList<Object>(1);
				params.add(route.getParameterMap());
			} else {
				Object o = json.parse(request.getReader());
				if (o instanceof List) {
					params = (List<Object>)o;
					if (params.isEmpty()) {
						params = new ArrayList<Object>(1);
						params.add(route.getParameterMap());
					} else if (params.get(0) instanceof Map) {
						params.set(0, route.mergeParameterMap((Map<?, ?>)params.get(0)));
					}
				} else if (o instanceof Map) {
					params = new ArrayList<Object>(1);
					params.add(route.mergeParameterMap((Map<?, ?>)o));
				} else {
					throw new IllegalArgumentException("failed to convert parameters from JSON.");
				}
			}
			json.setContext(component);
			res = invoke(json, component, methodName, params);
		} catch (ClassNotFoundException e) {
			container.debug("Class Not Found.", e);
			response.sendError(SC_NOT_FOUND, "Not Found");
			return;			
		} catch (NoSuchMethodException e) {
			container.debug("Method Not Found.", e);
			response.sendError(SC_NOT_FOUND, "Not Found");
			return;
		} catch (JSONException e) {
			container.debug("Fails to parse JSON.", e);
			response.sendError(SC_BAD_REQUEST, "Bad Request");
			return;
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			container.debug(cause.toString(), cause);
			if (cause instanceof IllegalStateException
				|| cause instanceof UnsupportedOperationException) {
				response.sendError(SC_NOT_FOUND, "Not Found");				
				return;
			} else if (cause instanceof Error) {
				response.sendError(SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
				return;
			}
			
			response.setStatus(SC_BAD_REQUEST);
			res = cause;
		} catch (Exception e) {
			container.error("Internal error occurred.", e);
			response.sendError(SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
			return;
		}
		
		response.setContentType((callback != null) ? "text/javascript" : "application/json");
		if (res == null
				|| res instanceof CharSequence
				|| res instanceof Boolean
				|| res instanceof Number
				|| res instanceof Date) {
			if (status != SC_CREATED) status = SC_NO_CONTENT;
			response.setStatus(status);
		} else {
			Writer writer = response.getWriter();
			json.setPrettyPrint(container.isDebugMode());
			
			if (callback != null) writer.append(callback).append("(");
			json.format(res, writer);
			if (callback != null) writer.append(");");
		}
	}
	
	@Override
	public void destroy() {
		container.destory();
		super.destroy();
	}
	
	
	/**
	 * Called before invoking the target method.
	 * 
	 * @param target The target instance.
	 * @param params The parameters of the target method.
	 * @return The parameters before processing.
	 */
	protected Object[] preinvoke(Object target, Object... params) {
		return params;
	}
		
	protected Object invoke(net.arnx.jsonic.JSON json, Object o, String methodName, List<?> args) throws Exception {
		if (args == null) args = Collections.emptyList();
		
		methodName = toLowerCamel(methodName);
		
		Class<?> c = o.getClass();
		
		Method init = null;
		Method method = null;
		Type[] paramTypes = null;
		Method destroy = null;
		
		boolean illegalInit = false;
		boolean illegalDestroy = false;
		for (Method m : c.getMethods()) {
			if (Modifier.isStatic(m.getModifiers())
					|| m.isSynthetic()
					|| m.isBridge()) {
				continue;
			}
			
			if (m.getName().equals(container.init)) {
				if (m.getParameterTypes().length == 0 && m.getReturnType().equals(void.class)) {
					init = m;
				} else {
					illegalInit = true;
				}
			} else if (m.getName().equals(container.destroy)) {
				if (m.getParameterTypes().length == 0 && m.getReturnType().equals(void.class)) {
					destroy = m;
				} else {
					illegalDestroy = true;
				}
			} else if (m.getName().equals(methodName)) {
				Type[] pTypes = m.getGenericParameterTypes();
				if (args.size() <= Math.max(1, pTypes.length)) {
					if (method == null || Math.abs(args.size() - pTypes.length) < Math.abs(args.size() - paramTypes.length)) {
						method = m;
						paramTypes = pTypes;
					} else if (pTypes.length == paramTypes.length) {
						throw new IllegalStateException("too many methods found: " + toPrintString(c, methodName, args));
					}
				}
			}
		}
		
		if (method == null || container.limit(c, method)) {
			throw new NoSuchMethodException("method missing: " + toPrintString(c, methodName, args));
		}
		
		if (container.isDebugMode() && init == null && illegalInit) {
			container.debug("Notice: init method must have no arguments.");
		}
		if (container.isDebugMode() && destroy == null && illegalDestroy) {
			container.debug("Notice: destroy method must have no arguments.");
		}
		
		Object[] params = new Object[paramTypes.length];
		for (int i = 0; i < params.length; i++) {
			params[i] = json.convert((i < args.size()) ? args.get(i) : null, paramTypes[i]);
		}
		
		if (init != null) {
			if (container.isDebugMode()) {
				container.debug("Execute: " + toPrintString(c, init.getName(), null));
			}
			init.invoke(o);
		}
		
		if (container.isDebugMode()) {
			container.debug("Execute: " + toPrintString(c, methodName, args));
		}
		params = preinvoke(o, params);
		Object ret = method.invoke(o, params);
		ret = postinvoke(o, ret);
		
		if (destroy != null) {
			if (container.isDebugMode()) {
				container.debug("Execute: " + toPrintString(c, destroy.getName(), null));
			}
			destroy.invoke(o);
		}
		
		return ret;
	}
	
	/**
	 * Called after invoked the target method.
	 * 
	 * @param target The target instance.
	 * @param result The returned value of the target method call.
	 * @return The returned value after processed.
	 */
	protected Object postinvoke(Object target, Object result) {
		return result;
	}
	
	private String toPrintString(Class<?> c, String methodName, List<?> args) {
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
	
	public static class JSON extends net.arnx.jsonic.JSON {
		@Override
		protected boolean ignore(Context context, Class<?> target, Member member) {
			return member.getDeclaringClass().equals(Throwable.class)
				|| super.ignore(context, target, member);
		}
	}
	
	static class RouteMapping {
		private static final Pattern PLACE_PATTERN = Pattern.compile("\\{\\s*(\\p{javaJavaIdentifierStart}[\\p{javaJavaIdentifierPart}\\.-]*)\\s*(?::\\s*((?:[^{}]|\\{[^{}]*\\})*)\\s*)?\\}");
		private static final Pattern DEFAULT_PATTERN = Pattern.compile("[^/().]+");
		
		private Pattern pattern;
		private List<String> names;
		private String target;
		
		public RouteMapping(String path, String target, Map<String, Pattern> definitions) {
			this.names = new ArrayList<String>();
			StringBuffer sb = new StringBuffer("^\\Q");
			Matcher m = PLACE_PATTERN.matcher(path);
			while (m.find()) {
				String name = m.group(1);
				names.add(name);
				Pattern p = DEFAULT_PATTERN;
				if (m.group(2) != null) {
					p = Pattern.compile(m.group(2));
				} else if (definitions.containsKey(name)) {
					p = definitions.get(name);
				}
				m.appendReplacement(sb, "\\\\E(" + p.pattern().replaceAll("\\((?!\\?)", "(?:").replace("\\", "\\\\") + ")\\\\Q");
			}
			m.appendTail(sb);
			sb.append("\\E$");
			this.pattern = Pattern.compile(sb.toString());
			this.target = target;
		}
		
		@SuppressWarnings("unchecked")
		public Route matches(HttpServletRequest request, String path) throws IOException {
			Matcher m = pattern.matcher(path);
			if (m.matches()) {
				Map<String, Object> params = new HashMap<String, Object>(); 
				for (int i = 0; i < names.size(); i++) {
					String key = names.get(i);
					Object value = m.group(i+1);
					if (params.containsKey(key)) {
						Object target = params.get(key);
						if (target instanceof List) {
							((List<Object>)target).add(value);
						} else {
							List<Object> list = new ArrayList<Object>(2);
							list.add(target);
							list.add(value);
						}
					} else {
						params.put(key, value);
					}
				}
				Route route = new Route(request, target, params);
				return route;
			}
			return null;
		}
	}
	
	static class Route {
		private static final Pattern REPLACE_PATTERN = Pattern.compile("\\$\\{(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)\\}");

		private String target;
		private String method;
		private int contentLength = 0;
		private Map<Object, Object> params;
		
		private boolean isRpcMode;
		
		@SuppressWarnings({"unchecked", "rawtypes"})
		public Route(HttpServletRequest request, String target, Map<String, Object> params) throws IOException {
			this.target = target;
			this.params = (Map)params;
			
			if ("rpc".equalsIgnoreCase(getParameter("class"))) {
				isRpcMode = true;
				
				this.method = request.getMethod().toUpperCase();
			} else {
				if (request.getQueryString() != null) {
					parseQueryString(new ByteArrayInputStream(request.getQueryString().getBytes("US-ASCII")), request.getCharacterEncoding());
				}
				if (!request.getMethod().equalsIgnoreCase("GET")) {
					contentLength = request.getContentLength();
					
					Map<String, String> options = parseHeaderLine(request.getContentType());
					String contentType = options.get(null);

					if (contentLength > 0 && contentType != null) {
						if (contentType.equals("application/x-www-form-urlencoded")) {
							parseQueryString(request.getInputStream(), request.getCharacterEncoding());
							contentLength = 0;
						}
					}
				}
				
				String m = getParameter("_method");
				if (m == null) m = request.getMethod();
				this.method = m.toUpperCase();
			}
		}
		
		public String getMethod() {
			return method;
		}
		
		public boolean isRpcMode() {
			return isRpcMode;
		}
		
		public String getParameter(String name) {
			Object o = params.get(name);
			
			if (o instanceof Map<?,?>) {
				Map<?, ?> map = (Map<?, ?>)o;
				if (map.containsKey(null)) o = map.get(null); 
			}
			
			if (o instanceof List<?>) {
				List<?> list = (List<?>)o;
				if (!list.isEmpty()) o = list.get(0);
			}
			
			return (o instanceof String) ? (String)o : null;
		}
		
		public Map<?, ?> getParameterMap() {
			return params;
		}
		
		@SuppressWarnings("unchecked")
		public Map<?, ?> mergeParameterMap(Map<?, ?> newParams) {
			for (Map.Entry<?, ?> entry : newParams.entrySet()) {
				if (params.containsKey(entry.getKey())) {
					Object target = params.get(entry.getKey());
					
					if (target instanceof Map) {
						Map<Object, Object> map = (Map<Object, Object>)target;
						if (map.containsKey(null)) {
							target = map.get(null);
							if (target instanceof List) {
								((List<Object>)target).add(entry.getValue());
							} else {
								List<Object> list = new ArrayList<Object>();
								list.add(target);
								list.add(entry.getValue());
								map.put(null, list);
							}
						} else {
							map.put(null, entry.getValue());
						}
					} else  if (target instanceof List) {
						((List<Object>)target).add(entry.getValue());
					} else {
						List<Object> list = new ArrayList<Object>();
						list.add(target);
						list.add(entry.getValue());
						params.put(entry.getKey(), list);
					}
				} else {
					params.put(entry.getKey(), entry.getValue());
				}
			}
			return params;
		}
		
		public boolean hasJSONContent() {
			return contentLength > 0;
		}
		
		public String getComponentClass(String sub) {
			Matcher m = REPLACE_PATTERN.matcher(target);
			StringBuffer sb = new StringBuffer();
			while (m.find()) {
				String key = m.group(1);
				String value = getParameter(key);
				
				if (key.equals("class")) {
					value = toUpperCamel((sub != null) ? sub 
						: (value != null) ? value : "");
				} else if (value == null) {
					value = "";
				} else if (key.equals("package")) {
					value = value.replace('/', '.');
				}
				m.appendReplacement(sb, value);
			}
			m.appendTail(sb);
			return sb.toString();
		}
		
		private void parseQueryString(InputStream in, String encoding) throws IOException {
			List<Object> pairs = new ArrayList<Object>();
			if (encoding == null) encoding = "UTF-8";

			int state = 0; // 0 '%' 1 'N' 2 ('N' | '=' | '&')
			
			ByteBuilder bb = new ByteBuilder(50);
			
			int before = 0;
			while (true) {
				int c = in.read();
				if (c == -1) {
					if (state == 2) bb.append((byte)before);
					if (pairs.size()%2 == 1){
						pairs.add(bb.toString(encoding));
					} else {
						pairs.add(bb.toString(encoding));
						pairs.add("");
					}
					break;
				}
				
				if (c == '&') {
					if (state == 2) bb.append((byte)before);
					
					if (pairs.size()%2 == 1){
						pairs.add(bb.toString(encoding));
					} else {
						pairs.add(bb.toString(encoding));
						pairs.add("");
					}
					bb.setLength(0);
					state = 0;
				} else if (c == '=') {
					if (state == 2) bb.append((byte)before);
					
					if (pairs.size()%2 == 1){
						bb.append((byte)c);
					} else {
						pairs.add(bb.toString(encoding));
						bb.setLength(0);
					}
					state = 0;
				} else if (state == 2){
					int d1 = Character.digit(before, 16);
					int d2 = Character.digit(c, 16);
						
					if (d1 != -1 && d2 != -1) {
						bb.append((byte)((d1 << 4) | d2));
					} else {
						bb.append((byte)before);
						bb.append((byte)c);
					}
					state = 0;
				} else if (state == 1) {
					state = 2;
				} else {
					if (c == '+') {
						bb.append((byte)' ');
						state = 0;
					} else if (c == '%') {
						state = 1;
					} else {
						bb.append((byte)c);
						state = 0;
					}
				}
				
				before = c;
			}
			
			parseParameter(pairs, params);
		}
		
		@SuppressWarnings("unchecked")
		private static void parseParameter(List<Object> pairs, Map<Object, Object> params) {
			for (int i = 0; i < pairs.size(); i+= 2) {
				String name = (String)pairs.get(i);
				Object value = pairs.get(i+1);
				
				int start = 0;
				char old = '\0';
				Map<Object, Object> current = params;
				for (int j = 0; j < name.length(); j++) {
					char c = name.charAt(j);
					if (c == '.' || c == '[') {
						String key = name.substring(start, (old == ']') ? j-1 : j);
						Object target = current.get(key);
						
						if (target instanceof Map) {
							current = (Map<Object, Object>)target;
						} else {
							Map<Object, Object> map = new LinkedHashMap<Object, Object>();
							if (target != null) map.put(null, target);
							current.put(key, map);
							current = map;
						}
						start = j+1;
					}
					old = c;
				}
				
				name = name.substring(start, (old == ']') ? name.length()-1 : name.length());

				if (current.containsKey(name)) {
					Object target = current.get(name);
					
					if (target instanceof Map) {
						Map<Object, Object> map = (Map<Object, Object>)target;
						if (map.containsKey(null)) {
							target = map.get(null);
							if (target instanceof List) {
								((List<Object>)target).add(value);
							} else {
								List<Object> list = new ArrayList<Object>();
								list.add(target);
								list.add(value);
								map.put(null, list);
							}
						} else {
							map.put(null, value);
						}
					} else if (target instanceof List) {
						((List<Object>)target).add(value);
					} else {
						List<Object> list = new ArrayList<Object>();
						list.add(target);
						list.add(value);
						current.put(name, list);
					}
				} else {
					current.put(name, value);
				}
			}
		}
		
		private static Map<String, String> parseHeaderLine(String line) {
			if (line == null) return Collections.emptyMap();
			
			Map<String, String> map = new HashMap<String, String>();
			
			int state = 0; // 0 LWS 1 <field value> 2 LWS ; 3 LWS 4 <key> 5 LWS = 6 LWS (7 <value> | " 8 <quoted value> ")   
			
			StringBuilder sb = new StringBuilder(line.length());
			String key = null;
			boolean escape = false;
			
			loop:for (int i = 0; i < line.length(); i++) {
				char c = line.charAt(i);
				
				if (state == 8) {
					if (escape) {
						if (c < 128) {
							sb.append(c);
							escape = false;
						} 
						else break;
					} 
					else if (c == '\\') escape = true;
					else if (c == '"') state = 2;
					else sb.append(c);
					continue;
				}
				
				switch (c) {
				case '\t':
				case ' ':
					if (state == 1 || state == 4) state++;
					else if (state == 7) state = 2;
					break;
				case ';':
					if (state == 1 || state == 2 || state == 7) {
						map.put(key, sb.toString());
						sb.setLength(0);
						state = 3;
						break;
					}
					break loop;
				case '=':
					if (state == 4 || state == 5) {
						key = sb.toString().toLowerCase();
						sb.setLength(0);
						state = 6;
						break;
					}
					break loop;
				case '"':
					if (state == 6) state = 8;
					else break loop;
					break;
				default:
					if (state == 0 || state == 3 || state == 6) {
						state++;
					}
					
					if (state == 1 || state == 4 || state == 7) {
						if ((c >= '0' && c >= '9')
							|| (c >= 'A' && c >= 'Z') 
							|| (c >= 'a' && c >= 'z') 
							|| "!#$%&'*+-.^_`|~".indexOf(c) != -1
							|| (state == 1 && c == '/')
						) {
							sb.append(c);
							break;
						}
					}
					break loop;
				}
			}
			
			if (state <= 2 || state == 7) {
				map.put(key, sb.toString());
			}
			
			return map;
		}
	}

	static class ByteBuilder {
		private int length = 0;
		private byte[] array;
		
		public ByteBuilder() {
			this(1024);
		}
		
		public ByteBuilder(int length) {
			array = new byte[length];
		}
		
		public void append(int c) {
			append((byte)c);
		}
		
		public void append(char c) {
			append((byte)c);
		}
		
		public void append(byte b) {
			if (length+1 > array.length) {
				byte[] newArray = new byte[(int)(array.length*1.5)];
				System.arraycopy(array, 0, newArray, 0, array.length);
				array = newArray;
			}
			array[length++] = (byte)b;
		}
		
		public void append(byte[] bytes) {
			append(bytes, 0, bytes.length);
		}
		
		public void append(byte[] bytes, int offset, int len) {
			if ((length + bytes.length) > array.length) {
				byte[] newArray = new byte[(int)((array.length + bytes.length)*1.5)];
				System.arraycopy(array, 0, newArray, 0, array.length);
				array = newArray;
			}
			System.arraycopy(bytes, offset, array, length, len);
			length += len;
		}
		
		public boolean startsWith(String str) {
			if (length < str.length()) return false;
			
			for (int i = 0; i < str.length(); i++) {
				if (str.charAt(i) != array[i]) return false;
			}
			return true;
		}
		
		public boolean endsWith(String str) {
			if (length < str.length()) return false;
			
			for (int i = 0; i < str.length(); i++) {
				if (str.charAt(str.length()-i-1) != array[length-i-1]) return false;
			}
			return true;
		}

		public boolean matches(String str) {
			if (length != str.length()) return false;
			
			for (int i = 0; i < str.length(); i++) {
				if (str.charAt(i) != array[i]) return false;
			}
			return true;
		}
		
		public byte byteAt(int pos) {
			return array[pos];
		}

		public int indexOf(char c) {
			for (int i = 0; i < length; i++) {
				if (array[i] == c) return i;
			}
			return -1;
		}
		
		public void setLength(int length) {
			this.length = length;
		}
		
		public int length() {
			return length;
		}
		
		public String substring(int start, int end, String encoding) throws UnsupportedEncodingException {
			return new String(array, start, end-start, encoding);
		}
		
		public String substring(int start, String encoding) throws UnsupportedEncodingException {
			return new String(array, start, length-start, encoding);
		}
		
		public String toString(String encoding) throws UnsupportedEncodingException {
			return new String(array, 0, length, encoding);
		}
	}
}