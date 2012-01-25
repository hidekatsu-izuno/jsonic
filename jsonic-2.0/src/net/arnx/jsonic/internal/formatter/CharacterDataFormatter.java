package net.arnx.jsonic.internal.formatter;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

import org.w3c.dom.CharacterData;

public class CharacterDataFormatter implements Formatter {
	public static final CharacterDataFormatter INSTANCE = new CharacterDataFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		return StringFormatter.INSTANCE.format(context, src,
				((CharacterData) o).getData(), out);
	}
}