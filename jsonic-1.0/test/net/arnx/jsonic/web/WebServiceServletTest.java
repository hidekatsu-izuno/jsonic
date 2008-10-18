package net.arnx.jsonic.web;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import net.arnx.jsonic.*;

import org.junit.*;
import org.seasar.framework.mock.servlet.MockHttpServletRequest;
import org.seasar.framework.mock.servlet.MockHttpServletRequestImpl;
import org.seasar.framework.mock.servlet.MockServletContextImpl;

import static org.junit.Assert.*;
import static javax.servlet.http.HttpServletResponse.*;

import winstone.Launcher;

@SuppressWarnings("unchecked")
public class WebServiceServletTest {	
	
	private static Launcher winstone;
	
	@BeforeClass
	public static void init() throws Exception {
		Map args = new HashMap();
		args.put("webroot", "sample");
		args.put("controlPort", "8081");
		args.put("preferredClassLoader", "winstone.classLoader.WebappDevLoader");
		
		Launcher.initLogger(args);
		winstone = new Launcher(args);
	}
	
	@AfterClass
	public static void destroy() throws Exception {
		winstone.shutdown();
	}
	
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
		write(con, "");
		con.connect();
		assertEquals(SC_OK, con.getResponseCode());
		assertEquals(JSON.decode("{\"result\":null,\"error\":{\"code\":-32600,\"message\":\"Invalid Request.\",\"data\":{}},\"id\":null}"), 
				JSON.decode(read(con.getInputStream())));
		con.disconnect();

		con = (HttpURLConnection)url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		write(con, "{\"method\":\"calc.plus\",\"params\":[1,2],\"id\":1}");
		con.connect();
		assertEquals(SC_OK, con.getResponseCode());
		assertEquals(JSON.decode("{\"result\":3,\"error\":null,\"id\":1}"), 
				JSON.decode(read(con.getInputStream())));
		con.disconnect();
		
		// PUT
		con = (HttpURLConnection)url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("PUT");
		write(con, "");
		con.connect();
		assertEquals(SC_METHOD_NOT_ALLOWED, con.getResponseCode());
		con.disconnect();
		
		// DELETE
		con = (HttpURLConnection)url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("DELETE");
		con.setRequestProperty("Content-Length", "0");
		con.connect();
		assertEquals(SC_METHOD_NOT_ALLOWED, con.getResponseCode());
		con.disconnect();
	}
	
	@Test
	public void testREST() throws Exception {
		String url = "http://localhost:8080/sample/rest/memo";
		HttpURLConnection con = null;
		
		List<Map<String, Object>> content = null;
		
		// POST
		con = (HttpURLConnection)new URL(url + ".json").openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "{title:\"title\",text:\"text\"}");
		con.connect();
		assertEquals(SC_CREATED, con.getResponseCode());
		con.disconnect();
		
		// GET
		con = (HttpURLConnection)new URL(url + ".json").openConnection();
		con.setRequestMethod("GET");
		con.connect();
		assertEquals(SC_OK, con.getResponseCode());
		content = (List<Map<String, Object>>)JSON.decode(read(con.getInputStream()));
		con.disconnect();
		
		// PUT
		con = (HttpURLConnection)new URL(url + "/" + content.get(0).get("id") + ".json").openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("PUT");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "{title:\"title\",text:\"text\"}");
		con.connect();
		assertEquals(SC_NO_CONTENT, con.getResponseCode());
		con.disconnect();
		
		// DELETE
		con = (HttpURLConnection)new URL(url + "/" + content.get(0).get("id") + ".json").openConnection();
		con.setRequestMethod("DELETE");
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestProperty("Content-Length", "0");
		con.connect();
		assertEquals(SC_NO_CONTENT, con.getResponseCode());
		con.disconnect();
		
		// DELETE
		con = (HttpURLConnection)new URL(url + "/" + content.get(0).get("id") + ".json").openConnection();
		con.setRequestMethod("DELETE");
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestProperty("Content-Length", "0");
		con.connect();
		assertEquals(SC_NOT_FOUND, con.getResponseCode());
		con.disconnect();
		
		// POST
		con = (HttpURLConnection)new URL(url + ".json").openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "[\"title\", \"text\"]");
		con.connect();
		assertEquals(SC_BAD_REQUEST, con.getResponseCode());
		con.disconnect();
	}
	
	@Test
	public void testRESTWithMethod() throws Exception {
		String url = "http://localhost:8080/sample/rest/memo.json";
		HttpURLConnection con = null;
		
		List<Map<String, Object>> content = null;
		
		// POST
		con = (HttpURLConnection)new URL(url + "?_method=POST").openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "{title:\"title\",text:\"text\"}");
		con.connect();
		assertEquals(SC_CREATED, con.getResponseCode());
		con.disconnect();
		
		// GET
		con = (HttpURLConnection)new URL(url + "?_method=GET").openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Length", "0");
		con.connect();
		assertEquals(SC_OK, con.getResponseCode());
		content = (List<Map<String, Object>>)JSON.decode(read(con.getInputStream()));
		con.disconnect();
		
		// PUT
		con = (HttpURLConnection)new URL(url + "?_method=PUT").openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "{id:" + content.get(0).get("id") + ",title:\"title\",text:\"text\"}");
		con.connect();
		assertEquals(SC_NO_CONTENT, con.getResponseCode());
		con.disconnect();
		
		// DELETE
		con = (HttpURLConnection)new URL(url + "?_method=DELETE").openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "{id:" + content.get(0).get("id") + "}");
		con.connect();
		assertEquals(SC_NO_CONTENT, con.getResponseCode());
		con.disconnect();
		
		// POST
		con = (HttpURLConnection)new URL(url + "?_method=POST").openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "[\"title\", \"text\"]");
		con.connect();
		assertEquals(SC_BAD_REQUEST, con.getResponseCode());
		con.disconnect();
	}
	
	@Test
	public void testGetParameterMap() throws Exception {
		Method m = WebServiceServlet.class.getDeclaredMethod("getParameterMap", HttpServletRequest.class);
		m.setAccessible(true);
		
		final Hashtable<String, String[]> v = new Hashtable<String, String[]>();
		
		MockHttpServletRequest request = new MockHttpServletRequestImpl(new MockServletContextImpl("/"), "/") {
			@Override
			public Enumeration getParameterNames() {
				return v.keys();
			}
			
			@Override
			public String[] getParameterValues(String name) {
				return v.get(name);
			}
		};
		
		v.put("aaa", new String[] {""});
		assertEquals(JSON.decode("{aaa:''}"), m.invoke(null, request));
		
		v.put("aaa.bbb", new String[] {"aaa", "bbb"});
		assertEquals(JSON.decode("{aaa:{'':'', bbb:['aaa', 'bbb']}}"), m.invoke(null, request));
		
		v.clear();
		v.put("aaa.bbb.", new String[] {"aaa", "bbb"});
		assertEquals(JSON.decode("{aaa:{bbb:{'':['aaa', 'bbb']}}}"), m.invoke(null, request));
		
		v.put("..", new String[] {"aaa", "bbb"});
		assertEquals(JSON.decode("{'':{'':{'':['aaa', 'bbb']}}, aaa:{bbb:{'':['aaa', 'bbb']}}}"), m.invoke(null, request));
		
		v.clear();
		v.put("aaa[bbb]", new String[] {"aaa", "bbb"});
		assertEquals(JSON.decode("{aaa:{bbb:['aaa', 'bbb']}}"), m.invoke(null, request));

		v.clear();
		v.put("aaa[bbb].ccc", new String[] {"aaa", "bbb"});
		assertEquals(JSON.decode("{aaa:{bbb:{ccc:['aaa', 'bbb']}}}"), m.invoke(null, request));

		v.clear();
		v.put("[aaa].bbb", new String[] {"aaa", "bbb"});
		assertEquals(JSON.decode("{'':{aaa:{bbb:['aaa', 'bbb']}}}"), m.invoke(null, request));
	}
	
	private static void write(HttpURLConnection con, String text) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(con.getOutputStream(), "UTF-8"));
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
