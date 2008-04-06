package net.arnx.jsonic.web;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import net.arnx.jsonic.*;

import org.junit.*;

import static org.junit.Assert.*;
import static javax.servlet.http.HttpServletResponse.*;

public class WebServiceServletTest {
	
	@Test
	public void testRPC() throws Exception {
		URL url = new URL("http://localhost:8080/sample/rpc/rpc.json");
		HttpURLConnection con = null;
		
		// GET
		con = (HttpURLConnection)url.openConnection();
		con.setRequestMethod("GET");
		con.connect();
		assertEquals(SC_METHOD_NOT_ALLOWED, con.getResponseCode());
		con.disconnect();
		
		// POST
		con = (HttpURLConnection)url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.connect();
		assertEquals(SC_BAD_REQUEST, con.getResponseCode());
		assertEquals(JSON.decode("{\"result\":null,\"error\":{\"code\":-32600,\"message\":\"Invalid Request.\"},\"id\":null}"), 
				JSON.decode(read(con.getErrorStream())));
		con.disconnect();

		con = (HttpURLConnection)url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		write(con.getOutputStream(), "{\"method\":\"calc.plus\",\"params\":[1,2],\"id\":1}");
		con.connect();
		assertEquals(SC_OK, con.getResponseCode());
		assertEquals(JSON.decode("{\"result\":3,\"error\":null,\"id\":1}"), 
				JSON.decode(read(con.getInputStream())));	
		con.disconnect();
		
		// PUT
		con = (HttpURLConnection)url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("PUT");
		con.connect();
		assertEquals(SC_METHOD_NOT_ALLOWED, con.getResponseCode());
		con.disconnect();
		
		// DELETE
		con = (HttpURLConnection)url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("DELETE");
		con.connect();
		assertEquals(SC_METHOD_NOT_ALLOWED, con.getResponseCode());
		con.disconnect();
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
		client.setRequestContent("[\"title\", \"text\"]");
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
	
	private static void write(OutputStream out, String text) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
		writer.write(text);
		writer.flush();
		writer.close();
	}
	
	private static String read(InputStream in) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		StringBuilder sb = new StringBuilder();
		char[] cb = new char[1024];
		int length = 0; 
		while ((length = reader.read(cb)) != -1) {
			sb.append(cb, 0, length);
		}
		reader.close();
		return sb.toString();
	}
}
