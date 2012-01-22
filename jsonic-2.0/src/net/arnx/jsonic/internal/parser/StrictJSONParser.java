package net.arnx.jsonic.internal.parser;

import java.io.IOException;
import java.util.Locale;

import net.arnx.jsonic.internal.io.InputSource;

public class StrictJSONParser implements JSONParser {
	private static final int BEFORE_ROOT = 0;
	private static final int AFTER_ROOT = 1;
	private static final int BEFORE_NAME = 2;
	private static final int AFTER_NAME = 3;
	private static final int BEFORE_VALUE = 4;
	private static final int AFTER_VALUE = 5;
	
	private int state = BEFORE_ROOT;
	private InputSource in;
	private ParseContext context;
	
	public StrictJSONParser(InputSource in, Locale locale, int maxDepth) {
		this.in = in;
		this.context = new ParseContext(locale, maxDepth);
	}
	
	public JSONEventType next() throws IOException {
		do {
			context.set(null, ParseContext.EMPTY);
			switch (state) {
			case BEFORE_ROOT:
				state = beforeRoot();
				break;
			case AFTER_ROOT:
				state = afterRoot();
				break;
			case BEFORE_NAME:
				state = beforeName();
				break;
			case AFTER_NAME:
				state = afterName();
				break;
			case BEFORE_VALUE:
				state = beforeValue();
				break;
			case AFTER_VALUE:
				state = afterValue();
				break;
			}
			if (state == -1) return null;
		} while (context.getType() == null);
		
		return context.getType();
	}
	
	public Object getValue() {
		return context.getValue();
	}
	
	private int beforeRoot() throws IOException {
		int n = in.next(true);
		if (n == '{') {
			context.push(JSONEventType.BEGIN_OBJECT);
			return BEFORE_NAME;
		} else if (n == '[') {
			context.push(JSONEventType.BEGIN_ARRAY);
			return BEFORE_VALUE;
		} else if (n != -1) {
			throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);
		} else {
			throw context.createParseException(in, "json.parse.EmptyInputError");
		}
	}
	
	private int afterRoot() throws IOException {
		int n = in.next(true);
		if (n != -1) {
			throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);
		}
		return -1;
	}
	
	private int beforeName() throws IOException {
		int n = in.next(true);
		if (n == '"') {
			in.back();
			context.set(JSONEventType.NAME, context.parseString(in));
			return AFTER_NAME;					
		} else if (n == '}' && context.getPrevType() == JSONEventType.BEGIN_OBJECT) {
			context.pop();
			if (context.getBeginType() == null) {
				return AFTER_ROOT;
			} else {
				return AFTER_VALUE;							
			}
		} else if (n != -1) {
			throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);
		} else {
			throw context.createParseException(in, "json.parse.ObjectNotClosedError");
		}
	}

	private int afterName() throws IOException {
		int n = in.next(true);
		if (n == ':') {
			return BEFORE_VALUE;
		} else if (n != -1) {
			throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);
		} else {
			throw context.createParseException(in, "json.parse.ObjectNotClosedError");
		}
	}
	
	private int beforeValue() throws IOException {
		int n = in.next(true);
		if (n != -1) {
			switch((char)n) {
			case '{':
				context.push(JSONEventType.BEGIN_OBJECT);
				return BEFORE_NAME;
			case '[':
				context.push(JSONEventType.BEGIN_ARRAY);
				return BEFORE_VALUE;
			case '"':
				in.back();
				context.set(JSONEventType.STRING, context.parseString(in));
				return AFTER_VALUE;
			case '-':
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				in.back();
				context.set(JSONEventType.NUMBER, context.parseNumber(in));
				return AFTER_VALUE;	
			case 't':
				in.back();
				context.set(JSONEventType.TRUE, context.parseLiteral(in, "true", Boolean.TRUE));
				return AFTER_VALUE;
			case 'f':
				in.back();
				context.set(JSONEventType.FALSE, context.parseLiteral(in, "false", Boolean.FALSE));
				return AFTER_VALUE;
			case 'n':
				in.back();
				context.set(JSONEventType.NULL, context.parseLiteral(in, "null", null));
				return AFTER_VALUE;
			case ']':
				if (context.getPrevType() == JSONEventType.BEGIN_ARRAY) {
					context.pop();
					if (context.getBeginType() == null) {
						return AFTER_ROOT;
					} else {
						return AFTER_VALUE;							
					}
				}
			default:
				throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);
			}
		} else {
			switch (context.getBeginType()) {
			case BEGIN_OBJECT:
				throw context.createParseException(in, "json.parse.ObjectNotClosedError");
			case BEGIN_ARRAY:
				throw context.createParseException(in, "json.parse.ArrayNotClosedError");
			default:
				throw new IllegalStateException();
			}
		}

	}
	
	private int afterValue() throws IOException {
		int n = in.next(true);
		if (n == ',') {
			if (context.getBeginType() == JSONEventType.BEGIN_OBJECT) {
				return BEFORE_NAME;
			} else if (context.getBeginType() == JSONEventType.BEGIN_ARRAY) {
				return BEFORE_VALUE;
			} else {
				throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);						
			}
		} else if (n == '}') {
			if (context.getBeginType() == JSONEventType.BEGIN_OBJECT) {
				context.pop();
				if (context.getBeginType() == null) {
					return AFTER_ROOT;
				} else {
					return AFTER_VALUE;							
				}
			} else {
				throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);						
			}
		} else if (n == ']') {
			if (context.getBeginType() == JSONEventType.BEGIN_ARRAY) {
				context.pop();
				if (context.getBeginType() == null) {
					return AFTER_ROOT;
				} else {
					return AFTER_VALUE;							
				}
			} else {
				throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);						
			}
		} else if (n != -1) {
			throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);
		} else {
			switch (context.getBeginType()) {
			case BEGIN_OBJECT:
				throw context.createParseException(in, "json.parse.ObjectNotClosedError");
			case BEGIN_ARRAY:
				throw context.createParseException(in, "json.parse.ArrayNotClosedError");
			default:
				throw new IllegalStateException();
			}
		}
	}
}
