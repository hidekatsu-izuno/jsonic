package net.arnx.jsonic.internal.formatter;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

public class ClassFormatter implements Formatter {
	public static final ClassFormatter INSTANCE = new ClassFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		return StringFormatter.INSTANCE.format(context, src,
				((Class<?>) o).getName(), out);
	}
}