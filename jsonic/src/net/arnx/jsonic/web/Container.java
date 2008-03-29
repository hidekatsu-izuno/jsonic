package net.arnx.jsonic.web;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class Container {
	public Boolean debug = Boolean.FALSE;
	public String init = "init";
	
	private ServletContext context;
	private HttpServletRequest request;
	private HttpServletResponse response;

	public void init(ServletContext context) {
		this.context = context;
	}

	public boolean isDebugMode() {
		return debug;
	}
	
	public void start(HttpServletRequest request, HttpServletResponse response) {
		this.request = request;
		this.response = response;
	}

	public <T> T getComponent(Class<? extends T> c) throws Exception {
		T o = c.newInstance();
		for (Field f : c.getFields()) {
			if (f.getName().equals("application") && f.getType().equals(ServletContext.class)) {
				f.set(o, context);
			} else if (f.getName().equals("request") && f.getType().equals(HttpServletRequest.class)) {
				f.set(o, request);
			} else if (f.getName().equals("response") && f.getType().equals(HttpServletResponse.class)) {
				f.set(o, response);
			} else if (f.getName().equals("session") && f.getType().equals(HttpSession.class)) {
				f.set(o, request.getSession(true));
			}
		}
		return o;
	}
	
	public void end() {
		request = null;
		response = null;
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