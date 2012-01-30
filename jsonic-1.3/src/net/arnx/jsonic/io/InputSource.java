package net.arnx.jsonic.io;

import java.io.IOException;

import net.arnx.jsonic.util.ValueCache;

public interface InputSource {
	int next() throws IOException;
	void back();
	
	long getLineNumber();
	long getColumnNumber();
	long getOffset();
	
	int mark();
	void copy(StringBuilder sb, int len);
	String copy(ValueCache cache, int len);
}
