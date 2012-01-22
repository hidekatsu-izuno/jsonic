package net.arnx.jsonic.internal.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class ReaderInputSource implements InputSource {
	private static int BACK = 1;
	
	long lines = 1l;
	long columns = 1l;
	long offset = 0;

	final Reader reader;
	final char[] buf = new char[256];
	int start = buf.length-1;
	int end = buf.length-1;
	
	public ReaderInputSource(InputStream in) throws IOException {
		if (!in.markSupported()) in = new BufferedInputStream(in);
		this.reader = new InputStreamReader(in, determineEncoding(in));
	}
	
	public ReaderInputSource(Reader reader) throws IOException {
		if (reader == null) {
			throw new NullPointerException();
		}
		this.reader = reader;
	}
	
	public int next() throws IOException {
		if (start < end) {
			start++;
		} else {
			System.arraycopy(buf, end - BACK + 1, buf, 0, BACK);
			int size = reader.read(buf, BACK, buf.length-BACK);
			if (size != -1) {
				start = BACK;
				end = BACK + size - 1;
			} else {
				return -1;
			}
		}
		
		char c = buf[start];
		if (c == '\r' || (c == '\n' && buf[start-1] != '\r')) {
			lines++;
			columns = 0;
		} else {
			columns++;
		}
		offset++;
		return c;
	}
	
	public void back() {
		offset--;
		columns--;
		start--;
		
		if (start < 0) {
			throw new IllegalStateException("no backup charcter");
		}
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
