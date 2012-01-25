package net.arnx.jsonic.internal.converter;

import java.io.IOException;
import java.lang.reflect.Type;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.StringBuilderOutputSource;

public class FormatConverter implements Converter {
	public static final FormatConverter INSTANCE = new FormatConverter();

	public Object convert(Context context, Object value, Class<?> c, Type t)
			throws Exception {
		Context context2 = context.clone();
		context2.skipHint = true;
		value = context2.preformat(value);
		StringBuilderOutputSource fs = new StringBuilderOutputSource(
				new StringBuilder(200));
		try {
			context2.format(value, fs);
		} catch (IOException e) {
			// no handle
		}
		fs.flush();

		context.skipHint = true;
		return context.postparse(fs.toString(), c, t);
	}
}