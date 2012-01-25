package net.arnx.jsonic.internal.formatter;

import java.text.NumberFormat;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

public class NumberFormatter implements Formatter {
	public static final NumberFormatter INSTANCE = new NumberFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		NumberFormat f = context.getNumberFormat();
		if (f != null) {
			StringFormatter.serialize(context, f.format(o), out);
		} else {
			out.append(o.toString());
		}
		return false;
	}
}