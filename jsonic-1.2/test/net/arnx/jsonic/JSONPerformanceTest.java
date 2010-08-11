package net.arnx.jsonic;

import org.junit.Test;


public class JSONPerformanceTest {
	@Test
	public void testCharArray() {
		StringBuilder sb = new StringBuilder(100000);
		
		String text = "ABCDEFGHIJKLMNOPQRSTUVWZ";
		
		for (int i = 0; i < 1000000; i++) {
			for (int j = 0; j < text.length(); j++) {
				char c = text.charAt(j);
				if (c == '"') continue; 
			}
			sb.append(text);
		}
	}
	
	@Test
	public void testString() {
		StringBuilder sb = new StringBuilder(100000);
		
		String text = "ABCDEFGHIJKLMNOPQRSTUVWZ";
		for (int i = 0; i < 1000000; i++) {
			sb.append(text);
		}
	}
}
