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
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.arnx.jsonic.JSON;
import net.arnx.jsonic.JSONException;

import static javax.servlet.http.HttpServletResponse.*;

public class WebServiceServlet extends HttpServlet {	
	static class Config {
		public Class<? extends Container> container;
		public Class<? extends JSON> processor;
		public String encoding;
		public Boolean expire;
		public Map<String, List<Object>> mappings;
		public Map<String, Pattern> definitions;
	}
	
	protected Container container;
	
	Config config;
	List<RouteMapping> mappings = new ArrayList<RouteMapping>();
	
	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
		
		String configText = servletConfig.getInitParameter("config");
		
		JSON json = new JSON();
		
		if (configText == null) {
			Map<String, String> map = new HashMap<String, String>();
			Enumeration<String> e =  cast(servletConfig.getInitParameterNames());
			while (e.hasMoreElements()) {
				map.put(e.nextElement(), servletConfig.getInitParameter(e.nextElement()));
			}
			configText = json.format(map);
		}
		
		try {
			config = json.parse(configText, Config.class);
			if (config.container == null) config.container = Container.class;
			container = json.parse(configText, config.container);
			container.init(servletConfig);
		} catch (Exception e) {
			throw new ServletException(e);
		}
		
		if (config.processor == null) config.processor = WebServiceJSON.class;
		
		if (config.definitions == null) config.definitions = new HashMap<String, Pattern>();
		if (!config.definitions.containsKey("package")) config.definitions.put("package", Pattern.compile(".+"));
		
		if (config.mappings != null) {
			for (Map.Entry<String, List<Object>> entry : config.mappings.entrySet()) {
				mappings.add(new RouteMapping(entry.getKey(), entry.getValue(), config.definitions));
			}
		}
	}
	
	protected void process(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		String uri = (request.getContextPath().equals("/")) ?
				request.getRequestURI() : 
				request.getRequestURI().substring(request.getContextPath().length());
		
		String encoding = config.encoding;
		Boolean expire = config.expire;
		
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
		
		File file = new File(getServletContext().getRealPath(uri));
		if (file.exists()) {
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
			return;
		}
		
		Route route = null;
		for (RouteMapping m : mappings) {
			if ((route = m.matches(request, uri)) != null) {
				break;
			}
		}
		
		if (route == null) {
			response.sendError(SC_NOT_FOUND, "Not Found");
			return;
		}
		
		if (route.isRpcMode()) {
			if ("POST".equals(route.getMethod())) {
				container.debug("Route found: " + request.getMethod() + " " + uri);
				doRPC(route, request, response);
			} else {
				response.addHeader("Allow", "POST");
				response.sendError(SC_METHOD_NOT_ALLOWED, "Method Not Allowd");
			}
		} else {
			container.debug("Route found: " + request.getMethod() + " " + uri);
			doREST(route, request, response);
		}
		
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		container.start(request, response);
		try {
			process(request, response);
		} finally {
			container.end(request, response);
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		container.start(request, response);
		try {
			process(request, response);
		} finally {
			container.end(request, response);
		}
	}
	
	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		container.start(request, response);
		try {
			process(request, response);
		} finally {
			container.end(request, response);
		}
	}
	
	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
		throws ServletException, IOException {
		container.start(request, response);
		try {
			process(request, response);
		} finally {
			container.end(request, response);
		}
	}
	
	static class RpcRequest {
		public String jsonrpc;
		public String method;
		public List<Object> params;
		public Object id;
	}
	
	protected void doRPC(Route route, HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		
		JSON json = createJSON(request.getLocale());
		
		// request processing
		RpcRequest req = null;
		Object result = null;
		
		int errorCode = 0;
		String errorName = null;
		String errorMessage = null;
		Throwable throwable = null;
		
		Object component = null;
		
		try {
			req = json.parse(request.getReader(), RpcRequest.class);
			if (req == null || req.method == null || req.params == null || req.jsonrpc != null) {
				throwable = new IllegalArgumentException(
						((req == null) ? "request" : (req.method == null) ? "method" : "params")
						+ "is null.");
				errorCode = -32600;
				errorName = "ReferenceError";
				errorMessage = "Invalid Request.";
			} else {
				int delimiter = req.method.lastIndexOf('.');
				if (delimiter <= 0 && delimiter+1 == req.method.length()) {
					throw new NoSuchMethodException(req.method);
				}
				
				String methodName = req.method.substring(delimiter+1);
				if (methodName.equals(container.init) || methodName.equals(container.destroy)) {
					throw new NoSuchMethodException(req.method);
				}
				
				component = container.getComponent(route.getComponentClass(container, req.method.substring(0, delimiter)));
				if (component == null) {
					throw new NoSuchMethodException(req.method);
				}
				
				json.setContext(component);
				
				Method method = container.getMethod(component, methodName, req.params);
				
				Produce produce = method.getAnnotation(Produce.class);
				if (produce != null) response.setContentType(produce.value());
								
				result = container.execute(json, component, method, req.params);
				
				if (produce != null) return;
			}
		} catch (ClassNotFoundException e) {
			container.debug("Class Not Found.", e);
			throwable = e;
			errorName = "ReferenceError";
			errorCode = -32601;
			errorMessage = "Method not found.";
		} catch (NoSuchMethodException e) {
			container.debug("Method Not Found.", e);
			throwable = e;
			errorCode = -32601;
			errorName = "ReferenceError";
			errorMessage = "Method not found.";
		} catch (JSONException e) {
			container.debug("Fails to parse JSON.", e);
			throwable = e;
			if (e.getErrorCode() == JSONException.POSTPARSE_ERROR) {
				errorCode = -32602;
				errorName = "TypeError";
				errorMessage = "Invalid params.";
			} else  {
				errorCode = -32700;
				errorName = "SyntaxError";
				errorMessage = "Parse error.";
			}
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			container.debug("Fails to invoke method.", e);
			throwable = cause;
			if (cause instanceof IllegalStateException
				|| cause instanceof UnsupportedOperationException) {
				errorCode = -32601;
				errorName = "ReferenceError";
				errorMessage = "Method not found.";
			} else if (cause instanceof IllegalArgumentException) {
				errorCode = -32602;
				errorName = "SyntaxError";
				errorMessage = "Invalid params.";
			} else {
				errorCode = -32603;
				errorName = cause.getClass().getSimpleName();
				errorMessage = cause.getMessage();
			}
		} catch (Exception e) {
			container.error("Internal error occurred.", e);
			throwable = e;
			errorCode = -32603;
			errorName = e.getClass().getSimpleName();
			errorMessage = "Internal error.";
		}
		
		// it's notification when id was null
		if (req != null && req.method != null && req.params != null && req.id == null) {
			response.setStatus(SC_ACCEPTED);
			return;
		}
		
		if (result instanceof Produce) return;

		// response processing
		response.setContentType("application/json");
		
		Map<String, Object> res = new LinkedHashMap<String, Object>();
		res.put("result", result);
		if (errorCode == 0) {
			res.put("error", null);
		} else {
			Map<String, Object> error = new LinkedHashMap<String, Object>();
			error.put("code", errorCode);
			error.put("name", errorName);
			error.put("message", errorMessage);
			error.put("data", throwable);
			res.put("error", error);
		}
		res.put("id", (req != null) ? req.id : null);
		
		Writer writer = response.getWriter();

		try {
			json.setContext(result);
			json.setPrettyPrint(container.isDebugMode());
			json.format(res, writer);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			container.error("Fails to format", e);
			res.clear();
			res.put("result", null);
			Map<String, Object> error = new LinkedHashMap<String, Object>();
			error.put("code", -32603);
			error.put("name", e.getClass().getSimpleName());
			error.put("message", "Internal error.");
			error.put("data", e);
			res.put("error", error);
			res.put("id", (req != null) ? req.id : null);
			json.format(res, writer);
			return;
		}
	}
	
	protected void doREST(Route route, HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		
		JSON json = createJSON(request.getLocale());
		
		int status = SC_OK;
		String callback = null;
		
		if ("GET".equals(route.getMethod())) {
			callback = route.getParameter("callback");
		} else if ("POST".equals(route.getMethod())) {
			status = SC_CREATED;
		}
		
		String methodName = route.getRestMethod();
		if (methodName == null || methodName.equals(container.init) || methodName.equals(container.destroy)) {
			container.debug("Method mapping not found: " + route.getMethod());
			response.sendError(SC_NOT_FOUND, "Not Found");
			return;
		}
		
		Object res = null;
		try {
			Object component = container.getComponent(route.getComponentClass(container, null));
			if (component == null) {
				container.debug("Component not found: " + route.getComponentClass(container, null));
				response.sendError(SC_NOT_FOUND, "Not Found");
				return;
			}
			
			List<Object> params = null;
			if (!route.hasJSONContent()) {
				params = new ArrayList<Object>(1);
				params.add(route.getParameterMap());
			} else {
				Object o = json.parse(request.getReader());
				if (o instanceof List<?>) {
					params = cast(o);
					if (params.isEmpty()) {
						params = new ArrayList<Object>(1);
						params.add(route.getParameterMap());
					} else if (params.get(0) instanceof Map<?, ?>) {
						params.set(0, route.mergeParameterMap((Map<?, ?>)params.get(0)));
					}
				} else if (o instanceof Map<?, ?>) {
					params = new ArrayList<Object>(1);
					params.add(route.mergeParameterMap((Map<?, ?>)o));
				} else {
					throw new IllegalArgumentException("failed to convert parameters from JSON.");
				}
			}
			json.setContext(component);
			
			Method method = container.getMethod(component, methodName, params);
			
			Produce produce = method.getAnnotation(Produce.class);
			if (produce != null) response.setContentType(produce.value());
			
			res = container.execute(json, component, method, params);
			
			if (produce != null) return;
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
		
		try {
			if (res == null
					|| res instanceof CharSequence
					|| res instanceof Boolean
					|| res instanceof Number
					|| res instanceof Date) {
				if (status != SC_CREATED) status = SC_NO_CONTENT;
				response.setStatus(status);
			} else {
				response.setContentType((callback != null) ? "text/javascript" : "application/json");
				Writer writer = response.getWriter();
				json.setPrettyPrint(container.isDebugMode());
				
				if (callback != null) writer.append(callback).append("(");
				json.format(res, writer);
				if (callback != null) writer.append(");");
			}
			
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			container.error("Fails to format.", e);
			response.sendError(SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
			return;
		}
	}
	
	@Override
	public void destroy() {
		container.destory();
		super.destroy();
	}
	
	JSON createJSON(Locale locale) throws ServletException {
		JSON json = null;
		try {
			json = (JSON)config.processor.newInstance();
		} catch (Exception e) {
			throw new ServletException(e);
		}
		json.setLocale(locale);		
		return json;
	}
	
	@SuppressWarnings("unchecked")
	static <T> T cast(Object o) {
		return (T)o;
	}
	
	public static class WebServiceJSON extends JSON {
		@Override
		protected boolean ignore(Context context, Class<?> target, Member member) {
			return member.getDeclaringClass().equals(Throwable.class)
				|| super.ignore(context, target, member);
		}
	}
	
	static class RouteMapping {
		private static final Pattern PLACE_PATTERN = Pattern.compile("\\{\\s*(\\p{javaJavaIdentifierStart}[\\p{javaJavaIdentifierPart}\\.-]*)\\s*(?::\\s*((?:[^{}]|\\{[^{}]*\\})*)\\s*)?\\}");
		private static final Pattern DEFAULT_PATTERN = Pattern.compile("[^/()]+");
		static final Map<String, String> DEFAULT_RESTMAP = new HashMap<String, String>();
		
		static {
			DEFAULT_RESTMAP.put("GET", "find");
			DEFAULT_RESTMAP.put("POST", "create");
			DEFAULT_RESTMAP.put("PUT", "update");
			DEFAULT_RESTMAP.put("DELETE", "delete");
		}
		
		Pattern pattern;
		List<String> names;
		String target;
		Map<String, String> restmap = DEFAULT_RESTMAP;
		
		@SuppressWarnings("unchecked")
		public RouteMapping(String path, List<Object> target, Map<String, Pattern> definitions) {
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
			this.target = (String)target.get(0);
			if (target.size() > 1) restmap = (Map<String, String>)target.get(1);
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
				return new Route(request, target, restmap, params);
			}
			return null;
		}
	}
	
	static class Route {
		private static final Pattern REPLACE_PATTERN = Pattern.compile("\\$\\{(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)\\}");

		private String target;
		private String method;
		private String restMethod;
		private Map<Object, Object> params;
		
		private boolean isRpcMode;
		private String contentType;
		
		@SuppressWarnings("unchecked")
		public Route(HttpServletRequest request, String target, Map<String, String> restmap, Map<String, Object> params) throws IOException {
			this.target = target;
			this.params = (Map)params;
			this.restMethod = getParameter("method");
			
			String contentType = request.getContentType();
			if (contentType == null) contentType = "";
			int index = contentType.indexOf(';');
			
			this.contentType = (index > -1) ? contentType.substring(0, index) : contentType;
			
			if ("rpc".equalsIgnoreCase(getParameter("class"))) {
				isRpcMode = true;
				
				this.method = request.getMethod().toUpperCase();
			} else {
				Map<String, String[]> pmap = request.getParameterMap();
				
				if ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType)
						&& request.getQueryString() != null
						&& request.getQueryString().trim().length() != 0) {
						
					Map<String, String[]> pairs = parseQueryString(request.getQueryString(), request.getCharacterEncoding());
					
					for (Map.Entry<String, String[]> entry : pairs.entrySet()) {
						String[] values = pmap.get(entry.getKey());
						if (values.length <= entry.getValue().length) continue;
						
						int size = values.length;
						for (String estr : entry.getValue()) {
							for (int i = 0; i < values.length; i++) {
								if (estr.equals(values[i])) {
									values[i] = null;
									size--;
									break;
								}
							}
						}
						
						String[] newValues = new String[size];
						int pos = 0;
						for (String pstr : values) {
							if (pstr != null) newValues[pos++] = pstr;
						}
						pmap.put(entry.getKey(), newValues);
					}
				}
				
				parseParameter(pmap, this.params);
				
				String m = getParameter("_method");
				if (m == null) m = request.getMethod();
				this.method = m.toUpperCase();
				
				if (this.restMethod == null) {
					this.restMethod = restmap.get(getMethod());
				}
			}
		}
		
		public String getMethod() {
			return method;
		}
		
		public String getRestMethod() {
			return restMethod;
		}
		
		public boolean isRpcMode() {
			return isRpcMode;
		}
		
		public String getParameter(String name) {
			Object o = params.get(name);
			
			if (o instanceof Map<?, ?>) {
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
		
		public String getComponentClass(Container container, String sub) {
			Matcher m = REPLACE_PATTERN.matcher(target);
			StringBuffer sb = new StringBuffer();
			while (m.find()) {
				String key = m.group(1);
				String value = getParameter(key);
				
				if (key.equals("class") && container.namingConversion) {
					value = toUpperCamel((sub != null) ? sub 
						: (value != null) ? value : "");
				} else if (key.equals("package")) {
					value = value.replace('/', '.');
				}
				
				m.appendReplacement(sb, (value != null) ? value : "");
			}
			m.appendTail(sb);
			return sb.toString();
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
			return "application/json".equalsIgnoreCase(contentType);
		}
		
		private Map<String, String[]> parseQueryString(String qs, String encoding) throws UnsupportedEncodingException {
			Map<String, String[]> pairs = new HashMap<String, String[]>();
			
			int start = 0;
			String key = null;
			
			for (int i = 0; i <= qs.length(); i++) {
				if (i == qs.length() || qs.charAt(i) == '&') {
					String value = null;
					String[] values = null;
					
					if (key == null) {
						key = URLDecoder.decode(qs.substring(start, i), encoding);
						value = "";
					} else {
						value = URLDecoder.decode(qs.substring(start, i), encoding);
					}
					
					if (pairs.containsKey(key)) {
						String[] tmp = pairs.get(key);
						values = new String[tmp.length+1];
						System.arraycopy(tmp, 0, values, 0, tmp.length);
						values[tmp.length] = value;
					} else {
						values = new String[] { value };
					}
					
					pairs.put(key, values);
					key = null;
					
					start = i+1;
				} else if (qs.charAt(i) == '=') {
					key = URLDecoder.decode(qs.substring(start, i), encoding);
					start = i+1;
				}
			}
			
			return pairs;
		}
		
		@SuppressWarnings("unchecked")
		private static void parseParameter(Map<String, String[]> pairs, Map<Object, Object> params) {
			for (Map.Entry<String, String[]> entry : pairs.entrySet()) {
				String name = entry.getKey();
				String[] values = entry.getValue();
				
				int start = 0;
				char old = '\0';
				Map<Object, Object> current = params;
				for (int i = 0; i < name.length(); i++) {
					char c = name.charAt(i);
					if (c == '.' || c == '[') {
						String key = name.substring(start, (old == ']') ? i-1 : i);
						Object target = current.get(key);
						
						if (target instanceof Map) {
							current = (Map<Object, Object>)target;
						} else {
							Map<Object, Object> map = new LinkedHashMap<Object, Object>();
							if (target != null) map.put(null, target);
							current.put(key, map);
							current = map;
						}
						start = i+1;
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
								List<Object> list = ((List<Object>)target);
								for (String value : values) list.add(value);
							} else {
								List<Object> list = new ArrayList<Object>();
								list.add(target);
								for (String value : values) list.add(value);
								map.put(null, list);
							}
						} else if (values.length > 1) {
							List<Object> list = new ArrayList<Object>();
							for (String value : values) list.add(value);
							map.put(null, list);
						} else {
							map.put(null, values[0]);						
						}
					} else if (target instanceof List) {
						List<Object> list = ((List<Object>)target);
						for (String value : values) list.add(value);
					} else {
						List<Object> list = new ArrayList<Object>();
						list.add(target);
						for (String value : values) list.add(value);
						current.put(name, list);
					}
				} else if (values.length > 1) {
					List<Object> list = new ArrayList<Object>();
					for (String value : values) list.add(value);
					current.put(name, list);
				} else {
					current.put(name, values[0]);
				}
			}
		}
			
		private String toUpperCamel(String name) {
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
	}
}
