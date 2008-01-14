package net.arnx.jsonic.servlet;

import javax.servlet.ServletContext;


import org.seasar.framework.container.factory.SingletonS2ContainerFactory;
import org.seasar.framework.env.Env;
import org.seasar.framework.log.Logger;

public class S2Container implements Container {
	private Logger logger = Logger.getLogger(S2Container.class);
	
	public Boolean debug;
	
	public void init(ServletContext context) {
	}

	public Object getComponent(Class c) throws Exception {
		return SingletonS2ContainerFactory
			.getContainer()
			.getComponent(c);
	}

	public boolean isDebugMode() {
		return (debug != null) ? debug : Env.UT.equals(Env.getValue());
	}

	public void debug(String message) {
		logger.debug(message);
	}

	public void error(String message, Throwable e) {
		logger.error(message, e);
	}
	
	public void destory() {
    }
}
