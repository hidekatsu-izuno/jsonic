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

public class WebServiceServlet extends HttpServlet {
	private static final long serialVersionUID = -63348112220078595L;
	
	class Config {
		public Class<? extends Container> container;
		public Class<? extends net.arnx.jsonic.JSON> processor;
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
		 
		net.arnx.jsonic.JSON json = new net.arnx.jsonic.JSON();
		
		try {
			config = json.parse(configText, Config.class);
			if (config.container == null) config.container = Container.class;
			
			container = (Container)json.parse(configText, config.container);
			container.init(servletConfig);
		} catch (Exception e) {
			throw new ServletException(e);
		}
		
		if (config.processor == null) config.processor = WebServiceServlet.JSON.class;
		
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
				throwable = new Throwable();
				errorCode = -32600;
				errorMessage = "Invalid Request.";
			} else {
				int delimiter = req.method.lastIndexOf('.');
				if (delimiter <= 0 && delimiter+1 == req.method.length()) {
					throw new NoSuchMethodException(req.method);
				} else {
					Object component = container.getComponent(
						route.getComponentClass(req.method.substring(0, delimiter)),
						request, response
					);
					if (component == null) {
						throw new NoSuchMethodException(req.method);
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
			error.put("message", "Internal error.");
			error.put("data", e);
			res.put("error", error);
			res.put("id", (req != null) ? req.id : null);
			json.format(res, writer);
			return;
		}
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
			Object component = container.getComponent(route.getComponentClass(null), request, response);
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
			if (Modifier.isStatic(m.getModifiers())) continue;
			
			if (m.getName().equals(container.init)) {
				if (m.getParameterTypes().length == 0 && m.getReturnType().equals(void.class)) {
					init = m;
				} else {
					illegalInit = false;
				}
			} else if (m.getName().equals(container.destroy)) {
				if (m.getParameterTypes().length == 0 && m.getReturnType().equals(void.class)) {
					destroy = m;
				} else {
					illegalDestroy = false;
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
		
		if (init == null && illegalInit) throw new IllegalStateException("init method must have no arguments.");
		if (destroy == null && illegalDestroy) throw new IllegalStateException("destroy method must have no arguments.");
		
		Object[] params = new Object[paramTypes.length];
		for (int i = 0; i < params.length; i++) {
			params[i] = json.convert((i < args.size()) ? args.get(i) : null, paramTypes[i]);
		}
		
		if (init != null) {
			if (container.isDebugMode()) container.debug("Execute: " + toPrintString(c, init.getName(), null));
			init.invoke(o);
		}
		
		if (container.isDebugMode()) container.debug("Execute: " + toPrintString(c, methodName, args));
		Object ret = method.invoke(o, params);
		
		if (destroy != null) {
			if (container.isDebugMode()) container.debug("Execute: " + toPrintString(c, destroy.getName(), null));
			destroy.invoke(o);
		}
		
		return ret;
	}
	
	private String toPrintString(Class<?> c, String methodName, List<?> args) {
		StringBuilder sb = new StringBuilder(c.getName());
		sb.append('#').append(methodName).append('(');
		String str = JSON.encode(args);
		sb.append(str, 1, str.length()-1);
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
}
	
class RouteMapping {
	private static final Pattern PLACE_PATTERN = Pattern.compile("[{\\[](\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)[\\]}]");
	
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
