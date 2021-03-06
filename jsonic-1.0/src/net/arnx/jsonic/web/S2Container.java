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

import org.seasar.framework.container.factory.SingletonS2ContainerFactory;
import org.seasar.framework.env.Env;
import org.seasar.framework.log.Logger;
import org.seasar.framework.util.ClassUtil;

@SuppressWarnings("unchecked")
public class S2Container extends Container {
	private static Logger log = Logger.getLogger(S2Container.class);
	
	@Override
	public Object getComponent(String className) throws Exception {
		return SingletonS2ContainerFactory
			.getContainer()
			.getComponent(ClassUtil.forName(className));
	}
	
	@Override
	public boolean isDebugMode() {
		return (debug != null) ? debug : Env.UT.equals(Env.getValue());
	}

	@Override
	public void debug(String message) {
		log.debug(message);
	}

	@Override
	public void error(String message, Throwable e) {
		log.error(message, e);
	}
}
