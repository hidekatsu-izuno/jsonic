package net.arnx.jsonic;

import java.io.IOException;
import java.io.Writer;

interface OutputSource {
	public void append(String text) throws IOException;
	public void append(String text, int start, int end) throws IOException;
	public void append(char c) throws IOException;
	public void flush() throws IOException;
}

final class WriterOutputSource implements OutputSource {
	private final Writer writer;
	
	private final char[] buf = new char[1000];
	private int pos = 0;
	
	public WriterOutputSource(Writer writer) {
		this.writer = writer;
	}
	
	@Override
	public void append(String text) throws IOException {
		append(text, 0, text.length());
	}
	
	@Override
	public void append(String text, int start, int end) throws IOException {
		int length = end-start;
		if (pos + length < buf.length) {
			text.getChars(start, end, buf, pos);
			pos += length;
		} else {
			writer.write(buf, 0, pos);
			if (length < buf.length) {
				text.getChars(start, end, buf, 0);
				pos = length;
			} else {
				writer.write(text, start, length);
				pos = 0;
			}
		}
	}
	
	@Override
	public void append(char c) throws IOException {
		if (pos + 1 >= buf.length) {
			writer.write(buf, 0, pos);
			pos = 0;
		}
		buf[pos++] = c;
	}
	
	public void flush() throws IOException {
		if (pos > 0) writer.write(buf, 0, pos);
		writer.flush();
	}
}

final class StringBuilderOutputSource implements OutputSource {
	private final StringBuilder sb;
	
	public StringBuilderOutputSource() {
		this.sb = new StringBuilder(1000);
	}
	
	public StringBuilderOutputSource(StringBuilder sb) {
		this.sb = sb;
	}
	
	@Override
	public void append(String text) {
		sb.append(text);
	}
	
	@Override
	public void append(String text, int start, int end) {
		sb.append(text, start, end);
	}
	
	@Override
	public void append(char c) {
		sb.append(c);
	}
	
	@Override
	public void flush() {
	}
	
	public void clear() {
		sb.setLength(0);
	}
	
	@Override
	public String toString() {
		return sb.toString();
	}
}

final class AppendableOutputSource implements OutputSource {
	private final Appendable ap;
	
	public AppendableOutputSource(Appendable ap) {
		this.ap = ap;
	}
	
	@Override
	public void append(String text) throws IOException {
		ap.append(text);
	}
	
	@Override
	public void append(String text, int start, int end) throws IOException {
		ap.append(text, start, end);
	}
	
	@Override
	public void append(char c) throws IOException {
		ap.append(c);
	}
	
	@Override
	public void flush() throws IOException {
	}
	
	@Override
	public String toString() {
		return ap.toString();
	}
}
