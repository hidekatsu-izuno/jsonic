package net.arnx.jsonic.servlet;

import java.util.Map;

import javax.servlet.ServletContext;

public class SimpleContainer implements Container {
	public boolean debug;
	public Map<String, Class<?>> mapping;
	public String encoding = "UTF-8";
	
	private ServletContext context;

	public void init(ServletContext context) {
		this.context = context;
	}

	public boolean isDebugMode() {
		return debug;
	}
	
	public String getCharacterEncoding() {
		return encoding;
	}

	public Object getComponent(String path) throws Exception {
		Class<?> target = mapping.get(path);
		if (target == null) {
			throw new IllegalArgumentException("target class is not found: " + path);
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