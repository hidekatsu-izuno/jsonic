package net.arnx.jsonic.internal.formatter;

import java.util.Calendar;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

public class CalendarFormatter implements Formatter {
	public static final CalendarFormatter INSTANCE = new CalendarFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		return DateFormatter.INSTANCE.format(context, src,
				((Calendar) o).getTime(), out);
	}
}