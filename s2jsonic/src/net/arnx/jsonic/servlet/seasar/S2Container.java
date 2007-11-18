package net.arnx.jsonic.servlet.seasar;

import net.arnx.jsonic.servlet.Container;

import org.seasar.framework.container.factory.SingletonS2ContainerFactory;
import org.seasar.framework.env.Env;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.seasar.framework.util.StringUtil;
import org.seasar.framework.convention.NamingConvention;

public class S2Container implements Container {
	private org.seasar.framework.container.S2Container container;
	private Log logger = LogFactory.getLog(S2Container.class);
	
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
		return Env.UT.equals(Env.getValue());
	}

	@Override
	public void log(String message, Throwable e) {
		logger.error(message, e);
	}
	
	@Override
	public void destory() {
	}
	
    protected String fromPathToConmponentName(String path) {
    	NamingConvention nc = (NamingConvention)container.getComponent(NamingConvention.class);
    	String nameSuffix = nc.getServiceSuffix();
        if (!path.startsWith(nc.getViewRootPath()) || !path.endsWith(nc.getViewExtension())) {
            throw new IllegalArgumentException(path);
        }
        String componentName = (path.substring(
                nc.adjustViewRootPath().length() + 1, path.length()
                        - nc.getViewExtension().length()) + nameSuffix).replace('/',
                '_');
        int pos = componentName.lastIndexOf('_');
        if (pos == -1) {
            return StringUtil.decapitalize(componentName);
        }
        return componentName.substring(0, pos + 1)
        	+ StringUtil.decapitalize(componentName.substring(pos + 1));
    }
}
