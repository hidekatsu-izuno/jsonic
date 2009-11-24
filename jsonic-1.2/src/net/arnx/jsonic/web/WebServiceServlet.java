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
import java.util.Date;
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
	private static final long serialVersionUID = -63348112220078595L;
	
	protected class Config {
		public Class<? extends Container> container;
		public Class<? extends net.arnx.jsonic.JSON> processor;
		public String encoding;
		public Boolean expire;
		public Map<String, List<Object>> mappings;
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
			config = json.parse(configText, getConfigClass());
			if (config.container == null) config.container = Container.class;
			
			container = (Container)json.parse(configText, config.container);
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
	
	protected Class<? extends Config> getConfigClass() {
		return Config.class;
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
	
	class RpcRequest {
		public String method;
		public List<Object> params;
		public Object id;
	}
	
	protected void doRPC(Route route, HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		
		JSON json = newJSON(request.getLocale());
		
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
			if (req == null || req.method == null || req.params == null) {
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
				
				Method method = container.findMethod(component, methodName, req.params);
				
				Produce produce = method.getAnnotation(Produce.class);
				if (produce != null) response.setContentType(produce.value());
								
				result = invoke(json, component, method, req.params);
				
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
		
		JSON json = newJSON(request.getLocale());
		
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
			
			Method method = container.findMethod(component, methodName, params);
			
			Produce produce = method.getAnnotation(Produce.class);
			if (produce != null) response.setContentType(produce.value());
			
			res = invoke(json, component, method, params);
			
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
	
	private Object invoke(JSON json, Object component, Method method, List<?> params) throws Exception {
		Object result = null;
		
		Method init = null;
		Method destroy = null;
		
		if (container.init != null || container.destroy != null) {
			boolean illegalInit = false;
			boolean illegalDestroy = false;
			
			for (Method m : component.getClass().getMethods()) {
				if (Modifier.isStatic(m.getModifiers())) continue;
				
				if (m.getName().equals(container.init)) {
					if (m.getReturnType().equals(void.class) && m.getParameterTypes().length == 0) {
						init = m;
					} else {
						illegalInit = true;
					}
					continue;
				}
				if (m.getName().equals(container.destroy)) {
					if (m.getReturnType().equals(void.class) && m.getParameterTypes().length == 0) {
						destroy = m;
					} else {
						illegalDestroy = true;
					}
					continue;
				}
			}
	
			if (illegalInit) container.debug("Notice: init method must have no arguments.");		
			if (illegalDestroy) container.debug("Notice: destroy method must have no arguments.");
		}
		
		Type[] argTypes = method.getParameterTypes();
		Object[] args = new Object[argTypes.length];
		for (int i = 0; i < args.length; i++) {
			args[i] = json.convert((i < params.size()) ? params.get(i) : null, argTypes[i]);
		}
		if (container.isDebugMode()) {
			container.debug("Execute: " + toPrintString(component, method, args));
		}
		
		if (init != null) {
			if (container.isDebugMode()) {
				container.debug("Execute: " + toPrintString(component, init, null));
			}
			init.invoke(component);
		}
		
		args = container.preinvoke(component, method, args);
		result = method.invoke(component, args);
		result = container.postinvoke(component, method, result);
		
		if (destroy != null) {
			if (container.isDebugMode()) {
				container.debug("Execute: " + toPrintString(component, destroy, null));
			}
			destroy.invoke(component);
		}
		
		return result;
	}
	
	@Override
	public void destroy() {
		container.destory();
		super.destroy();
	}
	
	private JSON newJSON(Locale locale) throws ServletException {
		JSON json = null;
		try {
			json = (JSON)config.processor.newInstance();
		} catch (Exception e) {
			throw new ServletException(e);
		}
		json.setLocale(locale);		
		return json;
	}
	
	private String toPrintString(Object o, Method method, Object[] args) {
		StringBuilder sb = new StringBuilder(o.getClass().getName());
		sb.append('#').append(method.getName()).append('(');
		if (args != null) {
			String str = JSON.encode(args);
			sb.append(str, 1, str.length()-1);
		}
		sb.append(')');
		return sb.toString();
	}
	
	@SuppressWarnings("unchecked")
	private static <T> T cast(Object o) {
		return (T)o;
	}
	
	public static class WebServiceJSON extends JSON {
		@Override
		protected boolean ignore(Context context, Class<?> target, Member member) {
			return member.getDeclaringClass().equals(Throwable.class)
				|| super.ignore(context, target, member);
		}
	}
}

class RouteMapping {
	private static final Pattern PLACE_PATTERN = Pattern.compile("\\{\\s*(\\p{javaJavaIdentifierStart}[\\p{javaJavaIdentifierPart}\\.-]*)\\s*(?::\\s*((?:[^{}]|\\{[^{}]*\\})*)\\s*)?\\}");
	private static final Pattern DEFAULT_PATTERN = Pattern.compile("[^/()]+");
	private static final Map<String, String> DEFAULT_RESTMAP = new HashMap<String, String>();
	
	static {
		DEFAULT_RESTMAP.put("GET", "find");
		DEFAULT_RESTMAP.put("POST", "create");
		DEFAULT_RESTMAP.put("PUT", "update");
		DEFAULT_RESTMAP.put("DELETE", "delete");
	}
	
	private Pattern pattern;
	private List<String> names;
	private String target;
	private Map<String, String> restmap = DEFAULT_RESTMAP;
	
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
