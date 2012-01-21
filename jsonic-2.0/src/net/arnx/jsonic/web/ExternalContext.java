/*
 * Copyright 2011 Hidekatsu Izuno
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

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class ExternalContext {
	private static final ThreadLocal<ExternalContext> THREAD_LOCAL = new InheritableThreadLocal<ExternalContext>() {
		protected ExternalContext initialValue() {
			throw new UnsupportedOperationException();
		};
	};
	
	static void start(ServletConfig config, ServletContext application, HttpServletRequest request, HttpServletResponse response) {
		synchronized (THREAD_LOCAL) {
			THREAD_LOCAL.set(new ExternalContext(config, application, request, response));
		}
	}
	
	public static ServletConfig getConfig() {
		return THREAD_LOCAL.get().config;
	}
	
	public static ServletContext getApplication() {
		return THREAD_LOCAL.get().application;
	}
	
	public static HttpServletRequest getRequest() {
		return THREAD_LOCAL.get().request;
	}
	
	public static HttpServletResponse getResponse() {
		return THREAD_LOCAL.get().response;
	}
	
	public static HttpSession getSession() {
		return THREAD_LOCAL.get().request.getSession();
	}
	
	static void end() {
		synchronized (THREAD_LOCAL) {
			THREAD_LOCAL.remove();
		}
	}
	
	private ServletConfig config;
	private ServletContext application;
	private HttpServletRequest request;
	private HttpServletResponse response;
	
	private ExternalContext(ServletConfig config, ServletContext application, HttpServletRequest request, HttpServletResponse response) {
		this.config = config;
		this.application = application;
		this.request = request;
		this.response = response;
	}
}
