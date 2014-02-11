package net.arnx.jsonic;

import static org.junit.Assert.*;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Test;

public class JSONWriterTest {
	@Test
	public void testWriterObject() throws Exception {
		StringWriter out = new StringWriter();
		
		JSON json = new JSON();
		
		JSONWriter w = json.getWriter(out);
		w.beginObject();
		w.endObject();
		w.flush();
		assertEquals("{}", out.toString());

		out.getBuffer().setLength(0);
		w = json.getWriter(out);
		w.value(new HashMap<String, String>());
		w.flush();
		assertEquals("{}", out.toString());
		
		out.getBuffer().setLength(0);
		w = json.getWriter(out);
		w.beginObject();
		w.name("hoge");
		w.value("hoge");
		w.name("hoge2");
		w.value("hoge");
		w.name("hoge3");
		w.value("hoge");
		w.endObject();
		w.flush();
		assertEquals("{\"hoge\":\"hoge\",\"hoge2\":\"hoge\",\"hoge3\":\"hoge\"}", out.toString());
		
		out.getBuffer().setLength(0);
		w = json.getWriter(out);
		w.beginObject();
		w.name("hoge");
		w.value("hoge");
		w.name("hoge2");
		w.beginArray();
		w.value("hoge");
		w.beginArray();
		w.value("hoge");
		w.endArray();
		w.endArray();
		w.name("hoge3");
		w.value("hoge");
		w.endObject();
		w.flush();
		assertEquals("{\"hoge\":\"hoge\",\"hoge2\":[\"hoge\",[\"hoge\"]],\"hoge3\":\"hoge\"}", out.toString());

		json.setPrettyPrint(true);
		out.getBuffer().setLength(0);
		w = json.getWriter(out);
		w.beginObject();
		w.endObject();
		w.flush();
		assertEquals("{}", out.toString());

		out.getBuffer().setLength(0);
		w = json.getWriter(out);
		w.beginObject();
		w.name("hoge");
		w.value("hoge");
		w.name("hoge2");
		w.value("hoge");
		w.name("hoge3");
		w.value("hoge");
		w.endObject();
		w.flush();
		assertEquals("{\n\t\"hoge\": \"hoge\",\n\t\"hoge2\": \"hoge\",\n\t\"hoge3\": \"hoge\"\n}", out.toString());
		
		out.getBuffer().setLength(0);
		w = json.getWriter(out);
		w.beginObject();
		w.name("hoge");
		w.value("hoge");
		w.name("hoge2");
		w.beginArray();
		w.value("hoge");
		w.beginArray();
		w.value("hoge");
		w.endArray();
		w.endArray();
		w.name("hoge3");
		w.value("hoge");
		w.endObject();
		w.flush();
		assertEquals("{\n\t\"hoge\": \"hoge\",\n\t\"hoge2\": [\n\t\t\"hoge\",\n\t\t[\n\t\t\t\"hoge\"\n\t\t]\n\t],\n\t\"hoge3\": \"hoge\"\n}", out.toString());
	}
	
	@Test
	public void testWriterArray() throws Exception {
		StringWriter out = new StringWriter();
		
		JSON json = new JSON();
		
		JSONWriter w = json.getWriter(out);
		w.beginArray();
		w.endArray();
		w.flush();
		assertEquals("[]", out.toString());

		out.getBuffer().setLength(0);
		w = json.getWriter(out);
		w.value(new ArrayList<String>());
		w.flush();
		assertEquals("[]", out.toString());
		
		out.getBuffer().setLength(0);
		w = json.getWriter(out);
		w.beginArray();
		w.value("hoge");
		w.value("hoge");
		w.value("hoge");
		w.endArray();
		w.flush();
		assertEquals("[\"hoge\",\"hoge\",\"hoge\"]", out.toString());
				
		json.setPrettyPrint(true);
		
		out.getBuffer().setLength(0);
		w = json.getWriter(out);
		w.beginArray();
		w.endArray();
		w.flush();
		assertEquals("[]", out.toString());
		
		out.getBuffer().setLength(0);
		w = json.getWriter(out);
		w.beginArray();
		w.value("hoge");
		w.value("hoge");
		w.value("hoge");
		w.endArray();
		w.flush();
		assertEquals("[\n\t\"hoge\",\n\t\"hoge\",\n\t\"hoge\"\n]", out.toString());
		
		out.getBuffer().setLength(0);
		w = json.getWriter(out);
		w.beginArray();
		w.value("hoge");
		w.beginObject();
		w.name("name").value("hoge");
		w.endObject();
		w.value("hoge");
		w.endArray();
		w.flush();
		assertEquals("[\n\t\"hoge\",\n\t{\n\t\t\"name\": \"hoge\"\n\t},\n\t\"hoge\"\n]", out.toString());
	}
	
	@Test
	public void testWriterError() throws Exception {
		StringWriter out = new StringWriter();
		
		JSON json = new JSON();
		
		JSONWriter w = json.getWriter(out);
		try {
			w.name("error");
			fail();
		} catch (JSONException e) {
			System.err.println(e.getMessage());
			assertNotNull(e);
		}
		
		try {
			w.value("error");
			fail();
		} catch (JSONException e) {
			System.err.println(e.getMessage());
			assertNotNull(e);
		}
		
		w.beginObject();
		try {
			w.value("error");
			fail();
		} catch (JSONException e) {
			System.err.println(e.getMessage());
			assertNotNull(e);
		}
		
		try {
			w.endArray();
			fail();
		} catch (JSONException e) {
			System.err.println(e.getMessage());
			assertNotNull(e);
		}
		w.endObject();
		
		w.beginArray();
		try {
			w.name("error");
			fail();
		} catch (JSONException e) {
			System.err.println(e.getMessage());
			assertNotNull(e);
		}		
		try {
			w.endObject();
			fail();
		} catch (JSONException e) {
			System.err.println(e.getMessage());
			assertNotNull(e);
		}
		w.endArray();
		
		w.flush();
		assertEquals("{}[]", out.toString());
	}
}
