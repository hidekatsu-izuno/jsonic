package net.arnx.jsonic.internal.formatter;

import java.text.DateFormat;
import java.util.Date;

import net.arnx.jsonic.JSONMode;
import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

public class DateFormatter implements Formatter {
	public static final DateFormatter INSTANCE = new DateFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		Date date = (Date) o;
		DateFormat f = context.getDateFormat();
		if (f != null) {
			StringFormatter.serialize(context, f.format(o), out);
		} else if (context.getMode() == JSONMode.SCRIPT) {
			out.append("new Date(");
			out.append(Long.toString(date.getTime()));
			out.append(")");
		} else {
			out.append(Long.toString(date.getTime()));
		}
		return false;
	}
}