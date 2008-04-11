package net.arnx.jsonic.web;

import java.lang.reflect.Method;

import javax.servlet.ServletContext;

public class Container {
	public Boolean debug = false;
	public String init = "init";
	public String destroy = "destroy";
	
	private ServletContext context;

	public void init(ServletContext context) {
		this.context = context;
	}

	public boolean isDebugMode() {
		return debug;
	}

	public <T> T getComponent(Class<? extends T> c) throws Exception {
		return c.newInstance();
	}
	
	protected boolean limit(Class<?> c, Method method) {
		return method.getDeclaringClass().equals(Object.class);
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