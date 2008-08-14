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

import java.lang.reflect.Method;

import javax.servlet.ServletContext;

public class Container {
	public Boolean debug = false;
	public String init = "init";
	public String destroy = "destroy";
	
	private ServletContext context;

	public void init(ServletContext context) {
		this.context = context;
	}

	public boolean isDebugMode() {
		return debug;
	}

	public <T> T getComponent(String className) throws Exception {
		return (T)Class.forName(className).newInstance();
	}
	
	protected boolean limit(Class<?> c, Method method) {
		return method.getDeclaringClass().equals(Object.class);
	}

	public void debug(String message) {
		context.log(message);
	}
	
	public void error(String message, Throwable e) {
		context.log(message, e);
	}

	public void destory() {
	}
}