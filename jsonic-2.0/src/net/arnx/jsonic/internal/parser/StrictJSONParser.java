package net.arnx.jsonic.internal.parser;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Locale;

import net.arnx.jsonic.internal.io.InputSource;
import net.arnx.jsonic.internal.io.StringCache;

public class StrictJSONParser implements JSONParser {
	private static final int BEFORE_ROOT = 0;
	private static final int AFTER_ROOT = 1;
	private static final int BEFORE_NAME = 2;
	private static final int AFTER_NAME = 3;
	private static final int BEFORE_VALUE = 4;
	private static final int AFTER_VALUE = 5;
	
	private static final int[] ESCAPE_CHARS = new int[128];
	
	static {
		for (int i = 0; i < 32; i++) {
			ESCAPE_CHARS[i] = 1;
		}
		ESCAPE_CHARS['"'] = 2;
		ESCAPE_CHARS['\\'] = 3;
		ESCAPE_CHARS[0x7F] = 1;
	}
	
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
				state = beforeRoot(in, context);
				break;
			case AFTER_ROOT:
				state = afterRoot(in, context);
				break;
			case BEFORE_NAME:
				state = beforeName(in, context);
				break;
			case AFTER_NAME:
				state = afterName(in, context);
				break;
			case BEFORE_VALUE:
				state = beforeValue(in, context);
				break;
			case AFTER_VALUE:
				state = afterValue(in, context);
				break;
			}
			if (state == -1) return null;
		} while (context.getType() == null);
		
		return context.getType();
	}
	
	public Object getValue() {
		return context.getValue();
	}
	
	private static int beforeRoot(InputSource in, ParseContext context) throws IOException {
		int n = skip(in, context);
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
	
	private static int afterRoot(InputSource in, ParseContext context) throws IOException {
		int n = skip(in, context);
		if (n != -1) {
			throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);
		}
		return -1;
	}
	
	private static int beforeName(InputSource in, ParseContext context) throws IOException {
		int n = skip(in, context);
		if (n == '"') {
			in.back();
			context.set(JSONEventType.NAME, parseString(in, context));
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

	private static int afterName(InputSource in, ParseContext context) throws IOException {
		int n = skip(in, context);
		if (n == ':') {
			return BEFORE_VALUE;
		} else if (n != -1) {
			throw context.createParseException(in, "json.parse.UnexpectedChar", (char)n);
		} else {
			throw context.createParseException(in, "json.parse.ObjectNotClosedError");
		}
	}
	
	private static int beforeValue(InputSource in, ParseContext context) throws IOException {
		int n = skip(in, context);
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
				context.set(JSONEventType.STRING, parseString(in, context));
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
				context.set(JSONEventType.NUMBER, parseNumber(in, context));
				return AFTER_VALUE;	
			case 't':
				in.back();
				context.set(JSONEventType.TRUE, parseLiteral(in, context, "true", Boolean.TRUE));
				return AFTER_VALUE;
			case 'f':
				in.back();
				context.set(JSONEventType.FALSE, parseLiteral(in, context, "false", Boolean.FALSE));
				return AFTER_VALUE;
			case 'n':
				in.back();
				context.set(JSONEventType.NULL, parseLiteral(in, context, "null", null));
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
	
	private static int afterValue(InputSource in, ParseContext context) throws IOException {
		int n = skip(in, context);
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
	
	private static int skip(InputSource in, ParseContext context) throws IOException {
		int n = -1;
		loop:while ((n = in.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case '\r':
			case '\n':
			case ' ':
			case '\t':
			case 0xFEFF: // BOM
				break;
			default:
				break loop;
			}
		}
		return n;
	}
	
	private static String parseString(InputSource in, ParseContext context) throws IOException {
		StringCache sc = context.getCachedBuffer();
		
		int n = in.next();

		int rest = in.mark();
		int len = 0;
		loop:while ((n = in.next()) != -1) {
			rest--;
			len++;
			
			char c = (char)n;
			if (c < ESCAPE_CHARS.length) {
				switch (ESCAPE_CHARS[c]) {
				case 0:
					if (rest == 0) in.copy(sc, len);
					break;
				case 1: // control chars
					throw context.createParseException(in, "json.parse.UnexpectedChar", c);
				case 2: // "
					if (len > 0) in.copy(sc, len - 1);
					break loop;						
				case 3: // escape chars
					if (len > 0) in.copy(sc, len - 1);
					in.back();
					c = parseEscape(in, context);
					sc.append(c);
					rest = 0;
					break;
				}
			} else if (c == 0xFEFF) {
				if (len > 0) in.copy(sc, len - 1);
				rest = 0;
			} else {
				if (rest == 0) in.copy(sc, len);
			}
			
			if (rest == 0) {
				rest = in.mark();
				len = 0;
			}
		}
		
		if (n != '\"') {
			throw context.createParseException(in, "json.parse.StringNotClosedError");
		}
		return sc.toString();
	}
	
	private static char parseEscape(InputSource in, ParseContext context) throws IOException {
		int point = 1; // 0 '\' 1 'u' 2 'x' 3 'x' 4 'x' 5 'x' E
		char escape = '\0';
		
		int n = in.next();
		loop:while ((n = in.next()) != -1) {
			char c = (char)n;
			if (c == 0xFEFF) continue; // BOM
			
			if (point == 1) {
				switch(c) {
				case 'b':
					escape = '\b';
					break loop;
				case 'f':
					escape = '\f';
					break loop;
				case 'n':
					escape = '\n';
					break loop;
				case 'r':
					escape = '\r';
					break loop;
				case 't':
					escape = '\t';
					break loop;
				case 'u':
					point = 2;
					break;
				default:
					escape = c;
					break loop;
				}
			} else {
				int hex = (c >= '0' && c <= '9') ? c-48 :
					(c >= 'A' && c <= 'F') ? c-65+10 :
					(c >= 'a' && c <= 'f') ? c-97+10 : -1;
				if (hex != -1) {
					escape |= (hex << ((5-point)*4));
					if (point != 5) {
						point++;
					} else {
						break loop;
					}
				} else {
					throw context.createParseException(in, "json.parse.IllegalUnicodeEscape", c);
				}
			}
		}
		
		return escape;
	}
	
	private static BigDecimal parseNumber(InputSource in, ParseContext context) throws IOException {
		int point = 0; // 0 '(-)' 1 '0' | ('[1-9]' 2 '[0-9]*') 3 '(.)' 4 '[0-9]' 5 '[0-9]*' 6 'e|E' 7 '[+|-]' 8 '[0-9]' 9 '[0-9]*' E
		StringCache sc = context.getCachedBuffer();
		
		int n = -1;
		loop:while ((n = in.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case 0xFEFF: // BOM
				break;
			case '+':
				if (point == 7) {
					sc.append(c);
					point = 8;
				} else {
					throw context.createParseException(in, "json.parse.UnexpectedChar", c);
				}
				break;
			case '-':
				if (point == 0) {
					sc.append(c);
					point = 1;
				} else if (point == 7) {
					sc.append(c);
					point = 8;
				} else {
					throw context.createParseException(in, "json.parse.UnexpectedChar", c);
				}
				break;
			case '.':
				if (point == 2 || point == 3) {
					sc.append(c);
					point = 4;
				} else {
					throw context.createParseException(in, "json.parse.UnexpectedChar", c);
				}
				break;
			case 'e':
			case 'E':
				if (point == 2 || point == 3 || point == 5 || point == 6) {
					sc.append(c);
					point = 7;
				} else {
					throw context.createParseException(in, "json.parse.UnexpectedChar", c);
				}
				break;
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
				if (point == 0 || point == 1) {
					sc.append(c);
					point = (c == '0') ? 3 : 2;
				} else if (point == 2 || point == 5 || point == 9) {
					sc.append(c);
				} else if (point == 4) {
					sc.append(c);
					point = 5;
				} else if (point == 7 || point == 8) {
					sc.append(c);
					point = 9;
				} else {
					throw context.createParseException(in, "json.parse.UnexpectedChar", c);
				}
				break;
			default:
				if (point == 2 || point == 3 || point == 5 || point == 6 || point == 9) {
					in.back();
					break loop;
				} else {
					throw context.createParseException(in, "json.parse.UnexpectedChar", c);
				}
			}
		}
		
		return sc.toBigDecimal();
	}
	
	private static Object parseLiteral(InputSource in, ParseContext context, String literal, Object result) throws IOException {
		int pos = 0;
		int n = -1;
		while ((n = in.next()) != -1) {
			char c = (char)n;
			
			if (pos < literal.length() && c == literal.charAt(pos++)) {
				if (pos == literal.length()) {
					return result;
				}
			} else {
				break;
			}
		}
		throw context.createParseException(in, "json.parse.UnrecognizedLiteral", literal.substring(0, pos));
	}
}
