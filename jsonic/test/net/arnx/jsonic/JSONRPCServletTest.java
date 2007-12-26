package net.arnx.jsonic;

import org.junit.Before;
import org.junit.Test;
import org.seasar.framework.mock.servlet.*;

public class JSONRPCServletTest {
	
	MockServletContextImpl context;
	
	@Before
	public void setup() {
		context = new MockServletContextImpl("/sample");
	}
	
	@Test
	public void testRPC() throws Exception {		
		JSONRPCServlet servlet = new JSONRPCServlet();
		
		MockServletConfigImpl config = new MockServletConfigImpl();
		servlet.init(config);
		
		MockHttpServletRequest request = context.createRequest("");
		MockHttpServletResponse response = new MockHttpServletResponseImpl(request);
		servlet.service(request, response);
		
		servlet.destroy();
	}
}
