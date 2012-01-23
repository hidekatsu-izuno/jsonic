package net.arnx.jsonic.internal.parser;

import java.io.IOException;
import java.util.Locale;

import net.arnx.jsonic.internal.io.InputSource;

public class TraditionalJSONParser implements JSONParser {
	private static final int BEFORE_ROOT = 0;
	private static final int AFTER_ROOT = 1;
	private static final int BEFORE_NAME = 2;
	private static final int AFTER_NAME = 3;
	private static final int BEFORE_VALUE = 4;
	private static final int AFTER_VALUE = 5;
	
	private int state = BEFORE_ROOT;
	private InputSource in;
	private ParseContext context;
	private boolean emptyRoot = false;
	private long nameLineNumber = Long.MAX_VALUE;
	
	public TraditionalJSONParser(InputSource in, Locale locale, int maxDepth, boolean ignoreWhirespace) {
		this.in = in;
		this.context = new ParseContext(locale, maxDepth, ignoreWhirespace);
	}
	
	public JSONEventType next() throws IOException {
		do {
			context.set(null, null, false);
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
	
	public int getDepth() {
		return context.getDepth();
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
			if (!context.isIgnoreWhitespace()) {
				context.set(JSONEventType.WHITESPACE, ws, false);
			}
			return BEFORE_ROOT;
		case '/':
			in.back();
			String comment = context.parseComment(in);
			if (!context.isIgnoreWhitespace()) {
				context.set(JSONEventType.COMMENT, comment, false);
			}
			return BEFORE_ROOT;
		case '{':
			context.push(JSONEventType.START_OBJECT);
			return BEFORE_NAME;
		case '[':
			context.push(JSONEventType.START_ARRAY);
			return BEFORE_VALUE;
		default:
			if (n != -1) in.back();
			emptyRoot = true;
			context.push(JSONEventType.START_OBJECT);
			return BEFORE_NAME;
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
			if (!context.isIgnoreWhitespace()) {
				context.set(JSONEventType.WHITESPACE, ws, false);
			}
			return AFTER_ROOT;
		case '/':
			in.back();
			String comment = context.parseComment(in);
			if (!context.isIgnoreWhitespace()) {
				context.set(JSONEventType.COMMENT, comment, false);
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
			if (!context.isIgnoreWhitespace()) {
				context.set(JSONEventType.WHITESPACE, ws, false);
			}
			return BEFORE_NAME;
		case '/':
			in.back();
			String comment = context.parseComment(in);
			if (!context.isIgnoreWhitespace()) {
				context.set(JSONEventType.COMMENT, comment, false);
			}
			return BEFORE_NAME;
		case '"':
		case '\'':
			in.back();
			context.set(JSONEventType.NAME, context.parseString(in, true), false);
			return AFTER_NAME;
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
			context.set(JSONEventType.NAME, context.parseNumber(in), false);
			return AFTER_NAME;
		case '}':
			if (context.isFirst()) {
				context.pop();
				if (context.getBeginType() == null) {
					if (emptyRoot) {
						throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);
					} else {
						return AFTER_ROOT;
					}
				} else {
					nameLineNumber = in.getLineNumber();
					return AFTER_VALUE;							
				}
			} else {
				throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);
			}
		case -1:
			context.pop();
			if (context.getBeginType() == null) {
				if (emptyRoot) {
					return -1;
				} else {
					throw context.createParseException(in, "json.parse.ObjectNotClosedError");
				}
			} else {
				throw context.createParseException(in, "json.parse.ObjectNotClosedError");
			}
		default:
			in.back();
			context.set(JSONEventType.NAME, context.parseLiteral(in), false);
			return AFTER_NAME;
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
			if (!context.isIgnoreWhitespace()) {
				context.set(JSONEventType.WHITESPACE, ws, false);
			}
			return AFTER_NAME;
		case '/':
			in.back();
			String comment = context.parseComment(in);
			if (!context.isIgnoreWhitespace()) {
				context.set(JSONEventType.COMMENT, comment, false);
			}
			return AFTER_NAME;
		case ':':
		case '=':
			return BEFORE_VALUE;
		case '{':
		case '[':
			in.back();
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
			if (!context.isIgnoreWhitespace()) {
				context.set(JSONEventType.WHITESPACE, ws, false);
			}
			return BEFORE_VALUE;
		case '/':
			in.back();
			String comment = context.parseComment(in);
			if (!context.isIgnoreWhitespace()) {
				context.set(JSONEventType.COMMENT, comment, false);
			}
			return BEFORE_VALUE;
		case '{':
			context.push(JSONEventType.START_OBJECT);
			return BEFORE_NAME;
		case '[':
			context.push(JSONEventType.START_ARRAY);
			return BEFORE_VALUE;
		case '"':
		case '\'':
			in.back();
			context.set(JSONEventType.STRING, context.parseString(in, true), true);
			nameLineNumber = in.getLineNumber();
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
			nameLineNumber = in.getLineNumber();
			return AFTER_VALUE;
		case ',':
			if (context.getBeginType() == JSONEventType.START_OBJECT) {
				context.set(JSONEventType.NULL, null, true);
				return BEFORE_NAME;
			} else if (context.getBeginType() == JSONEventType.START_ARRAY) {
				context.set(JSONEventType.NULL, null, true);
				return BEFORE_VALUE;
			} else {
				throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);
			}
		case '}':
			if (context.getBeginType() == JSONEventType.START_OBJECT) {
				context.set(JSONEventType.NULL, null, true);
				in.back();
				return AFTER_VALUE;
			} else {
				throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);
			}
		case ']':
			if (context.getBeginType() == JSONEventType.START_ARRAY) {
				if (context.isFirst()) {
					context.pop();
					if (context.getBeginType() == null) {
						return AFTER_ROOT;
					} else {
						nameLineNumber = in.getLineNumber();
						return AFTER_VALUE;
					}
				} else {
					context.set(JSONEventType.NULL, null, true);
					in.back();
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
			in.back();
			Object literal = context.parseLiteral(in);
			context.set(context.getType(), literal, true);
			nameLineNumber = in.getLineNumber();
			return AFTER_VALUE;
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
			if (!context.isIgnoreWhitespace()) {
				context.set(JSONEventType.WHITESPACE, ws, false);
			}
			return AFTER_VALUE;
		case '/':
			in.back();
			String comment = context.parseComment(in);
			if (!context.isIgnoreWhitespace()) {
				context.set(JSONEventType.COMMENT, comment, false);
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
					if (emptyRoot) {
						throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);
					} else {
						return AFTER_ROOT;
					}
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
				context.pop();
				if (context.getBeginType() == null) {
					if (emptyRoot) {
						return -1;
					} else {
						throw context.createParseException(in, "json.parse.ObjectNotClosedError");
					}
				} else {
					throw context.createParseException(in, "json.parse.ObjectNotClosedError");
				}
			} else if (context.getBeginType() == JSONEventType.START_ARRAY) {
				throw context.createParseException(in, "json.parse.ArrayNotClosedError");
			} else {
				throw new IllegalStateException();
			}
		default:
			if (in.getLineNumber() > nameLineNumber) {
				in.back();
				nameLineNumber = Long.MAX_VALUE;
				if (context.getBeginType() == JSONEventType.START_OBJECT) {
					return BEFORE_NAME;
				} else if (context.getBeginType() == JSONEventType.START_ARRAY) {
					return BEFORE_VALUE;
				} else {
					throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);
				}
			} else {
				throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);
			}
		}
	}
}
