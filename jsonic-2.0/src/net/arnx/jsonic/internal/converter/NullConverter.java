package net.arnx.jsonic.internal.converter;

import java.lang.reflect.Type;

import net.arnx.jsonic.JSON.Context;

public class NullConverter implements Converter {
	public static final NullConverter INSTANCE = new NullConverter();

	public Object convert(Context context, Object value, Class<?> c, Type t) {
		return null;
	}
}