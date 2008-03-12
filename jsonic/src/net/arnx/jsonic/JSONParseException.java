package net.arnx.jsonic;

public class JSONParseException extends RuntimeException {
	private static final long serialVersionUID = -8323989588488596436L;

	private long lineNumber = -1l;
	private long columnNumber = -1l;
	private long offset = -1l;
	
	JSONParseException(String message, long lineNumber, long columnNumber, long offset) {
		super(message);
		this.lineNumber = lineNumber;
		this.columnNumber = columnNumber;
		this.offset = offset;
	}
	
	JSONParseException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public long getLineNumber() {
		return lineNumber;
	}
	
	public long getColumnNumber() {
		return columnNumber;
	}
	
	public long getErrorOffset() {
		return offset;
	}
}
