package net.arnx.jsonic.internal.formatter;

import java.util.TimeZone;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

public class TimeZoneFormatter implements Formatter {
	public static final TimeZoneFormatter INSTANCE = new TimeZoneFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		return StringFormatter.INSTANCE.format(context, src,
				((TimeZone) o).getID(), out);
	}
}