package net.arnx.jsonic;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class JSONMultiClassLoaderTestTask extends Task {
	@Override
	public void execute() throws BuildException {
		log("" + JSONMultiClassLoaderTestTask.class.getClassLoader());
		log("" + Thread.currentThread().getContextClassLoader());
		
		JSON.decode("[]");
	}
}
