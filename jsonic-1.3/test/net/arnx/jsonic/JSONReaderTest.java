package net.arnx.jsonic;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Test;

public class JSONReaderTest {
	@Test
	public void testGetReaderTraditional() throws Exception {
		testGetReader(JSON.Mode.TRADITIONAL);
	}
	
	@Test
	public void testGetReaderScript() throws Exception {
		testGetReader(JSON.Mode.SCRIPT);
	}
	
	@Test
	public void testGetReaderStrict() throws Exception {
		testGetReader(JSON.Mode.STRICT);
	}
	
	private void testGetReader(JSON.Mode mode) throws Exception {
		List<Object> list = new ArrayList<Object>();
		JSONEventType type;
		JSONReader reader;

		JSON json = new JSON(mode);
		reader = json.getReader("");
		while ((type = reader.next()) != null) {
			switch (type) {
			case START_OBJECT:
				list.add(reader.getObject());
				break;
			}
		}
		assertEquals(1, list.size());
		assertEquals(new LinkedHashMap<Object, Object>(), list.get(0));
		
		list.clear();
		
		reader = json.getReader("[]{}[]");
		while ((type = reader.next()) != null) {
			switch (type) {
			case START_OBJECT:
				list.add(reader.getObject());
				break;
			case START_ARRAY:
				list.add(reader.getArray());
				break;
			}
		}
		assertEquals(3, list.size());
		assertEquals(new ArrayList<Object>(), list.get(0));
		assertEquals(new LinkedHashMap<Object, Object>(), list.get(1));
		assertEquals(new ArrayList<Object>(), list.get(2));
	}
}
