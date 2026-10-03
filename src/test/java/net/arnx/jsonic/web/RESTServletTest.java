package net.arnx.jsonic.web;

import static jakarta.servlet.http.HttpServletResponse.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;

import net.arnx.jsonic.JSON;
import net.arnx.jsonic.web.RESTServlet.RouteMapping;

@SuppressWarnings("unchecked")
public class RESTServletTest {

	private static WebTestServer server;

	@BeforeAll
	public static void init() throws Exception {
		server = new WebTestServer();
	}

	@AfterAll
	public static void destroy() throws Exception {
		if (server != null) server.close();
	}

	@Test
	public void testREST() throws Exception {
		testREST("basic");
	}


	@Test
	public void testRESTwithSpring() throws Exception {
		testREST("spring");
	}


	public void testREST(String app) throws Exception {
		System.out.println("\n<<START testRest: " + app + ">>");

		String url = server.url() + "/" + app + "/rest/memo";
		HttpURLConnection con = null;

		List<Map<String, Object>> content = null;

		// POST
		con = (HttpURLConnection)new URI(url + ".json").toURL().openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "{\"title\":\"title\",\"text\":\"text\"}");
		con.connect();
		assertEquals(SC_CREATED, con.getResponseCode());
		con.disconnect();

		// GET
		con = (HttpURLConnection)new URI(url + ".json").toURL().openConnection();
		con.setRequestMethod("GET");
		con.connect();
		assertEquals(SC_OK, con.getResponseCode());
		content = (List<Map<String, Object>>)JSON.decode(read(con.getInputStream()));
		con.disconnect();

		// PUT
		con = (HttpURLConnection)new URI(url + "/" + content.get(0).get("id") + ".json").toURL().openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("PUT");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "{\"title\":\"title\",\"text\":\"text\"}");
		con.connect();
		assertEquals(SC_NO_CONTENT, con.getResponseCode());
		con.disconnect();

		// POST
		con = (HttpURLConnection)new URI(url + "/" + content.get(0).get("id") + ".json").toURL().openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "{\"title\":\"title\",\"text\":\"text\"}");
		con.connect();
		assertEquals(SC_METHOD_NOT_ALLOWED, con.getResponseCode());
		con.disconnect();

		// DELETE
		con = (HttpURLConnection)new URI(url + "/" + content.get(0).get("id") + ".json").toURL().openConnection();
		con.setRequestMethod("DELETE");
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestProperty("Content-Length", "0");
		con.setRequestProperty("X-Requested-With", "XMLHttpRequest");
		con.connect();
		assertEquals(SC_NO_CONTENT, con.getResponseCode());
		con.disconnect();

		// DELETE
		con = (HttpURLConnection)new URI(url + "/" + content.get(0).get("id") + ".json").toURL().openConnection();
		con.setRequestMethod("DELETE");
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestProperty("Content-Length", "0");
		con.setRequestProperty("X-Requested-With", "XMLHttpRequest");
		con.connect();
		assertEquals(SC_NOT_FOUND, con.getResponseCode());
		con.disconnect();

		// POST
		con = (HttpURLConnection)new URI(url + ".json").toURL().openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "[\"title\", \"text\"]");
		con.connect();
		assertEquals(SC_BAD_REQUEST, con.getResponseCode());
		con.disconnect();

		// POST
		con = (HttpURLConnection)new URI(url + ".json").toURL().openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "[\"title\"]");
		con.connect();
		assertEquals(SC_BAD_REQUEST, con.getResponseCode());
		con.disconnect();

		// POST
		con = (HttpURLConnection)new URI(url + ".json").toURL().openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		con.setRequestProperty("X-Requested-With", "XMLHttpRequest");
		write(con, "title=title&text=text");
		con.connect();
		assertEquals(SC_CREATED, con.getResponseCode());
		con.disconnect();

		// HEAD
		con = (HttpURLConnection)new URI(url + ".json").toURL().openConnection();
		con.setRequestMethod("HEAD");
		con.connect();
		assertEquals(SC_METHOD_NOT_ALLOWED, con.getResponseCode());
		con.disconnect();

		// OPTIONS
		con = (HttpURLConnection)new URI(url + ".json").toURL().openConnection();
		con.setRequestMethod("OPTIONS");
		con.connect();
		assertEquals(SC_METHOD_NOT_ALLOWED, con.getResponseCode());
		con.disconnect();

		// methods specified
		con = (HttpURLConnection)new URI(url + ".print.json").toURL().openConnection();
		con.setRequestMethod("GET");
		con.connect();
		assertEquals(SC_OK, con.getResponseCode());
		con.disconnect();

		con = (HttpURLConnection)new URI(url + ".exception.json").toURL().openConnection();
		con.setRequestMethod("GET");
		con.connect();
		assertEquals(SC_NOT_ACCEPTABLE, con.getResponseCode());
		assertEquals(JSON.<Object>decode("{\"name\":\"MemoException\",\"message\":\"memo error!\",\"data\":{\"extensionProperty\":\"extension property\",\"rename\":\"rename property\"}}"),
				JSON.decode(read(con.getErrorStream())));
		con.disconnect();

		// DUMMY
		url = server.url() + "/" + app + "/rest/test";
		con = (HttpURLConnection)new URI(url + ".json").toURL().openConnection();
		con.setRequestMethod("GET");
		con.connect();
		assertEquals(SC_NOT_FOUND, con.getResponseCode());
		con.disconnect();

		System.out.println("<<END testRest: " + app + ">>\n");
	}

	@Test
	public void testRESTWithMethod() throws Exception {
		System.out.println("\n<<START testRESTWithMethod>>");

		String url = server.url() + "/basic/rest/memo.json";
		HttpURLConnection con = null;

		List<Map<String, Object>> content = null;

		// POST
		con = (HttpURLConnection)new URI(url + "?_method=POST").toURL().openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "{\"title\":\"title\",\"text\":\"text\"}");
		con.connect();
		assertEquals(SC_CREATED, con.getResponseCode());
		con.disconnect();

		// GET
		con = (HttpURLConnection)new URI(url + "?_method=GET").toURL().openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Length", "0");
		con.setRequestProperty("X-Requested-With", "XMLHttpRequest");
		con.connect();
		assertEquals(SC_OK, con.getResponseCode());
		content = (List<Map<String, Object>>)JSON.decode(read(con.getInputStream()));
		con.disconnect();

		// PUT
		con = (HttpURLConnection)new URI(url + "?_method=PUT").toURL().openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "{\"id\":" + content.get(0).get("id") + ",\"title\":\"title\",\"text\":\"text\"}");
		con.connect();
		assertEquals(SC_NO_CONTENT, con.getResponseCode());
		con.disconnect();

		// DELETE
		con = (HttpURLConnection)new URI(url + "?_method=DELETE").toURL().openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "{\"id\":" + content.get(0).get("id") + "}");
		con.connect();
		assertEquals(SC_NO_CONTENT, con.getResponseCode());
		con.disconnect();

		// POST
		con = (HttpURLConnection)new URI(url + "?_method=POST").toURL().openConnection();
		con.setDoOutput(true);
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		write(con, "[\"title\", \"text\"]");
		con.connect();
		assertEquals(SC_BAD_REQUEST, con.getResponseCode());
		con.disconnect();

		// POST
		con = (HttpURLConnection)new URI(url + "?_method=POST").toURL().openConnection();
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
		MockServletContext context = new MockServletContext("/");
		MockHttpServletRequest request = null;

		request = new MockHttpServletRequest(context, "GET", "/?aaa=");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("aaa", "");
		assertEquals(JSON.<Object>decode("{\"aaa\":\"\"}"), getParameterMap(request));

		request = new MockHttpServletRequest(context, "GET", "/?aaa=aaa=bbb");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("aaa", "aaa=bbb");
		assertEquals(JSON.<Object>decode("{\"aaa\":\"aaa=bbb\"}"), getParameterMap(request));

		request = new MockHttpServletRequest(context, "GET", "/?&");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("", "");
		request.addParameter("", "");
		assertEquals(JSON.<Object>decode("{\"\":[\"\",\"\"]}"), getParameterMap(request));

		request = new MockHttpServletRequest(context, "GET", "/?=&=");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("", "");
		request.addParameter("", "");
		assertEquals(JSON.<Object>decode("{\"\":[\"\",\"\"]}"),getParameterMap(request));

		request = new MockHttpServletRequest(context, "GET", "/");
		request.setContentType("application/x-www-form-urlencoded");
		assertEquals(JSON.<Object>decode("{}"), getParameterMap(request));

		request = new MockHttpServletRequest(context, "GET", "/?aaa.bbb=aaa");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("aaa.bbb", "aaa");
		assertEquals(JSON.<Object>decode("{\"aaa\":{\"bbb\":\"aaa\"}}"), getParameterMap(request));

		request = new MockHttpServletRequest(context, "GET", "/?" + URLEncoder.encode("諸行 無常", "UTF-8") + "=" + URLEncoder.encode("古今=東西", "UTF-8"));
		request.setContentType("application/x-www-form-urlencoded");
		request.setCharacterEncoding("UTF-8");
		request.addParameter("諸行 無常", "古今=東西");
		assertEquals(JSON.<Object>decode("{\"諸行 無常\":\"古今=東西\"}"), getParameterMap(request));

		request = new MockHttpServletRequest(context, "GET", "/?" + URLEncoder.encode("諸行 無常", "MS932") + "=" + URLEncoder.encode("古今=東西", "MS932"));
		request.setContentType("application/x-www-form-urlencoded");
		request.setCharacterEncoding("MS932");
		request.addParameter("諸行 無常", "古今=東西");
		assertEquals(JSON.<Object>decode("{\"諸行 無常\":\"古今=東西\"}"), getParameterMap(request));

		request = new MockHttpServletRequest(context, "GET", "/?aaa.bbb=aaa&aaa.bbb=bbb&aaa=aaa");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("aaa.bbb", "aaa");
		request.addParameter("aaa.bbb", "bbb");
		request.addParameter("aaa", "aaa");
		Map<Object, Object> nestedParameters = new LinkedHashMap<Object, Object>();
		nestedParameters.put("bbb", java.util.Arrays.asList("aaa", "bbb"));
		nestedParameters.put(null, "aaa");
		Map<String, Object> expectedParameters = new LinkedHashMap<String, Object>();
		expectedParameters.put("aaa", nestedParameters);
		assertEquals(expectedParameters, getParameterMap(request));

		request = new MockHttpServletRequest(context, "GET", "/?aaa.bbb=aaa&aaa.bbb=bbb&aaa=aaa&aaa=bbb");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("aaa.bbb", "aaa");
		request.addParameter("aaa.bbb", "bbb");
		request.addParameter("aaa", "aaa");
		request.addParameter("bbb", "bbb");
		expectedParameters.put("bbb", "bbb");
		assertEquals(expectedParameters, getParameterMap(request));

		request = new MockHttpServletRequest(context, "GET", "/?aaa.bbb=aaa&aaa.bbb=bbb");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("aaa.bbb", "aaa");
		request.addParameter("aaa.bbb", "bbb");
		assertEquals(JSON.<Object>decode("{\"aaa\":{\"bbb\":[\"aaa\",\"bbb\"]}}"), getParameterMap(request));

		request = new MockHttpServletRequest(context, "GET", "/?aaa.bbb.=aaa&aaa.bbb.=bbb");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("aaa.bbb.", "aaa");
		request.addParameter("aaa.bbb.", "bbb");
		assertEquals(JSON.<Object>decode("{\"aaa\":{\"bbb\":{\"\":[\"aaa\",\"bbb\"]}}}"), getParameterMap(request));

		request = new MockHttpServletRequest(context, "GET", "/?..=aaa&..=bbb");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("..", "aaa");
		request.addParameter("..", "bbb");
		assertEquals(JSON.<Object>decode("{\"\":{\"\":{\"\":[\"aaa\",\"bbb\"]}}}"), getParameterMap(request));

		request = new MockHttpServletRequest(context, "GET", "/?aaa[bbb]=aaa&aaa[bbb]=bbb");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("aaa[bbb]", "aaa");
		request.addParameter("aaa[bbb]", "bbb");
		assertEquals(JSON.<Object>decode("{\"aaa\":{\"bbb\":[\"aaa\",\"bbb\"]}}"), getParameterMap(request));

		request = new MockHttpServletRequest(context, "GET", "/?aaa[bbb]=aaa&aaa[bbb]=bbb");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("aaa[bbb][]", "aaa");
		assertEquals(JSON.<Object>decode("{\"aaa\":{\"bbb\":[\"aaa\"]}}"), getParameterMap(request));

		request = new MockHttpServletRequest(context, "GET", "/?aaa[bbb].ccc=aaa&aaa[bbb].ccc=bbb");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("aaa[bbb].ccc", "aaa");
		request.addParameter("aaa[bbb].ccc", "bbb");
		assertEquals(JSON.<Object>decode("{\"aaa\":{\"bbb\":{\"ccc\":[\"aaa\",\"bbb\"]}}}"), getParameterMap(request));

		request = new MockHttpServletRequest(context, "GET", "/?[aaa].bbb=aaa&[aaa].bbb=bbb");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter("[aaa].bbb", "aaa");
		request.addParameter("[aaa].bbb", "bbb");
		assertEquals(JSON.<Object>decode("{\"\":{\"aaa\":{\"bbb\":[\"aaa\",\"bbb\"]}}}"), getParameterMap(request));

		request = new MockHttpServletRequest(context, "GET", "/?.aaa.bbb=aaa&[aaa].bbb=bbb&[aaa].bbb=ccc");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter(".aaa.bbb", "aaa");
		request.addParameter("[aaa].bbb", "bbb");
		request.addParameter("[aaa].bbb", "ccc");
		assertEquals(JSON.<Object>decode("{\"\":{\"aaa\":{\"bbb\":[\"aaa\",\"bbb\",\"ccc\"]}}}"), getParameterMap(request));

		request = new MockHttpServletRequest(context, "GET", "/?.aaa.bbb=aaa&.aaa.bbb=bbb&[aaa].bbb=ccc");
		request.setContentType("application/x-www-form-urlencoded");
		request.addParameter(".aaa.bbb", "aaa");
		request.addParameter(".aaa.bbb", "bbb");
		request.addParameter("[aaa].bbb", "ccc");
		assertEquals(JSON.<Object>decode("{\"\":{\"aaa\":{\"bbb\":[\"aaa\",\"bbb\",\"ccc\"]}}}"), getParameterMap(request));
	}

	@SuppressWarnings("rawtypes")
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
		System.out.println(sb.toString());
		return sb.toString();
	}
}
