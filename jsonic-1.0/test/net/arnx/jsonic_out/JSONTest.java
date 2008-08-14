package net.arnx.jsonic_out;

import static org.junit.Assert.*;

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
		JSON json = new JSON();
		
		assertEquals(new Hoge(), json.parse("{\"a\":100}", Hoge.class));

		assertEquals(new InnerHoge(), json.parse("{\"a\":100}", InnerHoge.class));
		
		assertEquals(new PrivateInnerHoge(), json.parse("{\"a\":100}", PrivateInnerHoge.class));
		
		assertEquals(new InnerHoge(), JSON.decode("{\"a\":100}", InnerHoge.class));
		
		
		InnerHoge hoge = null;
		try {
			hoge = json.parse("{\"a\":100}", InnerHoge.class);
			hoge.accessEnclosingClass();
			fail();
		} catch (Exception e) {
			assertNotNull(e);
		}
		
		json.setContext(this);
		hoge = json.parse("{\"a\":100}", InnerHoge.class);
		hoge.accessEnclosingClass();
		
		try {
			json.setContext(new Object());
			hoge = json.parse("{\"a\":100}", InnerHoge.class);
			fail();
		} catch (Exception e) {
			assertNotNull(e);
		}
	}
	
	class InnerHoge {
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
			final InnerHoge other = (InnerHoge) obj;
			if (a != other.a)
				return false;
			return true;
		}
		
		public class InnerInnerHoge {
			
		}
		
		public void accessEnclosingClass() {
			JSONTest.this.toString();
		}
	}
	
	private class PrivateInnerHoge {
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
			final PrivateInnerHoge other = (PrivateInnerHoge) obj;
			if (a != other.a)
				return false;
			return true;
		}
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
