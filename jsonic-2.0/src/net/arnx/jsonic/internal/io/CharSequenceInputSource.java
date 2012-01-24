package net.arnx.jsonic.internal.io;

public class CharSequenceInputSource implements InputSource {
	private int lines = 1;
	private int columns = 0;
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
	public int next() {
		int n = -1;
		if (offset < cs.length()) {
			n = cs.charAt(offset++);
			if (n == '\r') {
				lines++;
				columns = 0;
			} else if (n == '\n') {
				if (offset < 2 || cs.charAt(offset-2) != '\r') {
					lines++;
					columns = 0;
				}
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
		int max = Math.min(offset, cs.length()) - 1;
		int spos = 0;
		int charCount = 0;
		for (int i = 0; i <= max && i < 20; i++) {
			char c = cs.charAt(max - i);
			if (c == '\r' || (c == '\n' && (i + 1 > max || cs.charAt(max - (i + 1)) != '\r'))) {
				if (charCount > 0) break;
			} else if (c != '\n') {
				spos = max - i;
				charCount++;
			}
		}
		return (spos < offset) ? cs.subSequence(spos, offset).toString() : "";
	}
}