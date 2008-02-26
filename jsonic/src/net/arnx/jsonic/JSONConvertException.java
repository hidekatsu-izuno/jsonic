package net.arnx.jsonic;

public class JSONConvertException extends RuntimeException {
	private static final long serialVersionUID = -6173125387096087580L;

	JSONConvertException() {
		super();
	}
	
	JSONConvertException(String message) {
		super(message);
	}
	
	JSONConvertException(Throwable cause) {
		super(cause);
	}
	
	JSONConvertException(String message, Throwable cause) {
		super(message, cause);
	}
}
