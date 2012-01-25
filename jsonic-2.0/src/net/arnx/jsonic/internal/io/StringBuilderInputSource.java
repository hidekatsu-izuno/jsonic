package net.arnx.jsonic.internal.io;

import net.arnx.jsonic.internal.util.StringCache;

public class StringBuilderInputSource extends CharSequenceInputSource {
	private final StringBuilder sb;
	
	public StringBuilderInputSource(StringBuilder sb) {
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
