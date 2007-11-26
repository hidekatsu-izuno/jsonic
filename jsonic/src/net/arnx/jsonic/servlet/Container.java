package net.arnx.jsonic.servlet;

import javax.servlet.ServletContext;

public interface Container {
	void init(ServletContext context);
	boolean isDebugMode();
	String getCharacterEncoding();
	Object getComponent(String path) throws Exception;
	void log(String message);
	void log(String message, Throwable e);
	void destory();
}
