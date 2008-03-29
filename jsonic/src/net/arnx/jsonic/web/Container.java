package net.arnx.jsonic.web;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import javax.servlet.ServletContext;

import net.arnx.jsonic.JSON;

public class Container {
	public Boolean debug = Boolean.FALSE;
	
	private ServletContext context;

	public void init(ServletContext context) {
		this.context = context;
	}

	public boolean isDebugMode() {
		return debug;
	}

	public <T> T getComponent(Class<? extends T> c) throws Exception {
		T o = c.newInstance();
		
		return o;
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