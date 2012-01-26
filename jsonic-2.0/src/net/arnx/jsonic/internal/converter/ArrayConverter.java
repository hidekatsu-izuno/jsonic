package net.arnx.jsonic.internal.converter;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.JSONHint;
import net.arnx.jsonic.internal.util.Base64;

public class ArrayConverter implements Converter {
	public static final ArrayConverter INSTANCE = new ArrayConverter();

	public Object convert(Context context, Object value, Class<?> c, Type t)
			throws Exception {
		if (value instanceof Map<?, ?>) {
			Map<?, ?> src = (Map<?, ?>) value;
			if (!(src instanceof SortedMap<?, ?>)) {
				src = new TreeMap<Object, Object>(src);
			}
			value = src.values();
		}

		if (value instanceof Collection) {
			Collection<?> src = (Collection<?>) value;
			Object array = Array.newInstance(c.getComponentType(), src.size());
			Class<?> pc = c.getComponentType();
			Type pt = (t instanceof GenericArrayType) ? ((GenericArrayType) t)
					.getGenericComponentType() : pc;

			Iterator<?> it = src.iterator();
			JSONHint hint = context.getHint();
			for (int i = 0; it.hasNext(); i++) {
				context.enter(i, hint);
				Array.set(array, i, context.postparse(it.next(), pc, pt));
				context.exit();
			}
			return array;
		} else {
			Class<?> ctype = c.getComponentType();
			if (value instanceof String) {
				if (byte.class.equals(ctype)) {
					return Base64.decode((String) value);
				} else if (char.class.equals(ctype)) {
					return ((String) value).toCharArray();
				}
			}
			Object array = Array.newInstance(ctype, 1);
			Class<?> pc = ctype;
			Type pt = (t instanceof GenericArrayType) ? ((GenericArrayType) t)
					.getGenericComponentType() : pc;
			context.enter(0, context.getHint());
			Array.set(array, 0, context.postparse(value, pc, pt));
			context.exit();
			return array;
		}
	}
}