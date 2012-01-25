package net.arnx.jsonic.internal.converter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;

import net.arnx.jsonic.JSONHint;
import net.arnx.jsonic.NamingStyle;
import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.util.ClassUtil;
import net.arnx.jsonic.internal.util.PropertyInfo;

public class ObjectConverter implements Converter {
	public static final ObjectConverter INSTANCE = new ObjectConverter();

	public Object convert(Context context, Object value, Class<?> c, Type t)
			throws Exception {
		Map<String, PropertyInfo> props = context.getSetProperties(c);
		if (value instanceof Map<?, ?>) {
			Object o = context.create(c);
			if (o == null)
				return null;
			for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
				String name = entry.getKey().toString();
				PropertyInfo target = props.get(name);
				if (target == null)
					target = props.get(NamingStyle.LOWER_CAMEL.to(name));
				if (target == null)
					continue;

				context.enter(name, target.getWriteAnnotation(JSONHint.class));
				Class<?> cls = target.getWriteType();
				Type gtype = target.getWriteGenericType();
				if (gtype instanceof TypeVariable<?>
						&& t instanceof ParameterizedType) {
					gtype = resolveTypeVariable((TypeVariable<?>) gtype,
							(ParameterizedType) t);
					cls = ClassUtil.getRawType(gtype);
				}
				target.set(o, context.postparse(entry.getValue(), cls, gtype));
				context.exit();
			}
			return o;
		} else if (value instanceof List<?>) {
			throw new UnsupportedOperationException("Cannot convert "
					+ value.getClass() + " to " + t);
		} else {
			JSONHint hint = context.getHint();
			if (hint != null && hint.anonym().length() > 0) {
				PropertyInfo target = props.get(hint.anonym());
				if (target == null)
					return null;
				Object o = context.create(c);
				if (o == null)
					return null;
				context.enter(hint.anonym(),
						target.getWriteAnnotation(JSONHint.class));
				Class<?> cls = target.getWriteType();
				Type gtype = target.getWriteGenericType();
				if (gtype instanceof TypeVariable<?>
						&& t instanceof ParameterizedType) {
					gtype = resolveTypeVariable((TypeVariable<?>) gtype,
							(ParameterizedType) t);
					cls = ClassUtil.getRawType(gtype);
				}
				target.set(o, context.postparse(value, cls, gtype));
				context.exit();
				return o;
			} else {
				throw new UnsupportedOperationException("Cannot convert "
						+ value.getClass() + " to " + t);
			}
		}
	}

	private static Type resolveTypeVariable(TypeVariable<?> type,
			ParameterizedType parent) {
		Class<?> rawType = ClassUtil.getRawType(parent);
		if (rawType.equals(type.getGenericDeclaration())) {
			String tvName = type.getName();
			TypeVariable<?>[] rtypes = ((Class<?>) rawType).getTypeParameters();
			Type[] atypes = parent.getActualTypeArguments();

			for (int i = 0; i < rtypes.length; i++) {
				if (tvName.equals(rtypes[i].getName()))
					return atypes[i];
			}
		}

		return type.getBounds()[0];
	}
}