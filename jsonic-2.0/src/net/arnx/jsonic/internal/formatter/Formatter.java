package net.arnx.jsonic.internal.formatter;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

public interface Formatter {
	boolean format(Context context, Object src, Object o, OutputSource out)
			throws Exception;
}