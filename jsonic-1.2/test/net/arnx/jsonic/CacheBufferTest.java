package net.arnx.jsonic;

import org.junit.Test;
import static org.junit.Assert.*;


public class CacheBufferTest {
	@Test
	public void testAppend() throws Exception {
		assertEquals("test", new JSON.CacheBuffer().append("test").toString());
		assertEquals("testtesttest", new JSON.CacheBuffer().append("test").append("test").append("test").toString());
		assertEquals("teesst", new JSON.CacheBuffer().append("test", 0, 2).append("test", 1, 3).append("test", 2, 4).toString());
	}
}
