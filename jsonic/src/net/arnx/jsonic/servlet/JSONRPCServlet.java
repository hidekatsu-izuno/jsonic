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
package net.arnx.jsonic.servlet;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.lang.reflect.InvocationTargetException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.arnx.jsonic.JSON;
import net.arnx.jsonic.util.DynamicInvoker;

public class JSONRPCServlet extends HttpServlet {
	private static final long serialVersionUID = 494827308910359676L;
	
	private Container container = null;
	
	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
		
		String containerName = servletConfig.getInitParameter("container");
		
		JSON json = new JSON(this);
		try {
			container = (Container)json.parse(servletConfig.getInitParameter("config"),
					(containerName != null) ? Class.forName(containerName) : SimpleContainer.class);
			container.init();
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String path = request.getServletPath();
		if (request.getPathInfo() != null) path += request.getPathInfo();
		
		JSON json = new JSON();
		
		Object result = null;
		String callback = request.getParameter("callback");
		
		Object o = null;
		try {
			o = container.getComponent(path);
		} catch (Exception e) {
			container.log(e.getMessage(), null);
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		try {
			List<Map<?,?>> params = new ArrayList<Map<?,?>>(1);
			params.add(request.getParameterMap());
			
			DynamicInvoker invoker = new DynamicInvoker();
			invoker.setContext(o);
			result = invoker.invoke(o, "get", params);
		} catch (Exception e) {
			throw new ServletException(e);
		}
		response.setContentType("text/javascript");
		response.setCharacterEncoding(request.getCharacterEncoding());
		
		Writer writer = response.getWriter();
		json.setPrettyPrint(!container.isDebugMode());
		
		if (callback != null) writer.append(callback).append("(");
		json.format(result, writer);
		if (callback != null) writer.append(");");
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String path = request.getServletPath();
		if (request.getPathInfo() != null) path += request.getPathInfo();

		JSON json = new JSON();
		DynamicInvoker invoker = new DynamicInvoker();
		
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
		
		try {
			json.setContext(this);
			req = json.parse(request.getReader(), Request.class);
			
			invoker.setContext(o);
			result = invoker.invoke(o, req.method, req.params);
		} catch (InvocationTargetException e) {
			error = new LinkedHashMap<String, Object>();
			error.put("name", "JSONError");
			error.put("code", 100);
			error.put("message", e.getCause().getMessage());
		} catch (Exception e) {
			if (e instanceof NoSuchMethodException) {
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
				container.log(sb.toString(), null);
			} else {
				container.log(e.getMessage(), e);
			}
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
	
	@Override
	public void destroy() {
		container.destory();
		super.destroy();
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

		@Override
		public void init() {
		}

		@Override
		public boolean isDebugMode() {
			return debug;
		}

		@Override
		public Object getComponent(String path) throws Exception {
			Class<?> target = mapping.get(path);
			if (target == null) {
				throw new IllegalArgumentException("target class is not found: " + path);
			}
			return target.newInstance();
		}

		@Override
		public void log(String message, Throwable e) {
			if (e != null) {
				JSONRPCServlet.this.log(message, e);
			} else {
				JSONRPCServlet.this.log(message);
			}
		}

		@Override
		public void destory() {
		}
	}
}
