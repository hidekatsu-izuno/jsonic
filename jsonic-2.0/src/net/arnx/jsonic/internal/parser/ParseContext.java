package net.arnx.jsonic.internal.parser;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.Locale;
import java.util.ResourceBundle;

import net.arnx.jsonic.JSONException;
import net.arnx.jsonic.internal.io.InputSource;

class ParseContext {
	public static final Object EMPTY = new Object();
	
	InputSource in;
	Locale locale;
	int maxDepth;
	
	private LinkedList<JSONEventType> stack = new LinkedList<JSONEventType>();
	private StringBuilder builderCache;
	
	private JSONEventType prevType;
	private JSONEventType type;
	private Object value;
	
	public ParseContext(InputSource in, Locale locale, int maxDepth) {
		this.in = in;
		this.locale = locale;
		this.maxDepth = maxDepth;
	}
	
	public int next() throws IOException {
		return in.next();
	}
	
	public void back() {
		in.back();
	}
	
	public Locale getLocale() {
		return locale;
	}
	
	public int getMaxDepth() {
		return maxDepth;
	}
	
	public int getDepth() {
		return stack.size();
	}
	
	public void push(JSONEventType type) {
		this.prevType = this.type;
		this.type = type;
		stack.add(type);
	}
	
	public void set(JSONEventType type, Object value) {
		this.prevType = this.type;
		this.type = type;
		this.value = value;
	}
	
	public void pop() {
		this.prevType = this.type;
		JSONEventType beginType = stack.removeLast();
		if (beginType == JSONEventType.BEGIN_OBJECT) {
			this.type = JSONEventType.END_OBJECT;
		} else if (beginType == JSONEventType.BEGIN_ARRAY) {
			this.type = JSONEventType.END_ARRAY;
		} else {
			throw new IllegalStateException();
		}
	}
	
	public JSONEventType getBeginType() {
		return (!stack.isEmpty()) ? stack.getLast() : null;
	}
	
	public JSONEventType getPrevType() {
		return prevType;
	}
	
	public JSONEventType getType() {
		return type;
	}
	
	public Object getValue() {
		return value;
	}
	
	public StringBuilder getCachedBuffer() {
		if (builderCache == null) {
			builderCache = new StringBuilder();
		} else {
			builderCache.setLength(0);
		}
		return builderCache;
	}
	
	public JSONException createParseException(String id) {
		return createParseException(id, (Object[])null);
	}
	
	public JSONException createParseException(String id, Object... args) {
		ResourceBundle bundle = ResourceBundle.getBundle("net.arnx.jsonic.Messages", locale);
		
		String message;
		if (args != null && args.length > 0) {
			message = MessageFormat.format(bundle.getString(id), args);
		} else {
			message = bundle.getString(id);
		}
		
		return new JSONException("" + in.getLineNumber() + ": " + message + "\n" + in.toString() + " <- ?",
				JSONException.PARSE_ERROR, in.getLineNumber(), in.getColumnNumber(), in.getOffset());
	}
}
