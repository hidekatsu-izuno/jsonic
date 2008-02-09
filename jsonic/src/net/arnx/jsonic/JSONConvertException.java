package net.arnx.jsonic;

public class JSONConvertException extends Exception {
	private static final long serialVersionUID = -6173125387096087580L;
	
	public JSONConvertException(String message) {
		super(message);
	}
	
	public JSONConvertException(Throwable cause) {
		super(cause);
	}
	
	public JSONConvertException(String message, Throwable cause) {
		super(message, cause);
	}
}
