package net.arnx.jsonic;

import java.io.IOException;
import java.io.Writer;

interface InputSource {
	public void append(String text) throws IOException;
	public void append(String text, int start, int end) throws IOException;
	public void append(char c) throws IOException;
	public void flush() throws IOException;
}

class WriterInputSource implements InputSource {
	private Writer writer;
	
	public WriterInputSource(Writer writer) {
		this.writer = writer;
	}
	
	@Override
	public void append(String text) throws IOException {
		writer.write(text);
	}
	
	@Override
	public void append(String text, int start, int end) throws IOException {
		writer.write(text, start, end-start);
	}
	
	@Override
	public void append(char c) throws IOException {
		writer.write(c);
	}
	
	public void flush() throws IOException {
		writer.flush();
	}
}

class BufferedWriterInputSource implements InputSource {
	private Writer writer;
	
	private char[] buf = new char[1000];
	private int pos = 0;
	
	public BufferedWriterInputSource(Writer writer) {
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

class StringBufferFormatSource implements InputSource {
	private StringBuffer sb;
	
	public StringBufferFormatSource() {
		this.sb = new StringBuffer(1000);
	}
	
	public StringBufferFormatSource(StringBuffer sb) {
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
	public void flush() throws IOException {
	}
	
	public void clear() {
		sb.setLength(0);
	}
	
	@Override
	public String toString() {
		return sb.toString();
	}
}

class StringBuilderInputSource implements InputSource {
	private StringBuilder sb;
	
	public StringBuilderInputSource() {
		this.sb = new StringBuilder(1000);
	}
	
	public StringBuilderInputSource(StringBuilder sb) {
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

class AppendableInputSource implements InputSource {
	private Appendable ap;
	
	public AppendableInputSource(Appendable ap) {
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
