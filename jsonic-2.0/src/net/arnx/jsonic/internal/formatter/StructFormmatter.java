package net.arnx.jsonic.internal.formatter;

import java.sql.SQLException;
import java.sql.Struct;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

public class StructFormmatter implements Formatter {
	public static final StructFormmatter INSTANCE = new StructFormmatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		Object value;
		try {
			value = ((Struct) o).getAttributes();
		} catch (SQLException e) {
			value = null;
		}
		if (value == null)
			value = new Object[0];
		return ObjectArrayFormatter.INSTANCE.format(context, src, o, out);
	}
}