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
		config.setServletContext(context);
		config.setInitParameter("config", ""
				+ "debug: true\n"
				+ "encoding: 'UTF-8'\n"
				+ "routes: {\n"
				+ "    '/(\\w+)': 'sample.\1'\n"
				+ "    '/': 'sample'\n"
				+ "}");
		
		servlet.init(config);
		
		MockHttpServletRequestImpl request = (MockHttpServletRequestImpl)context.createRequest("/sample/rpc.json");
		MockHttpServletResponseImpl response = new MockHttpServletResponseImpl(request);
		
		// Method Allow Error
		request.setMethod("GET");
		servlet.service(request, response);
		assertEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED, response.getStatus());

		request.setMethod("PUT");		
		servlet.service(request, response);
		assertEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED, response.getStatus());

		request.setMethod("DELETE");		
		servlet.service(request, response);
		assertEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED, response.getStatus());

		request.setMethod("POST");
		servlet.service(request, response);
		assertEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED, response.getStatus());	
		
		servlet.destroy();
	}
}
