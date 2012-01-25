package net.arnx.jsonic.internal.formatter;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

public class PlainFormatter implements Formatter {
	public static final PlainFormatter INSTANCE = new PlainFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		out.append(o.toString());
		return false;
	}
}