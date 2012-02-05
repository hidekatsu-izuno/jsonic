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
		
		reader = json.getReader("");
		while ((type = reader.next()) != null) {
			fail();
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
		
		reader = json.getReader("[{\"value\": \"a\"}, {\"value\": \"b\", \"child\": {\"value\": \"b1\"} }, {\"value\": \"c\"}]");
		while ((type = reader.next()) != null) {
			switch (type) {
			case START_OBJECT:
				list.add(reader.getValue(ReaderBean.class));
				break;
			}
		}
		assertEquals(3, list.size());
		assertEquals(new ReaderBean("a", null), list.get(0));
		assertEquals(new ReaderBean("b", new ReaderBean("b1", null)), list.get(1));
		assertEquals(new ReaderBean("c", null), list.get(2));
		
		list.clear();
		
		reader = json.getReader("{\"value\": \"a\"}\n{\"value\": \"b\", \"child\": {\"value\": \"b1\"} }\n{\"value\": \"c\"}");
		while ((type = reader.next()) != null) {
			switch (type) {
			case START_OBJECT:
				list.add(reader.getValue(ReaderBean.class));
				break;
			}
		}
		assertEquals(3, list.size());
		assertEquals(new ReaderBean("a", null), list.get(0));
		assertEquals(new ReaderBean("b", new ReaderBean("b1", null)), list.get(1));
		assertEquals(new ReaderBean("c", null), list.get(2));
		
		list.clear();
		if (mode == JSON.Mode.TRADITIONAL) {
			reader = json.getReader("{\"value\": \"a\"},\n{\"value\": \"b\", \"child\": {\"value\": \"b1\"} },\n{\"value\": \"c\"}");
			while ((type = reader.next()) != null) {
				switch (type) {
				case START_OBJECT:
					list.add(reader.getValue(ReaderBean.class));
					break;
				}
			}
			assertEquals(3, list.size());
			assertEquals(new ReaderBean("a", null), list.get(0));
			assertEquals(new ReaderBean("b", new ReaderBean("b1", null)), list.get(1));
			assertEquals(new ReaderBean("c", null), list.get(2));
		} else {
			try {
				reader = json.getReader("{\"value\": \"a\"},\n{\"value\": \"b\", \"child\": {\"value\": \"b1\"} },\n{\"value\": \"c\"}");
				while ((type = reader.next()) != null);
				fail();
			} catch (Exception e) {
				assertNotNull(e);
			}
		}
		
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
				list.add(null);
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

class ReaderBean {
	public ReaderBean() {
	}
	
	public ReaderBean(String value, ReaderBean child) {
		this.value = value;
		this.child = child;
	}
	
	public String value;
	
	public ReaderBean child;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((child == null) ? 0 : child.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReaderBean other = (ReaderBean) obj;
		if (child == null) {
			if (other.child != null)
				return false;
		} else if (!child.equals(other.child))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ReaderBean [value=" + value + ", child=" + child + "]";
	}
}