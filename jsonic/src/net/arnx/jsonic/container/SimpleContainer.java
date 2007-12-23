package net.arnx.jsonic.container;

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

	public void log(String message) {
		context.log(message);
	}
	
	public void log(String message, Throwable e) {
		context.log(message, e);
	}

	public void destory() {
	}
}