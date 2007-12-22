package net.arnx.jsonic;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.arnx.jsonic.container.Container;
import net.arnx.jsonic.container.SimpleContainer;

public class JSONRPCServlet extends HttpServlet {
	private static final long serialVersionUID = -63348112220078595L;
	
	private static Map<String, String> RESTFULL_METHODS = new HashMap<String, String>();
	
	static {
		RESTFULL_METHODS.put("GET", "find");
		RESTFULL_METHODS.put("POST", "create");
		RESTFULL_METHODS.put("PUT", "update");
		RESTFULL_METHODS.put("DELETE", "delete");
	}

	private Container container;
	private Config config;
	
	private Map<Pattern, String> routes = new LinkedHashMap<Pattern, String>();
	
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
		
		if (config.routes == null) {
			for (Map.Entry<String, String> entry : config.routes.entrySet()) {
				routes.put(Pattern.compile("^" + entry.getKey()), entry.getValue());
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
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
	}
	
	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
		throws ServletException, IOException {
	}
	
	protected void process(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		
		if (config.encoding != null) {
			request.setCharacterEncoding(config.encoding);
			response.setCharacterEncoding(config.encoding);
		}

		String path = request.getRequestURI();
		int start = (request.getContextPath().equals("/")) ? 0 : request.getContextPath().length();
		int end = (path.length() > 1 && path.charAt(path.length()-1) == '/') ? path.length()-1 : path.length();
		path = path.substring(start, end);
		
		// forward when no component or file existed
		if (path.equals("/") ||  new File(getServletContext().getRealPath(path)).exists()) {
			// TODO
			return;
		}
		
		// forward when root path not found
		String packageRoot = null;
		for (Map.Entry<Pattern, String> entry : routes.entrySet()) {
			Matcher m = entry.getKey().matcher(path);
			if (m.find()) {
				StringBuffer sb = new StringBuffer();
				
				// package root
				m.appendReplacement(sb, entry.getValue());
				packageRoot = sb.toString();
				
				// rest path
				sb.setLength(0);
				m.appendTail(sb);
				path = (sb.length() > 0 && sb.charAt(0) == '/') ? sb.substring(1) : sb.toString();
				break;
			}
		}
		if (packageRoot == null || path.length() == 0) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		
		JSON json = new JSON(this);
		
		Object component = null;
		String method = null;
		List params = null;
		
		Object result = null;
		Map error = null;
		Object id = null;
		
		// request processing
		try {
			component = container.getComponent(path);
			if (component == null) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			
			if (request.getMethod().equals("GET")) {
				params = new ArrayList();
				params.add(request.getParameterMap());
				method = request.getParameter("method");
				if (method == null) method = RESTFULL_METHODS.get(request.getMethod());
			} else {
				Request req = json.parse(request.getReader(), Request.class);
				method = req.method;
				params = req.params;
				id = req.id;
			}
			
			json.setContext(component);
			result = json.invokeDynamic(component, method, params);
		} catch (JSONParseException e) {
			if (container.isDebugMode()) container.log(e.getMessage(), e);
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		} catch (NoSuchMethodException e) {
			if (container.isDebugMode()) {
				StringBuilder sb = new StringBuilder("missing method: ");
				sb.append(component.getClass().getName()).append(".");
				sb.append(method).append("(");
				if (params != null) {
					for (int i = 0; i < params.size(); i++) {
						if (i != 0) sb.append(", ");
						sb.append(params.get(i));
					}
				}
				sb.append(")");
				container.log(e.getMessage(), e);
			}
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			container.log(cause.getMessage(), cause);
			
			error = new LinkedHashMap<String, Object>();
			error.put("name", "JSONError");
			error.put("code", 100);
			error.put("message", cause.getMessage());
		} catch (Exception e) {
			container.log(e.getMessage(), e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		
		// response processing
		String callback = (request.getMethod().equals("GET")) ? request.getParameter("callback") : null;
		response.setContentType((callback != null) ? "text/javascript" : "application/json");
		
		if (request.getMethod().equals("HEAD")) {
			return;
		}
		
		try {
			Map<String, Object> res = new LinkedHashMap<String, Object>();
			res.put("result", result);
			res.put("error", error);
			res.put("id", id);
			
			Writer writer = response.getWriter();
			json.setPrettyPrint(!container.isDebugMode());
			
			if (callback != null) writer.append(callback).append("(");
			json.format(result, writer);
			if (callback != null) writer.append(");");
		} catch (Exception e) {
			container.log(e.getMessage(), e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}		
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
		public String version = "1.0";
		public String method;
		public List<Object> params;
		public Object id;
	}
}
