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
package net.arnx.jsonic;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.Writer;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class JSONRPCServlet extends HttpServlet {
	private static final long serialVersionUID = 494827308910359676L;
	
	private boolean debug = false;
	private Map<String, Class> container = new HashMap<String, Class>();
	
	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		JSON json = new JSON();
		json.setExtendedMode(true);
		try {
			debug = Boolean.valueOf(servletConfig.getInitParameter("debug"));
			
			Map config = json.parse(servletConfig.getInitParameter("config"), Map.class);
			for (Object o : config.entrySet()) {
				Map.Entry entry = (Map.Entry)o;
				container.put(entry.getKey().toString(), 
						Class.forName(entry.getValue().toString()));
			}
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		JSON json = new JSON();
		
		Object result = null;
		String callback = request.getParameter("callback");
		
		Object o = null;
		try {
			o = getComponent(request.getServletPath());
		} catch (Exception e) {
			log(e.getMessage());
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		try {
			List<Map> params = new ArrayList<Map>(1);
			params.add(request.getParameterMap());
			
			json.setContext(o);
			result = json.invokeDynamic(o, "execute", params);
		} catch (Exception e) {
			throw new ServletException(e);
		}
		response.setContentType("application/json");
		response.setCharacterEncoding(request.getCharacterEncoding());
		
		Writer writer = response.getWriter();
		json.setExtendedMode(false);
		json.setPrettyPrint(debug);
		
		if (callback != null) writer.append(callback).append("(");
		json.format(result, writer);
		if (callback != null) writer.append(");");
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String content = read(request.getReader(), request.getCharacterEncoding());
		
		JSON json = new JSON();
		
		Object o = null;
		try {
			o = getComponent(request.getServletPath());
		} catch (Exception e) {
			log(e.getMessage());
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		Request req = null;
		
		Object result = null;
		Map<String, Object> error = null;
		
		try {
			json.setExtendedMode(true);
			json.setContext(this);
			req = json.parse(content, Request.class);
			
			json.setContext(o);
			result = json.invokeDynamic(o, req.method, req.params);
		} catch (InvocationTargetException e) {
			error = new LinkedHashMap<String, Object>();
			error.put("name", "JSONError");
			error.put("code", 100);
			error.put("message", e.getCause().getMessage());
		} catch (Exception e) {
			log(e.getMessage());
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
		json.setExtendedMode(false);
		json.setPrettyPrint(debug);
		writer.write(json.format(res));
	}
	
	protected Object getComponent(String path) throws Exception {
		Class target = container.get(path);
		if (target == null) {
			throw new IllegalArgumentException("target class is not found.");
		}
		return target.newInstance();
	}
	
	private String read(BufferedReader reader, String encoding) throws IOException {
		StringBuilder sb = new StringBuilder(8192);
		char[] cs = new char[1024];
		int len = 0;
		while ((len = reader.read(cs)) != -1) {
			sb.append(cs, 0, len);
		}
		return URLDecoder.decode(sb.toString(), encoding);
	}
}

class Request {
	public String version = "1.0";
	public String method;
	public List params;
	public Object id;
}