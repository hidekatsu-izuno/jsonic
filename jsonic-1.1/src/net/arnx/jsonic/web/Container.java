/*
 * Copyright 2007-2008 Hidekatsu Izuno
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class Container {
	public Boolean debug = false;
	public String init = "init";
	public String destroy = "destroy";
	
	private ServletConfig config;
	private ServletContext context;

	public void init(ServletConfig config) {
		this.config = config;
		this.context = config.getServletContext();
	}

	public boolean isDebugMode() {
		return (debug != null) ? debug : false;
	}
	
	public Object getComponent(String className, HttpServletRequest request, HttpServletResponse response) 
		throws Exception {
		
		Object o = findClass(className).newInstance();
		
		for (Field field : o.getClass().getFields()) {
			Class<?> c = field.getType();
			if (ServletContext.class.equals(c) && "application".equals(field.getName())) {
				field.set(o, context);
			} else if (ServletConfig.class.equals(c) && "config".equals(field.getName())) {
				field.set(o, config);
			} else if (HttpServletRequest.class.equals(c) && "request".equals(field.getName())) {
				field.set(o, request);
			} else if (HttpServletResponse.class.equals(c)	&& "response".equals(field.getName())) {
				field.set(o, response);
			} else if (HttpSession.class.equals(c) && "session".equals(field.getName())) {
				field.set(o, request.getSession(true));
			}
		}
		
		return o;
	}
	
	public Class<?> findClass(String name) throws ClassNotFoundException {
		Class<?> c = null;
		try {
			c = Class.forName(name, true, Thread.currentThread().getContextClassLoader());
		} catch (ClassNotFoundException e) {
			try {
				c = Class.forName(name, true, this.getClass().getClassLoader());
			} catch (ClassNotFoundException e2) {
				c = Class.forName(name);				
			}
		}
		
		return c;
	}
	
	protected boolean limit(Class<?> c, Method method) {
		return method.getDeclaringClass().equals(Object.class);
	}
	
	public void debug(String message) {
		debug(message, null);
	}
	
	public void debug(String message, Throwable e) {
		if (!isDebugMode()) return;
		
		if (e != null) {
			context.log(message, e);
		} else {
			context.log(message);
		}
	}
	
	public void error(String message, Throwable e) {
		if (e != null) {
			context.log(message, e);
		} else {
			context.log(message);
		}
	}

	public void destory() {
	}
}