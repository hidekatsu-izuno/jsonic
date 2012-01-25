package net.arnx.jsonic.internal.formatter;

import java.sql.SQLException;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

public class SQLArrayFormatter implements Formatter {
	public static final SQLArrayFormatter INSTANCE = new SQLArrayFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		Object array;
		try {
			array = ((java.sql.Array) o).getArray();
		} catch (SQLException e) {
			array = null;
		}
		if (array == null)
			array = new Object[0];
		return ObjectArrayFormatter.INSTANCE.format(context, src, array, out);
	}
}