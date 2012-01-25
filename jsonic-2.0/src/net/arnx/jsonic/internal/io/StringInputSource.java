package net.arnx.jsonic.internal.io;

import net.arnx.jsonic.internal.util.StringCache;

public class StringInputSource extends CharSequenceInputSource {
	private final String str;
	
	public StringInputSource(String str) {
		super(str);
		this.str = str;
	}
	
	@Override
	public void copy(StringCache sc, int len) {
		if (mark == -1) {
			throw new IllegalStateException("no mark");
		}
		sc.append(str, mark, mark + len);
	}
}
