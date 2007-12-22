package net.arnx.jsonic.container;

import java.util.Map;

import javax.servlet.ServletContext;

public class SimpleContainer implements Container {
	public boolean debug;
	public Map<String, Class<?>> mapping;
	
	private ServletContext context;

	public void init(ServletContext context) {
		this.context = context;
	}

	public boolean isDebugMode() {
		return debug;
	}

	public Object getComponent(String path) throws Exception {
		Class<?> target = mapping.get(path);
		if (target == null) {
			return null;
		}
		return target.newInstance();
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