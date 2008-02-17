package net.arnx.jsonic.web;

import javax.servlet.ServletContext;

public interface Container {
	void init(ServletContext context);
	boolean isDebugMode();
	<T> T getComponent(Class<? extends T> c) throws Exception;
	void debug(String message);
	void error(String message, Throwable e);
	void destory();
}
