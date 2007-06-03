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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
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
	
	private Map<String, Class> container = new HashMap<String, Class>();
	
	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		JSON json = new JSON();
		json.setExtendedMode(true);
		try {
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
		String callback = null;
		
		try {
			Class target = container.get(request.getServletPath());
			Object o = target.newInstance();
			
			Map pmap = request.getParameterMap();
			
			List<Map> params = null;
			if (pmap != null && !pmap.isEmpty()) {
				params = new ArrayList<Map>(1);
				params.add(pmap);
				callback = (String)pmap.get("callback");
			}
			
			result = invoke(json, o, "execute", params);
		} catch (Exception e) {
			throw new ServletException(e);
		}
		response.setContentType("application/json");
		response.setCharacterEncoding(request.getCharacterEncoding());
		
		Writer writer = response.getWriter();
		json.setExtendedMode(false);
		json.setPrettyPrint(true);
		
		if (callback != null) {
			writer.write(callback + "(" + json.format(result) + ");");
		} else {
			writer.write(json.format(result));
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String content = read(request.getReader(), request.getCharacterEncoding());
		
		JSON json = new JSON();
		
		Request req = null;
		
		Object result = null;
		Map<String, Object> error = null;
		
		try {
			Class target = container.get(request.getServletPath());
			Object o = target.newInstance();
			
			json.setExtendedMode(true);
			req = json.parse(content, Request.class);
			
			result = invoke(json, o, req.method, req.params);
		} catch (InvocationTargetException e) {
			error = new LinkedHashMap<String, Object>();
			error.put("name", "JSONError");
			error.put("code", 100);
			error.put("message", e.getCause().getMessage());
		} catch (Exception e) {
			throw new ServletException(e);
		}
		
		Map<String, Object> res = new LinkedHashMap<String, Object>();
		res.put("result", result);
		res.put("error", error);
		res.put("id", req.id);
		
		response.setContentType("application/json");
		response.setCharacterEncoding(request.getCharacterEncoding());
		
		Writer writer = response.getWriter();
		json.setExtendedMode(false);
		json.setPrettyPrint(true);
		writer.write(json.format(res));
	}
	
	protected Object invoke(JSON json, Object o, String methodName, List params) throws Exception {
		if (params == null) {
			params = Collections.EMPTY_LIST;
		}
		
		Class target = o.getClass();
		Method method = null;
		loop: do {
			for (Method m : target.getDeclaredMethods()) {
				if (methodName.equals(m.getName())
						&& !Modifier.isStatic(m.getModifiers())
						&& Modifier.isPublic(m.getModifiers())) {
					if (method == null && params.size() == m.getParameterTypes().length) {
						method = m;
						break loop;
					}
				}
			}
			
			target = target.getSuperclass();
		} while (method == null && target != null);
		
		if (method == null || limit(method)) {
			throw new NoSuchMethodException();
		}
		
		Class[] paramTypes = method.getParameterTypes();
		Object[] paramArray = new Object[paramTypes.length];
		for (int i = 0; i < paramArray.length; i++) {
			paramArray[i] = json.convert(null, params.get(i), paramTypes[i], paramTypes[i]);
		}
		return method.invoke(o, paramArray);
	}
	
	protected boolean limit(Method method) {
		return method.getDeclaringClass().equals(Object.class);
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