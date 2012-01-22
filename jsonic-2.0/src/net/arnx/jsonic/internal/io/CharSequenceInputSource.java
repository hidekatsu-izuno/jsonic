package net.arnx.jsonic.internal.io;

public class CharSequenceInputSource implements InputSource {
	private int lines = 1;
	private int columns = 1;
	private int offset = 0;
	
	private int mark = -1;
	
	private final CharSequence cs;
	
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
	
	public int mark() {
		mark = offset;
		return cs.length() - mark;
	}
	
	public void flush(StringBuilder sb, int len) {
		if (mark == -1) {
			throw new IllegalStateException("no mark");
		}
		sb.append(cs, mark, mark + len);
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