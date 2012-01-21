package net.arnx.jsonic.io;

import java.io.IOException;

public interface InputSource {
	int next() throws IOException;
	void back();
	long getLineNumber();
	long getColumnNumber();
	long getOffset();
}
