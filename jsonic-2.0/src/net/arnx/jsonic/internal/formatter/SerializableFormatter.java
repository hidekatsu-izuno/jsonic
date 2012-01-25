package net.arnx.jsonic.internal.formatter;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;
import net.arnx.jsonic.internal.util.Base64;
import net.arnx.jsonic.internal.util.ClassUtil;

public class SerializableFormatter implements Formatter {
	public static final SerializableFormatter INSTANCE = new SerializableFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		return StringFormatter.INSTANCE.format(context, src,
				Base64.encode(ClassUtil.serialize(o)), out);
	}
}