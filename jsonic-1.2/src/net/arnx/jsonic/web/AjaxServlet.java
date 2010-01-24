package net.arnx.jsonic.web;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
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
			if (route.getMethod() == null || route.getParameters() == null) {
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
								
				Method method = container.getMethod(component, methodName, route.getParameters());
				
				Produce produce = method.getAnnotation(Produce.class);
				if (produce != null) response.setContentType(produce.value());
				
				result = container.execute(json, component, method, route.getParameters());
				
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
				params = parseParameter(request.getParameterMap());
			} else if (contentType.equals("application/json")) {
				Object o = processor.parse(request.getInputStream());
				params = (o instanceof List<?>) ? (List<?>)o :  Arrays.asList(o);
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
				} catch (FileUploadException e) {
					throw new ServletException(e);
				}
				
				params = parseParameter(map);
			}
		}
		
		public List<?> getParameters() {
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
		
		public String getMethod() {
			return getParameter(urlmap, "method");
		}
		
		@SuppressWarnings("unchecked")
		private List<?> parseParameter(Map<String, Object[]> pairs) {
			List<Object> result = new ArrayList<Object>();
			Object[] pair = pairs.remove("__JSON__");
			if (pair != null) {
				for (int i = 0; i < pair.length; i ++) {
					if (pair[i] instanceof String) {
						String text = pair[i].toString();
						result = margeParameter(result, getProcessor().parse(text));
					}
				}
			}
			
			StringBuilder sb = new StringBuilder();
			for (Map.Entry<String, Object[]> entry : pairs.entrySet()) {
				//List<?> names = parseParameterName(entry.getKey(), sb);
				Object value = parseParameterValue(entry.getValue());
				
				Map<Object, Object> map = null;
				if (result.isEmpty()) {
					map = new LinkedHashMap<Object, Object>();
					result.add(map);
				} else {
					map = (Map<Object, Object>)result.get(0);
				}
				map.put(entry.getKey(), value);
				
				/*
				Object parent = null;
				Object key = null;
				Object current = result;
				for (int i = 0; i < names.size(); i++) {
					Object name = names.get(i);
					if (i == names.size()-1) {
						if (name instanceof BigDecimal) {
							int pos = ((BigDecimal)name).intValueExact();
							if (current instanceof Map) {
								((Map)current).put(name, value);
							} else {
								List list = (List)current;
								if (pos < list.size()) {
									Object target = list.get(pos);
									if (target == null) {
										list.set(pos, value);
									}
								}
							}
						} else {
							if (current instanceof Map) {
								((Map)current).put(name, value);
							} else {
								Map map = new LinkedHashMap();
								map.put(name, value);
								map.put(null, current);
								if (parent instanceof Map) {
									((Map)parent).put(key, map);
								} else {
									int pos = ((BigDecimal)key).intValueExact();
									((List)parent).set(pos, map);
								}
							}
						}
					} else {
						parent = current;
						key = name;
						current = getChildNode(name, current);
					}
				}
				*/
			}
			
			result = margeParameter(result, params);
			return result;
		}
		
		List<?> parseParameterName(String name, StringBuilder sb) {
			// 0 * 1 . 2 [ 3 ] 4
			int point = 0;
			
			List<Object> list = new ArrayList<Object>();
			
			sb.setLength(0);
			
			int length = name.length();
			if (name.endsWith("[]")) length -= 2;
			for (int i = 0; i < length; i++) {
				char c = name.charAt(i);
				switch (c) {
				case '.':
					if (point == 1) {
						if (list.isEmpty()) list.add(0);
						list.add(sb.toString());
						sb.setLength(0);
						continue;
					} else if (point == 4) {
						point = 0;
						continue;
					}
				case '[':
					if (point == 0 || point == 2 || point == 4) {
						point = 3;
						continue;
					} else if (point == 1) {
						if (list.isEmpty()) list.add(0);
						list.add(sb.toString());
						sb.setLength(0);
						point = 3;
						continue;
					}
				case ']':
					if (point == 3) {
						String value = sb.toString();
						if (value.length() > 1 && value.charAt(0) == '"' && value.charAt(value.length()-1) == '"') {
							if (list.isEmpty()) list.add(0);
							list.add(value.substring(1, value.length()-1));
						} else if (value.matches("\\d+")) {
							list.add(new BigDecimal(value).intValueExact());
						} else {
							if (list.isEmpty()) list.add(0);
							list.add(value);
						}
						sb.setLength(0);
						point = 4;
						continue;
					}
				}
				if(point == 0 || point == 4) point = 1;
				sb.append(c);
			}
			if (sb.length() > 0) {
				if (list.isEmpty()) list.add(0);
				list.add(sb.toString());
				sb.setLength(0);
			}
			
			return list;
		}
		
		Object parseParameterValue(Object[] values) {
			if (values == null) return null;
		
			if (values.length > 1) {
				List<Object> list = new ArrayList<Object>(values.length);
				for (Object v : values) list.add(v);
				return list;
			}
			return values[0];
		}
		
		@SuppressWarnings("unchecked")
		static List<Object> margeParameter(List<Object> params, Object target) {
			List<?> params2 = (target instanceof List) ? (List)target : Arrays.asList(target);
			
			for (int i = 0; i < params2.size(); i++) {
				Object p1 = null;
				Object p2 = params2.get(i);
				if (params.size() < i) {
					p1 = params.get(i);
				} else {
					params.add(null);
				}
				
				if (p1 == null) {
					p1 = p2;
				} else if (p1 instanceof Map) {
					p1 = margeMap((Map)p1, p2);
				} else if (p1 instanceof List<?>) {
					if (p2 instanceof Map) {
						p1 = margeMap((Map)p2, p1);
					} else if (p2 instanceof List) {
						((List)p1).addAll((List)p2);
					} else {
						((List)p1).add(p2);
					}
				} else {
					if (p2 instanceof Map) {
						p1 = margeMap((Map)p2, p1);
					} else if (p2 instanceof List) {
						((List)p2).add(p1);
						p1 = p2;
					} else {
						List list = new ArrayList();
						list.add(p1);
						list.add(p2);
						p1 = list;
					}
				}
				
				params.set(i, p1);
			}
			
			return params;
		}
		
		
		@SuppressWarnings("unchecked")
		private static Object margeMap(Map<Object, Object> p1, Object p2) {
			if (p2 instanceof Map<?, ?>) {
				for (Map.Entry<?, ?> entry : ((Map<?, ?>)p2).entrySet()) {
					if (p1.containsKey(entry.getKey())) {
						Object v1 = p1.get(entry.getKey());
						Object v2 = entry.getValue();
						if (v1 instanceof Map) {
							v1 = margeMap((Map)v1, v2);
						} else if (v1 instanceof List) {
							if (v2 instanceof List<?>) {
								((List)v1).addAll((List<?>)v2);
							} else {
								((List)v1).add(v2);
							}
						} else {
							if (v2 instanceof Map) {
								v1 = margeMap((Map)v2, v1);
							} else if (v2 instanceof List) {
								if (v2 instanceof List<?>) {
									((List)v2).addAll((List<?>)v1);
								} else {
									((List)v2).add(p1);
								}
								v1 = v2;
							} else {
								List list = new ArrayList();
								list.add(v1);
								list.add(v2);
								v1 = list;
							}
						}
						p1.put(entry.getKey(), v1);
					} else {
						p1.put(entry.getKey(), entry.getValue());
					}
				}
			} else {
				if (p1.containsKey(null)) {
					Object v1 = p1.get(null);
					if (v1 instanceof List) {
						if (p2 instanceof List<?>) {
							((List)v1).addAll((List<?>)p2);
						} else {
							((List)v1).add(p2);
						}
					} else {
						List<Object> list = new ArrayList<Object>();
						list.add(v1);
						if (p2 instanceof List<?>) {
							list.addAll((List<?>)p2);
						} else {
							list.add(p2);
						}
						p1.put(null, list);
					}
				} else {
					p1.put(null, p2);
				}
			}
			
			return p1;
		}
		
		static String getParameter(Map<String, Object> params, String name) {
			Object o = params.get(name);
			
			if (o == null) return null; 
			
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
