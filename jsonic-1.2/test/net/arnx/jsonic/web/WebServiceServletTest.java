package net.arnx.jsonic.web;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.arnx.jsonic.*;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.*;
import org.seasar.framework.mock.servlet.MockHttpServletRequest;
import org.seasar.framework.mock.servlet.MockServletContextImpl;

import static org.junit.Assert.*;
import static javax.servlet.http.HttpServletResponse.*;

@SuppressWarnings("unchecked")
public class WebServiceServletTest {	
	
	private static Server server;
	
	@BeforeClass
	public static void init() throws Exception {
		new File("sample-1.1/basic/WEB-INF/database.dat").delete();
		new File("sample-1.1/seasar2/WEB-INF/database.dat").delete();
		new File("sample-1.1/spring/WEB-INF/database.dat").delete();
		new File("sample-1.1/guice/WEB-INF/database.dat").delete();
	
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
		
		WebAppContext basic = new WebAppContext("sample-1.1/basic", "/basic");
		basic.setSystemClasses(concat(basic.getSystemClasses(), systemClasses));
		basic.setServerClasses(concat(basic.getServerClasses(), serverClasses));
		contexts.addHandler(basic);
		
		WebAppContext seasar2 = new WebAppContext("sample-1.1/seasar2", "/seasar2");
		seasar2.setSystemClasses(concat(seasar2.getSystemClasses(), systemClasses));
		seasar2.setServerClasses(concat(seasar2.getServerClasses(), serverClasses));
		contexts.addHandler(seasar2);
		
		WebAppContext spring = new WebAppContext("sample-1.1/spring", "/spring");
		spring.setSystemClasses(concat(spring.getSystemClasses(), systemClasses));
		spring.setServerClasses(concat(spring.getServerClasses(), serverClasses));
		contexts.addHandler(spring);
		
		WebAppContext guice = new WebAppContext("sample-1.1/guice", "/guice");
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
	public void testRPC() throws Exception {
		testRPC("basic");
	}
	
	@Test
	public void testRPCwithSeasar2() throws Exception {
		testRPC("seasar2");
	}
	
	@Test
	public void testRPCwithSpring() throws Exception {
		testRPC("spring");
	}
	
	@Test
	public void testRPCwithGuice() throws Exception {
		testRPC("guice");
	}
	
	public void testRPC(String app) throws Exception {
		System.out.println("\n<<START testRPC: " + app + ">>");
		
		URL url = new URL("http://localhost:16001/" + app + "/rpc/rpc.json");
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
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "");
		con.connect();
		assertEquals(SC_OK, con.getResponseCode());
		assertEquals(JSON.decode("{\"result\":null,\"error\":{\"code\":-32600,\"message\":\"Invalid Request.\",\"data\":{}},\"id\":null}"), 
				JSON.decode(read(con.getInputStream())));
		con.disconnect();

		con = (HttpURLConnection)url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
		write(con, "{\"method\":\"calc.plus\",\"params\":[1,2],\"id\":1}");
		con.connect();
		assertEquals(SC_OK, con.getResponseCode());
		assertEquals(JSON.decode("{\"result\":3,\"error\":null,\"id\":1}"), 
				JSON.decode(read(con.getInputStream())));
		con.disconnect();

		con = (HttpURLConnection)url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "{\"method\":\"calc.init\",\"params\":[],\"id\":1}");
		con.connect();
		assertEquals(SC_OK, con.getResponseCode());
		assertEquals(JSON.decode("{\"result\":null,\"error\":{\"code\":-32601,\"message\":\"Method not found.\",\"data\":{}},\"id\":1}"), 
				JSON.decode(read(con.getInputStream())));
		con.disconnect();
		con = (HttpURLConnection)url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "{\"method\":\"calc.destroy\",\"params\":[],\"id\":1}");
		con.connect();
		assertEquals(SC_OK, con.getResponseCode());
		assertEquals(JSON.decode("{\"result\":null,\"error\":{\"code\":-32601,\"message\":\"Method not found.\",\"data\":{}},\"id\":1}"), 
				JSON.decode(read(con.getInputStream())));
		con.disconnect();
		
		// PUT
		con = (HttpURLConnection)url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("PUT");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "");
		con.connect();
		assertEquals(SC_METHOD_NOT_ALLOWED, con.getResponseCode());
		con.disconnect();
		
		// DELETE
		con = (HttpURLConnection)url.openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("DELETE");
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestProperty("Content-Length", "0");
		con.connect();
		assertEquals(SC_METHOD_NOT_ALLOWED, con.getResponseCode());
		con.disconnect();
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
		con.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
		write(con, "[\"title\", \"text\"]");
		con.connect();
		assertEquals(SC_NOT_FOUND, con.getResponseCode());
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
	}
	
	@Test
	public void testRESTWithMethod() throws Exception {
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
		assertEquals(SC_NOT_FOUND, con.getResponseCode());
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
	}
	
	@Test
	public void testGetParameterMap() throws Exception {		
		MockServletContextImpl context = new MockServletContextImpl("/");
		MockHttpServletRequest request = null;
		
		request = context.createRequest("/?aaa=");
		request.addParameter("aaa", "");
		assertEquals(JSON.decode("{aaa:''}"), getParameterMap(request));
		
		request = context.createRequest("/?aaa=aaa=bbb");
		request.addParameter("aaa", "aaa=bbb");
		assertEquals(JSON.decode("{aaa:'aaa=bbb'}"), getParameterMap(request));

		request = context.createRequest("/?&");
		request.addParameter("", "");
		request.addParameter("", "");
		assertEquals(JSON.decode("{'':['','']}"), getParameterMap(request));

		request = context.createRequest("/?=&=");
		request.addParameter("", "");
		request.addParameter("", "");
		assertEquals(JSON.decode("{'':['','']}"),getParameterMap(request));
		
		request = context.createRequest("/");
		assertEquals(JSON.decode("{}"), getParameterMap(request));
		
		request = context.createRequest("/?aaa.bbb=aaa");
		request.addParameter("aaa.bbb", "aaa");
		assertEquals(JSON.decode("{aaa:{bbb:'aaa'}}"), getParameterMap(request));
		
		request = context.createRequest("/?" + URLEncoder.encode("諸行 無常", "UTF-8") + "=" + URLEncoder.encode("古今=東西", "UTF-8"));
		request.setCharacterEncoding("UTF-8");
		request.addParameter("諸行 無常", "古今=東西");
		assertEquals(JSON.decode("{'諸行 無常':'古今=東西'}"), getParameterMap(request));
		
		request = context.createRequest("/?" + URLEncoder.encode("諸行 無常", "MS932") + "=" + URLEncoder.encode("古今=東西", "MS932"));
		request.setCharacterEncoding("MS932");
		request.addParameter("諸行 無常", "古今=東西");
		assertEquals(JSON.decode("{'諸行 無常':'古今=東西'}"), getParameterMap(request));
		
		request = context.createRequest("/?aaa.bbb=aaa&aaa.bbb=bbb&aaa=aaa");
		request.addParameter("aaa.bbb", "aaa");
		request.addParameter("aaa.bbb", "bbb");
		request.addParameter("aaa", "aaa");
		assertEquals(JSON.decode("{aaa:{bbb:['aaa', 'bbb'], null: 'aaa'}}"), getParameterMap(request));
		
		request = context.createRequest("/?aaa.bbb=aaa&aaa.bbb=bbb&aaa=aaa&aaa=bbb");
		request.addParameter("aaa.bbb", "aaa");
		request.addParameter("aaa.bbb", "bbb");
		request.addParameter("aaa", "aaa");
		request.addParameter("bbb", "bbb");
		assertEquals(JSON.decode("{aaa:{bbb:['aaa', 'bbb'], null:['aaa', 'bbb']}}"), getParameterMap(request));
		
		request = context.createRequest("/?aaa.bbb=aaa&aaa.bbb=bbb");
		request.addParameter("aaa.bbb", "aaa");
		request.addParameter("aaa.bbb", "bbb");
		assertEquals(JSON.decode("{aaa:{bbb:['aaa', 'bbb']}}"), getParameterMap(request));
		
		request = context.createRequest("/?aaa.bbb.=aaa&aaa.bbb.=bbb");
		request.addParameter("aaa.bbb.", "aaa");
		request.addParameter("aaa.bbb.", "bbb");
		assertEquals(JSON.decode("{aaa:{bbb:{'':['aaa', 'bbb']}}}"), getParameterMap(request));
		
		request = context.createRequest("/?..=aaa&..=bbb");
		request.addParameter("..", "aaa");
		request.addParameter("..", "bbb");
		assertEquals(JSON.decode("{'':{'':{'':['aaa', 'bbb']}}}"), getParameterMap(request));
		
		request = context.createRequest("/?aaa[bbb]=aaa&aaa[bbb]=bbb");
		request.addParameter("aaa[bbb]", "aaa");
		request.addParameter("aaa[bbb]", "bbb");
		assertEquals(JSON.decode("{aaa:{bbb:['aaa', 'bbb']}}"), getParameterMap(request));

		request = context.createRequest("/?aaa[bbb].ccc=aaa&aaa[bbb].ccc=bbb");
		request.addParameter("aaa[bbb].ccc", "aaa");
		request.addParameter("aaa[bbb].ccc", "bbb");
		assertEquals(JSON.decode("{aaa:{bbb:{ccc:['aaa', 'bbb']}}}"), getParameterMap(request));

		request = context.createRequest("/?[aaa].bbb=aaa&[aaa].bbb=bbb");
		request.addParameter("[aaa].bbb", "aaa");
		request.addParameter("[aaa].bbb", "bbb");
		assertEquals(JSON.decode("{'':{aaa:{bbb:['aaa', 'bbb']}}}"), getParameterMap(request));

		request = context.createRequest("/?.aaa.bbb=aaa&[aaa].bbb=bbb&[aaa].bbb=ccc");
		request.addParameter(".aaa.bbb", "aaa");
		request.addParameter("[aaa].bbb", "bbb");
		request.addParameter("[aaa].bbb", "ccc");
		assertEquals(JSON.decode("{'':{aaa:{bbb:['aaa', 'bbb', 'ccc']}}}"), getParameterMap(request));

		request = context.createRequest("/?.aaa.bbb=aaa&.aaa.bbb=bbb&[aaa].bbb=ccc");
		request.addParameter(".aaa.bbb", "aaa");
		request.addParameter(".aaa.bbb", "bbb");
		request.addParameter("[aaa].bbb", "ccc");
		assertEquals(JSON.decode("{'':{aaa:{bbb:['aaa', 'bbb', 'ccc']}}}"), getParameterMap(request));
	}
	
	@Test
	public void testParseHeaderLine() throws Exception {
		assertEquals(JSON.decode("{null:''}"), parseHeaderLine(""));
		assertEquals(JSON.decode("{null:''}"), parseHeaderLine("   "));
		assertEquals(JSON.decode("{null:''}"), parseHeaderLine("   ;"));
		assertEquals(JSON.decode("{null:'aaa/bbb-yyy'}"), parseHeaderLine(" aaa/bbb-yyy "));
		assertEquals(JSON.decode("{null:'aaa/bbb-yyy'}"), parseHeaderLine(" aaa/bbb-yyy; "));
		assertEquals(JSON.decode("{null:'aaa/bbb-yyy'}"), parseHeaderLine("aaa/bbb-yyy;"));
		assertEquals(JSON.decode("{null:'aaa'}"), parseHeaderLine("aaa"));
		assertEquals(JSON.decode("{null:'a','a':'b','d':'e'}"), parseHeaderLine("a; a=b; d=e"));
		assertEquals(JSON.decode("{null:'abc','abc':'bcd','def':'efg'}"), parseHeaderLine(" abc ; abc = bcd ; def =efg;"));
		assertEquals(JSON.decode("{null:'abc','abc':'bcd','def':'efg'}"), parseHeaderLine(" abc ; abc = \"bcd\"; def =  \"efg\";"));
		assertEquals(JSON.decode("{null:'abc','abc':'bc\"d','def':'efg'}"), parseHeaderLine(" abc ; abc = \"bc\\\"d\"; def =  \"e\\fg\";"));
	}
	
	@SuppressWarnings({ "deprecation", "rawtypes" })
	private static Map getParameterMap(MockHttpServletRequest request) throws IOException {
		if (request.getCharacterEncoding() == null) request.setCharacterEncoding("UTF-8");
		return new WebServiceServlet.Route(request, null, new LinkedHashMap<String, Object>()).getParameterMap();
	}
	
	@SuppressWarnings({ "deprecation", "rawtypes" })
	private static Map parseHeaderLine(String line) throws Exception {
		Method m = WebServiceServlet.Route.class.getDeclaredMethod("parseHeaderLine", String.class);
		m.setAccessible(true);
		return (Map<String, String>)m.invoke(null, line);
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
