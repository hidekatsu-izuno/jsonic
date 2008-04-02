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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
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
	}
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.context = filterConfig.getServletContext();
		
		JSON json = new JSON();
		json.setContext(this);
		
		Map map = json.parse(filterConfig.getInitParameter("config"), Map.class);
		Map base = new LinkedHashMap();
		for (Map.Entry entry : (Set<Map.Entry>)map.entrySet()) {
			String key = entry.getKey().toString();
			Object value = entry.getValue();
			
			if (key.startsWith("/") && value instanceof Map) {
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
			if (pattern != null) {
				matcher = pattern.matcher(path);
				if (matcher.matches()) {
					config = entry.getValue();
					break;
				} else {
					matcher = null;
				}
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
		
		URI dest = null;
		if (config != null) {
			// set character encoding
			if (config.encoding != null) {
				request.setCharacterEncoding(config.encoding);
				response.setCharacterEncoding(config.encoding);
			}
			
			// set gzip filter
			if (config.compression != null && config.compression) {
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
			}
			
			// forward
			if (config.forward != null) {
				try {
					StringBuilder sb = new StringBuilder(matcher.replaceAll(config.forward));
					if (request.getQueryString() != null) {
						sb.append('?').append(request.getQueryString());
					}
					dest = new URI(sb.toString());
				} catch (URISyntaxException e) {
					throw new ServletException(e);
				}
			}
		}
		
		request.setAttribute(Config.class.getName(), config);
		
		if (dest != null) {
			if (dest.isAbsolute()) {
				HttpURLConnection con = (HttpURLConnection)dest.toURL().openConnection();
				Enumeration e = request.getHeaderNames();
				while (e.hasMoreElements()) {
					String key = (String)e.nextElement();
					con.addRequestProperty(key, request.getHeader(key));
				}
				byte[] buffer = new byte[1024];
				int length = 0;
				
				if (!request.getMethod().equals("GET")) {
					con.setRequestMethod(request.getMethod());
					con.setDoOutput(true);
					InputStream reqIn = request.getInputStream();
					OutputStream reqOut = con.getOutputStream();
					
					try {
						while ((length = reqIn.read(buffer)) > 0) {
							reqOut.write(buffer, 0, length);
						}
						reqOut.flush();
					} finally {
						reqIn.close();
						reqOut.close();
					}
				}
				
				con.connect();
				
				int code = con.getResponseCode();

				String charset = null;
				for (int i = 0; con.getHeaderField(i) != null; i++) {
					String key = con.getHeaderFieldKey(i);
					if (key == null) {
						continue;
					} else if (key.equals("Content-Type")) {
						String value = con.getHeaderField(i);
						int start = value.indexOf("charset=");
						if (start > 0) {
							int last = value.indexOf(";", start + 8);
							if (last == -1) last = value.length();
							charset = value.substring(start+8, last).trim();
							if (response.getCharacterEncoding().equalsIgnoreCase(charset)) {
								charset = null;
								response.addHeader(key, value);
							} else {
								StringBuilder sb = new StringBuilder();
								if (start != 0) sb.append(value, 0, start);
								sb.append(" charset=").append(response.getCharacterEncoding());
								if (last != value.length()) sb.append(value, last, value.length());
								
								response.addHeader(key, sb.toString());
							}
						} else {
							response.addHeader(key, value);
						}
					} else {
						response.addHeader(key, con.getHeaderField(i));
					}
				}
				
				
				InputStream resIn = null;
				if (code >= 200 && code < 300) {
					response.setStatus(code);
					resIn = con.getInputStream();
				} else {
					response.sendError(code, con.getResponseMessage());
					resIn = con.getErrorStream();
				}
				
				if (resIn != null) {
					if (charset != null) {
						Reader reader = new BufferedReader(new InputStreamReader(resIn, charset));
						Writer writer = response.getWriter();
						
						char[] cbuf = new char[1024];
						try {
							while ((length = reader.read(cbuf)) != -1) {
								if (length > 0) writer.write(cbuf, 0, length);
							}
						} finally {
							reader.close();
							con.disconnect();
						}
					} else {
						OutputStream resOut = response.getOutputStream();
						try {
							while ((length = resIn.read(buffer)) != -1) {
								if (length > 0) resOut.write(buffer, 0, length);
							}
						} finally {
							resIn.close();				
							con.disconnect();
						}
					}
				}
			} else {
				RequestDispatcher dispatcher = context.getRequestDispatcher(dest.getPath());
				dispatcher.forward(request, response);
			}
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
