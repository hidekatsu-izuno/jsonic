package net.arnx.jsonic.web;

import javax.servlet.ServletContext;


import org.seasar.framework.container.factory.SingletonS2ContainerFactory;
import org.seasar.framework.env.Env;
import org.seasar.framework.log.Logger;

public class S2Container implements Container {
	private Logger logger = Logger.getLogger(S2Container.class);
	
	public Boolean debug;
	
	public void init(ServletContext context) {
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getComponent(Class<? extends T> c) throws Exception {
		return (T)SingletonS2ContainerFactory
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
