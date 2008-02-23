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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
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
import net.arnx.jsonic.JSONConvertException;
import net.arnx.jsonic.JSONParseException;

import static javax.servlet.http.HttpServletResponse.*;

public class WebServiceServlet extends HttpServlet {
	private static final long serialVersionUID = -63348112220078595L;
	
	class Config {
		public Class<? extends Container> container;
		public String encoding = "UTF-8";
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
		 
		JSON json = new JSON();
		
		try {
			json.setContext(this);
			config = json.parse(configText, Config.class);
			if (config.container == null) config.container = Container.class;

			json.setContext(config.container);
			container = (Container)json.parse(configText, config.container);
			container.init(getServletContext());
		} catch (Exception e) {
			throw new ServletException(e);
		}
		
		if (config.definitions == null) config.definitions = new HashMap<String, Pattern>();
		if (!config.definitions.containsKey("package")) config.definitions.put("package", Pattern.compile(".+"));
		if (!config.definitions.containsKey(null)) config.definitions.put(null, Pattern.compile("[^/]+"));
		
		if (config.mappings != null) {
			for (Map.Entry<String, String> entry : config.mappings.entrySet()) {
				mappings.add(new RouteMapping(entry.getKey(), entry.getValue(), config.definitions));
			}
		}
	}

	protected Route preprocess(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		if (config.encoding != null) {
			request.setCharacterEncoding(config.encoding);
			response.setCharacterEncoding(config.encoding);
		}
		
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
				if (in != null) in.close();
			}
			return null;
		}
		
		for (RouteMapping m : mappings) {
			Route route = m.matches(request.getMethod(), uri);
			if (route != null) {
				container.debug("route found: " + request.getMethod() + " " + uri);
				return route;
			}
		}
		response.sendError(SC_NOT_FOUND);
		return null;
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {		
		Route route = preprocess(request, response);
		
		if (route == null) {
			return;
		} else if ("rpc".equalsIgnoreCase(route.get("class"))) {
			response.addHeader("Allow", "POST");
			response.sendError(SC_METHOD_NOT_ALLOWED);
			return;
		} else {
			doREST(route, request, response);
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Route route = preprocess(request, response);
		
		if (route == null) {
			return;
		} else if ("rpc".equalsIgnoreCase(route.get("class"))) {
			doRPC(route, request, response);
		} else {
			String method = request.getParameter("_method");
			if (method != null) route.setMethod(method);
			doREST(route, request, response);
		}
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Route route = preprocess(request, response);
		
		if (route == null) {
			return;
		} else if ("rpc".equalsIgnoreCase(route.get("class"))) {
			response.addHeader("Allow", "POST");
			response.sendError(SC_METHOD_NOT_ALLOWED);
		} else {
			doREST(route, request, response);
		}
	}
	
	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
		throws ServletException, IOException {
		Route route = preprocess(request, response);
		
		if (route == null) {
			return;
		} else if ("rpc".equalsIgnoreCase(route.get("class"))) {
			response.addHeader("Allow", "POST");
			response.sendError(SC_METHOD_NOT_ALLOWED);
		} else {
			doREST(route, request, response);
		}
	}
	
	class RpcRequest {
		public String method;
		public List params;
		public Object id;
	}
	
	class RpcError {
		public int code;
		public String message;
		
		public RpcError(int code, String message) {
			this.code = code;
			this.message = message;
		}
	}
	
	protected void doRPC(Route route, HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		
		JSONInvoker json = new JSONInvoker(this);
		
		// request processing
		RpcRequest req = null;
		Object result = null;
		RpcError error = null;
		try {			
			req = json.parse(request.getReader(), RpcRequest.class);
			if (req == null || req.method == null || req.params == null) {
				response.setStatus(SC_BAD_REQUEST);
				error = new RpcError(-32600, "Invalid Request.");
			} else {
				int delimiter = req.method.lastIndexOf('.');
				if (delimiter <= 0 && delimiter+1 == req.method.length()) {
					throw new NoSuchMethodException(req.method);
				} else {
					route.put("class", req.method.substring(0, delimiter));
					Object component = container.getComponent(route.getComponentClass());
					if (component == null) {
						throw new NoSuchMethodException(req.method);
					}
					
					json.setContext(component);
					result = json.invoke(component, req.method.substring(delimiter+1), req.params);
				}
			}
		} catch (ClassNotFoundException e) {
			container.debug(e.getMessage());
			response.sendError(SC_NOT_FOUND);
			error = new RpcError(-32601, "Method not found.");
		} catch (NoSuchMethodException e) {
			container.debug(e.getMessage());
			response.sendError(SC_NOT_FOUND);
			error = new RpcError(-32601, "Method not found.");
		} catch (JSONParseException e) {
			container.debug(e.getMessage());
			response.setStatus(SC_BAD_REQUEST);
			error = new RpcError(-32700, "Parse error.");
		} catch (JSONConvertException e) {
			container.debug(e.getMessage());
			response.setStatus(SC_BAD_REQUEST);
			error = new RpcError(-32602, "Invalid params.");
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			container.error(cause.getMessage(), cause);
			if (cause instanceof UnsupportedOperationException) {
				response.sendError(SC_NOT_FOUND);
				error = new RpcError(-32601, "Method not found.");				
			} else if (cause instanceof IllegalArgumentException) {
				response.setStatus(SC_BAD_REQUEST);
				error = new RpcError(-32602, "Invalid params.");
			} else {
				response.setStatus(SC_INTERNAL_SERVER_ERROR);
				error = new RpcError(-32603, cause.getMessage());
			}
		} catch (Exception e) {
			container.error(e.getMessage(), e);
			response.setStatus(SC_INTERNAL_SERVER_ERROR);
			error = new RpcError(-32603, "Internal error.");
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
		res.put("error", error);
		res.put("id", (req != null) ? req.id : null);

		Writer writer = response.getWriter();

		try {	
			json.setPrettyPrint(container.isDebugMode());
			json.format(res, writer);
		} catch (Exception e) {
			container.error(e.getMessage(), e);
			response.setStatus(SC_INTERNAL_SERVER_ERROR);
			res.clear();
			res.put("result", null);
			res.put("error", new RpcError(-32603, "Internal error."));
			res.put("id", (req != null) ? req.id : null);
			json.format(res, writer);
			return;
		}
	}
	
	@SuppressWarnings("unchecked")
	protected void doREST(Route route, HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		
		String methodName = route.getMethod();
		int status = SC_OK;
		String callback = null;
		
		if ("get".equals(route.getMethod())) {
			methodName = "find";
			callback = request.getParameter("callback");
		} else if ("post".equals(route.getMethod())) {
			methodName = "create";
			status = SC_CREATED;
		} else if ("put".equals(route.getMethod())) {
			methodName = "update";
		} else if ("delte".equals(route.getMethod())) {
			methodName = "delete";
		}
		
		// request processing
		JSONInvoker json = new JSONInvoker(this);
		
		Object res = null;
		try {
			Object component = container.getComponent(route.getComponentClass());
			if (component == null) {
				response.sendError(SC_NOT_FOUND);
				return;
			}
			
			List<Object> params = null;
			if ("get".equals(route.getMethod())) {
				params = new ArrayList<Object>();
				Map<String, Object> contents = getParameterMap(request);
				contents.putAll(route);
				params.add(contents);
			} else {
				Object o = json.parse(request.getReader());
				if (o instanceof List) {
					params = (List)o;
					Map<String, Object> contents = getParameterMap(request);
					contents.putAll(route);
					params.add(contents);
				} else if (o instanceof Map) {
					Map<String, Object> contents = (Map)o;
					contents.putAll(getParameterMap(request));
					contents.putAll(route);
					params = new ArrayList<Object>();
					params.add(contents);
				} else {
					throw new IllegalArgumentException("failed to convert parameters from JSON.");
				}
			}
			json.setContext(component);
			res = json.invoke(component, methodName, params);
		} catch (ClassNotFoundException e) {
			container.debug(e.getMessage());
			response.sendError(SC_NOT_FOUND);
			return;			
		} catch (NoSuchMethodException e) {
			container.debug(e.getMessage());
			response.sendError(SC_NOT_FOUND);
			return;
		} catch (JSONParseException e) {
			container.debug(e.getMessage());
			response.sendError(SC_BAD_REQUEST);
			return;
		} catch (JSONConvertException e) {
			container.debug(e.getMessage());
			response.sendError(SC_BAD_REQUEST);
			return;
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			container.error(cause.getMessage(), cause);
			if (cause instanceof UnsupportedOperationException) {
				response.sendError(SC_NOT_FOUND);				
			} else if (cause instanceof IllegalArgumentException) {
				response.sendError(SC_BAD_REQUEST, cause.getMessage());
			} else {
				response.sendError(SC_INTERNAL_SERVER_ERROR, cause.getMessage());
			}
		} catch (Exception e) {
			container.error(e.getMessage(), e);
			response.sendError(SC_INTERNAL_SERVER_ERROR);
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
		} catch (Exception e) {
			container.error(e.getMessage(), e);
			response.sendError(SC_INTERNAL_SERVER_ERROR);
		}		
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> getParameterMap(HttpServletRequest request) {
		Map<String, Object> map = new HashMap<String, Object>();
		
		for (Enumeration<String> e = request.getParameterNames(); e.hasMoreElements(); ) {
			String name = e.nextElement();
			String[] names = name.split("\\.");
			String[] values = request.getParameterValues(name);
			
			Map<String, Object> current = map;
			for (int i = 0; i < names.length-1; i++) {
				Map<String, Object> target = (Map<String, Object>)current.get(names[i]);
				if (target == null) {
					target = new HashMap<String, Object>();
					current.put(names[i], target);
				}
				current = target;
			}
			current.put(names[names.length-1], 
					(values == null || values.length == 0) ? null :
					(values.length == 1) ? values[0] : values);
		}
		
		return map;
	}
	
	@Override
	public void destroy() {
		container.destory();
		super.destroy();
	}
	
	class JSONInvoker extends JSON {
		public JSONInvoker(Object context) {
			super(context);
		}
		
		public Object invoke(Object o, String methodName, List args) throws Exception {
			if (args == null) {
				args = Collections.EMPTY_LIST;
			}
			
			Method method = container.findMethod(o, toLowerCamel(methodName), args);
			Class<?>[] paramTypes = method.getParameterTypes();
			Object[] params = new Object[Math.min(paramTypes.length, args.size())];
			List<Object> list = new ArrayList<Object>();
			for (int i = 0; i < params.length; i++) {
				params[i] = convert(list, args.get(i), paramTypes[i], paramTypes[i]);
			}
			
			return method.invoke(o, params);
		}
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
	
class RouteMapping {
	private static final Pattern PLACE_PATTERN = Pattern.compile("\\[(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)\\]");
	
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
			Pattern p = definitions.get(name);
			if (p == null) p = definitions.get(null);
			m.appendReplacement(sb, "\\\\E(" + p.pattern().replaceAll("\\((?!\\?)", "(?:") + ")\\\\Q");
		}
		m.appendTail(sb);
		sb.append("\\E$");
		this.pattern = Pattern.compile(sb.toString());
		this.target = target;
	}
	
	public Route matches(String method, String path) {
		Matcher m = pattern.matcher(path);
		if (m.matches()) {
			Route route = new Route(method, target);
			for (int i = 0; i < names.size(); i++) {
				route.put(names.get(i), m.group(i+1));
			}
			return route;
		}
		return null;
	}
}

class Route extends HashMap<String, String> {
	private static final long serialVersionUID = 9001379442185239302L;
	
	private static final Pattern REPLACE_PATTERN = Pattern.compile("\\$\\{(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)\\}");
	private String target;
	private String method;
	
	public Route(String method, String target) {
		this.method = method.toLowerCase();
		this.target = target;
	}
	
	public void setMethod(String method) {
		this.method = method.toLowerCase();
	}
	
	public String getMethod() {
		return method;
	}
	
	public Class<? extends Object> getComponentClass() throws ClassNotFoundException {
		Matcher m = REPLACE_PATTERN.matcher(target);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String key = m.group(1);
			String value = remove(key);
			if (value == null) {
				value = "";
			} else if (key.equals("class")) {
				value = toUpperCamel(value);
			} else if (key.equals("package")) {
				value = value.replace('/', '.');
			}
			m.appendReplacement(sb, value);
		}
		m.appendTail(sb);
		return Class.forName(sb.toString());
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