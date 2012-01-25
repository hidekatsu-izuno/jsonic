package net.arnx.jsonic.internal.formatter;

import java.util.Locale;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

public class LocaleFormatter implements Formatter {
	public static final LocaleFormatter INSTANCE = new LocaleFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		return StringFormatter.INSTANCE.format(context, src, ((Locale) o)
				.toString().replace('_', '-'), out);
	}
}