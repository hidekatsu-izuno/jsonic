package net.arnx.jsonic.io;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class WriterOutputSourceTest {
	@Test
	public void test() throws Exception {
		List<String> list = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 1000; i++) {
			sb.setLength(0);
			int max = (int)(Math.random() * 100);
			for (int j = 0; j < max; j++) {
				sb.append("0123456789".charAt(j%10));
			}
			list.add(sb.toString());
		}
		sb.setLength(0);
		
		StringWriter sw = new StringWriter();
		WriterOutputSource out = new WriterOutputSource(sw);
		for (String str : list) {
			if (str.length() == 1) {
				out.append(str.charAt(0));
			} else {
				out.append(str);
			}
			sb.append(str);
		}
		out.flush();
		assertEquals(sb.toString(), sw.toString());
	}
}
