package net.arnx.jsonic;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.arnx.jsonic.internal.io.InputSource;
import net.arnx.jsonic.internal.parser.JSONParser;
import net.arnx.jsonic.internal.parser.ParseContext;
import net.arnx.jsonic.internal.parser.ScriptJSONParser;
import net.arnx.jsonic.internal.parser.StrictJSONParser;
import net.arnx.jsonic.internal.parser.TraditionalJSONParser;

public class JSONReader {
	private final boolean suppressNull;
	
	private JSONParser parser;
	private JSONEventType type;
	
	JSONReader(JSONMode mode, InputSource in, Locale locale, int maxDepth, boolean suppressNull, boolean ignoreWhitespace) {
		this.suppressNull = suppressNull;
		
		ParseContext context = new ParseContext(locale, maxDepth, ignoreWhitespace);
		switch (mode) {
		case STRICT:
			parser = new StrictJSONParser(in, context);
			break;
		case SCRIPT:
			parser = new ScriptJSONParser(in, context);
			break;
		default:
			parser = new TraditionalJSONParser(in, context);
		}
	}
	
	public JSONEventType next() throws IOException {
		type = parser.next();
		return type;
	}
	
	public Map<?, ?> getObject() throws IOException {
		if (type == null) type = next();
		
		if (type == JSONEventType.START_OBJECT) {
			return (Map<?, ?>)getValue();
		} else {
			throw new IllegalStateException();
		}
	}
	
	public List<?> getArray() throws IOException {
		if (type == null) type = next();

		if (type == JSONEventType.START_ARRAY) {
			return (List<?>)getValue();
		} else {
			throw new IllegalStateException();
		}
	}
	
	public String getString() throws IOException {
		if (type == null) type = next();

		if (type == JSONEventType.STRING) {
			return (String)getValue();
		} else {
			throw new IllegalStateException();
		}
	}
	
	public BigDecimal getNumber() throws IOException {
		if (type == null) type = next();

		if (type == JSONEventType.NUMBER) {
			return (BigDecimal)getValue();
		} else {
			throw new IllegalStateException();
		}
	}
	
	public Boolean getBoolean() throws IOException {
		if (type == null) type = next();

		if (type == JSONEventType.BOOLEAN) {
			return (Boolean)getValue();
		} else {
			throw new IllegalStateException();
		}
	}
	
	@SuppressWarnings("unchecked")
	Object getValue() throws IOException {
		if (type == null) type = next();
		
		List<Object> stack = null;
		Object name = null;

		do {
			switch (type) {
			case START_OBJECT:
				Map<Object, Object> map = new LinkedHashMap<Object, Object>();
				if (stack == null) stack = new ArrayList<Object>();
				if (!stack.isEmpty()) {
					Object current = stack.get(stack.size()-1);
					if (current instanceof Map<?, ?>) {
						if (!(map == null && suppressNull)) {
							((Map<Object, Object>)current).put(name, map);
						}
					} else if (current instanceof List<?>) {
						((List<Object>)current).add(map);
					}
				}
				stack.add(map);
				break;
			case START_ARRAY:
				List<Object> list = new ArrayList<Object>();
				if (stack == null) stack = new ArrayList<Object>();
				if (!stack.isEmpty()) {
					Object current = stack.get(stack.size()-1);
					if (current instanceof Map<?, ?>) {
						if (!(list == null && suppressNull)) {
							((Map<Object, Object>)current).put(name, list);
						}
					} else if (current instanceof List<?>) {
						((List<Object>)current).add(list);
					}
				}
				stack.add(list);
				break;
			case END_ARRAY:
			case END_OBJECT:
				if (stack.size() > 1) {
					stack.remove(stack.size()-1);
				} else if (stack.size() == 1) {
					return stack.get(0);
				} else {
					throw new IllegalStateException();
				}
				break;	
			case NAME:
				if (stack != null && !stack.isEmpty()) {
					name = parser.getValue();
				} else {
					return parser.getValue();
				}
				break;
			case STRING:
			case NUMBER:
			case BOOLEAN:
			case NULL:
				if (stack != null && !stack.isEmpty()) {
					Object current = stack.get(stack.size()-1);
					if (current instanceof Map<?, ?>) {
						Object value = parser.getValue();
						if (!(value == null && suppressNull)) {
							((Map<Object, Object>)current).put(name, value);
						}
					} else if (current instanceof List<?>) {
						((List<Object>)current).add(parser.getValue());
					}
				} else {
					return parser.getValue();
				}
				break;
			}
		} while ((type = parser.next()) != null);
		
		throw new IllegalStateException();
	}
	
	public int getDepth() {
		return parser.getDepth();
	}
}
