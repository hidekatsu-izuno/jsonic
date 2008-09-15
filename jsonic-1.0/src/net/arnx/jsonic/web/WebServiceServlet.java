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
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URLDecoder;
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

@SuppressWarnings("unchecked")
public class WebServiceServlet extends HttpServlet {
	private static final long serialVersionUID = -63348112220078595L;
	
	class Config {
		public Class<? extends Container> container;
		public String encoding;
		public Boolean expire;
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
		
		for (RouteMapping m : mappings) {
			Route route = m.matches(request, uri);
			if (route != null) {
				container.debug("route found: " + request.getMethod() + " " + uri);
				return route;
			}
		}
		response.sendError(SC_NOT_FOUND, "Not Found");
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
			response.sendError(SC_METHOD_NOT_ALLOWED, "Method Not Allowd");
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
			String method = route.getParameter("_method");
			if (method == null) method = request.getMethod();
			if (method.equalsIgnoreCase("GET") 
				|| method.equalsIgnoreCase("POST")
				|| method.equalsIgnoreCase("PUT")
				|| method.equalsIgnoreCase("DELETE")) {
				route.setMethod(method);
				doREST(route, request, response);
			} else {
				response.sendError(SC_METHOD_NOT_ALLOWED, "Method Not Allowed");
				return;
			}
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
			response.sendError(SC_METHOD_NOT_ALLOWED, "Method Not Allowed");
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
		
		String methodName = route.getMethod();
		int status = SC_OK;
		String callback = null;
		
		if ("get".equals(route.getMethod())) {
			methodName = "find";
			callback = route.getParameter("callback");
		} else if ("post".equals(route.getMethod())) {
			methodName = "create";
			status = SC_CREATED;
		} else if ("put".equals(route.getMethod())) {
			methodName = "update";
		} else if ("delte".equals(route.getMethod())) {
			methodName = "delete";
		}
		
		// request processing
		JSONInvoker json = new JSONInvoker(request, response);
		json.setLocale(request.getLocale());
		
		Object res = null;
		try {
			Object component = container.getComponent(route.getComponentClass());
			if (component == null) {
				response.sendError(SC_NOT_FOUND, "Not Found");
				return;
			}
			
			List<Object> params = null;
			if ("get".equals(route.getMethod())) {
				params = new ArrayList<Object>();
				Map<String, Object> contents = route.getParameterMap();
				contents.putAll(route);
				params.add(contents);
			} else {
				Object o = json.parse(request.getReader());
				if (o instanceof List) {
					params = (List)o;
					Map<String, Object> contents = route.getParameterMap();
					contents.putAll(route);
					params.add(contents);
				} else if (o instanceof Map) {
					Map<String, Object> contents = (Map)o;
					contents.putAll(route.getParameterMap());
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
			Route route = new Route(request, target);
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
	private Map<String, Object> params;
	
	public Route(HttpServletRequest request, String target) {
		this.method = request.getMethod().toLowerCase();
		this.target = target;
		this.params = getParameterMap(request);
	}
	
	public void setMethod(String method) {
		this.method = method.toLowerCase();
	}
	
	public String getMethod() {
		return method;
	}
	
	public Map<String, Object> getParameterMap() {
		return params;
	}
	
	public String getParameter(String name) {
		Object o = params.get(name);
		return (o instanceof String) ? (String)o : null;
	}
	
	public String getComponentClass() {
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
		return sb.toString();
	}
	
	private static Map<String, Object> getParameterMap(HttpServletRequest request) {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		
		String query = request.getQueryString();
		if (query == null || query.length() == 0) {
			return result;
		}
		
		String encoding = request.getCharacterEncoding();
		
		Map<String, Object> params = new HashMap<String, Object>();
		int start = 0;
		String name = null;
		for (int i = 0; i <= query.length(); i++) {
			char c = (i != query.length()) ? query.charAt(i) : '&';
			if (c == '=' && name == null) {
				name = decode(query.substring(start, i), encoding);
				start = i+1;
			} else if (c == '&') {
				String value = decode(query.substring(start, i), encoding);
				if (name == null) {
					name = value;
					value = "";
				}
				
				if (params.containsKey(name)) {
					Object pvalue = params.get(name);
					if (pvalue instanceof List) {
						((List)params).add(value);
					} else {
						List list = new ArrayList();
						list.add(pvalue);
						list.add(value);
						params.put(name, list);
					}
				} else {
					params.put(name, value);
				}
				
				name = null;
				start = i+1;
			}
		}
		
		for (Map.Entry<String, Object> entry : params.entrySet()) {
			name = entry.getKey();
			start = 0;
			char old = '\0';
			Map<String, Object> current = result;
			for (int i = 0; i < name.length(); i++) {
				char c = name.charAt(i);
				if (c == '.' || c == '[') {
					String key = name.substring(start, (old == ']') ? i-1 : i);
					Object target = current.get(key);
					
					if (!(target instanceof Map)) {
						Map<String, Object> map = new LinkedHashMap<String, Object>();
						if (target != null) map.put("", target);
						current.put(key, map);
						current = map;
					} else {
						current = (Map<String, Object>)target;
					}
					start = i+1;
				}
				old = c;
			}
			current.put(name.substring(start, (old == ']') ? name.length()-1 : name.length()), entry.getValue());
		}
		
		return result;
	}
	
	private static String decode(String data, String encoding) {
		String result = null;
		try {
			result = URLDecoder.decode(data, encoding);
		} catch (UnsupportedEncodingException e) {
			// no handle
		}
		return result;
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