package net.arnx.jsonic.servlet.seasar;

import javax.servlet.ServletContext;

import net.arnx.jsonic.container.Container;

import org.seasar.framework.container.factory.SingletonS2ContainerFactory;
import org.seasar.framework.env.Env;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class S2Container implements Container {
	private Log logger = LogFactory.getLog(S2Container.class);
	
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

	public void log(String message) {
		logger.warn(message);
	}

	public void log(String message, Throwable e) {
		logger.error(message, e);
	}
	
	public void destory() {
    }
}
