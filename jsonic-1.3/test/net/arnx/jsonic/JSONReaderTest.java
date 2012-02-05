package net.arnx.jsonic;

import static org.junit.Assert.*;

import java.math.BigDecimal;
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
		
		if (mode == JSON.Mode.TRADITIONAL) {
			reader = json.getReader("");
			while ((type = reader.next()) != null) {
				switch (type) {
				case START_OBJECT:
					list.add(reader.getMap());
					break;
				}
			}
			assertEquals(1, list.size());
			assertEquals(new LinkedHashMap<Object, Object>(), list.get(0));
		}
		
		list.clear();
		
		reader = json.getReader("[]{}[]");
		while ((type = reader.next()) != null) {
			switch (type) {
			case START_OBJECT:
				list.add(reader.getMap());
				break;
			case START_ARRAY:
				list.add(reader.getList());
				break;
			}
		}
		assertEquals(3, list.size());
		assertEquals(new ArrayList<Object>(), list.get(0));
		assertEquals(new LinkedHashMap<Object, Object>(), list.get(1));
		assertEquals(new ArrayList<Object>(), list.get(2));
		
		list.clear();
		
		reader = json.getReader("[1, 2, 3]\n{ \"name\" : \"value\" }\n[true, false, null]");
		while ((type = reader.next()) != null) {
			switch (type) {
			case NAME:
				list.add(reader.getString());
				break;
			case STRING:
				list.add(reader.getString());
				break;
			case NUMBER:
				list.add(reader.getNumber());
				break;
			case BOOLEAN:
				list.add(reader.getBoolean());
				break;
			case NULL:
				list.add(reader.getValue());
				break;
			}
		}
		assertEquals(8, list.size());
		assertEquals(new BigDecimal(1), list.get(0));
		assertEquals(new BigDecimal(2), list.get(1));
		assertEquals(new BigDecimal(3), list.get(2));
		assertEquals("name", list.get(3));
		assertEquals("value", list.get(4));
		assertEquals(Boolean.TRUE, list.get(5));
		assertEquals(Boolean.FALSE, list.get(6));
		assertNull(list.get(7));
	}
}
