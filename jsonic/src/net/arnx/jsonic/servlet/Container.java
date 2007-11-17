package net.arnx.jsonic.servlet;

public interface Container {
	void init();
	boolean isDebugMode();
	Object getComponent(String path) throws Exception;
	void log(String message, Throwable e);
	void destory();
}
