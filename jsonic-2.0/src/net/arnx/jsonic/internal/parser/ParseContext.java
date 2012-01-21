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
	
	private LinkedList<TokenType> stack = new LinkedList<TokenType>();
	private StringBuilder builderCache;
	
	private TokenType type;
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
	
	public void push(TokenType type) {
		this.type = type;
		stack.push(type);
	}
	
	public void set(TokenType type, Object value) {
		this.type = type;
		this.value = value;
	}
	
	public void pop() {
		TokenType beginType = stack.removeLast();
		if (beginType == TokenType.BEGIN_OBJECT) {
			type = TokenType.END_OBJECT;
		} else if (beginType == TokenType.BEGIN_ARRAY) {
			type = TokenType.END_ARRAY;
		} else {
			throw new IllegalStateException();
		}
	}
	
	public TokenType getBeginType() {
		return (!stack.isEmpty()) ? stack.getLast() : null;
	}
	
	public TokenType getType() {
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
