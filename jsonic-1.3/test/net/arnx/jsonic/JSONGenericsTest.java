package net.arnx.jsonic;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;

import org.junit.Test;

public class JSONGenericsTest {

	@Test
	public void testList() {
		InheritList list = new InheritList();
		InheritMap<Integer, Date> map = new InheritMap<Integer, Date>();
		map.put(new Date(1000000), 20);
		list.add(map);
		
		assertEquals(list, JSON.decode(JSON.encode(list), new TypeReference<InheritList>() {}));
	}
	
	
	public static class InheritList extends ArrayList<InheritMap<Integer, Date>> {
		
	}
	
	public static class InheritMap<V, Y> extends LinkedHashMap<Y, V> implements ImplTest<Y> {
		
	}
	
	public static interface ImplTest<X> {
		
	}
}
