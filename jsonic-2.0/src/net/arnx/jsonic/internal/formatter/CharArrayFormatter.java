package net.arnx.jsonic.internal.formatter;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

public class CharArrayFormatter implements Formatter {
	public static final CharArrayFormatter INSTANCE = new CharArrayFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		return StringFormatter.INSTANCE.format(context, src,
				String.valueOf((char[]) o), out);
	}
}