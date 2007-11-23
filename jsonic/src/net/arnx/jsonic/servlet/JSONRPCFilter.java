package net.arnx.jsonic.servlet;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletContext;
import javax.servlet.http.*;

import net.arnx.jsonic.JSON;
import net.arnx.jsonic.util.DynamicInvoker;

public class JSONRPCFilter implements Filter {
	
	private ServletContext context = null;
	private Container container = null;

	public void init(FilterConfig config) throws ServletException {
		context = config.getServletContext();
		
		String containerName = config.getInitParameter("container");
		String configText = config.getInitParameter("config");
		if (configText == null || configText.trim().length() == 0) {
			configText = "{}";
		}
		
		try {
			Class containerClass = (containerName != null) ? 
					Class.forName(containerName) : SimpleContainer.class;
					
			JSON json = (containerName != null) ? 
					new JSON(containerClass) : new JSON(this);
					
			container = (Container)json.parse(configText, containerClass);
			container.init();
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		
		if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
			HttpServletRequest hrequest = (HttpServletRequest)request;
			HttpServletResponse hresponse = (HttpServletResponse)response;
			
			String path = hrequest.getRequestURI();
			if (!path.equals("/")) path = path.substring(hrequest.getContextPath().length());
			
			if (!new File(context.getRealPath(path)).exists()) {
				if ("GET".equalsIgnoreCase(hrequest.getMethod())) {
					doGet(hrequest, hresponse, path);
				} else if ("POST".equalsIgnoreCase(hrequest.getMethod())) {
					doPost(hrequest, hresponse, path);
				} else {
					hresponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
				}
				
				return;
			}
		}
		
		chain.doFilter(request, response);
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response, String path)
		throws IOException, ServletException {
		
		Object result = null;
		String callback = request.getParameter("callback");
		
		Object o = null;
		try {
			o = container.getComponent(path);
		} catch (Exception e) {
			container.log(e.getMessage(), e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		
		Map map = request.getParameterMap();
		try {
			List<Map<?,?>> params = new ArrayList<Map<?,?>>(1);
			if (!map.isEmpty()) {
				params.add(request.getParameterMap());
			}
			
			DynamicInvoker invoker = new DynamicInvoker();
			invoker.setContext(o);
			result = invoker.invoke(o, "get", params);
		} catch (NoSuchMethodException e) {
			StringBuilder sb = new StringBuilder("missing method: ");
			sb.append(o.getClass().getName()).append(".get(Map params)");
			container.log(sb.toString());
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		} catch (Exception e) {
			container.log(e.getMessage(), e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		response.setContentType("text/javascript");
		response.setCharacterEncoding(request.getCharacterEncoding());
		
		JSON json = new JSON();
		
		Writer writer = response.getWriter();
		json.setPrettyPrint(!container.isDebugMode());
		
		if (callback != null) writer.append(callback).append("(");
		json.format(result, writer);
		if (callback != null) writer.append(");");
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response, String path)
		throws IOException, ServletException {
		
		Object o = null;
		try {
			o = container.getComponent(path);
		} catch (Exception e) {
			container.log(e.getMessage(), e);
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		Request req = null;
		
		Object result = null;
		Map<String, Object> error = null;
		
		JSON json = new JSON(this);
		try {
			req = json.parse(request.getReader(), Request.class);
			
			DynamicInvoker invoker = new DynamicInvoker();
			invoker.setContext(o);
			result = invoker.invoke(o, req.method, req.params);
		} catch (InvocationTargetException e) {
			error = new LinkedHashMap<String, Object>();
			error.put("name", "JSONError");
			error.put("code", 100);
			error.put("message", e.getCause().getMessage());
		} catch (NoSuchMethodException e) {
			StringBuilder sb = new StringBuilder("missing method: ");
			sb.append(o.getClass().getName()).append(".");
			sb.append(req.method).append("(");
			if (req.params != null) {
				for (int i = 0; i < req.params.size(); i++) {
					if (i != 0) sb.append(", ");
					sb.append(req.params.get(i));
				}
			}
			sb.append(")");
			container.log(sb.toString());
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		} catch (Exception e) {
			container.log(e.getMessage(), e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		
		Map<String, Object> res = new LinkedHashMap<String, Object>();
		res.put("result", result);
		res.put("error", error);
		res.put("id", req.id);
		
		response.setContentType("application/json");
		response.setCharacterEncoding(request.getCharacterEncoding());
		
		Writer writer = response.getWriter();
		json.setPrettyPrint(container.isDebugMode());
		writer.write(json.format(res));
	}

	public void destroy() {
		container.destory();
	}
	
	class Request {
		public String version = "1.0";
		public String method;
		public List<Object> params;
		public Object id;
	}
	
	class SimpleContainer implements Container {
		public boolean debug;
		public Map<String, Class<?>> mapping;

		public void init() {
		}

		public boolean isDebugMode() {
			return debug;
		}

		public Object getComponent(String path) throws Exception {
			Class<?> target = mapping.get(path);
			if (target == null) {
				throw new IllegalArgumentException("target class is not found: " + path);
			}
			return target.newInstance();
		}

		public void log(String message) {
			context.log(message);
		}
		
		public void log(String message, Throwable e) {
			context.log(message, e);
		}

		public void destory() {
		}
	}
}
