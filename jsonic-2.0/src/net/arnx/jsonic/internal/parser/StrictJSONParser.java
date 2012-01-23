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
	
	public StrictJSONParser(InputSource in, Locale locale, int maxDepth, boolean skipWhitespace) {
		this.in = in;
		this.context = new ParseContext(locale, maxDepth, skipWhitespace, true);
	}
	
	public JSONEventType next() throws IOException {
		do {
			context.set(null, ParseContext.EMPTY, false);
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
		int n = in.next();
		if (n == 0xFEFF) n = in.next();
		switch (n) {
		case ' ':
		case '\t':
		case '\r':
		case '\n':
			in.back();
			String ws = context.parseWhitespace(in);
			if (!context.isSkipWhitespace()) {
				context.set(JSONEventType.WHITESPACE, ws, false);
			}
			return BEFORE_ROOT;
		case '{':
			context.push(JSONEventType.START_OBJECT);
			return BEFORE_NAME;
		case '[':
			context.push(JSONEventType.START_ARRAY);
			return BEFORE_VALUE;
		case -1:
			throw context.createParseException(in, "json.parse.EmptyInputError");
		default:
			throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);
		}
	}
	
	private int afterRoot() throws IOException {
		int n = in.next();
		switch (n) {
		case ' ':
		case '\t':
		case '\r':
		case '\n':
			in.back();
			String ws = context.parseWhitespace(in);
			if (!context.isSkipWhitespace()) {
				context.set(JSONEventType.WHITESPACE, ws, false);
			}
			return AFTER_ROOT;
		case -1:
			return -1;
		default:
			throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);
		}
	}
	
	private int beforeName() throws IOException {
		int n = in.next();
		switch (n) {
		case ' ':
		case '\t':
		case '\r':
		case '\n':
			in.back();
			String ws = context.parseWhitespace(in);
			if (!context.isSkipWhitespace()) {
				context.set(JSONEventType.WHITESPACE, ws, false);
			}
			return BEFORE_NAME;
		case '"':
			in.back();
			context.set(JSONEventType.NAME, context.parseString(in), false);
			return AFTER_NAME;
		case '}':
			if (context.isFirst() && context.getBeginType() == JSONEventType.START_OBJECT) {
				context.pop();
				if (context.getBeginType() == null) {
					return AFTER_ROOT;
				} else {
					return AFTER_VALUE;							
				}
			} else {
				throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);
			}
		case -1:
			throw context.createParseException(in, "json.parse.ObjectNotClosedError");
		default:
			throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);
		}
	}

	private int afterName() throws IOException {
		int n = in.next();
		switch (n) {
		case ' ':
		case '\t':
		case '\r':
		case '\n':
			in.back();
			String ws = context.parseWhitespace(in);
			if (!context.isSkipWhitespace()) {
				context.set(JSONEventType.WHITESPACE, ws, false);
			}
			return AFTER_NAME;
		case ':':
			return BEFORE_VALUE;
		case -1:
			throw context.createParseException(in, "json.parse.ObjectNotClosedError");
		default:
			throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);
		}
	}
	
	private int beforeValue() throws IOException {
		int n = in.next();
		switch (n) {
		case ' ':
		case '\t':
		case '\r':
		case '\n':
			in.back();
			String ws = context.parseWhitespace(in);
			if (!context.isSkipWhitespace()) {
				context.set(JSONEventType.WHITESPACE, ws, false);
			}
			return BEFORE_VALUE;
		case '{':
			context.push(JSONEventType.START_OBJECT);
			return BEFORE_NAME;
		case '[':
			context.push(JSONEventType.START_ARRAY);
			return BEFORE_VALUE;
		case '"':
			in.back();
			context.set(JSONEventType.STRING, context.parseString(in), true);
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
			context.set(JSONEventType.NUMBER, context.parseNumber(in), true);
			return AFTER_VALUE;	
		case 't':
			in.back();
			context.set(JSONEventType.TRUE, context.parseLiteral(in, "true", Boolean.TRUE, false), true);
			return AFTER_VALUE;
		case 'f':
			in.back();
			context.set(JSONEventType.FALSE, context.parseLiteral(in, "false", Boolean.FALSE, false), true);
			return AFTER_VALUE;
		case 'n':
			in.back();
			context.set(JSONEventType.NULL, context.parseLiteral(in, "null", null, false), true);
			return AFTER_VALUE;
		case ']':
			if (context.isFirst() && context.getBeginType() == JSONEventType.START_ARRAY) {
				context.pop();
				if (context.getBeginType() == null) {
					return AFTER_ROOT;
				} else {
					return AFTER_VALUE;							
				}
			} else{
				throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);
			}
		case -1:
			if (context.getBeginType() == JSONEventType.START_OBJECT) {
				throw context.createParseException(in, "json.parse.ObjectNotClosedError");
			} else if (context.getBeginType() == JSONEventType.START_ARRAY) {
				throw context.createParseException(in, "json.parse.ArrayNotClosedError");
			} else {
				throw new IllegalStateException();
			}
		default:
			throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);
		}
	}
	
	private int afterValue() throws IOException {
		int n = in.next();
		switch (n) {
		case ' ':
		case '\t':
		case '\r':
		case '\n':
			in.back();
			String ws = context.parseWhitespace(in);
			if (!context.isSkipWhitespace()) {
				context.set(JSONEventType.WHITESPACE, ws, false);
			}
			return AFTER_VALUE;
		case ',':
			if (context.getBeginType() == JSONEventType.START_OBJECT) {
				return BEFORE_NAME;
			} else if (context.getBeginType() == JSONEventType.START_ARRAY) {
				return BEFORE_VALUE;
			} else {
				throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);						
			}
		case '}':
			if (context.getBeginType() == JSONEventType.START_OBJECT) {
				context.pop();
				if (context.getBeginType() == null) {
					return AFTER_ROOT;
				} else {
					return AFTER_VALUE;							
				}
			} else {
				throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);						
			}
		case ']':
			if (context.getBeginType() == JSONEventType.START_ARRAY) {
				context.pop();
				if (context.getBeginType() == null) {
					return AFTER_ROOT;
				} else {
					return AFTER_VALUE;							
				}
			} else {
				throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);						
			}
		case -1:
			if (context.getBeginType() == JSONEventType.START_OBJECT) {
				throw context.createParseException(in, "json.parse.ObjectNotClosedError");
			} else if (context.getBeginType() == JSONEventType.START_ARRAY) {
				throw context.createParseException(in, "json.parse.ArrayNotClosedError");
			} else {
				throw new IllegalStateException();
			}
		default:
			throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);
		}
	}
}
