package net.arnx.jsonic.web;

import org.seasar.framework.container.factory.SingletonS2ContainerFactory;
import org.seasar.framework.env.Env;
import org.seasar.framework.log.Logger;

@SuppressWarnings("unchecked")
public class S2Container extends Container {
	private Logger logger = Logger.getLogger(S2Container.class);
	
	@Override
	public <T> T getComponent(Class<? extends T> c) throws Exception {
		return (T)SingletonS2ContainerFactory
			.getContainer()
			.getComponent(c);
	}
	
	@Override
	public boolean isDebugMode() {
		return (debug != null) ? debug : Env.UT.equals(Env.getValue());
	}

	@Override
	public void debug(String message) {
		logger.debug(message);
	}

	@Override
	public void error(String message, Throwable e) {
		logger.error(message, e);
	}
}
