package net.arnx.jsonic_out;

import static org.junit.Assert.*;

import java.lang.reflect.Type;
import org.junit.Test;
import net.arnx.jsonic.JSON;

public class JSONTest {
	
	@Test
	@SuppressWarnings("unused")
	public void testEncodeInnerClass() throws Exception {
		
		assertEquals("{\"a\":100}", JSON.encode((new Object() {
			public int a = 100;
		})));
	}
	
	@Test
	@SuppressWarnings("unused")
	public void testDecodeInnerClass() throws Exception {
		JSON json = new JSON() {
			protected void handleConvertError(String key, Object value, Class c, Type type, Exception e) throws Exception {
				throw e;
			}
		};
		
		assertEquals(new Hoge(), json.parse("{\"a\":100}", Hoge.class));
	}
}

class Hoge {
	public int a = 100;

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + a;
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
		final Hoge other = (Hoge) obj;
		if (a != other.a)
			return false;
		return true;
	}
}
