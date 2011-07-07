package net.arnx.jsonic;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

interface InputSource {
	int next() throws IOException;
	void back();
	long getLineNumber();
	long getColumnNumber();
	long getOffset();
}

final class CharSequenceInputSource implements InputSource {
	int lines = 1;
	int columns = 1;
	int offset = 0;
	
	final CharSequence cs;
	
	public CharSequenceInputSource(CharSequence cs) {
		if (cs == null) {
			throw new NullPointerException();
		}
		this.cs = cs;
	}
	
	public int next() {
		if (offset < cs.length()) {
			char c = cs.charAt(offset++);
			if (c == '\r' || (c == '\n' && offset > 1 && cs.charAt(offset-2) != '\r')) {
				lines++;
				columns = 0;
			} else {
				columns++;
			}
			return c;
		}
		return -1;
	}
	
	public void back() {
		offset--;
		columns--;
	}
	
	public long getLineNumber() {
		return lines;
	}
	
	public long getColumnNumber() {
		return columns;
	}
	
	public long getOffset() {
		return offset;
	}
	
	public String toString() {
		return cs.subSequence(offset-columns+1, offset).toString();
	}
}

final class ReaderInputSource implements InputSource {
	long lines = 1l;
	long columns = 1l;
	long offset = 0;

	final Reader reader;
	final char[] buf = new char[256];
	int start = 0;
	int end = 0;
	
	public ReaderInputSource(InputStream in) throws IOException {
		if (!in.markSupported()) in = new BufferedInputStream(in);
		this.reader = new InputStreamReader(in, determineEncoding(in));
	}
	
	public ReaderInputSource(Reader reader) {
		if (reader == null) {
			throw new NullPointerException();
		}
		this.reader = reader;
	}
	
	public int next() throws IOException {
		if (start == end) {
			int size = reader.read(buf, start, Math.min(buf.length-start, buf.length/2));
			if (size != -1) {
				end = (end + size) % buf.length;
			} else {
				return -1;
			}
		}
		char c = buf[start];
		if (c == '\r' || (c == '\n' && buf[(start+buf.length-1) % (buf.length)] != '\r')) {
			lines++;
			columns = 0;
		} else {
			columns++;
		}
		offset++;
		start = (start+1) % buf.length;
		return c;
	}
	
	public void back() {
		offset--;
		columns--;
		start = (start+buf.length-1) % buf.length;
	}
	
	public long getLineNumber() {
		return lines;
	}
	
	public long getColumnNumber() {
		return columns;
	}
	
	public long getOffset() {
		return offset;
	}
	
	String determineEncoding(InputStream in) throws IOException {
		String encoding = "UTF-8";

		in.mark(4);
		byte[] check = new byte[4];
		int size = in.read(check);
		if (size == 2) {
			if (((check[0] & 0xFF) == 0x00 && (check[1] & 0xFF) != 0x00) 
					|| ((check[0] & 0xFF) == 0xFE && (check[1] & 0xFF) == 0xFF)) {
				encoding = "UTF-16BE";
			} else if (((check[0] & 0xFF) != 0x00 && (check[1] & 0xFF) == 0x00) 
					|| ((check[0] & 0xFF) == 0xFF && (check[1] & 0xFF) == 0xFE)) {
				encoding = "UTF-16LE";
			}
		} else if (size == 4) {
			if (((check[0] & 0xFF) == 0x00 && (check[1] & 0xFF) == 0x00)) {
				encoding = "UTF-32BE";
			} else if (((check[2] & 0xFF) == 0x00 && (check[3] & 0xFF) == 0x00)) {
				encoding = "UTF-32LE";
			} else if (((check[0] & 0xFF) == 0x00 && (check[1] & 0xFF) != 0x00) 
					|| ((check[0] & 0xFF) == 0xFE && (check[1] & 0xFF) == 0xFF)) {
				encoding = "UTF-16BE";
			} else if (((check[0] & 0xFF) != 0x00 && (check[1] & 0xFF) == 0x00) 
					|| ((check[0] & 0xFF) == 0xFF && (check[1] & 0xFF) == 0xFE)) {
				encoding = "UTF-16LE";
			}
		}
		in.reset();
		
		return encoding;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		int maxlength = (columns-1 < buf.length) ? (int)columns-1 : buf.length-1;
		for (int i = maxlength; i >= 0; i--) {
			sb.append(buf[(start-2+buf.length-i) % (buf.length-1)]);
		}
		return sb.toString();
	}
}
