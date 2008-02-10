package net.arnx.jsonic;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class JSONConvertException extends RuntimeException {
	private static final long serialVersionUID = -6173125387096087580L;
	private List<String> keys = new ArrayList<String>();
	
	JSONConvertException(String message) {
		super(message);
	}
	
	JSONConvertException(String message, Throwable cause) {
		super(message, cause);
	}
	
	void push(Object key) {
		if (key == null) {
			keys.add("$");
		} else if (key instanceof Number) {
			keys.add("[" + key + "]");
		} else {
			keys.add("." + key);
		}
	}
	
	@Override
	public String getMessage() {
		StringBuilder key = new StringBuilder();
		for (int i = 0; i < keys.size(); i++) {
			key.append(keys.get(keys.size()-i-1));
		}
		return MessageFormat.format(super.getMessage(), key.toString());
	}
}
