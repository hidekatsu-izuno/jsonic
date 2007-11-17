package net.arnx.jsonic.servlet;

import org.seasar.framework.container.factory.SingletonS2ContainerFactory;
import org.seasar.framework.env.Env;
import org.seasar.framework.log.Logger;
import org.seasar.framework.util.StringUtil;
import org.seasar.framework.convention.NamingConvention;

public class S2Container implements Container {
	private org.seasar.framework.container.S2Container container;
	private Logger logger = Logger.getLogger(S2Container.class);
	private NamingConvention nc;
	
	@Override
	public void init() {
		container = SingletonS2ContainerFactory.getContainer();
		nc = (NamingConvention)container.getComponent(NamingConvention.class);
	}

	@Override
	public Object getComponent(String path) throws Exception {
		return container.getComponent(fromPathToConmponentName(path, nc.getServiceSuffix()));
	}

	@Override
	public boolean isDebugMode() {
		return Env.UT.equals(Env.getValue());
	}

	@Override
	public void log(String message, Throwable e) {
		logger.log(message, null, e);
	}
	
	@Override
	public void destory() {
	}
	
    protected String fromPathToConmponentName(String path, String nameSuffix) {
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
