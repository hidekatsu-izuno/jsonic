package net.arnx.jsonic.io;

import net.arnx.jsonic.util.ValueCache;

public class StringBufferInputSource extends CharSequenceInputSource {
	private final StringBuffer sb;
	
	public StringBufferInputSource(StringBuffer sb) {
		super(sb);
		this.sb = sb;
	}
	
	@Override
	public void copy(StringBuilder sb, int len) {
		if (mark == -1) throw new IllegalStateException("no mark");
		if (mark + len > this.sb.length()) throw new IndexOutOfBoundsException();
		
		sb.append(this.sb, mark, mark + len);
	}
	
	@Override
	public String copy(ValueCache cache, int len) {
		if (mark == -1) throw new IllegalStateException("no mark");
		if (mark + len > sb.length()) throw new IndexOutOfBoundsException();
		
		return cache.getString(sb, mark, len);
	}
}
