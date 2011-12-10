package net.arnx.jsonic.web;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class ExternalContext {
	private static final ThreadLocal<ExternalContext> THREAD_LOCAL = new InheritableThreadLocal<ExternalContext>();
	
	public static void init(ServletConfig config, ServletContext application, HttpServletRequest request, HttpServletResponse response) {
		synchronized (THREAD_LOCAL) {
			THREAD_LOCAL.set(new ExternalContext(config, application, request, response));
		}
	}
	
	public static ServletConfig getConfig() {
		return THREAD_LOCAL.get().config;
	}
	
	public static ServletContext getApplication() {
		return THREAD_LOCAL.get().application;
	}
	
	public static HttpServletRequest getRequest() {
		return THREAD_LOCAL.get().request;
	}
	
	public static HttpServletResponse getResponse() {
		return THREAD_LOCAL.get().response;
	}
	
	public static HttpSession getSession() {
		return THREAD_LOCAL.get().request.getSession();
	}
	
	public static void destory() {
		synchronized (THREAD_LOCAL) {
			THREAD_LOCAL.remove();
		}
	}
	
	private ServletConfig config;
	private ServletContext application;
	private HttpServletRequest request;
	private HttpServletResponse response;
	
	private ExternalContext(ServletConfig config, ServletContext application, HttpServletRequest request, HttpServletResponse response) {
		this.config = config;
		this.application = application;
		this.request = request;
		this.response = response;
	}
}
