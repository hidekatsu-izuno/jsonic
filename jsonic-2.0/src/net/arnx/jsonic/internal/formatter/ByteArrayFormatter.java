package net.arnx.jsonic.internal.formatter;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;
import net.arnx.jsonic.internal.util.Base64;

public class ByteArrayFormatter implements Formatter {
	public static final ByteArrayFormatter INSTANCE = new ByteArrayFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		StringFormatter.serialize(context, Base64.encode((byte[]) o), out);
		return false;
	}
}