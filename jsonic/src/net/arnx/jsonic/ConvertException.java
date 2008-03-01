package net.arnx.jsonic;

import java.util.ArrayList;
import java.util.List;

public class ConvertException extends RuntimeException {
	private static final long serialVersionUID = -6173125387096087580L;
	private List<Object> keys = new ArrayList<Object>();
	
	ConvertException(String message, Object key, Throwable cause) {
		super(message, cause);
		add(key);
	}
	
	void add(Object key) {
		keys.add(key);
	}

	@Override
	public String getMessage() {
		StringBuilder sb = new StringBuilder(super.getMessage());
		for (int i = 0; i < keys.size(); i++) {
			Object key = keys.get(keys.size()-i-1);
			if (key instanceof Number) {
				sb.append('[').append(key).append(']');
			} else {
				if (i != 0) sb.append('.');
				sb.append(key);
			}
		}
		return sb.toString();
	}
}
