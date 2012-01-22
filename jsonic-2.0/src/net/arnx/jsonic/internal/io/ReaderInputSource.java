package net.arnx.jsonic.internal.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class ReaderInputSource implements InputSource {
	private static int BACK = 1;
	
	private long lines = 1l;
	private long columns = 1l;
	private long offset = 0;

	private final Reader reader;
	private final char[] buf = new char[256];
	private int start = buf.length-1;
	private int end = buf.length-1;
	private int mark = -1;
	
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
	
	@Override
	public int next() throws IOException {
		if (start > end) {
			System.arraycopy(buf, end - BACK + 1, buf, 0, BACK);
			int size = reader.read(buf, BACK, buf.length-BACK);
			if (size != -1) {
				start = BACK;
				end = BACK + size - 1;
				mark = -1;
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
		start++;
		offset++;
		return c;
	}
	
	@Override
	public void back() {
		if (start == 0) {
			throw new IllegalStateException("no backup charcter");
		}
		offset--;
		columns--;
		start--;
	}
	
	public int mark() {
		mark = start;
		return end - mark + 1;
	}
	
	public void flush(StringBuilder sb, int len) {
		if (mark == -1) {
			throw new IllegalStateException("no mark");
		}
		
		sb.append(buf, mark, len);
	}
	
	@Override
	public long getLineNumber() {
		return lines;
	}
	
	@Override
	public long getColumnNumber() {
		return columns;
	}
	
	@Override
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
