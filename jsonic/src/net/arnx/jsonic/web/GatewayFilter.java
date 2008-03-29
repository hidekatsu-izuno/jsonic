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
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
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
	
	class Config {
		public String encoding = null;
		public Boolean compression = false;
		public String forward = null;
	}
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		JSON json = new JSON();
		json.setContext(this);
		
		Map map = json.parse(filterConfig.getInitParameter("config"), Map.class);
		Map base = new LinkedHashMap();
		for (Map.Entry entry : (Set<Map.Entry>)map.entrySet()) {
			String key = entry.getKey().toString();
			Object value = entry.getValue();
			
			if (value instanceof Map) {
				locations.put(Pattern.compile("^" + key + "$"), (Config)json.convert(value, Config.class));
			} else {
				base.put(key, value);
			}
		}
		locations.put(null, (Config)json.convert(base, Config.class));
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, 
		FilterChain chain) throws IOException, ServletException {
		
		if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
			doFilter((HttpServletRequest)request, (HttpServletResponse)response, chain);
		} else {
			throw new UnsupportedOperationException();
		}
	}
	
	protected void doFilter(HttpServletRequest request, HttpServletResponse response, 
			FilterChain chain) throws IOException, ServletException {
		
		String uri = (request.getContextPath().equals("/")) ?
				request.getRequestURI() : 
				request.getRequestURI().substring(request.getContextPath().length());
		
		Matcher matcher = null;
		Config config = null;
		for (Map.Entry<Pattern, Config> entry : locations.entrySet()) {
			Pattern pattern = entry.getKey();
			if (pattern != null) {
				matcher = pattern.matcher(uri);
				config = entry.getValue();
				break;
			}
		}
		if (config == null) {
			config = locations.get(null);
		} else {
			Config base = locations.get(null);
			if (config.encoding == null) config.encoding = base.encoding;
			if (config.compression == null) config.compression = base.compression;
			if (config.forward == null) config.forward = base.forward;
		}
		
		boolean forward = false;
		if (config != null) {
			// set character encoding
			if (config.encoding != null) {
				request.setCharacterEncoding(config.encoding);
				response.setCharacterEncoding(config.encoding);
			}
			
			// set gzip filter
			if (config.compression != null && config.compression) {
				if (request.getAttribute(GZIPResponse.class.getName()) == null) {
					Enumeration<String> e = request.getHeaders("Accept-Encoding");
					while (e.hasMoreElements()) {
						String header = e.nextElement();
						boolean compression = false;
						if (header.indexOf("x-gzip") != -1) {
							compression = true;
							response.addHeader("Content-Encoding", "x-gzip");
						} else if (header.indexOf("gzip") != -1) {
							compression = true;
							response.addHeader("Content-Encoding", "gzip");
						}
						
						if (compression) {
							response = new GZIPResponse(response);
							break;						
						}
					}
					request.setAttribute(GZIPResponse.class.getName(), response);
				}
			}
			
			if (config.forward != null) {
				forward = true;
				RequestDispatcher dispatcher = request.getRequestDispatcher(matcher.replaceAll(config.forward));
				dispatcher.forward(request, response);
			}
		}
		
		if (!forward) {
			chain.doFilter(request, response);
		}
		
		GZIPResponse gresponse = (GZIPResponse)request.getAttribute(GZIPResponse.class.getName());
		if (gresponse != null) gresponse.close();
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
