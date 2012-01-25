package net.arnx.jsonic.internal.formatter;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

public class IterableFormatter implements Formatter {
	public static final IterableFormatter INSTANCE = new IterableFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		return IteratorFormatter.INSTANCE.format(context, src,
				((Iterable<?>) o).iterator(), out);
	}
}