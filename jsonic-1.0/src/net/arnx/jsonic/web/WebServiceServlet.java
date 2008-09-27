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
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.arnx.jsonic.JSON;
import net.arnx.jsonic.JSONConvertException;
import net.arnx.jsonic.JSONParseException;

import static javax.servlet.http.HttpServletResponse.*;

public class WebServiceServlet extends HttpServlet {
	private static final long serialVersionUID = -63348112220078595L;
	
	class Config {
		public Class<? extends Container> container;
		public String encoding;
		public Boolean expire;
		public Map<String, String> mappings;
		public Map<String, Pattern> definitions;
		public String repository;
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
			config = json.parse(configText, Config.class);
			if (config.container == null) config.container = Container.class;
			
			container = (Container)json.parse(configText, config.container);
			container.init(getServletContext());
		} catch (Exception e) {
			throw new ServletException(e);
		}
		
		if (config.definitions == null) config.definitions = new HashMap<String, Pattern>();
		if (!config.definitions.containsKey("package")) config.definitions.put("package", Pattern.compile(".+"));
		if (!config.definitions.containsKey(null)) config.definitions.put(null, Pattern.compile("[^/()]+"));
		
		if (config.mappings != null) {
			for (Map.Entry<String, String> entry : config.mappings.entrySet()) {
				mappings.add(new RouteMapping(entry.getKey(), entry.getValue(), config.definitions));
			}
		}
		
		if (config.repository != null) {
			try {
				File repo = new File(config.repository);
				if (!repo.isAbsolute()) {
					repo = new File(servletConfig.getServletContext().getRealPath("/"), config.repository);
				}
				
				if (!repo.exists()) container.error("repository is not found: " + config.repository, null);
				if (!repo.isDirectory()) container.error("repository is not a directory: " + config.repository, null);
				if (!repo.canWrite()) container.error("repository is not writable: " + config.repository, null);
				
				config.repository = repo.getCanonicalPath();
			} catch (Exception e) {
				container.error("cannot access repository: " + config.repository, e);
			}
		}
	}

	protected Route preprocess(HttpServletRequest request, HttpServletResponse response)
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
			response.setHeader("Cache-Control","no-cache");
			response.setHeader("Pragma","no-cache");
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
		
		container.debug("route found: " + request.getMethod() + " " + uri);
		return route;
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {		
		Route route = preprocess(request, response);
		if (route == null) return;
		
		if (route.isRpcMode()) {
			response.addHeader("Allow", "POST");
			response.sendError(SC_METHOD_NOT_ALLOWED, "Method Not Allowd");
		} else {
			doREST(route, request, response);
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Route route = preprocess(request, response);
		if (route == null) return;

		if (route.isRpcMode()) {
			doRPC(route, request, response);
		} else {
			doREST(route, request, response);
		}
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Route route = preprocess(request, response);
		if (route == null) return;
		
		if (route.isRpcMode()) {
			response.addHeader("Allow", "POST");
			response.sendError(SC_METHOD_NOT_ALLOWED, "Method Not Allowed");
		} else {
			doREST(route, request, response);
		}
	}
	
	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
		throws ServletException, IOException {
		Route route = preprocess(request, response);
		if (route == null) return;
		
		if (route.isRpcMode()) {
			response.addHeader("Allow", "POST");
			response.sendError(SC_METHOD_NOT_ALLOWED, "Method Not Allowed");
		} else {
			doREST(route, request, response);
		}
	}
	
	class RpcRequest {
		public String method;
		public List params;
		public Object id;
	}
	
	protected void doRPC(Route route, HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
				
		JSONInvoker json = new JSONInvoker(request, response);
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
				throwable = new Throwable();
				errorCode = -32600;
				errorMessage = "Invalid Request.";
			} else {
				int delimiter = req.method.lastIndexOf('.');
				if (delimiter <= 0 && delimiter+1 == req.method.length()) {
					throw new NoSuchMethodException(req.method);
				} else {
					Object component = container.getComponent(route.getComponentClass(req.method.substring(0, delimiter)));
					if (component == null) {
						throw new NoSuchMethodException(req.method);
					}
					
					json.setContext(component);
					result = json.invoke(component, req.method.substring(delimiter+1), req.params);
				}
			}
		} catch (ClassNotFoundException e) {
			container.debug(e.getMessage());
			throwable = e;
			errorCode = -32601;
			errorMessage = "Method not found.";
		} catch (NoSuchMethodException e) {
			container.debug(e.getMessage());
			throwable = e;
			errorCode = -32601;
			errorMessage = "Method not found.";
		} catch (JSONConvertException e) {
			container.debug(e.getMessage());
			throwable = e;
			errorCode = -32602;
			errorMessage = "Invalid params.";
		} catch (JSONParseException e) {
			container.debug(e.getMessage());
			throwable = e;
			errorCode = -32700;
			errorMessage = "Parse error.";
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			container.debug(cause.toString());
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
			container.error(e.getMessage(), e);
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

		try {
			json.setContext(result);
			json.setPrettyPrint(container.isDebugMode());
			json.format(res, writer);
		} catch (Exception e) {
			container.error(e.getMessage(), e);
			res.clear();
			res.put("result", null);
			Map<String, Object> error = new LinkedHashMap<String, Object>();
			error.put("code", -32603);
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
		JSONInvoker json = new JSONInvoker(request, response);
		json.setLocale(request.getLocale());
		
		Object res = null;
		try {
			Object component = container.getComponent(route.getComponentClass(null));
			if (component == null) {
				response.sendError(SC_NOT_FOUND, "Not Found");
				return;
			}
			
			List<Object> params = new ArrayList<Object>();
			if (!route.hasJSONContent()) {
				params.add(route.getParameterMap());
			} else {
				Object o = json.parse(request.getReader());
				if (o instanceof List) {
					params.add(o);
					params.add(route.getParameterMap());
				} else if (o instanceof Map) {
					Map<String, Object> contents = route.getParameterMap();
					for (Map.Entry<String, Object> entry : ((Map<String, Object>)o).entrySet()) {
						if (contents.containsKey(entry.getKey())) {
							Object target = contents.get(entry.getKey());
							
							if (target instanceof Map) {
								Map map = (Map)target;
								if (map.containsKey(null)) {
									target = map.get(null);
								}
							}
							
							if (target instanceof Map) {
								((Map)target).put(null, entry.getValue());
							} else  if (target instanceof List) {
								((List)target).add(entry.getValue());
							} else {
								List<Object> list = new ArrayList<Object>();
								list.add(target);
								list.add(entry.getValue());
								contents.put(entry.getKey(), list);
							}
						} else {
							contents.put(entry.getKey(), entry.getValue());
						}
					}
					params.add(contents);
				} else {
					throw new IllegalArgumentException("failed to convert parameters from JSON.");
				}
			}
			json.setContext(component);
			res = json.invoke(component, methodName, params);
		} catch (ClassNotFoundException e) {
			container.debug(e.getMessage());
			response.sendError(SC_NOT_FOUND, "Not Found");
			return;			
		} catch (NoSuchMethodException e) {
			container.debug(e.getMessage());
			response.sendError(SC_NOT_FOUND, "Not Found");
			return;
		} catch (JSONConvertException e) {
			container.debug(e.getMessage());
			response.sendError(SC_BAD_REQUEST, "Bad Request");
			return;
		} catch (JSONParseException e) {
			container.debug(e.getMessage());
			response.sendError(SC_BAD_REQUEST, "Bad Request");
			return;
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			container.debug(cause.toString());
			if (cause instanceof IllegalStateException
				|| cause instanceof UnsupportedOperationException) {
				response.sendError(SC_NOT_FOUND, "Not Found");				
			} else if (cause instanceof IllegalArgumentException) {
				response.sendError(SC_BAD_REQUEST, cause.getMessage());
			} else {
				response.sendError(SC_INTERNAL_SERVER_ERROR, cause.getMessage());
			}
			return;
		} catch (Exception e) {
			container.error(e.getMessage(), e);
			response.sendError(SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
			return;
		}
		
		try {		
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
		} catch (Exception e) {
			container.error(e.getMessage(), e);
			response.sendError(SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
			return;
		}		
	}
	
	@Override
	public void destroy() {
		container.destory();
		super.destroy();
	}
	
	class JSONInvoker extends JSON {
		private HttpServletRequest request;
		private HttpServletResponse response;
		
		public JSONInvoker(HttpServletRequest request, HttpServletResponse response) {
			this.request = request;
			this.response = response;
		}
		
		public Object invoke(Object o, String methodName, List<Object> args) throws Exception {
			if (args == null) {
				args = Collections.EMPTY_LIST;
			}
			
			methodName = toLowerCamel(methodName);
			
			Class<?> c = o.getClass();
			
			Method init = null;
			Method method = null;
			Method destroy = null;
			
			int count = 0;
			if (container.init == null) count++;
			if (container.destroy == null) count++;
			for (Method m : c.getMethods()) {
				if (Modifier.isStatic(m.getModifiers())) {
					continue;
				}				
				
				if (container.init != null && m.getName().equals(container.init)) {
					init = m;
					count++;
				} else if (container.destroy != null && m.getName().equals(container.destroy)) {
					destroy = m;
					count++;
				} else if (m.getName().equals(methodName)) {
					method = m;
					count++;
				}
				
				if (count > 3) break;
			}
			
			if (method == null || container.limit(c, method)) {
				StringBuilder sb = new StringBuilder(c.getName());
				sb.append('#').append(methodName).append('(');
				String json = JSON.encode(args);
				sb.append(json, 1, json.length()-1);
				sb.append(')');
				throw new NoSuchMethodException("method missing: " + sb.toString());
			}
			
			Type[] paramTypes = method.getGenericParameterTypes();
			Object[] params = new Object[paramTypes.length];
			int length = Math.min(args.size(), params.length);
			for (int i = 0; i < length ; i++) {
				params[i] = convert(args.get(i), paramTypes[i]);
			}
			
			if (init != null) {
				Class<?>[] sTypes = init.getParameterTypes();
				if (sTypes.length > 0) {
					Object[] sparams = new Object[sTypes.length];
					for (int i = 0; i < sTypes.length; i++) {
						sparams[i] = get(sTypes[i]);
					}
					init.invoke(o, sparams);
				} else {
					init.invoke(o);
				}
			}
			
			Object ret = method.invoke(o, params);
			
			if (destroy != null) {
				Class<?>[] sTypes = destroy.getParameterTypes();
				if (sTypes.length > 0) {
					Object[] sparams = new Object[sTypes.length];
					for (int i = 0; i < sTypes.length; i++) {
						sparams[i] = get(sTypes[i]);
					}
					destroy.invoke(o, sparams);
				} else {
					destroy.invoke(o);
				}
			}
			
			return ret;
		}
		
		@Override
		protected boolean ignore(Class<?> target, Member member) {
			return member.getDeclaringClass().equals(Throwable.class)
				|| super.ignore(target, member);
		}
		
		private Object get(Type t) {
			Class c = (t instanceof Class) ? (Class)t : null;
			
			if (c != null) {
				if (ServletRequest.class.equals(c) || HttpServletRequest.class.equals(c)) {
					return request;
				} else if (ServletResponse.class.equals(c) || HttpServletResponse.class.equals(c)) {
					return response;
				} else if (ServletContext.class.equals(c)) {
					return getServletContext();
				} else if (HttpSession.class.equals(c)) {
					return request.getSession(true);
				}
			}
			return null;
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
						((List)target).add(value);
					} else {
						List list = new ArrayList(2);
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
