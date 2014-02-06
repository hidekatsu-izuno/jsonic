package net.arnx.jsonic;

import org.junit.Test;

public class JSONBugTest {
	@Test
	public void test() {
		JSON json = new JSON();
		json.setEnumStyle(null);
		System.out.println(json.format(new EnumTest()));
	}
	
	public static class EnumTest {
		@JSONHint(type = String.class)
		public Thread.State state1 = Thread.State.NEW;
		public Thread.State state2 = Thread.State.NEW;
	}
}
