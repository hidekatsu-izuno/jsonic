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
import java.io.PrintWriter;
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

public class GatewayFilter implements Filter {
	
	private Map<Pattern, Config> locations = new LinkedHashMap<Pattern, Config>();
	private ServletContext context;
	
	class Config {
		public String encoding = null;
		public Boolean compression = false;
		public String forward = null;
		public Set<String> access = null;
		public Locale locale = null;
	}
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.context = filterConfig.getServletContext();
		
		JSON json = new JSON();
		json.setContext(this);

		String configText = filterConfig.getInitParameter("config");
		if (configText == null) configText = "";

		Map map = json.parse(configText, Map.class);
		Config base = (Config)json.convert(map, Config.class);
		for (Map.Entry entry : (Set<Map.Entry>)map.entrySet()) {
			String key = entry.getKey().toString();
			Object value = entry.getValue();
			
			if (key.startsWith("/") && value instanceof Map) {
				Config config = (Config)json.convert(value, Config.class);
				if (config.encoding == null) config.encoding = base.encoding;
				if (config.compression == null) config.compression = base.compression;
				if (config.forward == null) config.forward = base.forward;
				if (config.access == null) config.access = base.access;
				if (config.locale == null) config.locale = base.locale;
				
				locations.put(Pattern.compile("^" + key + "$"), config);
			}
		}
		locations.put(Pattern.compile(".*"), base);
	}

	@Override
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
		
		if (request.getAttribute(Config.class.getName()) != null) {
			chain.doFilter(request, response);
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
				if (role == null || request.isUserInRole(role)) {
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
		
		// set gzip filter
		if (config.compression != null && config.compression) {
			Enumeration<String> e = request.getHeaders("Accept-Encoding");
			while (e.hasMoreElements()) {
				String header = e.nextElement();
				if (header.indexOf("gzip") != -1) {
					response.addHeader("Content-Encoding",
							(header.indexOf("x-gzip") != -1) ? "x-gzip" : "gzip");
					response = new GZIPResponse(response);
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
		
		request.setAttribute(Config.class.getName(), config);
		
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
	
	@Override
	public void destroy() {
		locations = null;
	}
	
	class GZIPResponse extends HttpServletResponseWrapper {
		private ServletOutputStream out = null;
		private PrintWriter writer = null;
		
		public GZIPResponse(HttpServletResponse response) {
			super(response);
		}
		
		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			if (out == null) {
				out = new ServletOutputStream() {
					private GZIPOutputStream cout = new GZIPOutputStream(GZIPResponse.super.getOutputStream());
					
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
