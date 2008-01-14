package net.arnx.jsonic.web;

import net.arnx.jsonic.*;

import org.junit.*;

import static org.junit.Assert.*;
import static javax.servlet.http.HttpServletResponse.*;

public class WebServiceServletTest {
	
	@Test
	public void testRPC() throws Exception {
		HttpClient client = new HttpClient("http://localhost:8080/sample/rpc/rpc.json");
		
		// GET
		client.setRequestMethod("GET");
		client.connect();
		assertEquals(SC_METHOD_NOT_ALLOWED, client.getResponseCode());
		client.clear();
		
		// POST
		client.setRequestMethod("POST");
		client.connect();
		assertEquals(SC_BAD_REQUEST, client.getResponseCode());
		assertEquals(JSON.decode("{\"result\":null,\"error\":{\"code\":-32700,\"message\":\"Invalid Request.\"},\"id\":null}"), 
				JSON.decode(client.getResponseContent()));
		client.clear();

		client.setRequestMethod("POST");
		client.setRequestContent("{\"method\":\"calc.plus\",\"params\":[1,2],\"id\":1}");
		client.connect();
		assertEquals(SC_OK, client.getResponseCode());
		assertEquals(JSON.decode("{\"result\":3,\"error\":null,\"id\":1}"), 
				JSON.decode(client.getResponseContent()));	
		client.clear();
		
		// PUT
		client.setRequestMethod("PUT");
		client.connect();
		assertEquals(SC_METHOD_NOT_ALLOWED, client.getResponseCode());
		client.clear();
		
		// DELETE
		client.setRequestMethod("DELETE");
		client.connect();
		assertEquals(SC_METHOD_NOT_ALLOWED, client.getResponseCode());
		client.clear();
	}
	
	@Test
	public void testREST() throws Exception {
		HttpClient client = new HttpClient("http://localhost:8080/sample/rest/memo.json");
		
		// GET
		client.setRequestMethod("GET");
		client.connect();
		assertEquals(SC_OK, client.getResponseCode());
		client.clear();
		
		// POST
		client.setRequestMethod("POST");
		client.connect();
		assertEquals(SC_OK, client.getResponseCode());
		client.clear();
		
		// PUT
		client.setRequestMethod("PUT");
		client.connect();
		assertEquals(SC_OK, client.getResponseCode());
		client.clear();
		
		// DELETE
		client.setRequestMethod("DELETE");
		client.connect();
		assertEquals(SC_OK, client.getResponseCode());
		client.clear();
	}
}
