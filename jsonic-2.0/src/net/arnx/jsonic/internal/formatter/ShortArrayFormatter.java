package net.arnx.jsonic.internal.formatter;

import java.text.NumberFormat;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

public class ShortArrayFormatter implements Formatter {
	public static final ShortArrayFormatter INSTANCE = new ShortArrayFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		NumberFormat f = context.getNumberFormat();
		short[] array = (short[]) o;
		out.append('[');
		for (int i = 0; i < array.length; i++) {
			if (f != null) {
				StringFormatter.serialize(context, f.format(array[i]), out);
			} else {
				out.append(String.valueOf(array[i]));
			}
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