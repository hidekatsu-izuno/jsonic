/*
 * Copyright 2007-2011 Hidekatsu Izuno
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
package net.arnx.jsonic.web.extension;

import java.lang.reflect.Method;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.arnx.jsonic.web.Container;
import net.arnx.jsonic.web.ExternalContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class SpringContainer extends Container {
	Log log;
	ApplicationContext appContext;
	
	@Override
	public void init(HttpServlet servlet) throws ServletException {
		super.init(servlet);
		this.log = LogFactory.getLog(servlet.getClass());		
		appContext = WebApplicationContextUtils.getWebApplicationContext(context);
	}
	
	@Override
	public Object getComponent(String className) throws Exception {
		Object component;
		try {
			component = appContext.getBean(className);
		} catch (Exception e) {
			throw new ClassNotFoundException("class not found: " + className, e);
		}
		
		if (component instanceof ApplicationContextAware) {
			((ApplicationContextAware)component).setApplicationContext(appContext);
		}
		
		for (Method method : component.getClass().getMethods()) {
			Class<?>[] params = method.getParameterTypes();
			if (void.class.equals(method.getReturnType())
					&& method.getName().startsWith("set")
					&& params.length == 1) {
				Class<?> c = params[0];
				if (HttpServletRequest.class.equals(c)) {
					method.invoke(component, ExternalContext.getRequest());
				} else if (HttpServletResponse.class.equals(c)) {
					method.invoke(component, ExternalContext.getResponse());
				}
			}
		}
		
		return component;
	}
	
	@Override
	public boolean isDebugMode() {
		return (debug != null) ? debug : log.isDebugEnabled();
	}

	@Override
	public void debug(String message, Throwable e) {
		if (e != null) {
			log.debug(message, e);
		} else {
			log.debug(message);
		}
	}
	
	@Override
	public void warn(String message, Throwable e) {
		if (e != null) {
			log.warn(message, e);
		} else {
			log.warn(message);
		}
	}

	@Override
	public void error(String message, Throwable e) {
		if (e != null) {
			log.error(message, e);
		} else {
			log.error(message);
		}
	}
}
