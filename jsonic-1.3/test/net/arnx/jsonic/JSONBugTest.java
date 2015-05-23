package net.arnx.jsonic;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Type;
import java.util.List;

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

	public void testPreformatNull() {
		JSON json = new JSON() {
			@Override
			protected Object preformatNull(Context context, Type type) throws Exception {
				if (type == String.class) {
					return "";
				}

				return super.preformatNull(context, type);
			}
		};

		assertEquals("null", json.format(null));
		assertEquals("{\"str\":\"\",\"num\":null,\"bool\":null}", json.format(new PreformatNullBean()));
	}

	@Test
	public void testGenerics() {
		Sample<Address> aList = JSON.decode("{items: [{name: 'a'}]}", new TypeReference<Sample<Address>>(){});
        Address address = aList.getItems().get(0);
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

	public static class PreformatNullBean {
		public PreformatNullBean() {
		}

		public String str;

		public Integer num;

		public Boolean bool;
	}

	class Address {
		private String name;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
	}

	class Sample<T> {
		private List<T> items;

		public List<T> getItems() {
			return items;
		}
		public void setItems(List<T> items) {
			this.items = items;
		}
	}
}
