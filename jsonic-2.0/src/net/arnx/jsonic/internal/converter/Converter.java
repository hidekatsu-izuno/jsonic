package net.arnx.jsonic.internal.converter;

import java.lang.reflect.Type;

import net.arnx.jsonic.JSON.Context;

public interface Converter {
	Object convert(Context context, Object value, Class<?> c, Type t)
			throws Exception;
}