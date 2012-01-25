package net.arnx.jsonic.internal.formatter;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

public class BooleanArrayFormatter implements Formatter {
	public static final BooleanArrayFormatter INSTANCE = new BooleanArrayFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		out.append('[');
		boolean[] array = (boolean[]) o;
		for (int i = 0; i < array.length; i++) {
			out.append(String.valueOf(array[i]));
			if (i != array.length - 1) {
				out.append(',');
				if (context.isPrettyPrint())
					out.append(' ');
			}
		}
		out.append(']');
		return true;
	}
}