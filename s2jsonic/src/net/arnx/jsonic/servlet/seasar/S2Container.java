package net.arnx.jsonic.servlet.seasar;

import net.arnx.jsonic.servlet.Container;

import org.seasar.framework.container.factory.SingletonS2ContainerFactory;
import org.seasar.framework.env.Env;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.seasar.framework.util.StringUtil;

public class S2Container implements Container {
	private Log logger = LogFactory.getLog(S2Container.class);
	
	public Boolean debug;
	public String encoding = "UTF-8";
	public ServiceConfig service;
	
	public void init() {
		if (service == null) service = new ServiceConfig();
		if (service.rootPath == null) service.rootPath = "";
		if (service.suffix == null) service.suffix = "Service";
		if (service.extension == null) service.extension = ".json";
	}
	
	public String getCharacterEncoding() {
		return encoding;
	}

	public Object getComponent(String path) throws Exception {
		return SingletonS2ContainerFactory
			.getContainer()
			.getComponent(fromPathToConmponentName(path));
	}

	public boolean isDebugMode() {
		return (debug != null) ? debug : Env.UT.equals(Env.getValue());
	}

	public void log(String message) {
		logger.warn(message);
	}

	public void log(String message, Throwable e) {
		logger.error(message, e);
	}
	
	public void destory() {
	}
	
    protected String fromPathToConmponentName(String path) {
        if (!path.startsWith(service.rootPath) || !path.endsWith(service.extension)) {
            throw new IllegalArgumentException(path);
        }
        
        String componentName = path.substring(
        		("/".equals(service.rootPath) ? "" : service.rootPath).length() + 1,
                path.length() - service.extension.length()
            ).replace('/','_') + service.suffix;
        
        int pos = componentName.lastIndexOf('_');
        
        if (pos == -1) {
            return StringUtil.decapitalize(componentName);
        }
        return componentName.substring(0, pos + 1)
        	+ StringUtil.decapitalize(componentName.substring(pos + 1));
    }
    
    class ServiceConfig {
    	public String rootPath;
    	public String suffix;
    	public String extension;
    }
}
