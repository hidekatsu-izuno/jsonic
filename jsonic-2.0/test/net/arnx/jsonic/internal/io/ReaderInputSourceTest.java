package net.arnx.jsonic.internal.io;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

public class ReaderInputSourceTest {

	@Test
	public void test() throws IOException {
		InputSource in = new ReaderInputSource(new StringReader("0123\n4567\r\n89\r"));
		
		assertEquals("", in.toString());
		assertEquals(0, in.getOffset());
		assertEquals(1, in.getLineNumber());
		assertEquals(0, in.getColumnNumber());
		
		assertEquals('0', (char)in.next());
		assertEquals("0", in.toString());
		assertEquals(1, in.getOffset());
		assertEquals(1, in.getLineNumber());
		assertEquals(1, in.getColumnNumber());
		
		assertEquals('1', (char)in.next());
		assertEquals("01", in.toString());
		assertEquals(2, in.getOffset());
		assertEquals(1, in.getLineNumber());
		assertEquals(2, in.getColumnNumber());
		
		in.back();
		assertEquals("0", in.toString());
		assertEquals(1, in.getOffset());
		assertEquals(1, in.getLineNumber());
		assertEquals(1, in.getColumnNumber());
		
		assertEquals('1', (char)in.next());
		assertEquals("01", in.toString());
		assertEquals(2, in.getOffset());
		assertEquals(1, in.getLineNumber());
		assertEquals(2, in.getColumnNumber());
		
		assertEquals('2', (char)in.next());
		assertEquals("012", in.toString());
		assertEquals(3, in.getOffset());
		assertEquals(1, in.getLineNumber());
		assertEquals(3, in.getColumnNumber());
		
		assertEquals('3', (char)in.next());
		assertEquals("0123", in.toString());
		assertEquals(4, in.getOffset());
		assertEquals(1, in.getLineNumber());
		assertEquals(4, in.getColumnNumber());
		
		assertEquals('\n', (char)in.next());
		assertEquals("0123\n", in.toString());
		assertEquals(5, in.getOffset());
		assertEquals(2, in.getLineNumber());
		assertEquals(0, in.getColumnNumber());
		
		assertEquals('4', (char)in.next());
		assertEquals("4", in.toString());
		assertEquals(6, in.getOffset());
		assertEquals(2, in.getLineNumber());
		assertEquals(1, in.getColumnNumber());
		
		assertEquals('5', (char)in.next());
		assertEquals("45", in.toString());
		assertEquals(7, in.getOffset());
		assertEquals(2, in.getLineNumber());
		assertEquals(2, in.getColumnNumber());
		
		assertEquals('6', (char)in.next());
		assertEquals("456", in.toString());
		assertEquals(8, in.getOffset());
		assertEquals(2, in.getLineNumber());
		assertEquals(3, in.getColumnNumber());
		
		assertEquals('7', (char)in.next());
		assertEquals("4567", in.toString());
		assertEquals(9, in.getOffset());
		assertEquals(2, in.getLineNumber());
		assertEquals(4, in.getColumnNumber());
		
		assertEquals('\r', (char)in.next());
		assertEquals("4567\r", in.toString());
		assertEquals(10, in.getOffset());
		assertEquals(3, in.getLineNumber());
		assertEquals(0, in.getColumnNumber());
		
		assertEquals('\n', (char)in.next());
		assertEquals("4567\r\n", in.toString());
		assertEquals(11, in.getOffset());
		assertEquals(3, in.getLineNumber());
		assertEquals(0, in.getColumnNumber());
		
		assertEquals('8', (char)in.next());
		assertEquals("8", in.toString());
		assertEquals(12, in.getOffset());
		assertEquals(3, in.getLineNumber());
		assertEquals(1, in.getColumnNumber());
		
		assertEquals('9', (char)in.next());
		assertEquals("89", in.toString());
		assertEquals(13, in.getOffset());
		assertEquals(3, in.getLineNumber());
		assertEquals(2, in.getColumnNumber());
		
		assertEquals('\r', (char)in.next());
		assertEquals("89\r", in.toString());
		assertEquals(14, in.getOffset());
		assertEquals(4, in.getLineNumber());
		assertEquals(0, in.getColumnNumber());
		
		assertEquals(-1, in.next());
		assertEquals("89\r", in.toString());
		assertEquals(14, in.getOffset());
		assertEquals(4, in.getLineNumber());
		assertEquals(0, in.getColumnNumber());
		
		in.back();
		assertEquals("89\r", in.toString());
		assertEquals(14, in.getOffset());
		assertEquals(4, in.getLineNumber());
		assertEquals(0, in.getColumnNumber());
		
		assertEquals(-1, in.next());
		assertEquals("89\r", in.toString());
		assertEquals(14, in.getOffset());
		assertEquals(4, in.getLineNumber());
		assertEquals(0, in.getColumnNumber());
		
		assertEquals(-1, in.next());
		assertEquals("89\r", in.toString());
		assertEquals(14, in.getOffset());
		assertEquals(4, in.getLineNumber());
		assertEquals(0, in.getColumnNumber());
	}

}
