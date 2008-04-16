package net.arnx.jsonic.web;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.support.WebApplicationContextUtils;

@SuppressWarnings("unchecked")
public class SpringContainer extends Container {
	private static Log log = LogFactory.getLog(SpringContainer.class);
	
	private ApplicationContext appContext;
	
	public void init(ServletContext context) {
		appContext = WebApplicationContextUtils.getWebApplicationContext(context);
	}
	
	@Override
	public <T> T getComponent(Class<? extends T> c) throws Exception {
		Object component = appContext.getBean(c.getName());
		
		if (component instanceof ApplicationContextAware) {
			((ApplicationContextAware)component).setApplicationContext(appContext);
		}
		
		return (T)component;
	}
	
	@Override
	public boolean isDebugMode() {
		return (debug != null) ? debug : false;
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
