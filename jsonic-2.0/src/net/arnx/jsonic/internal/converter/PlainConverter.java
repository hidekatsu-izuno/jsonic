package net.arnx.jsonic.internal.converter;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import net.arnx.jsonic.JSON.Context;

public class PlainConverter implements Converter {
	public static final PlainConverter INSTANCE = new PlainConverter();

	private static final Map<Class<?>, Object> PRIMITIVE_MAP = new HashMap<Class<?>, Object>(
			8);

	static {
		PRIMITIVE_MAP.put(boolean.class, false);
		PRIMITIVE_MAP.put(byte.class, (byte) 0);
		PRIMITIVE_MAP.put(short.class, (short) 0);
		PRIMITIVE_MAP.put(int.class, 0);
		PRIMITIVE_MAP.put(long.class, 0l);
		PRIMITIVE_MAP.put(float.class, 0.0f);
		PRIMITIVE_MAP.put(double.class, 0.0);
		PRIMITIVE_MAP.put(char.class, '\0');
	}

	public Object convert(Context context, Object value, Class<?> c, Type t) {
		return value;
	}

	public static Object getDefaultValue(Class<?> cls) {
		return PRIMITIVE_MAP.get(cls);
	}
}
