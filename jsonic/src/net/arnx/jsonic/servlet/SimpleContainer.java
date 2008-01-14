package net.arnx.jsonic.servlet;

import javax.servlet.ServletContext;

public class SimpleContainer implements Container {
	public boolean debug;
	
	private ServletContext context;

	public void init(ServletContext context) {
		this.context = context;
	}

	public boolean isDebugMode() {
		return debug;
	}

	public Object getComponent(Class c) throws Exception {
		return c.newInstance();
	}

	public void debug(String message) {
		context.log(message);
	}
	
	public void error(String message, Throwable e) {
		context.log(message, e);
	}

	public void destory() {
	}
}