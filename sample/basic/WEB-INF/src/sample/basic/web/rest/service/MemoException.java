package sample.basic.web.rest.service;

import net.arnx.jsonic.JSONHint;

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

	@JSONHint(ignore=true)
	public String getIgnoreProperty() {
		return "ignore property";
	}

	@JSONHint(name="rename")
	public String getRenameProperty() {
		return "rename property";
	}
}
