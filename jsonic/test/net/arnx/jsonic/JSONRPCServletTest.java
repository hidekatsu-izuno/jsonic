package net.arnx.jsonic;

import javax.servlet.http.HttpServletResponse;

import org.junit.*;
import static org.junit.Assert.*;
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
		
		MockHttpServletRequest request = context.createRequest("/sample/rpc.json");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponseImpl(request);
		servlet.service(request, response);
		
		servlet.destroy();
		
		assertEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED, response.getStatus());
	}
}
