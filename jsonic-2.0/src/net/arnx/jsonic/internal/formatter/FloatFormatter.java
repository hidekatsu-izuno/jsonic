package net.arnx.jsonic.internal.formatter;

import java.text.NumberFormat;

import net.arnx.jsonic.JSONMode;
import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

public class FloatFormatter implements Formatter {
	public static final FloatFormatter INSTANCE = new FloatFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		NumberFormat f = context.getNumberFormat();
		if (f != null) {
			StringFormatter.serialize(context, f.format(o), out);
		} else {
			double d = ((Number) o).doubleValue();
			if (Double.isNaN(d) || Double.isInfinite(d)) {
				if (context.getMode() != JSONMode.SCRIPT) {
					out.append('"');
					out.append(o.toString());
					out.append('"');
				} else if (Double.isNaN(d)) {
					out.append("Number.NaN");
				} else {
					out.append("Number.");
					out.append((d > 0) ? "POSITIVE" : "NEGATIVE");
					out.append("_INFINITY");
				}
			} else {
				out.append(o.toString());
			}
		}
		return false;
	}
}