package net.arnx.jsonic.web;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import net.arnx.jsonic.JSON;
import net.arnx.jsonic.JSONException;

public class AjaxServlet extends HttpServlet {
	static class Config {
		public Class<? extends Container> container;
		public Class<? extends JSON> processor;
		public String encoding;
		public Boolean expire;
		public Map<String, String> mappings;
		public Map<String, Pattern> definitions;
		public File repository;
		public Integer threshold;
		public Long maxSize;
		public Long maxFileSize;
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
		
		if (config.processor == null) config.processor = AjaxJSON.class;
		
		if (config.definitions == null) config.definitions = new HashMap<String, Pattern>();
		if (!config.definitions.containsKey("package")) config.definitions.put("package", Pattern.compile(".+"));
		
		if (config.mappings != null) {
			for (Map.Entry<String, String> entry : config.mappings.entrySet()) {
				mappings.add(new RouteMapping(entry.getKey(), entry.getValue(), config));
			}
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

		container.debug("Route found: " + request.getMethod() + " " + uri);
		
		// request processing
		Object result = null;
		
		int errorCode = 0;
		String errorName = null;
		String errorMessage = null;
		Throwable throwable = null;
		
		Object component = null;
		JSON json = route.getProcessor();
		
		try {
			if (route.getMethod() == null || route.getParams() == null) {
				throwable = new IllegalArgumentException(((route.getMethod() == null) ? "method" : "params") + "is null.");
				errorCode = -32600;
				errorName = "ReferenceError";
				errorMessage = "Invalid Request.";
			} else {				
				String methodName = route.getMethod();
				if (methodName.equals(container.init) || methodName.equals(container.destroy)) {
					throw new NoSuchMethodException(methodName);
				}
				
				component = container.getComponent(route.getComponentClass(container));
				if (component == null) {
					throw new NoSuchMethodException(methodName);
				}
				json.setContext(component);
								
				Method method = container.getMethod(component, methodName, route.getParams());
				
				Produce produce = method.getAnnotation(Produce.class);
				if (produce != null) response.setContentType(produce.value());
				
				result = container.execute(json, component, method, route.getParams());
				
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
		
		if (result instanceof Produce) return;

		// response processing
		response.setContentType("application/json");
		
		Map<String, Object> res = new LinkedHashMap<String, Object>();
		if (errorCode == 0) {
			res.put("result", result);
		} else {
			Map<String, Object> error = new LinkedHashMap<String, Object>();
			error.put("code", errorCode);
			error.put("name", errorName);
			error.put("message", errorMessage);
			error.put("data", throwable);
			res.put("error", error);
		}
		
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
			Map<String, Object> error = new LinkedHashMap<String, Object>();
			error.put("code", -32603);
			error.put("name", e.getClass().getSimpleName());
			error.put("message", "Internal error.");
			error.put("data", e);
			res.put("error", error);
			json.format(res, writer);
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
	
	public static class AjaxJSON extends JSON {
		@Override
		protected boolean ignore(Context context, Class<?> target, Member member) {
			return member.getDeclaringClass().equals(Throwable.class)
				|| super.ignore(context, target, member);
		}
	}
	
	
	static class RouteMapping {
		private static final Pattern PLACE_PATTERN = Pattern.compile("\\{\\s*(\\p{javaJavaIdentifierStart}[\\p{javaJavaIdentifierPart}\\.-]*)\\s*(?::\\s*((?:[^{}]|\\{[^{}]*\\})*)\\s*)?\\}");
		private static final Pattern DEFAULT_PATTERN = Pattern.compile("[^/()]+");
		
		Pattern pattern;
		List<String> names;
		
		String target;
		Config config;
		
		public RouteMapping(String path, String target, Config config) {
			this.target = target;
			this.config = config;
			
			this.names = new ArrayList<String>();
			
			StringBuffer sb = new StringBuffer("^\\Q");
			Matcher m = PLACE_PATTERN.matcher(path);
			while (m.find()) {
				String name = m.group(1);
				names.add(name);
				Pattern p = DEFAULT_PATTERN;
				if (m.group(2) != null) {
					p = Pattern.compile(m.group(2));
				} else if (config.definitions.containsKey(name)) {
					p = config.definitions.get(name);
				}
				m.appendReplacement(sb, "\\\\E(" + p.pattern().replaceAll("\\((?!\\?)", "(?:").replace("\\", "\\\\") + ")\\\\Q");
			}
			m.appendTail(sb);
			sb.append("\\E$");
			this.pattern = Pattern.compile(sb.toString());
		}
		
		@SuppressWarnings("unchecked")
		public Route matches(HttpServletRequest request, String path) throws IOException, ServletException {
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
				return new Route(request, target, params, config);
			}
			return null;
		}
	}
	
	
	static class Route {
		private static final Pattern REPLACE_PATTERN = Pattern.compile("\\$\\{(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)\\}");

		private String target;
		private Map<String, Object> urlmap;
		private String method;
		private List<?> params;
		private JSON processor;
		
		@SuppressWarnings("unchecked")
		public Route(HttpServletRequest request, String target, Map<String, Object> urlmap, Config config) throws IOException, ServletException {
			this.target = target;
			this.urlmap = urlmap;
			
			try {
				processor = config.processor.newInstance();
			} catch (Exception e) {
				throw new ServletException(e);
			}
			
			
			String contentType = request.getContentType();
			if (contentType == null) contentType = "";
			int index = contentType.indexOf(';');
			contentType = (index > -1) ? contentType.substring(0, index) : contentType;
			contentType = contentType.toLowerCase();
			
			if (contentType.equals("application/x-www-form-urlencoded")) {
				Map<String,Object[]> map = request.getParameterMap();
				Object[] array = map.get("_method");
				if (array != null && array.length > 0 && array[0] instanceof String) {
					method = (String)array[0];
					params = Arrays.asList(parseParameter(map));
				}
			} else if (contentType.equals("application/json")) {
				Map<?, ?> map = processor.parse(request.getInputStream(), Map.class);
				method = map.get("method").toString();
				params = (List<?>)map.get("params");
			} else if (contentType.startsWith("multipart/")) {
				Map<String, Object[]> map = new LinkedHashMap<String, Object[]>();
				DiskFileItemFactory factory = new DiskFileItemFactory();
				if (config.repository != null) factory.setRepository(config.repository);
				if (config.threshold != null) factory.setSizeThreshold(config.threshold);
				
				ServletFileUpload upload = new ServletFileUpload(factory);
				if (config.maxSize != null) upload.setSizeMax(config.maxSize);
				if (config.maxFileSize != null) upload.setFileSizeMax(config.maxFileSize);
				
				try {
					for (FileItem item : (List<FileItem>)upload.parseRequest(request)) {
						Object value = (item.isFormField()) ? item.getString() : item;
						Object[] values = map.get(item.getFieldName());
						if (values == null) {
							values = new Object[] { value };
						} else {
							Object[] newValues = new Object[values.length+1];
							System.arraycopy(values, 0, newValues, 0, values.length);
							newValues[values.length] = value;
							values = newValues;
						}
						map.put(item.getFieldName(), values);
					}
					Object[] array = map.get("_method");
					if (array != null && array.length > 0 && array[0] instanceof String) {
						method = (String)array[0];
						params = Arrays.asList(parseParameter(map));
					}
				} catch (FileUploadException e) {
					throw new ServletException(e);
				}
			}
		}
		
		public String getMethod() {
			return method;
		}
		
		public List<?> getParams() {
			return params;
		}
		
		public JSON getProcessor() {
			return processor;
		}
		
		public String getComponentClass(Container container) {
			Matcher m = REPLACE_PATTERN.matcher(target);
			StringBuffer sb = new StringBuffer();
			while (m.find()) {
				String key = m.group(1);
				String value = getParameter(urlmap, key);
				
				if (key.equals("class") && container.namingConversion) {
					value = (value != null) ? toUpperCamel(value) : "";
				} else if (key.equals("package")) {
					value = value.replace('/', '.');
				}
				
				m.appendReplacement(sb, (value != null) ? value : "");
			}
			m.appendTail(sb);
			return sb.toString();
		}
		
		@SuppressWarnings("unchecked")
		private static Map<?, ?> parseParameter(Map<String, Object[]> pairs) {
			Map<Object, Object> params = new LinkedHashMap<Object, Object>();
			for (Map.Entry<String, Object[]> entry : pairs.entrySet()) {
				String name = entry.getKey();
				Object[] values = entry.getValue();
				
				if ("_method".equals(name)) continue;
				
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
								for (Object value : values) list.add(value);
							} else {
								List<Object> list = new ArrayList<Object>();
								list.add(target);
								for (Object value : values) list.add(value);
								map.put(null, list);
							}
						} else if (values.length > 1) {
							List<Object> list = new ArrayList<Object>();
							for (Object value : values) list.add(value);
							map.put(null, list);
						} else {
							map.put(null, values[0]);						
						}
					} else if (target instanceof List) {
						List<Object> list = ((List<Object>)target);
						for (Object value : values) list.add(value);
					} else {
						List<Object> list = new ArrayList<Object>();
						list.add(target);
						for (Object value : values) list.add(value);
						current.put(name, list);
					}
				} else if (values.length > 1) {
					List<Object> list = new ArrayList<Object>();
					for (Object value : values) list.add(value);
					current.put(name, list);
				} else {
					current.put(name, values[0]);						
				}
			}
			
			return params;
		}
		
		static String getParameter(Map<String, Object> params, String name) {
			Object o = params.get(name);
			
			if (o instanceof Map<?, ?>) {
				Map<?, ?> map = (Map<?, ?>)o;
				if (map.containsKey(null)) o = map.get(null); 
			}
			
			if (o instanceof List<?>) {
				List<?> list = (List<?>)o;
				if (!list.isEmpty()) o = list.get(0);
			} else if (o.getClass().isArray()) {
				if (Array.getLength(o) > 0) {
					o = Array.get(o, 0);
				} else {
					o = null;
				}
			}
			
			return (o instanceof String) ? (String)o : null;
		}
			
		static String toUpperCamel(String name) {
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
