package net.arnx.jsonic.parse;

import java.io.IOException;

import net.arnx.jsonic.JSONEventType;
import net.arnx.jsonic.io.InputSource;

public class ScriptParser implements Parser {
	private static final int BEFORE_ROOT = 0;
	private static final int AFTER_ROOT = 1;
	private static final int BEFORE_NAME = 2;
	private static final int AFTER_NAME = 3;
	private static final int BEFORE_VALUE = 4;
	private static final int AFTER_VALUE = 5;
	
	private int state = BEFORE_ROOT;
	private InputSource in;
	private ParseContext context;
	
	public ScriptParser(InputSource in, ParseContext context) {
		this.in = in;
		this.context = context;
	}
	
	@Override
	public ParseContext getContext() {
		return context;
	}
	
	public JSONEventType next() throws IOException {
		JSONEventType type = null;
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
			
			if (context.getDepth() <= context.getMaxDepth()) {
				type = context.getType();
			}
		} while (state != -1 && type == null);
		
		return type;
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
		case '"':
		case '\'':
			in.back();
			context.set(JSONEventType.STRING, context.parseString(in, true), true);
			return AFTER_ROOT;
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
			return AFTER_ROOT;	
		case 't':
			in.back();
			context.set(JSONEventType.BOOLEAN, context.parseLiteral(in, "true", Boolean.TRUE), true);
			return AFTER_ROOT;
		case 'f':
			in.back();
			context.set(JSONEventType.BOOLEAN, context.parseLiteral(in, "false", Boolean.FALSE), true);
			return AFTER_ROOT;
		case 'n':
			in.back();
			context.set(JSONEventType.NULL, context.parseLiteral(in, "null", null), true);
			return AFTER_ROOT;
		case -1:
			if (context.isInterpretterMode()) {
				return -1;
			}
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
		case '{':
		case '[':
		case '"':
		case '\'':
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
		case 't':
		case 'f':
		case 'n':
			if (context.isInterpretterMode()) {
				in.back();
				return BEFORE_ROOT;
			}
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
			Object num = context.parseNumber(in);
			context.set(JSONEventType.NAME, (num != null) ? num.toString() : null, false);
			return AFTER_NAME;
		case '}':
			if (context.isFirst()) {
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
			in.back();
			context.set(JSONEventType.NAME, context.parseLiteral(in, false), false);
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
			context.set(JSONEventType.BOOLEAN, context.parseLiteral(in, "true", Boolean.TRUE), true);
			return AFTER_VALUE;
		case 'f':
			in.back();
			context.set(JSONEventType.BOOLEAN, context.parseLiteral(in, "false", Boolean.FALSE), true);
			return AFTER_VALUE;
		case 'n':
			in.back();
			context.set(JSONEventType.NULL, context.parseLiteral(in, "null", null), true);
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
