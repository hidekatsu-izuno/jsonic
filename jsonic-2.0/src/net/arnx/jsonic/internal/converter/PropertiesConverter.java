package net.arnx.jsonic.internal.converter;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.arnx.jsonic.JSON.Context;

public class PropertiesConverter implements Converter {
	public static final PropertiesConverter INSTANCE = new PropertiesConverter();

	public Object convert(Context context, Object value, Class<?> c, Type t)
			throws Exception {
		Properties prop = (Properties) context.create(c);
		if (value instanceof Map<?, ?> || value instanceof List<?>) {
			flattenProperties(new StringBuilder(32), value, prop);
		} else if (value != null) {
			prop.setProperty(value.toString(), null);
		}
		return prop;
	}

	private static void flattenProperties(StringBuilder key, Object value,
			Properties props) {
		if (value instanceof Map<?, ?>) {
			for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
				int pos = key.length();
				if (pos > 0)
					key.append('.');
				key.append(entry.getKey());
				flattenProperties(key, entry.getValue(), props);
				key.setLength(pos);
			}
		} else if (value instanceof List<?>) {
			List<?> list = (List<?>) value;
			for (int i = 0; i < list.size(); i++) {
				int pos = key.length();
				if (pos > 0)
					key.append('.');
				key.append(i);
				flattenProperties(key, list.get(i), props);
				key.setLength(pos);
			}
		} else {
			props.setProperty(key.toString(), value.toString());
		}
	}
}