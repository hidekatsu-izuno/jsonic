package net.arnx.jsonic;

public class JSONParseException extends Exception {
	private static final long serialVersionUID = -8323989588488596436L;
	
	private JSONSource s;
	
	JSONParseException(String message, JSONSource s) {
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
