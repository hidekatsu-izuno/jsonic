/*
 * Copyright 2007-2009 Hidekatsu Izuno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package net.arnx.jsonic;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class JSONObject {
	JSON json;
	Object root;
	Object current;
	
	public JSONObject() {
		this.json = new JSON();
	}
	
	public JSONObject(JSON json) {
		this.json = json;
	}
	
	public JSONObject get(String path) {
		if (path == null || (path = path.trim()).length() == 0) {
			return this;
		}
		
		JSONObject jo = new JSONObject(json);
		jo.root = root;
		jo.current = current;
		
		List<Object> expression = new ArrayList<Object>();
		// 0 $ 1 (. 2 key ) || ([ 3 key 4 ]) E
		int pos = 0;
		
		CharSequenceParserSource s = new CharSequenceParserSource(path, 16);
		int n = -1;
		while ((n = s.next()) != -1) {
			char c = (char)n;
			switch (c) {
			case '$':
				if (pos == 0) {
					jo.current = jo.root;
					pos = 1;
					break;
				} else {
					throw createParseException(getMessage("json.jsonpath.UnexpectedChar", c), s);
				}
			case '.':
				if (pos == 0 || pos == 1) {
					pos = 2;
				} else {
					throw createParseException(getMessage("json.jsonpath.UnexpectedChar", c), s);
				}
			case '[':
				if (pos == 1) {
					pos = 3;
				} else {
					throw createParseException(getMessage("json.jsonpath.UnexpectedChar", c), s);
				}
			case ']':
				if (pos == 4) {
					pos = 1;
				} else {
					throw createParseException(getMessage("json.jsonpath.UnexpectedChar", c), s);
				}
			case '"':
			case '\'':
				if (pos == 3) {
					s.back();
					expression.add(parseString(s));
					pos = 4;
				} else {
					throw createParseException(getMessage("json.jsonpath.UnexpectedChar", c), s);
				}
			default:
				if (pos == 2) {
					s.back();
					expression.add(parseLiteral(s));
					pos = 1;
				} else if (pos == 3) {
					s.back();
					if ((c == '-') || (c >= '0' && c <= '9')) {
						expression.add(parseNumber(s));
					} else {
						expression.add(parseLiteral(s));
					}
					pos = 4;
				} else {
					throw createParseException(getMessage("json.jsonpath.UnexpectedChar", c), s);
				}
			}
		}
		if (pos == 2 || pos == 4) {
			throw createParseException(getMessage("json.jsonpath.NotCompletedError"), s);
		}
		
		for (Object key : expression) {
			if (jo.current instanceof Map<?, ?>) {
				Map<?, ?> map = (Map<?, ?>)jo.current;
				if (map.containsKey(key)) {
					jo.current = map.get(key);
				} else {
					return null;
				}
			} else if (jo.current instanceof List<?>) {
				try {
					jo.current = ((List<?>)jo.current).get(((BigDecimal)key).intValueExact());
				} catch(Exception e) {
					return null;
				}
			} else {
				return null;
			}
		}
		
		return jo;
	}
	
	public <T> T get(String path, Class<T> cls) {
		return cls.cast(get(path, (Type)cls));
	}
	
	public Object get(String path, Type t) {
		JSONObject jo = get(path);
		if (jo == null) return null;

		json.setContext(t);
		return json.convert(jo.root, t);
	}
	
	public void set(String path, Object value) {
		value = normalize(value);
	}
	
	public void add(String path, Object value) {
		value = normalize(value);
		
	}
	
	public boolean isNull() {
		return (current == null);
	}
	
	public boolean isNumber() {
		return (current instanceof BigDecimal);
	}
	
	public boolean isString() {
		return (current instanceof String);
	}
	
	public boolean isArray() {
		return (current instanceof List);
	}
	
	public boolean isObject() {
		return (current instanceof Map);
	}

	public String toString(boolean prettyPrint) {
		return JSON.encode(current, prettyPrint);
	}

	@Override
	public String toString() {
		return JSON.encode(current);
	}
	
	private String parseString(CharSequenceParserSource s) throws JSONException {
		int point = 0; // 0 '"|'' 1 'c' ... '"|'' E
		StringBuilder sb = s.getCachedBuilder();
		char start = '\0';
		
		int n = -1;
		loop:while ((n = s.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case 0xFEFF: // BOM
				break;
			case '\\':
				if (point == 1) {
					if (start == '"') {
						s.back();
						sb.append(parseEscape(s));
					} else {
						sb.append(c);
					}
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '\'':
			case '"':
				if (point == 0) {
					start = c;
					point = 1;
					break;
				} else if (point == 1) {
					if (start == c) {
						break loop;						
					} else {
						sb.append(c);
					}
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			default:
				if (point == 1) {
					sb.append(c);
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
			}
		}
		
		if (n != start) {
			throw createParseException(getMessage("json.parse.StringNotClosedError"), s);
		}
		return sb.toString();
	}
	
	private Object parseLiteral(CharSequenceParserSource s) throws JSONException {
		int point = 0; // 0 'IdStart' 1 'IdPart' ... !'IdPart' E
		StringBuilder sb = s.getCachedBuilder();

		int n = -1;
		loop:while ((n = s.next()) != -1) {
			char c = (char)n;
			if (c == 0xFEFF) continue;
			
			if (c == '\\') {
				s.back();
				c = parseEscape(s);
			}
			
			if (point == 0 && Character.isJavaIdentifierStart(c)) {
				sb.append(c);
				point = 1;
			} else if (point == 1 && Character.isJavaIdentifierPart(c)){
				sb.append(c);
			} else {
				s.back();
				break loop;
			}
		}
		
		String str = sb.toString();
		
		if ("null".equals(str)) return null;
		if ("true".equals(str)) return true;
		if ("false".equals(str)) return false;

		return str;
	}
	
	private Number parseNumber(CharSequenceParserSource s) throws JSONException {
		int point = 0; // 0 '(-)' 1 '0' | ('[1-9]' 2 '[0-9]*') 3 '(.)' 4 '[0-9]' 5 '[0-9]*' 6 'e|E' 7 '[+|-]' 8 '[0-9]' E
		StringBuilder sb = s.getCachedBuilder();
		
		int n = -1;
		loop:while ((n = s.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case 0xFEFF: // BOM
				break;
			case '+':
				if (point == 7) {
					sb.append(c);
					point = 8;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '-':
				if (point == 0) {
					sb.append(c);
					point = 1;
				} else if (point == 7) {
					sb.append(c);
					point = 8;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case '.':
				if (point == 2 || point == 3) {
					sb.append(c);
					point = 4;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			case 'e':
			case 'E':
				if (point == 2 || point == 3 || point == 5 || point == 6) {
					sb.append(c);
					point = 7;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
				break;
			default:
				if (c >= '0' && c <= '9') {
					if (point == 0 || point == 1) {
						sb.append(c);
						point = (c == '0') ? 3 : 2;
					} else if (point == 2 || point == 5) {
						sb.append(c);
					} else if (point == 4) {
						sb.append(c);
						point = 5;
					} else if (point == 7 || point == 8) {
						sb.append(c);
						break loop;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
				} else if (point == 2 || point == 3 || point == 5 || point == 6) {
					s.back();
					break loop;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
			}
		}
		
		return new BigDecimal(sb.toString());
	}

	private char parseEscape(CharSequenceParserSource s) throws JSONException {
		int point = 0; // 0 '\' 1 'u' 2 'x' 3 'x' 4 'x' 5 'x' E
		char escape = '\0';
		
		int n = -1;
		loop:while ((n = s.next()) != -1) {
			char c = (char)n;
			if (c == 0xFEFF) continue; // BOM
			
			if (point == 0) {
				if (c == '\\') {
					point = 1;
				} else {
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
			} else if (point == 1) {
				switch(c) {
				case '"':
				case '\\':
				case '/':
					escape = c;
					break loop;
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
					throw createParseException(getMessage("json.parse.IllegalUnicodeEscape", c), s);
				}
			}
		}
		
		return escape;
	}
	
	private JSONException createParseException(String message, ParserSource s) {
		return new JSONException("" + s.getLineNumber() + ": " + message + "\n" + s.toString() + " <- ?",
				JSONException.JSONPATH_ERROR, s.getLineNumber(), s.getColumnNumber(), s.getOffset());
	}
	
	private String getMessage(String id, Object... args) {
		ResourceBundle bundle = ResourceBundle.getBundle("net.arnx.jsonic.Messages", json.locale);
		return MessageFormat.format(bundle.getString(id), args);
	}

	private Object normalize(Object value) {
		if (value == null || value instanceof BigDecimal || value instanceof Boolean) {
			// no handle
		} else if (value instanceof CharSequence || value instanceof Character) {
			value = value.toString();
		} else if (value instanceof Number) {
			if (value instanceof Byte) {
				value = new BigDecimal((Byte)value);
			} else if (value instanceof Short) {
				value = new BigDecimal((Short)value);
			} else if (value instanceof Integer) {
				value = new BigDecimal((Integer)value);
			} else if (value instanceof Long) {
				value = new BigDecimal((Long)value);
			} else if (value instanceof Float) {
				value = new BigDecimal((Float)value);
			} else if (value instanceof Double) {
				value = new BigDecimal((Double)value);
			} else if (value instanceof BigInteger) {
				value = new BigDecimal((BigInteger)value);
			}
		} else {
			List<?> list = (List<?>)json.parse(json.format(Arrays.asList(value)));
			value = list.get(0);
		}
		
		return value;
	}
}
