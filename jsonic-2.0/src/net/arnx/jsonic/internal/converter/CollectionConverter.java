package net.arnx.jsonic.internal.converter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.util.ClassUtil;

public class CollectionConverter implements Converter {
	public static final CollectionConverter INSTANCE = new CollectionConverter();

	@SuppressWarnings("unchecked")
	public Object convert(Context context, Object value, Class<?> c, Type t)
			throws Exception {
		if (value instanceof Map) {
			Map<?, ?> src = (Map<?, ?>) value;
			if (!(src instanceof SortedMap<?, ?>)) {
				src = new TreeMap<Object, Object>(src);
			}
			value = src.values();
		}

		Collection<Object> collection = (Collection<Object>) context.create(c);
		t = ClassUtil.resolveParameterizedType(t, Collection.class);

		Class<?> pc = Object.class;
		Type pt = Object.class;
		if (t instanceof ParameterizedType) {
			Type[] pts = ((ParameterizedType) t).getActualTypeArguments();
			pt = (pts != null && pts.length > 0) ? pts[0] : Object.class;
			pc = ClassUtil.getRawType(pt);
		}

		if (value instanceof Collection) {
			Collection<?> src = (Collection<?>) value;

			if (!Object.class.equals(pc)) {
				Iterator<?> it = src.iterator();
				for (int i = 0; it.hasNext(); i++) {
					context.enter(i);
					collection.add(context.postparse(it.next(), pc, pt));
					context.exit();
				}
			} else {
				collection.addAll(src);
			}
		} else {
			if (!Object.class.equals(pc)) {
				context.enter(0);
				collection.add(context.postparse(value, pc, pt));
				context.exit();
			} else {
				collection.add(value);
			}
		}

		return collection;
	}
}