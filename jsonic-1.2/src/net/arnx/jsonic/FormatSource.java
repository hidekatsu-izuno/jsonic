package net.arnx.jsonic;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

interface FormatSource {
	public FormatSource append(String text) throws IOException;
	public FormatSource append(String text, int start, int end) throws IOException;
	public FormatSource append(char c) throws IOException;
	public void flush() throws IOException;
}

class WriterFormatSource implements FormatSource {
	private Writer writer;
	
	public WriterFormatSource(Writer writer) {
		if (writer instanceof OutputStreamWriter || writer instanceof FileWriter) {
			this.writer = new BufferedWriter(writer);
		} else {
			this.writer = writer;
		}
	}
	
	@Override
	public FormatSource append(String text) throws IOException {
		writer.write(text);
		return this;
	}
	
	@Override
	public FormatSource append(String text, int start, int end) throws IOException {
		writer.write(text, start, end-start);
		return this;
	}
	
	@Override
	public FormatSource append(char c) throws IOException {
		writer.write(c);
		return this;
	}
	
	public void flush() throws IOException {
		writer.flush();
	}
}

class StringBufferFormatSource implements FormatSource {
	private StringBuffer sb;
	
	public StringBufferFormatSource() {
		this.sb = new StringBuffer(1000);
	}
	
	public StringBufferFormatSource(StringBuffer sb) {
		this.sb = sb;
	}
	
	@Override
	public FormatSource append(String text) {
		sb.append(text);
		return this;
	}
	
	@Override
	public FormatSource append(String text, int start, int end) {
		sb.append(text, start, end);
		return this;
	}
	
	@Override
	public FormatSource append(char c) {
		sb.append(c);
		return this;
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

class StringBuilderFormatSource implements FormatSource {
	private StringBuilder sb;
	
	public StringBuilderFormatSource() {
		this.sb = new StringBuilder(1000);
	}
	
	public StringBuilderFormatSource(StringBuilder sb) {
		this.sb = sb;
	}
	
	@Override
	public FormatSource append(String text) {
		sb.append(text);
		return this;
	}
	
	@Override
	public FormatSource append(String text, int start, int end) {
		sb.append(text, start, end);
		return this;
	}
	
	@Override
	public FormatSource append(char c) {
		sb.append(c);
		return this;
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


class AppendableFormatSource implements FormatSource {
	private Appendable ap;
	
	public AppendableFormatSource(Appendable ap) {
		this.ap = ap;
	}
	
	@Override
	public FormatSource append(String text) throws IOException {
		ap.append(text);
		return this;
	}
	
	@Override
	public FormatSource append(String text, int start, int end) throws IOException {
		ap.append(text, start, end);
		return this;
	}
	
	@Override
	public FormatSource append(char c) throws IOException {
		ap.append(c);
		return this;
	}
	
	@Override
	public void flush() throws IOException {
	}
	
	@Override
	public String toString() {
		return ap.toString();
	}
}
