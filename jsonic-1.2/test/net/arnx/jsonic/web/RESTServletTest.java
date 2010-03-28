package net.arnx.jsonic.web;

import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.arnx.jsonic.JSON;
import net.arnx.jsonic.web.RESTServlet.RouteMapping;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.seasar.framework.mock.servlet.MockHttpServletRequest;
import org.seasar.framework.mock.servlet.MockServletContextImpl;

@SuppressWarnings("unchecked")
public class RESTServletTest {
	
	private static Server server;
	
	@BeforeClass
	public static void init() throws Exception {
		new File("sample/basic/WEB-INF/database.dat").delete();
		new File("sample/seasar2/WEB-INF/database.dat").delete();
		new File("sample/spring/WEB-INF/database.dat").delete();
		new File("sample/guice/WEB-INF/database.dat").delete();
	
		server = new Server(16001);

		ContextHandlerCollection contexts = new ContextHandlerCollection();
		
		String[] systemClasses = new String[] {
				"org.apache.commons.",
				"org.aopalliance.",
				"ognl.",
				"javassist.",
				"net.arnx.",	
				"org.seasar.",
				"org.springframework.",
				"com.google.inject."
		};
		
		String[] serverClasses = new String[] {
		};
		
		WebAppContext basic = new WebAppContext("sample/basic", "/basic");
		basic.setSystemClasses(concat(basic.getSystemClasses(), systemClasses));
		basic.setServerClasses(concat(basic.getServerClasses(), serverClasses));
		contexts.addHandler(basic);
		
		WebAppContext seasar2 = new WebAppContext("sample/seasar2", "/seasar2");
		seasar2.setSystemClasses(concat(seasar2.getSystemClasses(), systemClasses));
		seasar2.setServerClasses(concat(seasar2.getServerClasses(), serverClasses));
		contexts.addHandler(seasar2);
		
		WebAppContext spring = new WebAppContext("sample/spring", "/spring");
		spring.setSystemClasses(concat(spring.getSystemClasses(), systemClasses));
		spring.setServerClasses(concat(spring.getServerClasses(), serverClasses));
		contexts.addHandler(spring);
		
		WebAppContext guice = new WebAppContext("sample/guice", "/guice");
		guice.setSystemClasses(concat(guice.getSystemClasses(), systemClasses));
		guice.setServerClasses(concat(guice.getServerClasses(), serverClasses));
		contexts.addHandler(guice);
		
		server.setHandler(contexts);
		server.start();
	}
	
	private static String[] concat(String[] a, String[] b) {
		String[] result = new String[a.length + b.length];
		System.arraycopy(a, 0, result, 0, a.length);
		System.arraycopy(b, 0, result, a.length, b.length);
		return result;
	}
	
	@AfterClass
	public static void destroy() throws Exception {
		server.stop();
	}
	
	@Test
	public void testREST() throws Exception {
		testREST("basic");
	}
	
	@Test
	public void testRESTwithSeasar2() throws Exception {
		testREST("seasar2");
	}
	
	@Test
	public void testRESTwithSpring() throws Exception {
		testREST("spring");
	}
	
	@Test
	public void testRESTwithGuice() throws Exception {
		testREST("guice");
	}
	
	public void testREST(String app) throws Exception {
		System.out.println("\n<<START testRest: " + app + ">>");
		
		String url = "http://localhost:16001/" + app + "/rest/memo";
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
		
		// POST
		con = (HttpURLConnection)new URL(url + ".json").openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "[\"title\"]");
		con.connect();
		assertEquals(SC_BAD_REQUEST, con.getResponseCode());
		con.disconnect();
		
		// POST
		con = (HttpURLConnection)new URL(url + ".json").openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		write(con, "title=title&text=text");
		con.connect();
		assertEquals(SC_CREATED, con.getResponseCode());
		con.disconnect();
		
		// HEAD
		con = (HttpURLConnection)new URL(url + ".json").openConnection();
		con.setRequestMethod("HEAD");
		con.connect();
		assertEquals(SC_METHOD_NOT_ALLOWED, con.getResponseCode());
		con.disconnect();
		
		// OPTIONS
		con = (HttpURLConnection)new URL(url + ".json").openConnection();
		con.setRequestMethod("OPTIONS");
		con.connect();
		assertEquals(SC_METHOD_NOT_ALLOWED, con.getResponseCode());
		con.disconnect();
		
		System.out.println("<<END testRest: " + app + ">>\n");
	}
	
	@Test
	public void testRESTWithMethod() throws Exception {
		System.out.println("\n<<START testRESTWithMethod>>");
		
		String url = "http://localhost:16001/basic/rest/memo.json";
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
		
		// POST
		con = (HttpURLConnection)new URL(url + "?_method=POST").openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "[\"title\"]");
		con.connect();
		assertEquals(SC_BAD_REQUEST, con.getResponseCode());
		con.disconnect();
		
		System.out.println("\n<<END testRESTWithMethod>>");
	}
	
	@Test
	public void testGetParameterMap() throws Exception {		
		MockServletContextImpl context = new MockServletContextImpl("/");
		MockHttpServletRequest request = null;
		
		request = context.createRequest("/?aaa=");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("aaa", "");
		assertEquals(JSON.decode("{aaa:''}"), getParameterMap(request));
		
		request = context.createRequest("/?aaa=aaa=bbb");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("aaa", "aaa=bbb");
		assertEquals(JSON.decode("{aaa:'aaa=bbb'}"), getParameterMap(request));

		request = context.createRequest("/?&");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("", "");
		request.addParameter("", "");
		assertEquals(JSON.decode("{'':['','']}"), getParameterMap(request));

		request = context.createRequest("/?=&=");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("", "");
		request.addParameter("", "");
		assertEquals(JSON.decode("{'':['','']}"),getParameterMap(request));
		
		request = context.createRequest("/");
		request.setContentType("application/x-www-form-urlencoded");
		assertEquals(JSON.decode("{}"), getParameterMap(request));
		
		request = context.createRequest("/?aaa.bbb=aaa");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("aaa.bbb", "aaa");
		assertEquals(JSON.decode("{aaa:{bbb:'aaa'}}"), getParameterMap(request));
		
		request = context.createRequest("/?" + URLEncoder.encode("諸行 無常", "UTF-8") + "=" + URLEncoder.encode("古今=東西", "UTF-8"));
		request.setContentType("application/x-www-form-urlencoded");
		request.setCharacterEncoding("UTF-8");
		request.addParameter("諸行 無常", "古今=東西");
		assertEquals(JSON.decode("{'諸行 無常':'古今=東西'}"), getParameterMap(request));
		
		request = context.createRequest("/?" + URLEncoder.encode("諸行 無常", "MS932") + "=" + URLEncoder.encode("古今=東西", "MS932"));
		request.setContentType("application/x-www-form-urlencoded");
		request.setCharacterEncoding("MS932");
		request.addParameter("諸行 無常", "古今=東西");
		assertEquals(JSON.decode("{'諸行 無常':'古今=東西'}"), getParameterMap(request));
		
		request = context.createRequest("/?aaa.bbb=aaa&aaa.bbb=bbb&aaa=aaa");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("aaa.bbb", "aaa");
		request.addParameter("aaa.bbb", "bbb");
		request.addParameter("aaa", "aaa");
		assertEquals(JSON.decode("{aaa:{bbb:['aaa', 'bbb'], null: 'aaa'}}"), getParameterMap(request));
		
		request = context.createRequest("/?aaa.bbb=aaa&aaa.bbb=bbb&aaa=aaa&aaa=bbb");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("aaa.bbb", "aaa");
		request.addParameter("aaa.bbb", "bbb");
		request.addParameter("aaa", "aaa");
		request.addParameter("bbb", "bbb");
		assertEquals(JSON.decode("{aaa:{bbb:['aaa', 'bbb'], null:'aaa'}, 'bbb':'bbb'}"), getParameterMap(request));
		
		request = context.createRequest("/?aaa.bbb=aaa&aaa.bbb=bbb");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("aaa.bbb", "aaa");
		request.addParameter("aaa.bbb", "bbb");
		assertEquals(JSON.decode("{aaa:{bbb:['aaa', 'bbb']}}"), getParameterMap(request));
		
		request = context.createRequest("/?aaa.bbb.=aaa&aaa.bbb.=bbb");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("aaa.bbb.", "aaa");
		request.addParameter("aaa.bbb.", "bbb");
		assertEquals(JSON.decode("{aaa:{bbb:{'':['aaa', 'bbb']}}}"), getParameterMap(request));
		
		request = context.createRequest("/?..=aaa&..=bbb");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("..", "aaa");
		request.addParameter("..", "bbb");
		assertEquals(JSON.decode("{'':{'':{'':['aaa', 'bbb']}}}"), getParameterMap(request));
		
		request = context.createRequest("/?aaa[bbb]=aaa&aaa[bbb]=bbb");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("aaa[bbb]", "aaa");
		request.addParameter("aaa[bbb]", "bbb");
		assertEquals(JSON.decode("{aaa:{bbb:['aaa', 'bbb']}}"), getParameterMap(request));
		
		request = context.createRequest("/?aaa[bbb]=aaa&aaa[bbb]=bbb");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("aaa[bbb][]", "aaa");
		assertEquals(JSON.decode("{aaa:{bbb:['aaa']}}"), getParameterMap(request));

		request = context.createRequest("/?aaa[bbb].ccc=aaa&aaa[bbb].ccc=bbb");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("aaa[bbb].ccc", "aaa");
		request.addParameter("aaa[bbb].ccc", "bbb");
		assertEquals(JSON.decode("{aaa:{bbb:{ccc:['aaa', 'bbb']}}}"), getParameterMap(request));

		request = context.createRequest("/?[aaa].bbb=aaa&[aaa].bbb=bbb");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("[aaa].bbb", "aaa");
		request.addParameter("[aaa].bbb", "bbb");
		assertEquals(JSON.decode("{'':{aaa:{bbb:['aaa', 'bbb']}}}"), getParameterMap(request));

		request = context.createRequest("/?.aaa.bbb=aaa&[aaa].bbb=bbb&[aaa].bbb=ccc");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter(".aaa.bbb", "aaa");
		request.addParameter("[aaa].bbb", "bbb");
		request.addParameter("[aaa].bbb", "ccc");
		assertEquals(JSON.decode("{'':{aaa:{bbb:['aaa', 'bbb', 'ccc']}}}"), getParameterMap(request));

		request = context.createRequest("/?.aaa.bbb=aaa&.aaa.bbb=bbb&[aaa].bbb=ccc");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter(".aaa.bbb", "aaa");
		request.addParameter(".aaa.bbb", "bbb");
		request.addParameter("[aaa].bbb", "ccc");
		assertEquals(JSON.decode("{'':{aaa:{bbb:['aaa', 'bbb', 'ccc']}}}"), getParameterMap(request));
	}
	
	private static Map getParameterMap(MockHttpServletRequest request) throws IOException {
		if (request.getCharacterEncoding() == null) request.setCharacterEncoding("UTF-8");
		Map map = new LinkedHashMap<Object, Object>();
		RouteMapping.parseParameter((Map)request.getParameterMap(), map);
		return map;
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
