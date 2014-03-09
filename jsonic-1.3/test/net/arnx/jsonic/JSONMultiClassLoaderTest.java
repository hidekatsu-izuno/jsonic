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
      SecurityManager securityManager = new SecurityManager() {
        public void checkPermission(Permission permission) {
          if ("exitVM".equals(permission.getName())) {
            System.out.println("System.exit[exitVM]が呼ばれた");
          }
        }
 
        public void checkExit(int status) {
          throw new ExitException(status);
        }
      };
      System.setSecurityManager(securityManager);
    }
 
    @After
    public void tearDown() throws Exception {
      System.setSecurityManager(sm);
    }
 
    protected class ExitException extends SecurityException {
      public int state = 0;
 
      public ExitException(int state) {
        this.state = state;
      }
    }
	
	@Test
	public void test() throws Exception {
		try {
			Launcher.main(new String[] {
				"-f", "test/build.xml"	
			});
		} catch (ExitException e) {
			// no handle
		}
	}
}
