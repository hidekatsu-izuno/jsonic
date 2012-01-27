/*
 * Copyright 2008 Hidekatsu Izuno
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
package net.arnx.jsonic.web;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import net.arnx.jsonic.JSON;

@SuppressWarnings({"unchecked", "rawtypes"})
public class GatewayFilter implements Filter {
	public static final String GATEWAY_KEY = Config.class.getName();
	
	Map<Pattern, Config> locations = new LinkedHashMap<Pattern, Config>();
	ServletContext context;
	
	class Config {
		public String encoding = null;
		public Boolean compression = false;
		public Boolean expire = false;
		public String forward = null;
		public Set<String> access = null;
		public Locale locale = null;
	}
	
	public void init(FilterConfig filterConfig) throws ServletException {
		this.context = filterConfig.getServletContext();
		
		JSON json = new JSON();
		json.setContext(this);

		String configText = filterConfig.getInitParameter("config");
		if (configText == null) configText = "";

		Map map = json.parse(configText, Map.class);
		
		Map<String, Object> baseMap = new LinkedHashMap<String, Object>();
		for (Field field : Config.class.getFields()) {
			baseMap.put(field.getName(), map.get(field.getName()));
		}
		
		Config base = (Config)json.convert(baseMap, Config.class);
		for (Map.Entry entry : (Set<Map.Entry>)map.entrySet()) {
			if (!baseMap.containsKey(entry.getKey()) && entry.getValue() instanceof Map) {
				Map valueMap = (Map)entry.getValue();
				for (Map.Entry<String, Object> baseEntry : baseMap.entrySet()) {
					if (valueMap.get(baseEntry.getKey()) == null) {
						valueMap.put(baseEntry.getKey(), baseEntry.getValue());
					}
				}
				
				Config config = (Config)json.convert(valueMap, Config.class);
				locations.put(Pattern.compile("^" + entry.getKey() + "$"), config);
			}
		}
		locations.put(Pattern.compile(".*"), base);
	}

	public void doFilter(ServletRequest request, ServletResponse response, 
		FilterChain chain) throws IOException, ServletException {
		
		if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
			doFilter((HttpServletRequest)request, (HttpServletResponse)response, chain);
		} else {
			chain.doFilter(request, response);
		}
	}
	
	protected void doFilter(HttpServletRequest request, HttpServletResponse response, 
			FilterChain chain) throws IOException, ServletException {
		
		if (request.getAttribute(GATEWAY_KEY) != null) {
			chain.doFilter(request, response);
			return;
		}
		
		String path = (request.getContextPath().equals("/")) ?
				request.getRequestURI() : 
				request.getRequestURI().substring(request.getContextPath().length());
		
		Matcher matcher = null;
		Config config = null;
		for (Map.Entry<Pattern, Config> entry : locations.entrySet()) {
			Pattern pattern = entry.getKey();
			Matcher m = pattern.matcher(path);
			if (m.matches()) {
				matcher = m;
				config = entry.getValue();
				break;
			}
		}
		
		URI dest = null;
		// access check
		if (config.access != null) {
			boolean access = false;
			for (String role : config.access) {
				if (request.isUserInRole(role)) {
					access = true;
					break;
				}
			}
			
			if (!access) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
				return;
			}
		}
		
		// set character encoding
		if (config.encoding != null) {
			request.setCharacterEncoding(config.encoding);
			response.setCharacterEncoding(config.encoding);
		}
		
		// set response locale
		if (config.locale != null) {
			response.setLocale(config.locale);
		}
		
		// set no-cache
		if (config.expire != null && config.expire) {
			response.setHeader("Cache-Control","no-cache");
			response.setHeader("Pragma","no-cache");
			response.setHeader("Expires", "Tue, 29 Feb 2000 12:00:00 GMT");
		}
		
		// set gzip filter
		if (config.compression != null && config.compression) {
			Enumeration<String> e = request.getHeaders("Accept-Encoding");
			while (e.hasMoreElements()) {
				String header = e.nextElement();
				if (header.indexOf("gzip") != -1) {
					response.setHeader("Content-Encoding",
							(header.indexOf("x-gzip") != -1) ? "x-gzip" : "gzip");
					response = new GZIPResponse(response);
					break;
				}
			}
		}
		
		// forward
		if (config.forward != null) {
			try {
				dest = new URI(matcher.replaceAll(config.forward));
			} catch (URISyntaxException e) {
				throw new ServletException(e);
			}
		}
		
		request.setAttribute(GATEWAY_KEY, config);
		
		if (dest != null) {
			RequestDispatcher dispatcher = context.getRequestDispatcher(dest.toString());
			dispatcher.forward(request, response);
		} else {
			chain.doFilter(request, response);
		}

		if (response instanceof GZIPResponse) {
			((GZIPResponse)response).close();
		}
	}
	
	public void destroy() {
		locations = null;
	}
	
	class GZIPResponse extends HttpServletResponseWrapper {
		ServletOutputStream out = null;
		PrintWriter writer = null;
		
		public GZIPResponse(HttpServletResponse response) {
			super(response);
		}
		
		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			if (out == null) {
				out = new ServletOutputStream() {
					GZIPOutputStream cout = new GZIPOutputStream(GZIPResponse.super.getOutputStream());
					
					@Override
					public void write(byte[] b, int off, int len) throws IOException {
						cout.write(b, off, len);
					}
					
					@Override
					public void write(byte[] b) throws IOException {
						cout.write(b);
					}

					@Override
					public void write(int b) throws IOException {
						cout.write(b);
					}
					
					@Override
					public void flush() throws IOException {
						cout.flush();
					}
					
					@Override
					public void close() throws IOException {
						cout.close();
					}
				};
			}
			return out;
		}
		
		@Override
		public PrintWriter getWriter() throws IOException {
			if (writer == null) {
				writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), getCharacterEncoding()));
			}
			return writer;
		}

		public void close() throws IOException {
			if (writer != null) {
				writer.flush();
				writer.close();
			} else if (out != null) {
				out.flush();
				out.close();
			}
		}
	}
}
