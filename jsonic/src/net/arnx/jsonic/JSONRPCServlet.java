/*
 * Copyright 2007 Hidekatsu Izuno
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
package net.arnx.jsonic;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
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

import net.arnx.jsonic.container.Container;
import net.arnx.jsonic.container.SimpleContainer;

import static javax.servlet.http.HttpServletResponse.*;

public class JSONRPCServlet extends HttpServlet {
	private static final long serialVersionUID = -63348112220078595L;
	private static final Pattern URL_PATTERN = Pattern.compile("^(/(?:[^/]+/)*)([^/.]+)((?:\\.[^/]+)+)?$");
	
	class Config {
		public Class container;
		public String encoding = "UTF-8";
		public Map<String, String> routes;
	}
	
	private Container container;
	private Config config;
	
	private Map<Pattern, String> routes = new LinkedHashMap<Pattern, String>();
	
	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
		
		String configText = servletConfig.getInitParameter("config");
		if (configText == null) configText = "";
		 
		JSON json = new JSON() {
			protected void handleConvertError(String key, Object value, Class c, Type type, Exception e) throws Exception {
				throw e;
			}
		};
		
		try {
			json.setContext(this);
			config = json.parse(configText, Config.class);
		} catch (Exception e) {
			throw new ServletException(e);
		}
		
		try {
			if (config.container == null) config.container = SimpleContainer.class;
			
			json.setContext(config.container);
			container = (Container)json.parse(configText, config.container);
			container.init(getServletContext());
		} catch (Exception e) {
			throw new ServletException(e);
		}
		
		if (config.routes != null) {
			for (Map.Entry<String, String> entry : config.routes.entrySet()) {
				if (entry.getKey().startsWith("/")) {
					routes.put(Pattern.compile(entry.getKey().equals("/") ? "^/" : "^" + entry.getKey() + "/"), entry.getValue());					
				} else {
					container.debug("route needs to start with '/': " + entry.getKey());
				}
			}
		}
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		if (config.encoding != null) {
			request.setCharacterEncoding(config.encoding);
			response.setCharacterEncoding(config.encoding);
		}
		
		Matcher m = URL_PATTERN.matcher(request.getRequestURI());
		if (!m.matches()){
			response.sendError(SC_NOT_FOUND);
			return;
		}
		
		String[] pathes = new String[] {m.group(1), m.group(2), m.group(3)};
		pathes[0] = (request.getContextPath().equals("/")) ? pathes[0] : pathes[0].substring(request.getContextPath().length());
		
		request.setAttribute("pathes", pathes);
		
		super.service(request, response);
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String[] pathes = (String[])request.getAttribute("pathes");
		
		if ("rpc".equalsIgnoreCase(pathes[1])) {
			response.addHeader("Allow", "POST");
			response.sendError(SC_METHOD_NOT_ALLOWED);
			return;
		} else {	
			doREST("GET", pathes, request, response);
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String[] pathes = (String[])request.getAttribute("pathes");
		
		if ("rpc".equalsIgnoreCase(pathes[1])) {
			doRPC(pathes, request, response);
		} else {
			String method = request.getParameter("_method");
			if (method == null || !("GET".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method))) {
				method = "POST";
			}
			doREST(method, pathes, request, response);
		}
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String[] pathes = (String[])request.getAttribute("pathes");
		
		if ("rpc".equalsIgnoreCase(pathes[1])) {
			response.addHeader("Allow", "POST");
			response.sendError(SC_METHOD_NOT_ALLOWED);
			return;
		} else {
			doREST("PUT", pathes, request, response);
		}
	}
	
	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
		throws ServletException, IOException {
		String[] pathes = (String[])request.getAttribute("pathes");
		
		if ("rpc".equalsIgnoreCase(pathes[1])) {
			response.addHeader("Allow", "POST");
			response.sendError(SC_METHOD_NOT_ALLOWED);
			return;
		} else {
			doREST("DELETE", pathes, request, response);
		}
	}
	
	class Request {
		public String method;
		public List params;
		public Object id;
	}
	
	protected void doRPC(String[] pathes, HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		
		if (!".json".equalsIgnoreCase(pathes[2])) {
			response.sendError(SC_NOT_FOUND);
			return;			
		}
		
		JSONInvoker json = new JSONInvoker(this);
		
		// request processing
		Request req = null;
		Object res = null;
		Map<String, Object> error = null;
		try {			
			req = json.parse(request.getReader(), Request.class);
			if (req == null || req.method == null || req.params == null) {
				response.setStatus(SC_BAD_REQUEST);
				error = new LinkedHashMap<String, Object>();
				error.put("code", -32700);
				error.put("message", "Invalid Request.");	
			} else {
				String[] targets = req.method.split("\\.");
				if (targets.length != 2 || targets[0].length() == 0 || targets[1].length() == 0) {
					throw new NoSuchMethodException(req.method);
				} else {
					pathes[1] = targets[0];
					
					Class c = getClassFromPath(pathes);
					if (c == null) {
						response.sendError(SC_NOT_FOUND);
						return;
					}
					
					Object component = container.getComponent(c);
					if (component == null) {
						throw new NoSuchMethodException(req.method);
					}
					
					json.setContext(component);
					res = json.invoke(component, targets[1], req.params, false);
				}
			}
		} catch (ClassNotFoundException e) {
			container.debug(e.getMessage());
			response.setStatus(SC_BAD_REQUEST);
			error = new LinkedHashMap<String, Object>();
			error.put("code", -32601);
			error.put("message", "Method not found.");
		} catch (JSONParseException e) {
			container.debug(e.getMessage());
			response.setStatus(SC_BAD_REQUEST);
			error = new LinkedHashMap<String, Object>();
			error.put("code", -32700);
			error.put("message", "Parse error.");
		} catch (NoSuchMethodException e) {
			container.debug(e.getMessage());
			response.setStatus(SC_BAD_REQUEST);
			error = new LinkedHashMap<String, Object>();
			error.put("code", -32601);
			error.put("message", "Method not found.");
		} catch (IllegalArgumentException e) {
			container.debug(e.getMessage());
			response.setStatus(SC_BAD_REQUEST);
			error = new LinkedHashMap<String, Object>();
			error.put("code", -32602);
			error.put("message", "Invalid params.");
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			container.error(cause.getMessage(), cause);
			response.setStatus(SC_INTERNAL_SERVER_ERROR);
			error = new LinkedHashMap<String, Object>();
			error.put("code", -32603);
			error.put("message", cause.getMessage());
		} catch (Exception e) {
			container.error(e.getMessage(), e);
			response.setStatus(SC_INTERNAL_SERVER_ERROR);
			error = new LinkedHashMap<String, Object>();
			error.put("code", -32603);
			error.put("message", "Internal error.");
		}
		
		// it's notification when id was null
		if (req.method != null && req.params != null && req.id == null) {
			response.setStatus(SC_ACCEPTED);
			return;
		}

		// response processing
		response.setContentType("application/json");
		
		try {
			Map<String, Object> map = new LinkedHashMap<String, Object>();
			map.put("result", res);
			map.put("error", error);
			map.put("id", (req != null) ? req.id : null);
				
			Writer writer = response.getWriter();
			json.setPrettyPrint(container.isDebugMode());
			
			json.format(map, writer);
		} catch (Exception e) {
			container.error(e.getMessage(), e);
			response.sendError(SC_INTERNAL_SERVER_ERROR);
			return;
		}
	}
	
	protected void doREST(String method, String[] pathes, HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		
		if (!".json".equalsIgnoreCase(pathes[2])) {
			response.sendError(SC_NOT_FOUND);
			return;			
		}
		
		String methodName = null;
		String callback = null;
		
		method = method.toUpperCase();
		if ("GET".equals(method)) {
			methodName = "find";
			callback = request.getParameter("callback");
		} else if ("POST".equals(method)) {
			methodName = "create";
			response.setStatus(SC_CREATED);
		} else if ("PUT".equals(method)) {
			methodName = "update";
		} else if ("DELETE".equals(method)) {
			methodName = "delete";
		}
		
		// request processing
		JSONInvoker json = new JSONInvoker(this);
		
		Object res = null;
		try {
			Class c = getClassFromPath(pathes);
			if (c == null) {
				response.sendError(SC_NOT_FOUND);
				return;				
			}
			
			Object component = container.getComponent(c);
			if (component == null) {
				response.sendError(SC_NOT_FOUND);
				return;
			}
			
			List params = null;
			if ("GET".equals(method)) {
				params = new ArrayList();
				params.add(getParameterMap(request));
			} else {
				Object contents = json.parse(request.getReader());
				if (contents instanceof List) {
					params = (List)contents;
				} else {
					params = new ArrayList();
					params.add(contents);
				}
			}
			
			json.setContext(component);
			res = json.invoke(component, methodName, params, true);
		} catch (ClassNotFoundException e) {
			container.debug(e.getMessage());
			response.sendError(SC_NOT_FOUND);
			return;			
		} catch (NoSuchMethodException e) {
			container.debug(e.getMessage());
			response.sendError(SC_NOT_FOUND);
			return;
		} catch (IllegalArgumentException e) {
			container.debug(e.getMessage());
			response.sendError(SC_BAD_REQUEST);
			return;
		} catch (JSONParseException e) {
			container.debug(e.getMessage());
			response.sendError(SC_BAD_REQUEST);
			return;
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			container.error(cause.getMessage(), cause);
			response.sendError(SC_INTERNAL_SERVER_ERROR, cause.getMessage());
		} catch (Exception e) {
			container.error(e.getMessage(), e);
			response.sendError(SC_INTERNAL_SERVER_ERROR);
			return;
		}
		
		// primitive object can't convert JSON
		if (res == null || res instanceof CharSequence || res instanceof Boolean || res instanceof Number || res instanceof Date) {
			response.setStatus(SC_NO_CONTENT);
			return;
		}

		// response processing
		response.setContentType((callback != null) ? "text/javascript" : "application/json");
		
		try {
			Writer writer = response.getWriter();
			json.setPrettyPrint(container.isDebugMode());
			
			if (callback != null) writer.append(callback).append("(");
			json.format(res, writer);
			if (callback != null) writer.append(");");
		} catch (Exception e) {
			container.error(e.getMessage(), e);
			response.sendError(SC_INTERNAL_SERVER_ERROR);
			return;
		}		
	}
	
	protected Class getClassFromPath(String[] pathes) throws ClassNotFoundException {
		StringBuffer className = new StringBuffer();

		String prefix = "";
		String suffix = "Service";
		for (Map.Entry<Pattern, String> entry : routes.entrySet()) {
			Matcher m = entry.getKey().matcher(pathes[0]);
			if (m.find()) {
				className.setLength(0);
				
				// package root
				m.appendReplacement(className, entry.getValue());
				if (className.length() > 0) className.append('.');
				
				// rest path
				m.appendTail(className);
				
				// normalize
				char old = '.';
				for (int i = 0; i < className.length(); i++) {
					char c = className.charAt(i);
					if (c == '.' || c == '/') {
						c = '.';
					} else 	if ((old == '.') ? !Character.isJavaIdentifierStart(c) : !Character.isJavaIdentifierPart(c)) {
						c = '_';
					}
					className.setCharAt(i, c);
					old = c;
				}
				break;
			}
		}
		if (className.length() == 0) return null;
		
		className.append(prefix);
		boolean isStart = true;
		for (int i = 0; i < pathes[1].length(); i++) {
			char c = pathes[1].charAt(i);
			if (isStart) {
				className.append(Character.toUpperCase(c));
				isStart = false;
			} else if (c == ' ' || c == '_' || c == '-') {
				isStart = true;
			} else {
				className.append(c);
			}
		}
		className.append(suffix);

		return Class.forName(className.toString());
	}
	
	private static final Pattern SPLIT_PATTERN = Pattern.compile("\\.");
	private Map getParameterMap(HttpServletRequest request) {
		Map<String, Object> map = new HashMap<String, Object>();
		
		for (Enumeration<String> e = request.getParameterNames(); e.hasMoreElements(); ) {
			String name = e.nextElement();
			String[] names = SPLIT_PATTERN.split(name);
			String[] values = request.getParameterValues(name);
			
			Map<String, Object> current = map;
			for (int i = 0; i < names.length-1; i++) {
				Map target = (Map)current.get(names[i]);
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
		
		public Object invoke(Object o, String methodName, List values, boolean vlength) throws Exception {
			if (values == null) {
				values = Collections.EMPTY_LIST;
			}
			
			methodName = toLowerCamel(methodName);
			
			Class c = o.getClass();
			Class target = c;
			Method method = null;
			boolean exists = false;
			for (Method m : target.getMethods()) {
				if (!methodName.equals(m.getName()) || Modifier.isStatic(m.getModifiers())) {
					continue;
				}

				int length = m.getParameterTypes().length;
				if (values.size() == length) {
					method = m;
					break; 
				} else if (length == 0) {
					method = m;
				}				
				exists = true;
			}
			
			if (method == null || limit(c, method)) {
				StringBuilder sb = new StringBuilder(c.getName());
				sb.append('#').append(methodName).append('(');
				String json = encode(values);
				sb.append(json, 1, json.length()-1);
				sb.append(')');
				if (exists) {
					throw new IllegalArgumentException(getMessage("json.invoke.MismatchParametersError", sb.toString()));
				} else {
					throw new NoSuchMethodException(getMessage("json.invoke.NoSuchMethodError", sb.toString()));
				}
			}
			
			Class<?>[] paramTypes = method.getParameterTypes();
			Object[] params = new Object[paramTypes.length];
			for (int i = 0; i < params.length; i++) {
				params[i] = convert(values.get(i), paramTypes[i], paramTypes[i]);
			}
			
			return method.invoke(o, params);
		}
		
		public boolean limit(Class c, Method method) {
			return method.getDeclaringClass().equals(Object.class);
		}
	}
}