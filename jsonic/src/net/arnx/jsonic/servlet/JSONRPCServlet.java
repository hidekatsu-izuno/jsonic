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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Cipher;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.arnx.jsonic.JSON;
import net.arnx.jsonic.util.DynamicInvoker;

public class JSONRPCServlet extends HttpServlet {
	private static final long serialVersionUID = 494827308910359676L;
	
	private Config config;
	
	private static final byte[] NULL_STATE = new byte[0];
	private static final String ENCRYPT_ALGORISM = "AES";
	private SecretKeySpec encryptKey;
	
	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
		
		JSON json = new JSON(this);
		try {
			config = json.parse(servletConfig.getInitParameter("config"), Config.class);
			if (config.key != null) {
				encryptKey = new SecretKeySpec(config.key.getBytes("UTF-8"), ENCRYPT_ALGORISM);
			}
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (request.getPathInfo().startsWith("/jsonic")) {
			String name = request.getPathInfo().substring("/jsonic/".length());
			
			if (name.endsWith(".js")) {
				response.setCharacterEncoding("UTF-8");
				response.setContentType("text/javascript");
				
				
				InputStream in = getClass().getResourceAsStream(name);
				if (in != null) { 
					OutputStream out = response.getOutputStream();
					
					byte[] buffer = new byte[1024];
					int n = -1;
					while ((n = in.read(buffer)) != -1) {
						out.write(buffer, 0, n);
					}
					in.close();
					return;
				}
			}
			
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		JSON json = new JSON();
		
		Object result = null;
		String callback = request.getParameter("callback");
		
		Object o = null;
		try {
			o = getComponent(request.getPathInfo());
		} catch (Exception e) {
			log(e.getMessage());
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
		json.setPrettyPrint(config.debug);
		
		if (callback != null) writer.append(callback).append("(");
		json.format(result, writer);
		if (callback != null) writer.append(");");
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		JSON json = new JSON();
		
		Object o = null;
		try {
			o = getComponent(request.getPathInfo());
		} catch (Exception e) {
			log(e.getMessage());
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		Request req = null;
		
		Object result = null;
		Map<String, Object> error = null;
		
		try {
			json.setContext(this);
			req = json.parse(request.getReader(), Request.class);
			
			DynamicInvoker invoker = new DynamicInvoker();
			invoker.setContext(o);

			if (req.state != null && req.state != NULL_STATE) {
				invoker.apply(o, decrypt(req.state));
			}
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
				log(sb.toString());
			} else {
				log(e.getMessage(), e);
			}
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		
		Map<String, Object> res = new LinkedHashMap<String, Object>();
		res.put("result", result);
		res.put("error", error);
		res.put("id", req.id);
		if (req.state != null) {
			res.put("state", encrypt(o));
		}
		
		response.setContentType("application/json");
		response.setCharacterEncoding(request.getCharacterEncoding());
		
		Writer writer = response.getWriter();
		json.setPrettyPrint(config.debug);
		writer.write(json.format(res));
	}
	
	protected Object getComponent(String path) throws Exception {
		Class<?> target = config.mapping.get(path);
		if (target == null) {
			throw new IllegalArgumentException("target class is not found: " + path);
		}
		return target.newInstance();
	}
	
	protected byte[] encrypt(Object o) {
		try {
			Cipher cipher = Cipher.getInstance(ENCRYPT_ALGORISM);
			cipher.init(Cipher.ENCRYPT_MODE, encryptKey);
			return cipher.doFinal(JSON.encode(o).getBytes("UTF-8"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	protected Object decrypt(byte[] data) {
		try {
			Cipher cipher = Cipher.getInstance(ENCRYPT_ALGORISM);
			cipher.init(Cipher.DECRYPT_MODE, encryptKey);
			return JSON.decode(new String(cipher.doFinal(data), "UTF-8"));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	class Config {
		public boolean debug;
		public Map<String, Class<?>> mapping;
		public String key;
	}
	
	class Request {
		public String version = "1.0";
		public String method;
		public List<Object> params;
		public Object id;
		public byte[] state = NULL_STATE;
	}
}
