package net.arnx.jsonic.internal.converter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import net.arnx.jsonic.JSONHint;
import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.util.ClassUtil;

public class MapConverter implements Converter {
	public static final MapConverter INSTANCE = new MapConverter();

	@SuppressWarnings("unchecked")
	public Object convert(Context context, Object value, Class<?> c, Type t)
			throws Exception {
		Map<Object, Object> map = (Map<Object, Object>) context.create(c);
		t = ClassUtil.resolveParameterizedType(t, Map.class);

		Type pt0 = Object.class;
		Type pt1 = Object.class;
		Class<?> pc0 = Object.class;
		Class<?> pc1 = Object.class;
		if (t instanceof ParameterizedType) {
			Type[] pts = ((ParameterizedType) t).getActualTypeArguments();
			pt0 = (pts != null && pts.length > 0) ? pts[0] : Object.class;
			pt1 = (pts != null && pts.length > 1) ? pts[1] : Object.class;
			pc0 = ClassUtil.getRawType(pt0);
			pc1 = ClassUtil.getRawType(pt1);
		}

		if (value instanceof Map<?, ?>) {
			if (Object.class.equals(pc0) && Object.class.equals(pc1)) {
				map.putAll((Map<?, ?>) value);
			} else {
				JSONHint hint = context.getHint();
				for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
					context.enter('.', hint);
					Object key = context.postparse(entry.getKey(), pc0, pt0);
					context.exit();

					context.enter(entry.getKey(), hint);
					map.put(key, context.postparse(entry.getValue(), pc1, pt1));
					context.exit();
				}
			}
		} else if (value instanceof List<?>) {
			if (Object.class.equals(pc0) && Object.class.equals(pc1)) {
				List<?> src = (List<?>) value;
				for (int i = 0; i < src.size(); i++) {
					map.put(i, src.get(i));
				}
			} else {
				List<?> src = (List<?>) value;
				JSONHint hint = context.getHint();
				for (int i = 0; i < src.size(); i++) {
					context.enter('.', hint);
					Object key = context.postparse(i, pc0, pt0);
					context.exit();

					context.enter(i, hint);
					map.put(key, context.postparse(src.get(i), pc1, pt1));
					context.exit();
				}
			}
		} else {
			JSONHint hint = context.getHint();

			Object key = (hint != null && hint.anonym().length() > 0) ? hint
					.anonym() : null;
			if (Object.class.equals(pc0) && Object.class.equals(pc1)) {
				map.put(value, null);
			} else {
				context.enter('.', hint);
				key = context.postparse(key, pc0, pt0);
				context.exit();

				context.enter(key, hint);
				map.put(key, context.postparse(value, pc1, pt1));
				context.exit();
			}
		}
		return map;
	}
}