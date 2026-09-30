package net.arnx.jsonic;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Test;

public class JSONGenericsTest {

	@Test
	public void testList() {
		InheritList list = new InheritList();
		InheritMap<Integer, Date> map = new InheritMap<Integer, Date>();
		map.put(new Date(1000000), 20);
		list.add(map);

		assertEquals(list, JSON.decode(JSON.encode(list), new TypeReference<InheritList>() {}));
	}


	public static class InheritList extends ArrayList<InheritMap<Integer, Date>> {

	}

	public static class InheritMap<V, Y> extends LinkedHashMap<Y, V> implements ImplTest<Y> {

	}

	public static interface ImplTest<X> {

	}

	@Test
	public void testParameterizedType() {
		ParameterizedTypeBean<ValueBean> bean = new ParameterizedTypeBean<ValueBean>();
		bean.valueProp = new ValueBean();
		bean.valueProp.value = "test";
		bean.listProp = new ArrayList<ValueBean>();
		bean.listProp.add(new ValueBean());
		bean.listProp.get(0).value = "test";

		assertEquals(bean, JSON.decode(JSON.encode(bean), new TypeReference<ParameterizedTypeBean<ValueBean>>() {}));

		ParameterizedTypeBean<ParameterizedTypeBean<ValueBean>> bean2 = new ParameterizedTypeBean<ParameterizedTypeBean<ValueBean>>();
		bean2.valueProp = bean;
		bean2.listProp = new ArrayList<ParameterizedTypeBean<ValueBean>>();
		bean2.listProp.add(bean);

		assertEquals(bean2, JSON.decode(JSON.encode(bean2), new TypeReference<ParameterizedTypeBean<ParameterizedTypeBean<ValueBean>>>() {}));
	}

	public static class ParameterizedTypeBean<T> {
		public T valueProp;
		public List<T> listProp;

		public ParameterizedTypeBean() {
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((valueProp == null) ? 0 : valueProp.hashCode());
			result = prime * result
					+ ((listProp == null) ? 0 : listProp.hashCode());
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
			ParameterizedTypeBean<?> other = (ParameterizedTypeBean<?>) obj;
			if (valueProp == null) {
				if (other.valueProp != null)
					return false;
			} else if (!valueProp.equals(other.valueProp))
				return false;
			if (listProp == null) {
				if (other.listProp != null)
					return false;
			} else if (!listProp.equals(other.listProp))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "ParameterizedTypeBean [valueProp=" + valueProp
					+ ", listProp=" + listProp + "]";
		}
	}

	public static class ValueBean {
		public String value;

		public ValueBean() {
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((value == null) ? 0 : value.hashCode());
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
			ValueBean other = (ValueBean) obj;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "ValueBean [value=" + value + "]";
		}
	}
}
