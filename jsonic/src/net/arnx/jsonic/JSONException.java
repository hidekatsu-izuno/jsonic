package net.arnx.jsonic;

public class JSONException extends Exception {
	private static final long serialVersionUID = 551341051680722132L;

	public JSONException() {
		super();
	}
	
	public JSONException(String message) {
		super(message);
	}
	
	public JSONException(Throwable cause) {
		super(cause);
	}
	
	public JSONException(String message, Throwable cause) {
		super(message, cause);
	}
}
