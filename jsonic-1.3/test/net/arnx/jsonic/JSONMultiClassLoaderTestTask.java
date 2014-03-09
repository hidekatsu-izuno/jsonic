package net.arnx.jsonic;

import org.apache.commons.beanutils.BasicDynaBean;
import org.apache.commons.beanutils.BasicDynaClass;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaClass;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class JSONMultiClassLoaderTestTask extends Task {
	@Override
	public void execute() throws BuildException {
		log("" + JSONMultiClassLoaderTestTask.class.getClassLoader());
		log("" + Thread.currentThread().getContextClassLoader());
		
		try {
			DynaClass dcls = new BasicDynaClass();
			DynaBean dbean = new BasicDynaBean(dcls);
			log(JSON.encode(dbean));
		} catch (Error t) {
			t.printStackTrace();
			throw t;
		}
	}
}
