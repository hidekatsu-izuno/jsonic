package net.arnx.jsonic;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
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
	private static final Pattern URL_PATTERN = Pattern.compile("^(/(?:[^/]+/)*)([^/]+)(\\.[^/]+)?$");
	
	private Container container;
	private Config config;
	
	private Map<Pattern, String> routesRPC = new LinkedHashMap<Pattern, String>();
	private Map<Pattern, String> routesREST = new LinkedHashMap<Pattern, String>();
	
	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
		
		String configText = servletConfig.getInitParameter("config");
		if (configText == null || configText.trim().length() == 0) {
			configText = "{}";
		}
		
		JSON json = new JSON();
		
		try {
			json.setContext(this);
			config = json.parse(configText, Config.class);
		} catch (Exception e) {
			throw new ServletException(e);
		}
		
		if (config.routes != null) {
			for (Map.Entry<String, String> entry : config.routes.entrySet()) {
				routesRPC.put(Pattern.compile("^" + entry.getKey() + "/$"), entry.getValue());
				routesREST.put(Pattern.compile("^" + entry.getKey() + "/"), entry.getValue());
			}
		}
		
		try {
			Class containerClass = (config.container != null) ? 
					Class.forName(config.container) : SimpleContainer.class;
			
			json.setContext(containerClass);		
			container = (Container)json.parse(configText, containerClass);
			container.init(getServletContext());
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		if (config.encoding != null) {
			request.setCharacterEncoding(config.encoding);
			response.setCharacterEncoding(config.encoding);
		}
		
		super.service(request, response);
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Matcher m = URL_PATTERN.matcher(request.getRequestURI());
		if (!m.matches()){
			response.sendError(SC_NOT_FOUND);
			return;
		}
		
		String[] pathes = new String[] {m.group(1), m.group(2), m.group(3)};
		pathes[0] = (request.getContextPath().equals("/")) ? pathes[0] : pathes[0].substring(request.getContextPath().length());
		
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
		Matcher m = URL_PATTERN.matcher(request.getRequestURI());
		if (!m.matches()){
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		String[] pathes = new String[] {m.group(1), m.group(2), m.group(3)};
		pathes[0] = (request.getContextPath().equals("/")) ? pathes[0] : pathes[0].substring(request.getContextPath().length());
		
		if ("rpc".equalsIgnoreCase(m.group(2))) {
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
		Matcher m = URL_PATTERN.matcher(request.getRequestURI());
		if (!m.matches()){
			response.sendError(SC_NOT_FOUND);
			return;
		}
		
		String[] pathes = new String[] {m.group(1), m.group(2), m.group(3)};
		pathes[0] = (request.getContextPath().equals("/")) ? pathes[0] : pathes[0].substring(request.getContextPath().length());
		
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
		Matcher m = URL_PATTERN.matcher(request.getRequestURI());
		if (!m.matches()){
			response.sendError(SC_NOT_FOUND);
			return;
		}
		
		String[] pathes = new String[] {m.group(1), m.group(2), m.group(3)};
		pathes[0] = (request.getContextPath().equals("/")) ? pathes[0] : pathes[0].substring(request.getContextPath().length());
		
		if ("rpc".equalsIgnoreCase(pathes[1])) {
			response.addHeader("Allow", "POST");
			response.sendError(SC_METHOD_NOT_ALLOWED);
			return;
		} else {
			doREST("DELETE", pathes, request, response);
		}
	}
	
	protected void doRPC(String[] pathes, HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		
		JSON json = new JSON(this);
		
		String rootPackage = null;
		for (Map.Entry<Pattern, String> entry : routesRPC.entrySet()) {
			Matcher m = entry.getKey().matcher(pathes[0]);
			if (m.matches()) {
				rootPackage = entry.getValue();
				break;
			}
		}
		if (rootPackage == null) {
			response.sendError(SC_NOT_FOUND);
			return;
		}
		
		// request processing
		Request req = null;
		Object res = null;
		Map<String, Object> error = null;
		try {
			Class c = null;
			
			Object component = container.getComponent(c);
			if (component == null) {
				response.sendError(SC_NOT_FOUND);
				return;
			}
			
			req = json.parse(request.getReader(), Request.class);
			
			json.setContext(component);
			res = json.invokeDynamic(component, req.method, req.params);
		} catch (ClassNotFoundException e) {
			if (container.isDebugMode()) container.log(e.getMessage(), e);
			response.sendError(SC_NOT_FOUND);
			return;			
		} catch (JSONParseException e) {
			if (container.isDebugMode()) container.log(e.getMessage(), e);
			error = new LinkedHashMap<String, Object>();
			error.put("code", -32700);
			error.put("message", e.getMessage());
		} catch (NoSuchMethodException e) {
			if (container.isDebugMode()) container.log(e.getMessage(), e);
			error = new LinkedHashMap<String, Object>();
			error.put("code", -32601);
			error.put("message", e.getMessage());
		} catch (IllegalArgumentException e) {
			if (container.isDebugMode()) container.log(e.getMessage(), e);
			error = new LinkedHashMap<String, Object>();
			error.put("code", -32602);
			error.put("message", e.getMessage());
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			container.log(cause.getMessage(), cause);
			error = new LinkedHashMap<String, Object>();
			error.put("code", -32603);
			error.put("message", cause.getMessage());
		} catch (Exception e) {
			container.log(e.getMessage(), e);
			error = new LinkedHashMap<String, Object>();
			error.put("code", -32603);
			error.put("message", e.getMessage());
		}
		
		// it's notification when id was null
		if (req.id == null) {
			response.setStatus(SC_NO_CONTENT);
			return;
		}

		// response processing
		response.setContentType("application/json");
		
		try {
			Map<String, Object> map = new LinkedHashMap<String, Object>();
			map.put("result", res);
			map.put("error", error);
			map.put("id", req.id);
				
			Writer writer = response.getWriter();
			json.setPrettyPrint(!container.isDebugMode());
			
			json.format(map, writer);
		} catch (Exception e) {
			container.log(e.getMessage(), e);
			response.sendError(SC_INTERNAL_SERVER_ERROR);
			return;
		}
	}
	
	protected void doREST(String method, String[] pathes, HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		
		String methodName = null;
		String callback = null;
		
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
		JSON json = new JSON(this);
		
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
				params.add(request.getParameterMap());
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
			res = json.invokeDynamic(component, methodName, params);
		} catch (ClassNotFoundException e) {
			if (container.isDebugMode()) container.log(e.getMessage(), e);
			response.sendError(SC_NOT_FOUND);
			return;			
		} catch (NoSuchMethodException e) {
			if (container.isDebugMode()) container.log(e.getMessage(), e);
			response.sendError(SC_NOT_FOUND);
			return;
		} catch (IllegalArgumentException e) {
			if (container.isDebugMode()) container.log(e.getMessage(), e);
			response.sendError(SC_BAD_REQUEST);
			return;
		} catch (JSONParseException e) {
			if (container.isDebugMode()) container.log(e.getMessage(), e);
			response.sendError(SC_BAD_REQUEST);
			return;
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			container.log(cause.getMessage(), cause);
			response.sendError(SC_INTERNAL_SERVER_ERROR, cause.getMessage());
		} catch (Exception e) {
			container.log(e.getMessage(), e);
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
			json.setPrettyPrint(!container.isDebugMode());
			
			if (callback != null) writer.append(callback).append("(");
			json.format(res, writer);
			if (callback != null) writer.append(");");
		} catch (Exception e) {
			container.log(e.getMessage(), e);
			response.sendError(SC_INTERNAL_SERVER_ERROR);
			return;
		}		
	}
	
	protected Class getClassFromPath(String[] pathes) throws ClassNotFoundException {
		StringBuffer className = new StringBuffer();
	
		boolean find = false;
		String prefix = "";
		String suffix = "Service";
		for (Map.Entry<Pattern, String> entry : routesREST.entrySet()) {
			Matcher m = entry.getKey().matcher(pathes[0]);
			if (m.find()) {
				// package root
				m.appendReplacement(className, entry.getValue());
				if (className.length() > 0 && className.charAt(className.length()-1) != '.') className.append('.');
				
				// rest path
				m.appendTail(className);
				
				// normalize
				char old = '.';
				for (int i = 0; i < className.length(); i++) {
					char c = className.charAt(i);
					if (c == '/') {
						c = '.';
					} else 	if ((old == '.') ? !Character.isJavaIdentifierStart(c) : !Character.isJavaIdentifierPart(c)) {
						c = '_';
					}
					className.append(c);
					old = c;
				}
				find = true;
				break;
			}
		}
		if (!find) return null;
		
		className.append(suffix);
		boolean isStart = true;
		for (int i = 0; i < pathes[1].length(); i++) {
			char c = pathes[1].charAt(0);
			if (isStart) {
				className.append(Character.toUpperCase(c));
			} else if (c == ' ' || c == '_' || c == '-') {
				isStart = true;
			} else {
				className.append(c);
			}
			isStart = false;
		}
		className.append(prefix);

		return Class.forName(className.toString());
	}
	
	@Override
	public void destroy() {
		container.destory();
		super.destroy();
	}
	
	class Config {
		public String container;
		public String encoding = "UTF-8";
		public Map<String, String> routes;
	}
	
	class Request {
		public String method;
		public List params;
		public Object id;
	}
	
	class Error {
		public int code;
		public String message;
		
		public Error(int code, String message) {
			this.code = code;
			this.message = message;
		}
	}
}