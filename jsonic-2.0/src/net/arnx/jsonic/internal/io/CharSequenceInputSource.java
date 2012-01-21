package net.arnx.jsonic.internal.io;

public class CharSequenceInputSource implements InputSource {
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