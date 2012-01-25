package net.arnx.jsonic.internal.converter;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.util.ClassUtil;

public class InetAddressConverter implements Converter {
	public static final InetAddressConverter INSTANCE = new InetAddressConverter();

	public Object convert(Context context, Object value, Class<?> c, Type t)
			throws Exception {
		if (value instanceof Map<?, ?>) {
			value = ((Map<?, ?>) value).get(null);
		} else if (value instanceof List<?>) {
			List<?> src = (List<?>) value;
			value = (!src.isEmpty()) ? src.get(0) : null;
		}

		if (value != null) {
			Class<?> inetAddressClass = ClassUtil
					.findClass("java.net.InetAddress");
			return inetAddressClass.getMethod("getByName", String.class)
					.invoke(null, value.toString().trim());
		}
		return null;
	}
}