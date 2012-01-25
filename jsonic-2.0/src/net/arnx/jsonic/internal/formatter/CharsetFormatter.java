package net.arnx.jsonic.internal.formatter;

import java.nio.charset.Charset;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

public class CharsetFormatter implements Formatter {
	public static final CharsetFormatter INSTANCE = new CharsetFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		return StringFormatter.INSTANCE.format(context, src,
				((Charset) o).name(), out);
	}
}