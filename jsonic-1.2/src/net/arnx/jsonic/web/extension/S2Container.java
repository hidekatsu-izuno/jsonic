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
package net.arnx.jsonic.web.extension;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import net.arnx.jsonic.web.Container;

import org.seasar.framework.container.factory.SingletonS2ContainerFactory;
import org.seasar.framework.log.Logger;

public class S2Container extends Container {
	Logger log;
	
	@Override
	public void init(HttpServlet servlet) throws ServletException {
		super.init(servlet);
		this.log = Logger.getLogger(servlet.getClass());
	}
	
	@Override
	public Object getComponent(String className) throws Exception {
		return SingletonS2ContainerFactory
			.getContainer()
			.getComponent(findClass(className));
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
