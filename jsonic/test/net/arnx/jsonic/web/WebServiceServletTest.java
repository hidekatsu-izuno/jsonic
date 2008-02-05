package net.arnx.jsonic.web;

import java.util.List;
import java.util.Map;

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
		List<Map<String, Object>> content = null;
		
		// POST
		client.setRequestMethod("POST");
		client.setRequestHeader("Content-type", "application/json");
		client.setRequestContent("{title:\"title\",text:\"text\"}");
		client.connect();
		assertEquals(SC_CREATED, client.getResponseCode());
		client.clear();
		
		// GET
		client.setRequestMethod("GET");
		client.connect();
		assertEquals(SC_OK, client.getResponseCode());
		content = (List<Map<String, Object>>)JSON.decode(client.getResponseContent());
		client.clear();
		
		// PUT
		client.setRequestMethod("PUT");
		client.setRequestHeader("Content-type", "application/json");
		client.setRequestContent("{id:" + content.get(0).get("id") + ",title:\"title\",text:\"text\"}");
		client.connect();
		assertEquals(SC_NO_CONTENT, client.getResponseCode());
		client.clear();
		
		// DELETE
		client.setRequestMethod("DELETE");
		client.setRequestHeader("Content-type", "application/json");
		client.setRequestContent("{id:" + content.get(0).get("id") + "}");
		client.connect();
		assertEquals(SC_NO_CONTENT, client.getResponseCode());
		client.clear();
		
		// POST
		client.setRequestMethod("POST");
		client.setRequestHeader("Content-type", "application/json");
		client.setRequestContent("[title:\"title\",text:\"text\"]");
		client.connect();
		assertEquals(SC_BAD_REQUEST, client.getResponseCode());
		client.clear();
	}
	
	@Test
	public void testRESTWithMethod() throws Exception {
		HttpClient client = new HttpClient();
		List<Map<String, Object>> content = null;
		
		// POST
		client.setURL("http://localhost:8080/sample/rest/memo.json?_method=POST");
		client.setRequestMethod("POST");
		client.setRequestHeader("Content-type", "application/json");
		client.setRequestContent("{title:\"title\",text:\"text\"}");
		client.connect();
		assertEquals(SC_CREATED, client.getResponseCode());
		client.clear();
		
		// GET
		client.setURL("http://localhost:8080/sample/rest/memo.json?_method=GET");
		client.setRequestMethod("POST");
		client.setRequestHeader("Content-type", "application/json");
		client.connect();
		assertEquals(SC_OK, client.getResponseCode());
		content = (List<Map<String, Object>>)JSON.decode(client.getResponseContent());
		client.clear();
		
		// PUT
		client.setURL("http://localhost:8080/sample/rest/memo.json?_method=PUT");
		client.setRequestMethod("POST");
		client.setRequestHeader("Content-type", "application/json");
		client.setRequestContent("{id:" + content.get(0).get("id") + ",title:\"title\",text:\"text\"}");
		client.connect();
		assertEquals(SC_NO_CONTENT, client.getResponseCode());
		client.clear();
		
		// DELETE
		client.setURL("http://localhost:8080/sample/rest/memo.json?_method=DELETE");
		client.setRequestMethod("POST");
		client.setRequestHeader("Content-type", "application/json");
		client.setRequestContent("{id:" + content.get(0).get("id") + "}");
		client.connect();
		assertEquals(SC_NO_CONTENT, client.getResponseCode());
		client.clear();
	}
}
