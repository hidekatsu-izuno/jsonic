package net.arnx.jsonic;

import java.io.IOException;

public class ParseException extends RuntimeException {
	private static final long serialVersionUID = -8323989588488596436L;
	
	private ParserSource s;
	
	ParseException(IOException e, ParserSource s) {
		super("IOError: " + e.getMessage(), e);
		this.s = s;
	}
	
	ParseException(String message, ParserSource s) {
		super("" + s.getLineNumber() + ": " + message + "\n" + s.toString() + " <- ?");
		this.s = s;
	}
	
	public long getLineNumber() {
		return s.getLineNumber();
	}
	
	public long getColumnNumber() {
		return s.getColumnNumber();
	}
	
	public long getErrorOffset() {
		return s.getOffset();
	}
}
