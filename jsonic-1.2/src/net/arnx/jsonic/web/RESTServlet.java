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
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
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

import static javax.servlet.http.HttpServletResponse.*;
import static net.arnx.jsonic.web.Container.*;

public class RESTServlet extends HttpServlet {	
	static class Config {
		public Class<? extends Container> container;
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
			container.init(this);
		} catch (Exception e) {
			throw new ServletException(e);
		}
		
		if (config.definitions == null) config.definitions = new HashMap<String, Pattern>();
		if (!config.definitions.containsKey("package")) config.definitions.put("package", Pattern.compile(".+"));
		
		if (config.mappings != null) {
			for (Map.Entry<String, List<Object>> entry : config.mappings.entrySet()) {
				mappings.add(new RouteMapping(entry.getKey(), entry.getValue(), config.definitions));
			}
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		container.start(request, response);
		try {
			doREST(request, response);
		} finally {
			container.end(request, response);
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		container.start(request, response);
		try {
			doREST(request, response);
		} finally {
			container.end(request, response);
		}
	}
	
	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		container.start(request, response);
		try {
			doREST(request, response);
		} finally {
			container.end(request, response);
		}
	}
	
	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
		throws ServletException, IOException {
		container.start(request, response);
		try {
			doREST(request, response);
		} finally {
			container.end(request, response);
		}
	}
	
	protected void doREST(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		
		String uri = (request.getContextPath().equals("/")) ?
				request.getRequestURI() : 
				request.getRequestURI().substring(request.getContextPath().length());
		
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
				try {
					if (in != null) in.close();
				} catch (IOException e2) {
					// no handle
				}
			}
			return;
		}
		
		Route route = null;
		for (RouteMapping m : mappings) {
			if ((route = m.matches(request, uri)) != null) {
				container.debug("Route found: " + request.getMethod() + " " + uri);
				break;
			}
		}
		
		if (route == null) {
			response.sendError(SC_NOT_FOUND, "Not Found");
			return;
		}
		
		
		int status = SC_OK;
		String callback = null;
		
		if ("GET".equals(route.getHttpMethod())) {
			callback = route.getParameter("callback");
		} else if ("POST".equals(route.getHttpMethod())) {
			status = SC_CREATED;
		}
		
		String methodName = route.getRestMethod();
		if (methodName == null || methodName.equals(container.init) || methodName.equals(container.destroy)) {
			container.debug("Method mapping not found: " + route.getHttpMethod());
			response.sendError(SC_NOT_FOUND, "Not Found");
			return;
		}
		
		Object result = null;
		
		JSON json = container.createJSON(request.getLocale());
		
		try {
			Object component = container.getComponent(route.getComponentClass(container));
			if (component == null) {
				container.debug("Component not found: " + route.getComponentClass(container));
				response.sendError(SC_NOT_FOUND, "Not Found");
				return;
			}
			
			List<Object> params = null;
			if (!isJSONType(request.getContentType())) {
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
			Method method = container.getMethod(component, methodName, params);
			
			Produce produce = method.getAnnotation(Produce.class);
			if (produce == null) {
				json.setContext(component);
				result = container.execute(json, component, method, params);
			} else {
				response.setContentType(produce.value());
				json.setContext(component);
				container.execute(json, component, method, params);
				return;
			}
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
			result = cause;
		} catch (Exception e) {
			container.error("Internal error occurred.", e);
			response.sendError(SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
			return;
		}
		
		if (result == null
				|| result instanceof CharSequence
				|| result instanceof Boolean
				|| result instanceof Number
				|| result instanceof Date) {
			if (status != SC_CREATED) status = SC_NO_CONTENT;
			response.setStatus(status);
		} else {
			response.setContentType((callback != null) ? "text/javascript" : "application/json");
			Writer writer = response.getWriter();
			json.setPrettyPrint(container.isDebugMode());
			
			if (callback != null) writer.append(callback).append("(");
			json.format(result, writer);
			if (callback != null) writer.append(");");
		}
	}
	
	@Override
	public void destroy() {
		container.destory();
		super.destroy();
	}
	
	static class RouteMapping {
		private static final Pattern PLACE_PATTERN = Pattern.compile("\\{\\s*(\\p{javaJavaIdentifierStart}[\\p{javaJavaIdentifierPart}\\.-]*)\\s*(?::\\s*((?:[^{}]|\\{[^{}]*\\})*)\\s*)?\\}");
		private static final Pattern DEFAULT_PATTERN = Pattern.compile("[^/().]+");
		
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
		Map<?, ?> options = DEFAULT_RESTMAP;
		
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
			this.target = (String)target.get(0);
			if (target.size() > 1 && target.get(1) instanceof Map<?, ?>) {
				options = cast(target.get(1));
			}
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
				
				String httpMethod = request.getParameter("_method");
				if (httpMethod == null) httpMethod = request.getMethod();
				
				Object restMethod = params.get("method");
				if (restMethod == null) {
					restMethod = options.get(httpMethod);
				}
				
				if (restMethod instanceof String) {
					parseParameter(request.getParameterMap(), (Map)params);
					return new Route(httpMethod, (String)restMethod, target, options, params);
				}
			}
			return null;
		}
		
		
		@SuppressWarnings("unchecked")
		static void parseParameter(Map<String, String[]> pairs, Map<Object, Object> params) {
			for (Map.Entry<String, String[]> entry : pairs.entrySet()) {
				String name = entry.getKey();
				boolean multiValue = false;
				if (name.endsWith("[]")) {
					name = name.substring(0, name.length()-2);
					multiValue = true;
				}
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
								List<Object> list = new ArrayList<Object>(values.length+1);
								list.add(target);
								for (String value : values) list.add(value);
								map.put(null, list);
							}
						} else if (multiValue || values.length > 1) {
							List<Object> list = new ArrayList<Object>(values.length);
							for (String value : values) list.add(value);
							map.put(null, list);
						} else {
							map.put(null, values[0]);						
						}
					} else if (target instanceof List) {
						List<Object> list = ((List<Object>)target);
						for (String value : values) list.add(value);
					} else {
						List<Object> list = new ArrayList<Object>(values.length+1);
						list.add(target);
						for (String value : values) list.add(value);
						current.put(name, list);
					}
				} else if (multiValue || values.length > 1) {
					List<Object> list = new ArrayList<Object>(values.length);
					for (String value : values) list.add(value);
					current.put(name, list);
				} else {
					current.put(name, values[0]);
				}
			}
		}
	}
	
	static class Route {
		private static final Pattern REPLACE_PATTERN = Pattern.compile("\\$\\{(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)\\}");
		
		private String target;
		private String httpMethod;
		private String restMethod;
		private Map<Object, Object> params;
		
		@SuppressWarnings("unchecked")
		public Route(String httpMethod, String restMethod, String target, Map<?, ?> options, Map<String, Object> params) throws IOException {
			this.httpMethod = httpMethod;
			this.restMethod = restMethod;
			this.target = target;
			this.params = (Map)params;
		}
		
		public String getHttpMethod() {
			return httpMethod;
		}
		
		public String getRestMethod() {
			return restMethod;
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
		
		public String getComponentClass(Container container) {
			Matcher m = REPLACE_PATTERN.matcher(target);
			StringBuffer sb = new StringBuffer();
			while (m.find()) {
				String key = m.group(1);
				String value = getParameter(key);
				
				if (key.equals("class") && container.namingConversion) {
					value = toUpperCamel(value);
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
	}
}
