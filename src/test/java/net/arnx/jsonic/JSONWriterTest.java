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
		assertEquals("{}", out.toString());

		out.getBuffer().setLength(0);
		w = json.getWriter(out);
		w.value(new HashMap<String, String>());
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
		assertEquals("{\"hoge\":\"hoge\",\"hoge2\":[\"hoge\",[\"hoge\"]],\"hoge3\":\"hoge\"}", out.toString());

		json.setPrettyPrint(true);
		out.getBuffer().setLength(0);
		w = json.getWriter(out);
		w.beginObject();
		w.endObject();
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
		assertEquals("{\n\t\"hoge\": \"hoge\",\n\t\"hoge2\": [\n\t\t\"hoge\",\n\t\t[\n\t\t\t\"hoge\"\n\t\t]\n\t],\n\t\"hoge3\": \"hoge\"\n}", out.toString());


		out.getBuffer().setLength(0);
		w = json.getWriter(out);
		w.beginArray();
		{
			w.beginObject();
			{
				w.name("hoge");
				w.value("hoge");
				w.name("object");
				w.beginObject();
				w.endObject();
			}
			w.endObject();
			w.beginObject();
			{
				w.name("hoge");
				w.value("hoge");
				w.name("array");
				w.beginArray();
				w.endArray();
			}
			w.endObject();
			w.beginArray();
			{
				w.beginObject();
				w.endObject();
				w.value("hoge");
				w.beginArray();
				w.endArray();
			}
			w.endArray();
		}
		w.endArray();
		assertEquals("[\n\t{\n\t\t\"hoge\": \"hoge\",\n\t\t\"object\": {}\n\t},\n\t{\n\t\t\"hoge\": \"hoge\",\n\t\t\"array\": []\n\t},\n\t[\n\t\t{},\n\t\t\"hoge\",\n\t\t[]\n\t]\n]", out.toString());


		out.getBuffer().setLength(0);
		w = json.getWriter(out);
		w.beginArray();
		{
			w.beginObject();
			{
				w.name("hoge");
				w.append("{\"hoge\":\"hoge\",\"object\":{}}");
			}
			w.endObject();
			w.beginArray();
			{
				w.append("{\"hoge\":\"hoge\",\"array\":[]}");
			}
			w.endArray();
		}
		w.endArray();
		assertEquals("[\n\t{\n\t\t\"hoge\": {\"hoge\":\"hoge\",\"object\":{}}\n\t},\n\t[\n\t\t{\"hoge\":\"hoge\",\"array\":[]}\n\t]\n]", out.toString());

	}

	@Test
	public void testWriterArray() throws Exception {
		StringWriter out = new StringWriter();

		JSON json = new JSON();

		JSONWriter w = json.getWriter(out);
		w = json.getWriter(out);
		w.value("test");
		assertEquals("\"test\"", out.toString());

		out.getBuffer().setLength(0);
		w = json.getWriter(out);
		w.value(100);
		assertEquals("100", out.toString());

		out.getBuffer().setLength(0);
		w = json.getWriter(out);
		w.value(true);
		assertEquals("true", out.toString());

		out.getBuffer().setLength(0);
		w.beginArray();
		w.endArray();
		w.flush();
		assertEquals("[]", out.toString());

		out.getBuffer().setLength(0);
		w = json.getWriter(out);
		w.value(new ArrayList<String>());
		assertEquals("[]", out.toString());

		out.getBuffer().setLength(0);
		w = json.getWriter(out);
		w.beginArray();
		w.value("hoge");
		w.value("hoge");
		w.value("hoge");
		w.endArray();
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

		assertEquals("{}[]", out.toString());
	}
}
