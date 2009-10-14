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

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class SpringContainer extends Container {
	private static Log log = LogFactory.getLog(WebServiceServlet.class);
	
	private ApplicationContext appContext;
	
	@Override
	public void init(ServletConfig config) {
		super.init(config);
		appContext = WebApplicationContextUtils.getWebApplicationContext(config.getServletContext());
	}
	
	@Override
	public Object getComponent(String className, HttpServletRequest request, HttpServletResponse response) throws Exception {
		Object component = appContext.getBean(className);
		
		if (component instanceof ApplicationContextAware) {
			((ApplicationContextAware)component).setApplicationContext(appContext);
		}
		
		return component;
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
	public void error(String message, Throwable e) {
		if (e != null) {
			log.error(message, e);
		} else {
			log.error(message);
		}
	}
}
