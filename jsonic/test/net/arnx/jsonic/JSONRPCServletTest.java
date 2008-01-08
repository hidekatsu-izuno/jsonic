package net.arnx.jsonic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.*;

import static org.junit.Assert.*;
import static java.net.HttpURLConnection.*;

public class JSONRPCServletTest {
	
	@Test
	public void testRPC() throws Exception {
		URL url = new URL("http://localhost:8080/sample/rpc.json");
		
		HttpURLConnection con = null;
		
		// GET
		con = (HttpURLConnection)url.openConnection();
		con.setRequestMethod("GET");
		con.connect();
		assertEquals(HTTP_BAD_METHOD, con.getResponseCode());
		con.disconnect();
		
		// POST
		con = (HttpURLConnection)url.openConnection();
		con.setRequestMethod("POST");
		con.connect();
		assertEquals(HTTP_ACCEPTED, con.getResponseCode());
		assertEquals("{\"result\":null,\"error\":-32700,\"id\":null}", toString(con.getInputStream()));
		con.disconnect();
		
		// PUT
		con = (HttpURLConnection)url.openConnection();
		con.setRequestMethod("PUT");
		con.connect();
		assertEquals(HTTP_BAD_METHOD, con.getResponseCode());
		con.disconnect();
		
		// DELETE
		con = (HttpURLConnection)url.openConnection();
		con.setRequestMethod("DELETE");
		con.connect();
		assertEquals(HTTP_BAD_METHOD, con.getResponseCode());
		con.disconnect();
	}
	
	private String toString(InputStream in) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		
		StringBuffer sb = new StringBuffer();
		String line = null;
		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}
		reader.close();
		
		return sb.toString();
	}
}
