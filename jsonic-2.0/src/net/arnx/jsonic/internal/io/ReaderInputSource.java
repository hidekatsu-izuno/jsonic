package net.arnx.jsonic.internal.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;


public class ReaderInputSource implements InputSource {
	private static int BACK = 20;
	
	private long lines = 1L;
	private long columns = 0L;
	private long offset = 0L;

	private final Reader reader;
	private final char[] buf = new char[256 + BACK];
	private int back = BACK;
	private int start = BACK;
	private int end = BACK - 1;
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
		int n = -1;
		if ((n = get()) != -1) {
			offset++;
			if (n == '\r') {
				lines++;
				columns = 0;
			} else if (n == '\n') {
				if (start < 2 || buf[start-2] != '\r') {
					lines++;
					columns = 0;
				}
			} else {
				columns++;
			}
		}
		return n;
	}
	
	private int get() throws IOException {
		if (start > end) {
			if (end > BACK) {
				int len = Math.min(BACK, end + 1 - BACK);
				System.arraycopy(buf, end - len, buf, BACK - len, len);
				back = BACK - len;
			}
			int size = reader.read(buf, BACK, buf.length-BACK);
			if (size != -1) {
				mark = (mark > end - BACK) ? BACK - (end - mark + 1) : -1;
				start = BACK;
				end = BACK + size - 1;
			} else {
				start++;
				return -1;
			}
		}
		return buf[start++];
	}
	
	@Override
	public void back() {
		if (start <= back) {
			throw new IllegalStateException("no backup charcter");
		}
		start--;
		if (start < end) {
			offset--;
			columns--;
		}
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
		int pos = Math.min((int)(start-columns), BACK);
		int len = Math.min((int)(end-pos+1), 0);
		return (len > 0) ? String.valueOf(buf, pos, len) : "";
	}
}
