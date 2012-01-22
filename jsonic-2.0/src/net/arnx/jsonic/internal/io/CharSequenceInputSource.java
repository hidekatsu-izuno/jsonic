package net.arnx.jsonic.internal.io;

import java.io.IOException;


public class CharSequenceInputSource implements InputSource {
	private int lines = 1;
	private int columns = 1;
	private int offset = 0;
	
	int mark = -1;
	
	private final CharSequence cs;
	
	public CharSequenceInputSource(CharSequence cs) {
		if (cs == null) {
			throw new NullPointerException();
		}
		this.cs = cs;
	}
	
	@Override
	public int skip() throws IOException {
		int n = -1;
		for (int i = offset; i < cs.length(); i++) {
			n = cs.charAt(offset++);
			if (n == ' ' || n == '\t' || n == 0xFEFF) {
				columns++;
			} else if (n == '\r') {
				lines++;
				columns = 0;
			} else if (n == '\n') {
				if (offset > 1 && cs.charAt(offset-2) != '\r') {
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
	public int next() {
		int n = -1;
		if (offset < cs.length()) {
			n = cs.charAt(offset++);
			if (n == '\r' || (n == '\n' && offset > 1 && cs.charAt(offset-2) != '\r')) {
				lines++;
				columns = 0;
			} else {
				columns++;
			}
		}
		return n;
	}
	
	@Override
	public void back() {
		offset--;
		columns--;
	}
	
	@Override
	public int mark() {
		mark = offset;
		return cs.length() - mark;
	}
	
	@Override
	public void copy(StringCache sc, int len) {
		if (mark == -1) {
			throw new IllegalStateException("no mark");
		}
		sc.append(cs, mark, mark + len);
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
	
	@Override
	public String toString() {
		return cs.subSequence(offset-columns+1, offset).toString();
	}
}