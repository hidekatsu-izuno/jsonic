package net.arnx.jsonic.container;

import javax.servlet.ServletContext;

public interface Container {
	void init(ServletContext context);
	boolean isDebugMode();
	Object getComponent(Class c) throws Exception;
	void debug(String message);
	void error(String message, Throwable e);
	void destory();
}
