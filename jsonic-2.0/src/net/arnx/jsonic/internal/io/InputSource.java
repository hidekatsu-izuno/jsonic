package net.arnx.jsonic.internal.io;

import java.io.IOException;

import net.arnx.jsonic.internal.util.StringCache;


public interface InputSource {
	int next() throws IOException;
	void back();
	int mark();
	void copy(StringCache sc, int len);
	long getLineNumber();
	long getColumnNumber();
	long getOffset();
}
