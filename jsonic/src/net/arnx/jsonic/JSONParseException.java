package net.arnx.jsonic;

public class JSONParseException extends RuntimeException {
	private static final long serialVersionUID = -8323989588488596436L;
	
	private JSON.JSONSource s;
	
	JSONParseException(String message, JSON.JSONSource s) {
		super(createMessage(message, s));
		this.s = s;
	}
	
	static String createMessage(String message, JSON.JSONSource s) {
		StringBuilder sb = new StringBuilder();
		sb.append('[').append(s.getLines()).append(',').append(s.getColumns()).append(']');
		sb.append(' ').append(message).append('\n');
		sb.append(s.toString()).append(" <- ?");
		
		return sb.toString();
	}
	
	public long getLines() {
		return s.getLines();
	}
	
	public long getColumns() {
		return s.getColumns();
	}
}