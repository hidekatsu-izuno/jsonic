package net.arnx.jsonic.internal.formatter;

import java.text.NumberFormat;

import net.arnx.jsonic.JSONMode;
import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

public class FloatArrayFormatter implements Formatter {
	public static final FloatArrayFormatter INSTANCE = new FloatArrayFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		NumberFormat f = context.getNumberFormat();
		float[] array = (float[]) o;
		out.append('[');
		for (int i = 0; i < array.length; i++) {
			if (Float.isNaN(array[i]) || Float.isInfinite(array[i])) {
				if (context.getMode() != JSONMode.SCRIPT) {
					out.append('"');
					out.append(Float.toString(array[i]));
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