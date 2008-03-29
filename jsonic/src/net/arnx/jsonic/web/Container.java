package net.arnx.jsonic.web;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.arnx.jsonic.JSON;

public class Container {
	public Boolean debug = Boolean.FALSE;
	
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
	
	public Method findMethod(Object target, String name, List<Object> args) throws NoSuchMethodException {
		Class<?> c = target.getClass();
		Method method = null;
		for (Method m : c.getMethods()) {
			if (!name.equals(m.getName()) || Modifier.isStatic(m.getModifiers())) {
				continue;
			}
			method = m;				
		}
		
		if (method == null || limit(c, method)) {
			StringBuilder sb = new StringBuilder(c.getName());
			sb.append('#').append(name).append('(');
			String json = JSON.encode(args);
			sb.append(json, 1, json.length()-1);
			sb.append(')');
			throw new NoSuchMethodException("method missing: " + sb.toString());
		}
		
		return method;
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