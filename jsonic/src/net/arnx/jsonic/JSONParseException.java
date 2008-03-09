package net.arnx.jsonic;

public class JSONParseException extends RuntimeException {
	private static final long serialVersionUID = -8323989588488596436L;
	
	ParserSource s;
	
	JSONParseException(String message, ParserSource s) {
		super("" + s.getLineNumber() + ": " + message + "\n" + s.toString() + " <- ?");
		this.s = s;
	}
	
	JSONParseException(String message, Throwable cause) {
		super(message, cause);
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
