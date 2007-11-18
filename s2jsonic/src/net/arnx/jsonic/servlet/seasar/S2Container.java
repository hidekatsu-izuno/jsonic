package net.arnx.jsonic.servlet.seasar;

import net.arnx.jsonic.servlet.Container;

import org.seasar.framework.container.factory.SingletonS2ContainerFactory;
import org.seasar.framework.env.Env;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.seasar.framework.util.StringUtil;

public class S2Container implements Container {
	private org.seasar.framework.container.S2Container container;
	private Log logger = LogFactory.getLog(S2Container.class);
	
	public Boolean debug;
	public ServiceConfig service;
	
	@Override
	public void init() {
		container = SingletonS2ContainerFactory.getContainer();
	}

	@Override
	public Object getComponent(String path) throws Exception {
		return container.getComponent(fromPathToConmponentName(path));
	}

	@Override
	public boolean isDebugMode() {
		return (debug != null) ? debug : Env.UT.equals(Env.getValue());
	}

	@Override
	public void log(String message, Throwable e) {
		logger.error(message, e);
	}
	
	@Override
	public void destory() {
	}
	
    protected String fromPathToConmponentName(String path) {
        if (!path.startsWith(service.rootPath) || !path.endsWith(service.extension)) {
            throw new IllegalArgumentException(path);
        }
        String componentName = (path.substring(
                ("/".equals(service.rootPath) ? "" : service.rootPath).length() + 1,
                path.length() - service.extension.length())
                + service.suffix).replace('/','_');
        int pos = componentName.lastIndexOf('_');
        if (pos == -1) {
            return StringUtil.decapitalize(componentName);
        }
        return componentName.substring(0, pos + 1)
        	+ StringUtil.decapitalize(componentName.substring(pos + 1));
    }
    
    class ServiceConfig {
    	public String rootPath = "";
    	public String suffix = "Service";
    	public String extension = ".json";
    }
}
