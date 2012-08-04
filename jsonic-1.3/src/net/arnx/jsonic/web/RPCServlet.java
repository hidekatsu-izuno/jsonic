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

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
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

import net.arnx.jsonic.JSON;
import net.arnx.jsonic.JSONException;
import net.arnx.jsonic.JSONHint;
import net.arnx.jsonic.util.ClassUtil;

import static javax.servlet.http.HttpServletResponse.*;
import static net.arnx.jsonic.web.Container.*;

public class RPCServlet extends HttpServlet {	
	static class Config {
		public Class<? extends Container> container;
		
		@JSONHint(anonym="target")
		public Map<String, RouteMapping> mappings;
		
		public Map<String, Pattern> definitions;
		public Map<String, Integer> errors;
	}
	
	protected Container container;
	
	Config config;
	
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
			container.init(this);
		} catch (Exception e) {
			throw new ServletException(e);
		}
		
		if (config.definitions == null) config.definitions = new HashMap<String, Pattern>();
		if (!config.definitions.containsKey("package")) config.definitions.put("package", Pattern.compile(".+"));
		
		if (config.errors == null) config.errors = Collections.emptyMap();
		
		if (config.mappings == null) config.mappings = Collections.emptyMap();
		for (Map.Entry<String, RouteMapping> entry : config.mappings.entrySet()) {
			entry.getValue().init(entry.getKey(), config);
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doRPC(request, response);
	}
	
	protected void doRPC(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		
		JSON json = null;
		boolean isBatch = false;
		List<Object> responseList = new ArrayList<Object>();
		
		try {
			ExternalContext.start(getServletConfig(), getServletContext(), request, response);		
			container.start(request, response);
			
			String uri = (request.getContextPath().equals("/")) ?
					request.getRequestURI() : 
					request.getRequestURI().substring(request.getContextPath().length());
			
			Route route = null;
			for (RouteMapping m : config.mappings.values()) {
				if ((route = m.matches(request, uri)) != null) {
					if (container.isDebugMode()) {
						container.debug("Route found: " + request.getMethod() + " " + uri + " -> " + route);
					}
					break;
				}
			}
			
			if (route == null || !isJSONType(request.getContentType())) {
				response.sendError(SC_NOT_FOUND, "Not Found");
				return;
			}
			
			json = container.createJSON(request.getLocale());
			
			// request processing
			List<Object> requestList = new ArrayList<Object>(0);
			Object value = json.parse(request.getReader());
			if (value instanceof List<?> && !((List<?>)value).isEmpty()) {
				requestList = cast(value);					
				isBatch = true;
			} else if (value instanceof Map<?,?> && !((Map<?,?>)value).isEmpty()) {
				requestList = Arrays.asList(value);
			} else {
				throw new IllegalArgumentException("Request is empty.");
			}
			
			for (int i = 0; i < requestList.size(); i++) {
				Map<?,?> req = (Map<?,?>)requestList.get(i);
				
				String rjsonrpc = null;
				String rmethod = null;
				Object rparams = null;
				Object rid = null;
				
				Object result = null;
				Map<String, Object> error = null;
	
				try {
					if (req.get("jsonrpc") == null || "2.0".equals(req.get("jsonrpc"))) {
						rjsonrpc = (String)req.get("jsonrpc");
					} else {
						throw new IllegalArgumentException("jsonrpc is unrecognized version: " + req.get("jsonrpc"));
					}
					
					if (req.get("method") instanceof String) {
						rmethod = (String)req.get("method");
						if (rjsonrpc != null && rmethod.startsWith("rpc.")) {
							container.warn("Method names that begin with 'rpc.' are reserved for system extensions.");
						}
					} else {
						throw new IllegalArgumentException("method must " + ((req.get("method") == null) ? "not be null." : "be string."));
					}
					
					if (req.get("params") instanceof List<?> || (rjsonrpc != null && req.get("params") instanceof Map<?, ?>)) {
						rparams = req.get("params");
					} else if (rjsonrpc != null && req.get("params") == null) {
						rparams = new ArrayList<Object>(0);
					} else {
						throw new IllegalArgumentException("params must be array" + ((rjsonrpc != null) ? " or object." : "."));
					}
					
					if (rjsonrpc == null || (req.get("id") == null || req.get("id") instanceof String || req.get("id") instanceof Number)) {
						rid = req.get("id");
					} else {
						throw new IllegalArgumentException("id must be string, number or null.");
					}
					
					String subcompName = null;
					String methodName = rmethod;
					if (route.getParameter("class") == null) {
						int sep = rmethod.lastIndexOf('.');
						subcompName = (sep != -1) ? rmethod.substring(0, sep) : null;
						methodName = (sep != -1) ? rmethod.substring(sep+1) : rmethod;
					}
					
					Object component = container.getComponent(route.getComponentClass(container, subcompName));
					if (component == null) {
						throw new NoSuchMethodException("Method not found: " + rmethod);
					}
					
					List<?> params = (rparams instanceof List<?>) ? (List<?>)rparams : Arrays.asList(rparams);
					Method method = container.getMethod(component, methodName, params);
					if (method == null) {
						throw new NoSuchMethodException("Method not found: " + rmethod);					
					}
					
					json.setContext(component);
					result = container.execute(json, component, method, params);
				} catch (Exception e) {
					error = new LinkedHashMap<String, Object>();
					if (e instanceof IllegalArgumentException) {
						container.debug("Invalid Request.", e);
						container.exception(e, request, response);
						error.put("code", -32600);
						error.put("message", "Invalid Request.");
					} else if (e instanceof ClassNotFoundException) {
						container.debug("Class Not Found.", e);
						container.exception(e, request, response);
						error.put("code", -32601);
						error.put("message", "Method not found.");
					} else if (e instanceof NoSuchMethodException) {
						container.debug("Method Not Found.", e);
						container.exception(e, request, response);
						error.put("code", -32601);
						error.put("message", "Method not found.");
					} else if (e instanceof JSONException) {
						container.debug("Invalid params.", e);
						container.exception(e, request, response);
						error.put("code", -32602);
						error.put("message", "Invalid params.");
					} else if (e instanceof InvocationTargetException) {
						Throwable cause = e.getCause();
						if (cause instanceof Error) {
							throw (Error)cause;
						}
						
						container.debug("Fails to invoke method.", cause);
						container.exception((Exception)cause, request, response);
						
						if (cause instanceof IllegalStateException || cause instanceof UnsupportedOperationException) {
							error.put("code", -32601);
							error.put("message", "Method not found.");
						} else if (cause instanceof IllegalArgumentException) {
							error.put("code", -32602);
							error.put("message", "Invalid params.");
						} else {
							Integer errorCode = null;
							for (Map.Entry<String, Integer> entry : config.errors.entrySet()) {
								Class<?> cls = ClassUtil.findClass(entry.getKey());
								if (cls != null && cls.isAssignableFrom(cause.getClass()) && entry.getValue() != null) {
									errorCode = entry.getValue();
									break;
								}
							}
							if (errorCode != null) {
								error.put("code", errorCode);
								error.put("message",  cause.getClass().getSimpleName() + ": " + cause.getMessage());
								error.put("data", container.getErrorData(cause));
							} else {
								container.error("Internal error occurred.", cause);
								error.put("code", -32603);
								error.put("message", "Internal error.");
							}
						}
					} else {
						container.error("Internal error occurred.", e);
						container.exception(e, request, response);
						error.put("code", -32603);
						error.put("message", "Internal error.");
					}
				}
				
				// it's notification when id was null
				if (rmethod != null && (rjsonrpc == null && rid == null) || (rjsonrpc != null && req != null && !req.containsKey("id"))) {
					continue;
				}
				
				Map<String, Object> responseData = new LinkedHashMap<String, Object>();
				if (rjsonrpc != null) {
					responseData.put("jsonrpc", rjsonrpc);
					if (result != null) responseData.put("result", result);
					if (error != null) responseData.put("error", error);
				} else {
					responseData.put("result", result);
					responseData.put("error", error);
				}
				responseData.put("id", rid);
				
				responseList.add(responseData);
			}
		} catch (Exception e) {
			Map<String, Object> error = new LinkedHashMap<String, Object>();
			if (e instanceof JSONException) {
				container.debug("Fails to parse JSON.", e);
				error.put("code", -32700);
				error.put("message", "Parse error.");
				error.put("data", container.getErrorData(e));
			} else {
				container.debug("Invalid Request.", e);
				error.put("code", -32600);
				error.put("message", "Invalid Request.");
			}
			
			Map<String, Object> responseData = new LinkedHashMap<String, Object>();
			responseData.put("jsonrpc", "2.0");
			responseData.put("error", error);
			responseData.put("id", null);
			
			responseList.add(responseData);
		} finally {
			try {
				container.end(request, response);				
			} finally {
				ExternalContext.end();				
			}
		}
		
		if (response.isCommitted()) return;
		
		// it's notification when id was null for all requests.
		if (responseList.isEmpty()) {
			response.setStatus(SC_ACCEPTED);
			return;
		}
		
		// response processing
		response.setContentType("application/json");
		
		Writer writer = response.getWriter();
		
		Object target = (isBatch) ? responseList : responseList.get(0);
		json.setContext(target);
		json.format(target, writer);
	}
	
	@Override
	public void destroy() {
		container.destory();
		super.destroy();
	}
	
	static class RouteMapping {
		static final Pattern PLACE_PATTERN = Pattern.compile("\\{\\s*(\\p{javaJavaIdentifierStart}[\\p{javaJavaIdentifierPart}\\.-]*)\\s*(?::\\s*((?:[^{}]|\\{[^{}]*\\})*)\\s*)?\\}");
		static final Pattern DEFAULT_PATTERN = Pattern.compile("[^/().]+");
		
		public String target;
		
		Config config;
		Pattern pattern;
		List<String> names;
		
		public RouteMapping() {
		}
		
		public void init(String path, Config config) {
			this.config = config;
			
			this.names = new ArrayList<String>();
			StringBuffer sb = new StringBuffer("^\\Q");
			Matcher m = PLACE_PATTERN.matcher(path);
			while (m.find()) {
				String name = m.group(1);
				names.add(name);
				Pattern p = (m.group(2) != null) ?  Pattern.compile(m.group(2)) : null;
				if (p == null && config.definitions.containsKey(name)) {
					p = config.definitions.get(name);
				}
				if (p == null) p = DEFAULT_PATTERN; 
				m.appendReplacement(sb, "\\\\E(" + p.pattern().replaceAll("\\((?!\\?)", "(?:").replace("\\", "\\\\") + ")\\\\Q");
			}
			m.appendTail(sb);
			sb.append("\\E$");
			this.pattern = Pattern.compile(sb.toString());
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
				return new Route(target, params);
			}
			return null;
		}
	}
	
	static class Route {
		static final Pattern REPLACE_PATTERN = Pattern.compile("\\$\\{(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)\\}");

		private String target;
		private Map<Object, Object> params;
		
		public Route(String target, Map<String, Object> params) throws IOException {
			this.target = target;
			this.params = cast(params);
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
					value = ClassUtil.toUpperCamel((value != null) ? value  : (sub != null) ? sub : "?");
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

		@Override
		public String toString() {
			return "Route [target=" + target + ", params=" + params + "]";
		}
	}
}
