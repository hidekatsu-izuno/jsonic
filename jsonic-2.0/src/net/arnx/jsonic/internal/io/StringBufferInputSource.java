package net.arnx.jsonic.internal.io;

import net.arnx.jsonic.internal.util.StringCache;

public class StringBufferInputSource extends CharSequenceInputSource {
	private final StringBuffer sb;
	
	public StringBufferInputSource(StringBuffer sb) {
		super(sb);
		this.sb = sb;
	}
	
	@Override
	public void copy(StringCache sc, int len) {
		if (mark == -1) {
			throw new IllegalStateException("no mark");
		}
		sc.append(sb, mark, mark + len);
	}
}
