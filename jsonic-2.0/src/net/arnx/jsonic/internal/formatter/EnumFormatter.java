package net.arnx.jsonic.internal.formatter;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

public class EnumFormatter implements Formatter {
	public static final EnumFormatter INSTANCE = new EnumFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		if (context.getEnumStyle() != null) {
			return StringFormatter.INSTANCE.format(context, src, context
					.getPropertyStyle().to(((Enum<?>) o).name()), out);
		} else {
			return NumberFormatter.INSTANCE.format(context, src,
					((Enum<?>) o).ordinal(), out);
		}
	}
}