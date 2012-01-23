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
	private int start = buf.length;
	private int end = buf.length-1;
	private int mark = -1;
	
	public ReaderInputSource(InputStream in) throws IOException {
		if (!in.markSupported()) in = new BufferedInputStream(in);
		this.reader = new InputStreamReader(in, determineEncoding(in));
		if (get() != 0xFEFF) back();
	}
	
	public ReaderInputSource(Reader reader) throws IOException {
		if (reader == null) {
			throw new NullPointerException();
		}
		this.reader = reader;
		if (get() != 0xFEFF) back();
	}
	
	@Override
	public int skip() throws IOException {
		int n = -1;
		while ((n = get()) != -1) {
			if (n == ' ' || n == '\t') {
				columns++;
			} else if (n == '\r') {
				lines++;
				columns = 0;
			} else if (n == '\n') {
				if (buf[start-2] != '\r') {
					lines++;
					columns = 0;
				} else {
					columns++;
				}
			} else {
				columns++;
				break;
			}
		}
		return n;
	}
	
	@Override
	public int next() throws IOException {
		int n = -1;
		if ((n = get()) != -1) {
			if (n == '\r' || (n == '\n' && buf[start-2] != '\r')) {
				lines++;
				columns = 0;
			} else {
				columns++;
			}
		}
		return n;
	}
	
	private int get() throws IOException {
		if (start > end) {
			buf[0] = buf[end];
			int size = reader.read(buf, BACK, buf.length-BACK);
			if (size != -1) {
				mark = (mark > end && mark <= end + BACK) ? (end + BACK - mark) : -1;
				start = BACK;
				end = BACK + size - 1;
			} else {
				return -1;
			}
		}
		offset++;
		return buf[start++];
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
	
	@Override
	public int mark() {
		mark = start;
		return end - mark + 1;
	}
	
	@Override
	public void copy(StringCache sc, int len) {
		if (mark == -1) {
			throw new IllegalStateException("no mark");
		}
		
		sc.append(buf, mark, len);
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
	
	private static String determineEncoding(InputStream in) throws IOException {
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
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		int maxlength = (columns-1 < buf.length) ? (int)columns-1 : buf.length-1;
		for (int i = maxlength; i >= 0; i--) {
			sb.append(buf[(start-2+buf.length-i) % (buf.length-1)]);
		}
		return sb.toString();
	}
}
