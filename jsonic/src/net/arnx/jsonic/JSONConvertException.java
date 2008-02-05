package net.arnx.jsonic;

public class JSONConvertException extends Exception {
	private static final long serialVersionUID = -6173125387096087580L;
	
	JSONConvertException(String message) {
		super(message);
	}
	
	JSONConvertException(String message, Throwable cause) {
		super(message, cause);
	}
}
