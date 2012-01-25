package net.arnx.jsonic.internal.formatter;

import java.util.List;

import net.arnx.jsonic.JSONHint;
import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;
import net.arnx.jsonic.internal.util.PropertyInfo;

public class ObjectFormatter implements Formatter {
	public static final ObjectFormatter INSTANCE = new ObjectFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		List<PropertyInfo> props = context.getGetProperties(o.getClass());

		out.append('{');
		int count = 0;
		final int length = props.size();
		for (int p = 0; p < length; p++) {
			PropertyInfo prop = props.get(p);
			Object value = null;
			Exception cause = null;

			try {
				value = prop.get(o);
				if (value == src || (context.isSuppressNull() && value == null))
					continue;

				if (count != 0)
					out.append(',');
				if (context.isPrettyPrint()) {
					out.append('\n');
					for (int j = 0; j < context.getLevel() + 1; j++)
						out.append('\t');
				}
			} catch (Exception e) {
				cause = e;
			}

			StringFormatter.serialize(context, prop.getName(), out);
			out.append(':');
			if (context.isPrettyPrint())
				out.append(' ');
			context.enter(prop.getName(),
					prop.getReadAnnotation(JSONHint.class));
			if (cause != null)
				throw cause;

			value = context.preformat(value);
			context.format(value, out);
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