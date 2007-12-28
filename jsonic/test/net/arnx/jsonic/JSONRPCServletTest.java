package net.arnx.jsonic;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.junit.*;
import static org.junit.Assert.*;

import com.meterware.httpunit.*;
import com.meterware.servletunit.*;

public class JSONRPCServletTest {

	ServletRunner runner;
	
	@Before
	public void setup() throws Exception {
		runner = new ServletRunner(getClass().getResourceAsStream("web.xml"));
	}
	
	@Test
	public void testRPC() throws Exception {
		ServletUnitClient client = runner.newClient();

		try {
			client.getResponse(createRequest("GET", "http://localhost:8080/sample/rpc.json"));
			fail();
		} catch (HttpException e) {
			assertEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED, e.getResponseCode());			
		}

		try {
			client.getResponse(createRequestWithBody("PUT", "http://localhost:8080/sample/rpc.json",
					"", "application/json"));
			fail();
		} catch (HttpException e) {
			assertEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED, e.getResponseCode());			
		}

		try {
			client.getResponse(createRequestWithBody("DELETE", "http://localhost:8080/sample/rpc.json",
					"", "application/json"));
			fail();
		} catch (HttpException e) {
			assertEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED, e.getResponseCode());			
		}
	}
	
	private WebRequest createRequest(final String method, String url) {
		WebRequest request = new HeaderOnlyWebRequest(url) {
			public String getMethod() { return method; }
		};
		System.out.println(request);
		return request;
	}
	
	private WebRequest createRequestWithBody(final String method, String url) {
		WebRequest request = new PostMethodWebRequest(url) {
			public String getMethod() { return method; }
		};
		System.out.println(request);
		return request;
	}
	
	private WebRequest createRequestWithBody(final String method, String url, String body, String contentType) throws Exception {
		WebRequest request = new PostMethodWebRequest(url, new ByteArrayInputStream(body.getBytes("UTF-8")), contentType) {
			public String getMethod() { return method; }
		};
		System.out.println(request);
		return request;
	}
}
