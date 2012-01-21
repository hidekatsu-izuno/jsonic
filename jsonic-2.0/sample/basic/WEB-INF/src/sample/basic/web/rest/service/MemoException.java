package sample.basic.web.rest.service;

public class MemoException extends RuntimeException {

	public MemoException() {
		super();
	}

	public MemoException(String message) {
		super(message);
	}

	public MemoException(Throwable t) {
		super(t);
	}

	public MemoException(String message, Throwable t) {
		super(message, t);
	}
	
	public String getExtensionProperty() {
		return "extension property";
	}

}
