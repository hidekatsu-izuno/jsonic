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
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
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
		public String encoding = "UTF-8";
		public boolean compression = false;
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
				locations.put(Pattern.compile(key), (Config)json.convert(value, Config.class));
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
		
		Config config = locations.get(null);
		for (Map.Entry<Pattern, Config> entry : locations.entrySet()) {
			Pattern pattern = entry.getKey();
			if (pattern != null && pattern.matcher(uri).matches()) {
				config = entry.getValue();
				break;
			}
		}
		
		if (config != null) {
			if (config.encoding != null) {
				request.setCharacterEncoding(config.encoding);
				response.setCharacterEncoding(config.encoding);
			}
			
			if (config.compression) {
				Enumeration<String> e = request.getHeaders("Accept-Encoding");
				while (e.hasMoreElements()) {
					String encoding = e.nextElement();
					if (encoding.indexOf("gzip") > 0) {
						response.addHeader("Content-Encoding", "gzip");
						response = new GZIPResponseWrapper(response, response.getCharacterEncoding());
						break;
					}
				}
			}
		}
		
		chain.doFilter(request, response);
	}
	
	@Override
	public void destroy() {
		locations = null;
	}
	
	class GZIPResponseWrapper extends HttpServletResponseWrapper {
		private ServletOutputStream out = null;
		private PrintWriter writer = null;
		private String encoding = null;
		
		public GZIPResponseWrapper(HttpServletResponse response, String encoding) {
			super(response);
			this.encoding = encoding;
		}
		
		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			if (out != null) {
				out = new ServletOutputStream() {
					private GZIPOutputStream out = new GZIPOutputStream(GZIPResponseWrapper.super.getOutputStream());
					
					@Override
					public void write(byte[] b, int off, int len) throws IOException {
						out.write(b, off, len);
					}
					
					@Override
					public void write(byte[] b) throws IOException {
						out.write(b);
					}

					@Override
					public void write(int b) throws IOException {
						out.write(b);
					}
					
					@Override
					public void flush() throws IOException {
						out.flush();
					}
					
					@Override
					public void close() throws IOException {
						out.close();
					}
				};
			}
			return out;
		}
		
		@Override
		public PrintWriter getWriter() throws IOException {
			if (writer != null) {
				if (encoding != null) {
					writer = new PrintWriter(new OutputStreamWriter(out, encoding));
				} else {
					writer = new PrintWriter(out);
				}
			}
			return writer;
		}
	}
}
