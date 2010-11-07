package net.arnx.jsonic;

import org.junit.Test;
import static org.junit.Assert.*;


public class CacheBufferTest {
	@Test
	public void testAppend() throws Exception {
		assertEquals("test", new JSON.CacheBuffer().append("test").toString());
		assertEquals("testtesttest", new JSON.CacheBuffer().append("test").append("test").append("test").toString());
	}
}
