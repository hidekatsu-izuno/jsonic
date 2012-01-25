package net.arnx.jsonic.internal.formatter;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

public class ByteFormatter implements Formatter {
	public static final ByteFormatter INSTANCE = new ByteFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		out.append(Integer.toString(((Byte) o).byteValue() & 0xFF));
		return false;
	}
}