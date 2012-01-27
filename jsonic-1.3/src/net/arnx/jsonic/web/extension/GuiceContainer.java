/*
 * Copyright 2007-2009 Hidekatsu Izuno
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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import net.arnx.jsonic.web.Container;

import com.google.inject.Injector;

public class GuiceContainer extends Container {
	Logger log;
	Injector injector;
	
	@Override
	public void init(HttpServlet servlet)  throws ServletException {
		super.init(servlet);
		this.log = Logger.getLogger(servlet.getClass().getName());
		this.injector = (Injector)context.getAttribute(Injector.class.getName());
	}
	
	@Override
	public Object getComponent(String className) throws Exception {
		return injector.getInstance(findClass(className));
	}
	
	@Override
	public boolean isDebugMode() {
		return (debug != null) ? debug : log.isLoggable(Level.FINE);
	}
	
	@Override
	public void debug(String message, Throwable e) {
		if (e != null) {
			log.log(Level.FINE, message, e);
		} else {
			log.log(Level.FINE, message);
		}
	}
	
	@Override
	public void warn(String message, Throwable e) {
		if (e != null) {
			log.log(Level.WARNING, message, e);
		} else {
			log.log(Level.WARNING, message);
		}
	}

	@Override
	public void error(String message, Throwable e) {
		if (e != null) {
			log.log(Level.SEVERE, message, e);
		} else {
			log.log(Level.SEVERE, message);
		}
	}
}
