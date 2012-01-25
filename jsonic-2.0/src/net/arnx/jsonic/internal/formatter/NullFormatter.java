package net.arnx.jsonic.internal.formatter;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

public class NullFormatter implements Formatter {
	public static final NullFormatter INSTANCE = new NullFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		out.append("null");
		return false;
	}
}