package net.arnx.jsonic.internal.parser;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Locale;

import net.arnx.jsonic.internal.io.InputSource;

public class StrictJSONParser implements JSONParser {
	private static final ParseState BEFORE_ROOT = new BeforeRoot();
	private static final ParseState AFTER_ROOT = new AfterRoot();
	private static final ParseState BEFORE_NAME = new BeforeName();
	private static final ParseState AFTER_NAME = new AfterName();
	private static final ParseState BEFORE_VALUE = new BeforeValue();
	private static final ParseState AFTER_VALUE = new AfterValue();
	
	private static final int[] ESCAPE_CHARS = new int[128];
	
	static {
		for (int i = 0; i < 32; i++) {
			ESCAPE_CHARS[i] = 1;
		}
		ESCAPE_CHARS['"'] = 2;
		ESCAPE_CHARS['\\'] = 3;
		ESCAPE_CHARS[0x7F] = 1;
	}
	
	private ParseState state = BEFORE_ROOT;
	private ParseContext context;
	
	public StrictJSONParser(InputSource in, Locale locale, int maxDepth) {
		this.context = new ParseContext(in, locale, maxDepth);
	}
	
	public JSONEventType next() throws IOException {
		do {
			context.set(null, ParseContext.EMPTY);
			state = state.next(context);
			if (state == null) return null;
		} while (context.getType() == null);
		
		return context.getType();
	}
	
	public Object getValue() {
		return context.getValue();
	}
	
	public static class BeforeRoot implements ParseState {
		@Override
		public ParseState next(ParseContext context) throws IOException {
			int n = skip(context);
			if (n != -1) {
				while ((n = context.next()) != -1) {
					char c = (char)n;
					switch(c) {
					case '{':
						context.push(JSONEventType.BEGIN_OBJECT);
						return BEFORE_NAME;
					case '[':
						context.push(JSONEventType.BEGIN_ARRAY);
						return BEFORE_VALUE;
					default:
						throw context.createParseException("json.parse.UnexpectedChar", c);
					}
				}
			}
			throw context.createParseException("json.parse.EmptyInputError");
		}
	}
	
	public static class AfterRoot implements ParseState {
		@Override
		public ParseState next(ParseContext context) throws IOException {
			int n = skip(context);
			if (n != -1) {
				char c = (char)n;
				throw context.createParseException("json.parse.UnexpectedChar", c);
			}
			return null;
		}
	}
	
	public static class BeforeName implements ParseState {
		@Override
		public ParseState next(ParseContext context) throws IOException {
			int n = skip(context);
			if (n != -1) {
				while ((n = context.next()) != -1) {
					char c = (char)n;
					switch(c) {
					case '"':
						context.set(JSONEventType.NAME, parseString(context));
						return AFTER_NAME;					
					case '}':
						if (context.getPrevType() == JSONEventType.BEGIN_OBJECT) {
							context.pop();
							if (context.getBeginType() == null) {
								return AFTER_ROOT;
							} else {
								return AFTER_VALUE;							
							}
						}
					default:
						throw context.createParseException("json.parse.UnexpectedChar", c);
					}
				}
			}
			throw context.createParseException("json.parse.ObjectNotClosedError");
		}
	}
	
	public static class AfterName implements ParseState {
		@Override
		public ParseState next(ParseContext context) throws IOException {
			int n = skip(context);
			if (n != -1) {
				while ((n = context.next()) != -1) {
					char c = (char)n;
					switch(c) {
					case ':':
						return BEFORE_VALUE;
					default:
						throw context.createParseException("json.parse.UnexpectedChar", c);
					}
				}
			}
			throw context.createParseException("json.parse.ObjectNotClosedError");
		}
	}
	
	public static class BeforeValue implements ParseState {
		@Override
		public ParseState next(ParseContext context) throws IOException {
			int n = skip(context);
			if (n != -1) {
				while ((n = context.next()) != -1) {
					char c = (char)n;
					switch(c) {
					case '{':
						context.push(JSONEventType.BEGIN_OBJECT);
						return BEFORE_NAME;
					case '[':
						context.push(JSONEventType.BEGIN_ARRAY);
						return BEFORE_VALUE;
					case '"':
						context.set(JSONEventType.STRING, parseString(context));
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
						context.set(JSONEventType.NUMBER, parseNumber(context));
						return AFTER_VALUE;	
					case 't':
						context.set(JSONEventType.TRUE, parseLiteral(context, "true", Boolean.TRUE));
						return AFTER_VALUE;
					case 'f':
						context.set(JSONEventType.FALSE, parseLiteral(context, "false", Boolean.FALSE));
						return AFTER_VALUE;
					case 'n':
						context.set(JSONEventType.NULL, parseLiteral(context, "null", null));
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
						throw context.createParseException("json.parse.UnexpectedChar", c);
					}
				}
			}
			
			switch (context.getBeginType()) {
			case BEGIN_OBJECT:
				throw context.createParseException("json.parse.ObjectNotClosedError");
			case BEGIN_ARRAY:
				throw context.createParseException("json.parse.ArrayNotClosedError");
			default:
				throw new IllegalStateException();
			}
		}		
	}
	
	public static class AfterValue implements ParseState {
		@Override
		public ParseState next(ParseContext context) throws IOException {
			int n = skip(context);
			if (n != -1) {
				while ((n = context.next()) != -1) {
					char c = (char)n;
					switch(c) {
					case ',':
						if (context.getBeginType() == JSONEventType.BEGIN_OBJECT) {
							return BEFORE_NAME;
						} else if (context.getBeginType() == JSONEventType.BEGIN_ARRAY) {
							return BEFORE_VALUE;
						}
					case '}':
						if (context.getBeginType() == JSONEventType.BEGIN_OBJECT) {
							context.pop();
							if (context.getBeginType() == null) {
								return AFTER_ROOT;
							} else {
								return AFTER_VALUE;							
							}
						}
					case ']':
						if (context.getBeginType() == JSONEventType.BEGIN_ARRAY) {
							context.pop();
							if (context.getBeginType() == null) {
								return AFTER_ROOT;
							} else {
								return AFTER_VALUE;							
							}
						}
					default:
						throw context.createParseException("json.parse.UnexpectedChar", c);						
					}
				}
			}
			
			switch (context.getBeginType()) {
			case BEGIN_OBJECT:
				throw context.createParseException("json.parse.ObjectNotClosedError");
			case BEGIN_ARRAY:
				throw context.createParseException("json.parse.ArrayNotClosedError");
			default:
				throw new IllegalStateException();
			}
		}
	}
	
	private static int skip(ParseContext context) throws IOException {
		int n = -1;
		loop:while ((n = context.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case '\r':
			case '\n':
			case ' ':
			case '\t':
			case 0xFEFF: // BOM
				break;
			default:
				context.back();
				break loop;
			}
		}
		return n;
	}
	
	private static String parseString(ParseContext context) throws IOException {
		StringBuilder sb = (context.getDepth() <= context.getMaxDepth()) ? context.getCachedBuffer() : null;
		
		int n = -1;
		loop:while ((n = context.next()) != -1) {
			char c = (char)n;
			if (c < ESCAPE_CHARS.length) {
				switch (ESCAPE_CHARS[c]) {
				case 0:
					if (sb != null) sb.append(c);
					break;
				case 1: // control chars
					throw context.createParseException("json.parse.UnexpectedChar", c);
				case 2: // "
					break loop;						
				case 3: // escape chars
					c = parseEscape(context);
					if (sb != null) sb.append(c);
					break;
				}
			} else if (c != 0xFEFF) {
				if (sb != null) sb.append(c);
			}
		}
		
		if (n != '\"') {
			throw context.createParseException("json.parse.StringNotClosedError");
		}
		return (sb != null) ? sb.toString() : null;
	}
	
	private static char parseEscape(ParseContext context) throws IOException {
		int point = 1; // 0 '\' 1 'u' 2 'x' 3 'x' 4 'x' 5 'x' E
		char escape = '\0';
		
		int n = -1;
		loop:while ((n = context.next()) != -1) {
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
					throw context.createParseException("json.parse.IllegalUnicodeEscape", c);
				}
			}
		}
		
		return escape;
	}
	
	private static BigDecimal parseNumber(ParseContext context) throws IOException {
		context.back();
		
		int point = 0; // 0 '(-)' 1 '0' | ('[1-9]' 2 '[0-9]*') 3 '(.)' 4 '[0-9]' 5 '[0-9]*' 6 'e|E' 7 '[+|-]' 8 '[0-9]' 9 '[0-9]*' E
		StringBuilder sb = (context.getDepth() <= context.getMaxDepth()) ? context.getCachedBuffer() : null;
		
		int n = -1;
		loop:while ((n = context.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case 0xFEFF: // BOM
				break;
			case '+':
				if (point == 7) {
					if (sb != null) sb.append(c);
					point = 8;
				} else {
					throw context.createParseException("json.parse.UnexpectedChar", c);
				}
				break;
			case '-':
				if (point == 0) {
					if (sb != null) sb.append(c);
					point = 1;
				} else if (point == 7) {
					if (sb != null) sb.append(c);
					point = 8;
				} else {
					throw context.createParseException("json.parse.UnexpectedChar", c);
				}
				break;
			case '.':
				if (point == 2 || point == 3) {
					if (sb != null) sb.append(c);
					point = 4;
				} else {
					throw context.createParseException("json.parse.UnexpectedChar", c);
				}
				break;
			case 'e':
			case 'E':
				if (point == 2 || point == 3 || point == 5 || point == 6) {
					if (sb != null) sb.append(c);
					point = 7;
				} else {
					throw context.createParseException("json.parse.UnexpectedChar", c);
				}
				break;
			default:
				if (c >= '0' && c <= '9') {
					if (point == 0 || point == 1) {
						if (sb != null) sb.append(c);
						point = (c == '0') ? 3 : 2;
					} else if (point == 2 || point == 5 || point == 9) {
						if (sb != null) sb.append(c);
					} else if (point == 4) {
						if (sb != null) sb.append(c);
						point = 5;
					} else if (point == 7 || point == 8) {
						if (sb != null) sb.append(c);
						point = 9;
					} else {
						throw context.createParseException("json.parse.UnexpectedChar", c);
					}
				} else if (point == 2 || point == 3 || point == 5 || point == 6 || point == 9) {
					context.back();
					break loop;
				} else {
					throw context.createParseException("json.parse.UnexpectedChar", c);
				}
			}
		}
		
		return (sb != null) ? new BigDecimal(sb.toString()) : null;
	}
	
	private static Object parseLiteral(ParseContext context, String literal, Object result) throws IOException {
		context.back();
		
		int pos = 0;
		int n = -1;
		while ((n = context.next()) != -1) {
			char c = (char)n;
			
			if (pos < literal.length() && c == literal.charAt(pos++)) {
				if (pos == literal.length()) {
					return result;
				}
			} else {
				break;
			}
		}
		throw context.createParseException("json.parse.UnrecognizedLiteral", literal.substring(0, pos));
	}
}
