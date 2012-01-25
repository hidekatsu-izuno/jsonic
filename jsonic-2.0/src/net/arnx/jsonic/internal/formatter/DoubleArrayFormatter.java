package net.arnx.jsonic.internal.formatter;

import java.text.NumberFormat;

import net.arnx.jsonic.JSONMode;
import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

public class DoubleArrayFormatter implements Formatter {
	public static final DoubleArrayFormatter INSTANCE = new DoubleArrayFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		NumberFormat f = context.getNumberFormat();
		double[] array = (double[]) o;
		out.append('[');
		for (int i = 0; i < array.length; i++) {
			if (Double.isNaN(array[i]) || Double.isInfinite(array[i])) {
				if (context.getMode() != JSONMode.SCRIPT) {
					out.append('"');
					out.append(Double.toString(array[i]));
					out.append('"');
				} else if (Double.isNaN(array[i])) {
					out.append("Number.NaN");
				} else {
					out.append("Number.");
					out.append((array[i] > 0) ? "POSITIVE" : "NEGATIVE");
					out.append("_INFINITY");
				}
			} else if (f != null) {
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