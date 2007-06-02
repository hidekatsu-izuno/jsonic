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

public class JSONRpcServlet extends HttpServlet {
	private static final long serialVersionUID = 494827308910359676L;
	
	private Map<String, Object> container = new HashMap<String, Object>();
	
	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		JSON json = new JSON();
		json.setExtendedMode(true);
		try {
			Map config = json.parse(servletConfig.getInitParameter("config"), Map.class);
			for (Object o : config.entrySet()) {
				Map.Entry entry = (Map.Entry)o;
				container.put(entry.getKey().toString(), 
						Class.forName(entry.getValue().toString()).newInstance());
			}
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String content = read(request.getReader(), request.getCharacterEncoding());
		
		JSON json = new JSON();
		
		Request req = null;
		Map<String, Object> res = new LinkedHashMap<String, Object>();
		
		Object o = container.get(request.getServletPath());
		Method method = null;
		Object[] params = null;
		
		try {
			json.setExtendedMode(true);
			req = json.parse(content, Request.class);
			
			if (req.params == null) {
				req.params = Collections.EMPTY_LIST;
			}
			
			Class target = o.getClass();
			Class[] paramTypes = null;
			do {
				for (Method m : target.getDeclaredMethods()) {
					if (req.method.equals(m.getName())
							&& !Modifier.isStatic(m.getModifiers())
							&& Modifier.isPublic(m.getModifiers())) {
						paramTypes = m.getParameterTypes();
						if (method == null && req.params.size() == paramTypes.length) {
							method = m;
						}
					}
				}
				
				target = target.getSuperclass();
			} while (method == null && target != null);
			
			if (method == null || limit(method)) {
				throw new NoSuchMethodException();
			}
			
			params = new Object[paramTypes.length];
			for (int i = 0; i < paramTypes.length; i++) {
				params[i] = json.convert(null, req.params.get(i), paramTypes[i], paramTypes[i]);
			}
			res.put("result", method.invoke(o, params));
			res.put("error", null);
			res.put("id", req.id);
		} catch (InvocationTargetException e) {
			Map<String, Object> error = new LinkedHashMap<String, Object>();
			error.put("name", "JSONError");
			error.put("code", 0);
			error.put("message", e.getCause().getMessage());
			
			res.put("result", null);
			res.put("error", error);
			res.put("id", req.id);
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					e.getMessage());
			return;
		}
		
		response.setContentType("application/json");
		
		Writer writer = response.getWriter();
		json.setExtendedMode(false);
		writer.write(json.format(res));
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
	public String version;
	public String method;
	public List params;
	public Object id;
}