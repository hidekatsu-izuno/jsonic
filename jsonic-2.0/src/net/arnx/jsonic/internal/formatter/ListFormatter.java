package net.arnx.jsonic.internal.formatter;

import java.util.List;

import net.arnx.jsonic.JSONHint;
import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

public class ListFormatter implements Formatter {
	public static final ListFormatter INSTANCE = new ListFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		final List<?> list = (List<?>) o;
		final JSONHint hint = context.getHint();
		final int length = list.size();

		Class<?> lastClass = null;
		Formatter lastFormatter = null;

		out.append('[');
		int count = 0;
		while (count < length) {
			Object item = list.get(count);
			if (item == src)
				item = null;

			if (count != 0)
				out.append(',');
			if (context.isPrettyPrint()) {
				out.append('\n');
				for (int j = 0; j < context.getDepth() + 1; j++)
					out.append('\t');
			}
			context.enter(count, hint);
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
			count++;
		}
		if (context.isPrettyPrint() && count > 0) {
			out.append('\n');
			for (int j = 0; j < context.getDepth(); j++)
				out.append('\t');
		}
		out.append(']');
		return true;
	}
}