package net.arnx.jsonic.internal.parser;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import net.arnx.jsonic.JSONException;
import net.arnx.jsonic.internal.io.InputSource;
import net.arnx.jsonic.internal.io.StringCache;

class ParseContext {
	private static final int[] ESCAPE_CHARS = new int[128];
	
	static {
		for (int i = 0; i < 32; i++) {
			ESCAPE_CHARS[i] = 3;
		}
		ESCAPE_CHARS['\\'] = 1;
		ESCAPE_CHARS['"'] = 2;
		ESCAPE_CHARS['\''] = 2;
		ESCAPE_CHARS[0x7F] = 3;
	}
	
	private Locale locale;
	private int maxDepth;
	private boolean ignoreWhirespace;
	
	private List<JSONEventType> stack = new ArrayList<JSONEventType>();
	private StringCache cache;
	
	private JSONEventType type;
	private Object value;
	private boolean first;
	
	public ParseContext(Locale locale, int maxDepth, boolean ignoreWhirespace) {
		this.locale = locale;
		this.maxDepth = maxDepth;
		this.ignoreWhirespace = ignoreWhirespace;
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
	
	public boolean isIgnoreWhitespace() {
		return ignoreWhirespace;
	}
	
	public void push(JSONEventType type) {
		this.type = type;
		stack.add(type);
		this.first = true;
	}
	
	public void set(JSONEventType type, Object value, boolean isValue) {
		this.type = type;
		this.value = value;
		if (isValue) first = false;
	}
	
	public void pop() {
		JSONEventType beginType = stack.remove(stack.size()-1);
		if (beginType == JSONEventType.START_OBJECT) {
			this.type = JSONEventType.END_OBJECT;
		} else if (beginType == JSONEventType.START_ARRAY) {
			this.type = JSONEventType.END_ARRAY;
		} else {
			throw new IllegalStateException();
		}
		this.first = false;
	}
	
	public JSONEventType getBeginType() {
		return (!stack.isEmpty()) ? stack.get(stack.size()-1) : null;
	}
	
	public JSONEventType getType() {
		return type;
	}
	
	public Object getValue() {
		return value;
	}
	
	public boolean isFirst() {
		return first;
	}
	
	public StringCache getCachedBuffer() {
		if (cache == null) {
			cache = new StringCache(120);
		} else {
			cache.clear();
		}
		return cache;
	}
	
	public final Object parseString(InputSource in) throws IOException {
		StringCache sc = (getDepth() <= getMaxDepth()) ? getCachedBuffer() : StringCache.EMPTY_CACHE;
		
		int start = in.next();

		int rest = in.mark();
		int len = 0;
		
		int n = -1;
		while ((n = in.next()) != -1) {
			rest--;
			len++;
			
			if (n < ESCAPE_CHARS.length) {
				int type = ESCAPE_CHARS[n];
				if (type == 0) {
					if (rest == 0) in.copy(sc, len);
				} else if (type == 1) { // escape chars
					if (len > 0) in.copy(sc, len - 1);
					rest = 0;
					
					in.back();
					sc.append(parseEscape(in));
				} else if (type == 2) { // "'
					if (n == start) {
						if (len > 1) in.copy(sc, len - 1);
						break;
					} else {
						if (rest == 0) in.copy(sc, len);
					}
				} else { // control chars
					throw createParseException(in, "json.parse.UnexpectedChar", (char)n);
				}
			} else {
				if (rest == 0) in.copy(sc, len);
			}
			
			if (rest == 0) {
				rest = in.mark();
				len = 0;
			}
		}
		
		if (n != start) {
			throw createParseException(in, "json.parse.StringNotClosedError");
		}
		return sc.toString();
	}
	
	public char parseEscape(InputSource in) throws IOException {
		int point = 1; // 0 '\' 1 'u' 2 'x' 3 'x' 4 'x' 5 'x' E
		char escape = '\0';
		
		int n = in.next();
		loop:while ((n = in.next()) != -1) {
			char c = (char)n;
			
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
					throw createParseException(in, "json.parse.IllegalUnicodeEscape", c);
				}
			}
		}
		
		return escape;
	}
	
	public Object parseNumber(InputSource in) throws IOException {
		int point = 0; // 0 '(-)' 1 '0' | ('[1-9]' 2 '[0-9]*') 3 '(.)' 4 '[0-9]' 5 '[0-9]*' 6 'e|E' 7 '[+|-]' 8 '[0-9]' 9 '[0-9]*' E
		StringCache sc = (getDepth() <= getMaxDepth()) ? getCachedBuffer() : StringCache.EMPTY_CACHE;
		
		int n = -1;
		
		int rest = in.mark();
		int len = 0;
		loop:while ((n = in.next()) != -1) {
			rest--;
			len++;
			
			char c = (char)n;
			switch(c) {
			case '+':
				if (point == 7) {
					if (rest == 0) in.copy(sc, len);
					point = 8;
				} else {
					throw createParseException(in, "json.parse.UnexpectedChar", c);
				}
				break;
			case '-':
				if (point == 0) {
					if (rest == 0) in.copy(sc, len);
					point = 1;
				} else if (point == 7) {
					if (rest == 0) in.copy(sc, len);
					point = 8;
				} else {
					throw createParseException(in, "json.parse.UnexpectedChar", c);
				}
				break;
			case '.':
				if (point == 2 || point == 3) {
					if (rest == 0) in.copy(sc, len);
					point = 4;
				} else {
					throw createParseException(in, "json.parse.UnexpectedChar", c);
				}
				break;
			case 'e':
			case 'E':
				if (point == 2 || point == 3 || point == 5 || point == 6) {
					if (rest == 0) in.copy(sc, len);
					point = 7;
				} else {
					throw createParseException(in, "json.parse.UnexpectedChar", c);
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
					if (rest == 0) in.copy(sc, len);
					point = (c == '0') ? 3 : 2;
				} else if (point == 2 || point == 5 || point == 9) {
					if (rest == 0) in.copy(sc, len);
				} else if (point == 4) {
					if (rest == 0) in.copy(sc, len);
					point = 5;
				} else if (point == 7 || point == 8) {
					if (rest == 0) in.copy(sc, len);
					point = 9;
				} else {
					throw createParseException(in, "json.parse.UnexpectedChar", c);
				}
				break;
			default:
				if (point == 2 || point == 3 || point == 5 || point == 6 || point == 9) {
					if (len > 1) in.copy(sc, len - 1);
					in.back();
					break loop;
				} else {
					throw createParseException(in, "json.parse.UnexpectedChar", c);
				}
			}
			
			if (rest == 0) {
				rest = in.mark();
				len = 0;
			}
		}
		
		return sc.toBigDecimal();
	}
	
	public Object parseLiteral(InputSource in, String expected, Object result) throws IOException {
		int pos = 0;
		int n = -1;
		while ((n = in.next()) != -1) {
			char c = (char)n;
			if (pos < expected.length() && c == expected.charAt(pos++)) {
				if (pos == expected.length()) {
					return (getDepth() <= getMaxDepth()) ? result : null;
				}
			} else {
				break;
			}
		}
		
		throw createParseException(in, "json.parse.UnrecognizedLiteral", expected.substring(0, pos));
	}

	public Object parseLiteral(InputSource in) throws IOException {
		int point = 0; // 0 'IdStart' 1 'IdPart' ... !'IdPart' E
		boolean cache = (getDepth() <= getMaxDepth());
		StringCache sc = cache ? getCachedBuffer() : StringCache.EMPTY_CACHE;
		
		int n = -1;
		while ((n = in.next()) != -1) {
			if (n == '\\') {
				in.back();
				n = parseEscape(in);
			}
			
			if (point == 0) {
				if (Character.isJavaIdentifierStart(n)) {
					if (!cache && (n == 'n' || n == 't' || n == 'f')) {
						sc = getCachedBuffer();
					}
					
					sc.append((char)n);
					point = 1;
				} else {
					throw createParseException(in, "json.parse.UnexpectedChar", (char)n);
				}
			} else if (point == 1 && (Character.isJavaIdentifierPart(n) || n == '.')) {
				if (!cache && sc.getLength() == 5) {
					sc = StringCache.EMPTY_CACHE;
				}
				
				sc.append((char)n);
			} else {
				in.back();
				break;
			}
		}
		
		String str = sc.toString();
		if (str != null) {
			if ("null".equals(str)) {
				type = JSONEventType.NULL;
				return null;
			} else if ("true".equals(str)) {
				type = JSONEventType.TRUE;
				return (cache) ? Boolean.TRUE : null;
			} else if ("false".equals(str)) {
				type = JSONEventType.FALSE;
				return (cache) ? Boolean.FALSE : null;
			}
		}
		return (cache) ? str : null;
	}
	
	public String parseComment(InputSource in) throws IOException {
		int point = 0; // 0 '/' 1 '*' 2  '*' 3 '/' E or  0 '/' 1 '/' 4  '\r|\n|\r\n' E
		StringCache sc = isIgnoreWhitespace() ? StringCache.EMPTY_CACHE : getCachedBuffer();
		
		int n = -1;
		
		int rest = in.mark();
		int len = 0;
		loop:while ((n = in.next()) != -1) {
			rest--;
			len++;
			
			switch(n) {
			case '/':
				if (point == 0) {
					point = 1;
				} else if (point == 1) {
					point = 4;
				} else if (point == 3) {
					if (len > 1) in.copy(sc, len - 2);
					break loop;
				} else if (point == 2 || point == 4) {
					if (rest == 0) in.copy(sc, len);
				} else {
					throw createParseException(in, "json.parse.UnexpectedChar", (char)n);
				}
				break;
			case '*':
				if (point == 1) {
					point = 2;
				} else if (point == 2) {
					point = 3;
				} else if (point == 3 || point == 4) {
					if (rest == 0) in.copy(sc, len);
				} else {
					throw createParseException(in, "json.parse.UnexpectedChar", (char)n);
				}
				break;
			case '\n':
			case '\r':
				if (point == 2 || point == 3) {
					if (rest == 0) in.copy(sc, len);
					point = 2;
				} else if (point == 4) {
					if (len > 1) in.copy(sc, len - 1);
					break loop;
				} else {
					throw createParseException(in, "json.parse.UnexpectedChar", (char)n);
				}
				break;
			default:
				if (point == 3) {
					if (rest == 0) in.copy(sc, len);
					point = 2;
				} else if (point == 2 || point == 4) {
					if (rest == 0) in.copy(sc, len);
				} else {
					throw createParseException(in, "json.parse.UnexpectedChar", (char)n);
				}
			}
			
			if (rest == 0) {
				rest = in.mark();
				len = 0;
			}
		}
		
		return sc.toString();
	}
	
	public String parseWhitespace(InputSource in) throws IOException {
		StringCache sc = isIgnoreWhitespace() ? StringCache.EMPTY_CACHE : getCachedBuffer();
		
		int n = -1;
		
		int rest = in.mark();
		int len = 0;
		while ((n = in.next()) != -1) {
			rest--;
			len++;
			
			if (n == ' ' || n == '\t' || n == '\r' || n == '\n') {
				if (rest == 0) in.copy(sc, len);
			} else {
				if (len > 1) in.copy(sc, len - 1);
				in.back();
				break;
			}
			
			if (rest == 0) {
				rest = in.mark();
				len = 0;
			}
		}
		
		return sc.toString();
	}
	
	public JSONException createParseException(InputSource in, String id) {
		return createParseException(in, id, (Object[])null);
	}
	
	public JSONException createParseException(InputSource in, String id, Object... args) {
		ResourceBundle bundle = ResourceBundle.getBundle("net.arnx.jsonic.Messages", getLocale());
		
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
