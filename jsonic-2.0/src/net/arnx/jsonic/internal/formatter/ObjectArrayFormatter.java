package net.arnx.jsonic.internal.formatter;

import net.arnx.jsonic.JSONHint;
import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

public class ObjectArrayFormatter implements Formatter {
	public static final ObjectArrayFormatter INSTANCE = new ObjectArrayFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		final Object[] array = (Object[]) o;
		final JSONHint hint = context.getHint();

		Class<?> lastClass = null;
		Formatter lastFormatter = null;

		out.append('[');
		int i = 0;
		for (; i < array.length; i++) {
			Object item = array[i];
			if (item == src)
				item = null;

			if (i != 0)
				out.append(',');
			if (context.isPrettyPrint()) {
				out.append('\n');
				for (int j = 0; j < context.getLevel() + 1; j++)
					out.append('\t');
			}
			context.enter(i);
			item = context.preformat(item);
			if (item == null) {
				NullFormatter.INSTANCE.format(context, src, item, out);
			} else if (hint == null) {
				if (item.getClass().equals(lastClass)) {
					lastFormatter.format(context, src, item, out);
				} else {
					lastFormatter = context.format(item, out);
					lastClass = item.getClass();
				}
			} else {
				context.format(item, out);
			}
			context.exit();
		}
		if (context.isPrettyPrint() && i > 0) {
			out.append('\n');
			for (int j = 0; j < context.getLevel(); j++)
				out.append('\t');
		}
		out.append(']');
		return true;
	}
}