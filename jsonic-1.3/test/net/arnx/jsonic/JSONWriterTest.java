package net.arnx.jsonic;

import static org.junit.Assert.*;

import java.io.StringWriter;

import org.junit.Test;

public class JSONWriterTest {
	@Test
	public void testWriter() throws Exception {
		StringWriter out = new StringWriter();
		
		JSON json = new JSON();
		
		JSONWriter w = json.getWriter(out);
		w.beginObject();
		w.endObject();
		assertEquals("{}", out.toString());
		
		out.getBuffer().setLength(0);
		
		w = json.getWriter(out);
		w.beginObject();
		w.name("hoge");
		w.value("hoge");
		w.endObject();
		assertEquals("{\"hoge\":\"hoge\"}", out.toString());
		
	}
}
