package net.arnx.jsonic.internal.io;

import java.io.IOException;

public interface InputSource {
	int next() throws IOException;
	void back();
	int mark();
	void copy(StringBuilder sb, int len);
	long getLineNumber();
	long getColumnNumber();
	long getOffset();
}
