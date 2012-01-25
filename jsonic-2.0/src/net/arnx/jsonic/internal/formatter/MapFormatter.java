package net.arnx.jsonic.internal.formatter;

import java.util.Map;

import net.arnx.jsonic.JSONHint;
import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

public class MapFormatter implements Formatter {
	public static final MapFormatter INSTANCE = new MapFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		final Map<?, ?> map = (Map<?, ?>) o;
		final JSONHint hint = context.getHint();

		Class<?> lastClass = null;
		Formatter lastFormatter = null;

		out.append('{');
		int count = 0;
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			Object key = entry.getKey();
			if (key == null)
				continue;

			Object value = entry.getValue();
			if (value == src || (context.isSuppressNull() && value == null))
				continue;

			if (count != 0)
				out.append(',');
			if (context.isPrettyPrint()) {
				out.append('\n');
				for (int j = 0; j < context.getLevel() + 1; j++)
					out.append('\t');
			}
			StringFormatter.serialize(context, key.toString(), out);
			out.append(':');
			if (context.isPrettyPrint())
				out.append(' ');
			context.enter(key, hint);
			value = context.preformat(value);
			if (value == null) {
				NullFormatter.INSTANCE.format(context, src, value, out);
			} else if (hint == null) {
				if (value.getClass().equals(lastClass)) {
					lastFormatter.format(context, src, value, out);
				} else {
					lastFormatter = context.format(value, out);
					lastClass = value.getClass();
				}
			} else {
				context.format(value, out);
			}
			context.exit();
			count++;
		}
		if (context.isPrettyPrint() && count > 0) {
			out.append('\n');
			for (int j = 0; j < context.getLevel(); j++)
				out.append('\t');
		}
		out.append('}');
		return true;
	}
}