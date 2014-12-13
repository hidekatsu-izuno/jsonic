package net.arnx.jsonic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.inject.internal.Objects;

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

	@Test
	public void testSimple() {
		assertEquals("\"test\"", JSON.encode("test"));
	}

	@Test
	public void testSuperClass() {
		TestClass test = new TestClass();
		assertEquals("{\"test\":{\"hoge\":\"hoge\"}}", JSON.encode(test));
		((SubClass)test.test).hoge = "bar";
		assertEquals(test, JSON.decode("{\"test\":{\"hoge\":\"bar\"}}", TestClass.class));
	}

	public static class TestClass {
		@JSONHint(type = SubClass.class)
		public SuperClass test = new SubClass();

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((test == null) ? 0 : test.hashCode());
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
			TestClass other = (TestClass) obj;
			if (test == null) {
				if (other.test != null)
					return false;
			} else if (!test.equals(other.test))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "TestClass [test=" + test + "]";
		}
	}

	public static class SuperClass {
		public SuperClass() {
		}

		@Override
		public int hashCode() {
			return 0;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			return true;
		}
	}

	public static class SubClass extends SuperClass {
		public SubClass() {
		}

		public String hoge = "hoge";

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((hoge == null) ? 0 : hoge.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			SubClass other = (SubClass) obj;
			if (hoge == null) {
				if (other.hoge != null)
					return false;
			} else if (!hoge.equals(other.hoge))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "SubClass [hoge=" + hoge + "]";
		}
	}
}
