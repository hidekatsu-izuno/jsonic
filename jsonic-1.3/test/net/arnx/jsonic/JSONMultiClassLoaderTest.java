package net.arnx.jsonic;

import java.security.Permission;

import org.apache.tools.ant.launch.Launcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JSONMultiClassLoaderTest {
    final SecurityManager sm = System.getSecurityManager();
    
    @Before
    public void setUp() throws Exception {
      System.setSecurityManager(new SecurityManager() {
    	  @Override
    	  public void checkPermission(Permission perm) {
    	  }
    	  
          public void checkExit(int status) {
              throw new SecurityException();
          }
      });
    }
 
    @After
    public void tearDown() throws Exception {
      System.setSecurityManager(sm);
    }
    
	@Test
	public void test() throws Exception {
		try {
			Launcher.main(new String[] {
				"-f", "test/build.xml"	
			});
		} catch (SecurityException e) {
			// no handle
		}
	}
}
