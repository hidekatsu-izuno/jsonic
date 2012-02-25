package net.arnx.jsonic;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.JSON.Mode;
import net.arnx.jsonic.io.OutputSource;

public class JSONWriter {
	private static final int[] ESCAPE_CHARS = new int[128];
	private static final int NO_SEPARATOR = 0;
	private static final int COLON_SEPARATOR = 1;
	private static final int COMMA_SEPARATOR = 2;

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
	
	private Context context;
	private OutputSource out;
	
	private int sep = NO_SEPARATOR;
	
	JSONWriter(Context context, OutputSource out) {
		this.context = context;
		this.out = out;
	}
	
	public void startObject() throws IOException {
		sep = NO_SEPARATOR;
		separator();
		out.append('{');
	}
	
	public void endObject() throws IOException {
		sep = NO_SEPARATOR;
		separator();
		out.append('}');
		sep = COMMA_SEPARATOR;
	}
	
	public void startArray() throws IOException {
		sep = NO_SEPARATOR;
		separator();
		out.append('[');
		sep = NO_SEPARATOR;
	}
	
	public void endArray() throws IOException {
		sep = NO_SEPARATOR;
		separator();
		out.append(']');
		sep = COMMA_SEPARATOR;
	}
	
	public void name(String name) throws IOException {
		separator();
		value(name);
		sep = COLON_SEPARATOR;
	}
	
	public void value(String value) throws IOException {
		separator();
		if (value == null) {
			out.append("null");
		} else {
			out.append('"');
			int start = 0;
			final int length = value.length();
			for (int i = 0; i < length; i++) {
				int c = value.charAt(i);
				if (c < ESCAPE_CHARS.length) {
					int x = ESCAPE_CHARS[c];
					if (x == 0) {
						// no handle
					} else if (x > 0) {
						if (start < i) out.append(value, start, i);
						out.append('\\');
						out.append((char)x);
						start = i + 1;
					} else if (x == -1 || (x == -2 && context.getMode() != Mode.STRICT)) {
						if (start < i) out.append(value, start, i);
						out.append("\\u00");
						out.append("0123456789ABCDEF".charAt(c / 16));
						out.append("0123456789ABCDEF".charAt(c % 16));
						start = i + 1;
					}
				} else if (c == '\u2028') {
					if (start < i) out.append(value, start, i);
					out.append("\\u2028");
					start = i + 1;
				} else if (c == '\u2029') {
					if (start < i) out.append(value, start, i);
					out.append("\\u2029");
					start = i + 1;
				}
			}
			if (start < length) out.append(value, start, length);
			out.append('"');
		}
		sep = COMMA_SEPARATOR;
	}
	
	public void value(boolean value) throws IOException {
		separator();
		out.append(Boolean.toString(value));
		sep = COMMA_SEPARATOR;
	}
	
	public void value(char value) throws IOException {
		value(Character.toString(value));
	}
	
	public void value(byte value) throws IOException {
		separator();
		out.append(Integer.toString(value & 0xFF));
		sep = COMMA_SEPARATOR;
	}
	
	public void value(short value) throws IOException {
		separator();
		out.append(Short.toString(value));
		sep = COMMA_SEPARATOR;
	}
	
	public void value(short value, NumberFormat format) throws IOException {
		value(format.format(value));
	}
	
	public void value(int value) throws IOException {
		separator();
		out.append(Integer.toString(value));
		sep = COMMA_SEPARATOR;
	}
	
	public void value(int value, NumberFormat format) throws IOException {
		value(format.format(value));
	}
	
	public void value(long value) throws IOException {
		separator();
		out.append(Long.toString(value));
		sep = COMMA_SEPARATOR;
	}
	
	public void value(long value, NumberFormat format) throws IOException {
		value(format.format(value));
	}
	
	public void value(float value) throws IOException {
		separator();
		if (Float.isNaN(value) || Float.isInfinite(value)) {
			if (context.getMode() != Mode.SCRIPT) {
				out.append('"');
				out.append(Float.toString(value));
				out.append('"');
			} else if (Float.isNaN(value)) {
				out.append("Number.NaN");
			} else {
				out.append("Number.");
				out.append((value > 0) ? "POSITIVE" : "NEGATIVE");
				out.append("_INFINITY");
			}
		} else {
			out.append(Float.toString(value));
		}
		sep = COMMA_SEPARATOR;
	}
	
	public void value(float value, NumberFormat format) throws IOException {
		value(format.format(value));
	}
	
	public void value(double value) throws IOException {
		separator();
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			if (context.getMode() != Mode.SCRIPT) {
				out.append('"');
				out.append(Double.toString(value));
				out.append('"');
			} else if (Double.isNaN(value)) {
				out.append("Number.NaN");
			} else {
				out.append("Number.");
				out.append((value > 0) ? "POSITIVE" : "NEGATIVE");
				out.append("_INFINITY");
			}
		} else {
			out.append(Double.toString(value));
		}
		sep = COMMA_SEPARATOR;
	}
	
	public void value(double value, NumberFormat format) throws IOException {
		value(format.format(value));
	}

	public void value(BigInteger value) throws IOException {
		separator();
		if (value == null) {
			out.append("null");
		} else {
			out.append(value.toString());
		}
		sep = COMMA_SEPARATOR;
	}

	public void value(BigInteger value, NumberFormat format) throws IOException {
		separator();
		if (value == null) {
			out.append("null");
		} else {
			value(format.format(value));
		}
		sep = COMMA_SEPARATOR;
	}

	public void value(BigDecimal value) throws IOException {
		separator();
		if (value == null) {
			out.append("null");
		} else {
			out.append(value.toString());
		}
		sep = COMMA_SEPARATOR;
	}

	public void value(BigDecimal value, NumberFormat format) throws IOException {
		separator();
		if (value == null) {
			out.append("null");
		} else {
			value(format.format(value));
		}
		sep = COMMA_SEPARATOR;
	}
	
	public void flush() throws IOException {
		out.flush();
	}
	
	private void separator() throws IOException {
		switch (sep) {
		case COLON_SEPARATOR:
			out.append(':');
			if (context.isPrettyPrint()) out.append(' ');
			break;
		case COMMA_SEPARATOR:
			out.append(',');
		default:
			if (context.isPrettyPrint()) {
				out.append('\n');
				int indent = context.getInitialIndent() + context.getDepth() + 1;
				for (int j = 0; j < indent; j++) {
					out.append(context.getIndentText());
				}
			}
		}
	}
}
