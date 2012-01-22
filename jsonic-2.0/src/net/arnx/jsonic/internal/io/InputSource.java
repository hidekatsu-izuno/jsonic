package net.arnx.jsonic.internal.io;

import java.io.IOException;


public interface InputSource {
	int skip() throws IOException;
	int next() throws IOException;
	void back();
	int mark();
	void copy(StringCache sc, int len);
	long getLineNumber();
	long getColumnNumber();
	long getOffset();
}
