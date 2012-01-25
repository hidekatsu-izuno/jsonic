package net.arnx.jsonic.internal.formatter;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

import org.w3c.dom.Document;

public class DOMDocumentFormatter implements Formatter {
	public static final DOMDocumentFormatter INSTANCE = new DOMDocumentFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		return DOMElementFormatter.INSTANCE.format(context, src,
				((Document) o).getDocumentElement(), out);
	}
}