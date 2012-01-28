package net.arnx.jsonic;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.io.InputSource;
import net.arnx.jsonic.parser.Parser;
import net.arnx.jsonic.parser.ParseContext;
import net.arnx.jsonic.parser.ScriptParser;
import net.arnx.jsonic.parser.StrictParser;
import net.arnx.jsonic.parser.TraditionalParser;

public class JSONReader {
	private Context context;
	private Parser parser;
	private JSONEventType type;
	
	JSONReader(Context context, InputSource in, boolean ignoreWhitespace) {
		this.context = context;
		
		ParseContext pcontext = new ParseContext(context, ignoreWhitespace);
		switch (context.getMode()) {
		case STRICT:
			parser = new StrictParser(in, pcontext);
			break;
		case SCRIPT:
			parser = new ScriptParser(in, pcontext);
			break;
		default:
			parser = new TraditionalParser(in, pcontext);
		}
	}
	
	public JSONEventType next() throws IOException {
		type = parser.next();
		return type;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getValue(Class<T> cls) throws IOException {
		return (T)context.convertInternal(getValue(), cls);
	}
	
	public Object getValue(Type t) throws IOException {
		return context.convertInternal(getValue(), t);
	}
	
	public Map<?, ?> getObject() throws IOException {
		if (type == JSONEventType.START_OBJECT) {
			return (Map<?, ?>)getValue();
		} else {
			throw new IllegalStateException();
		}
	}
	
	public List<?> getArray() throws IOException {
		if (type == JSONEventType.START_ARRAY) {
			return (List<?>)getValue();
		} else {
			throw new IllegalStateException();
		}
	}
	
	public String getString() throws IOException {
		if (type == JSONEventType.STRING) {
			return (String)parser.getValue();
		} else {
			throw new IllegalStateException();
		}
	}
	
	public BigDecimal getNumber() throws IOException {
		if (type == JSONEventType.NUMBER) {
			return (BigDecimal)parser.getValue();
		} else {
			throw new IllegalStateException();
		}
	}
	
	public Boolean getBoolean() throws IOException {
		if (type == JSONEventType.BOOLEAN) {
			return (Boolean)parser.getValue();
		} else {
			throw new IllegalStateException();
		}
	}
	
	public String getComment() throws IOException {
		if (type == JSONEventType.COMMENT) {
			return (String)parser.getValue();
		} else {
			throw new IllegalStateException();
		}
	}
	
	public String getWhitespace() throws IOException {
		if (type == JSONEventType.WHITESPACE) {
			return (String)parser.getValue();
		} else {
			throw new IllegalStateException();
		}
	}
	
	@SuppressWarnings("unchecked")
	Object getValue() throws IOException {
		if (type == null) {
			throw new IllegalStateException("you should call next.");
		}
		
		Object root = null;
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
						if (!(map == null && context.isSuppressNull())) {
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
						if (!(list == null && context.isSuppressNull())) {
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
					if (parser.getDepth() > 1) {
						return stack.remove(0);
					} else {
						root = stack.remove(0);
					}
				} else {
					throw new IllegalStateException();
				}
				break;	
			case NAME:
				name = parser.getValue();
				break;
			case STRING:
			case NUMBER:
			case BOOLEAN:
			case NULL:
				if (stack != null) {
					Object current = stack.get(stack.size()-1);
					if (current instanceof Map<?, ?>) {
						Object value = parser.getValue();
						if (!(value == null && context.isSuppressNull())) {
							((Map<Object, Object>)current).put(name, value);
						}
					} else if (current instanceof List<?>) {
						((List<Object>)current).add(parser.getValue());
					}
				} else {
					if (parser.getDepth() > 1) {
						return parser.getValue();
					} else {
						root = parser.getValue();
					}
				}
				break;
			}
		} while ((type = parser.next()) != null);
		
		return root;
	}
	
	public int getDepth() {
		return parser.getDepth();
	}
}
