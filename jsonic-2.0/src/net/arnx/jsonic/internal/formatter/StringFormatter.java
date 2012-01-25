package net.arnx.jsonic.internal.formatter;

import net.arnx.jsonic.JSONMode;
import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

public class StringFormatter implements Formatter {
	public static final StringFormatter INSTANCE = new StringFormatter();

	private static final int[] ESCAPE_CHARS = new int[128];

	static {
		for (int i = 0; i < 32; i++) {
			ESCAPE_CHARS[i] = -1;
		}
		ESCAPE_CHARS['\b'] = 'b';
		ESCAPE_CHARS['\t'] = 't';
		ESCAPE_CHARS['\n'] = 'n';
		ESCAPE_CHARS['\f'] = 'f';
		ESCAPE_CHARS['\r'] = 'r';
		ESCAPE_CHARS['"'] = '"';
		ESCAPE_CHARS['\\'] = '\\';
		ESCAPE_CHARS['<'] = -2;
		ESCAPE_CHARS['>'] = -2;
		ESCAPE_CHARS[0x7F] = -1;
	}

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		serialize(context, o.toString(), out);
		return false;
	}

	public static void serialize(final Context context, final String s,
			final OutputSource out) throws Exception {
		out.append('"');
		int start = 0;
		final int length = s.length();
		for (int i = 0; i < length; i++) {
			int c = s.charAt(i);
			if (c < ESCAPE_CHARS.length) {
				int x = ESCAPE_CHARS[c];
				if (x == 0) {
					// no handle
				} else if (x > 0) {
					if (start < i)
						out.append(s, start, i);
					out.append('\\');
					out.append((char) x);
					start = i + 1;
				} else if (x == -1
						|| (x == -2 && context.getMode() == JSONMode.SCRIPT)) {
					if (start < i)
						out.append(s, start, i);
					out.append("\\u00");
					out.append("0123456789ABCDEF".charAt(c / 16));
					out.append("0123456789ABCDEF".charAt(c % 16));
					start = i + 1;
				}
			} else if (c == '\u2028') {
				out.append("\\u2028");
				start = i + 1;
			} else if (c == '\u2029') {
				out.append("\\u2029");
				start = i + 1;
			}
		}
		if (start < length)
			out.append(s, start, length);
		out.append('"');
	}
}