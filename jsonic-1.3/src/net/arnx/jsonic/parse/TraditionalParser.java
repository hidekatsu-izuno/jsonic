/*
 * Copyright 2014 Hidekatsu Izuno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.arnx.jsonic.parse;

import java.io.IOException;
import java.util.LinkedList;

import net.arnx.jsonic.JSONEventType;
import net.arnx.jsonic.io.InputSource;
import net.arnx.jsonic.util.LocalCache;

public class TraditionalParser extends JSONParser {
	private InputSource in;

	private boolean emptyRoot = false;
	private long nameLineNumber = Long.MAX_VALUE;

	private int backupState = -1;
	private LinkedList<Token> backupTokens;

	private class Token {
		public JSONEventType type;
		public Object value;
		public boolean isValue;

		public Token(JSONEventType type, Object value, boolean isValue) {
			this.type = type;
			this.value = value;
			this.isValue = isValue;
		}
	}

	public TraditionalParser(InputSource in, int maxDepth, boolean interpretterMode, boolean ignoreWhirespace, LocalCache cache) {
		super(in, maxDepth, interpretterMode, ignoreWhirespace, cache);
		this.in = in;
	}

	@Override
	int beforeRoot() throws IOException {
		int n = in.next();
		if (n == 0xFEFF) n = in.next();
		switch (n) {
		case ' ':
		case '\t':
		case '\r':
		case '\n':
			in.back();
			String ws = parseWhitespace();
			if (!isIgnoreWhitespace()) {
				set(JSONEventType.WHITESPACE, ws, false);
			}
			return BEFORE_ROOT;
		case '/':
			in.back();
			String comment = parseComment();
			if (!isIgnoreWhitespace()) {
				set(JSONEventType.COMMENT, comment, false);
			}
			return BEFORE_ROOT;
		case '{':
			push(JSONEventType.START_OBJECT);
			return BEFORE_NAME;
		case '[':
			push(JSONEventType.START_ARRAY);
			return BEFORE_VALUE;
		case -1:
			if (isInterpretterMode()) {
				return -1;
			} else {
				emptyRoot = true;
				push(JSONEventType.START_OBJECT);
				return BEFORE_NAME;
			}
		case '"':
		case '\'':
			in.back();
			parseTokens(JSONEventType.STRING, parseString(true));
			return OTHER_STATE;
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
			parseTokens(JSONEventType.NUMBER, parseNumber());
			return OTHER_STATE;
		default:
			in.back();
			Object literal = parseLiteral(true);
			parseTokens(getParsedType(), literal);
			return OTHER_STATE;
		}
	}

	@Override
	int afterRoot() throws IOException {
		int n = in.next();
		switch (n) {
		case ' ':
		case '\t':
		case '\r':
		case '\n':
			in.back();
			String ws = parseWhitespace();
			if (!isIgnoreWhitespace()) {
				set(JSONEventType.WHITESPACE, ws, false);
			}
			return AFTER_ROOT;
		case '/':
			in.back();
			String comment = parseComment();
			if (!isIgnoreWhitespace()) {
				set(JSONEventType.COMMENT, comment, false);
			}
			return AFTER_ROOT;
		case -1:
			return -1;
		case ',':
			if (isInterpretterMode()) {
				return BEFORE_ROOT;
			}
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
			if (isInterpretterMode()) {
				in.back();
				return BEFORE_ROOT;
			}
		default:
			throw createParseException(in, "json.parse.UnexpectedChar", (char)n);
		}
	}

	@Override
	int beforeName() throws IOException {
		int n = in.next();
		switch (n) {
		case ' ':
		case '\t':
		case '\r':
		case '\n':
			in.back();
			String ws = parseWhitespace();
			if (!isIgnoreWhitespace()) {
				set(JSONEventType.WHITESPACE, ws, false);
			}
			return BEFORE_NAME;
		case '/':
			in.back();
			String comment = parseComment();
			if (!isIgnoreWhitespace()) {
				set(JSONEventType.COMMENT, comment, false);
			}
			return BEFORE_NAME;
		case '"':
		case '\'':
			in.back();
			set(JSONEventType.NAME, parseString(true), false);
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
			Object num = parseNumber();
			set(JSONEventType.NAME, (num != null) ? num.toString() : null, false);
			return AFTER_NAME;
		case '}':
			if (isFirst() && getBeginType() == JSONEventType.START_OBJECT) {
				pop();
				if (getBeginType() == null) {
					if (emptyRoot) {
						throw createParseException(in, "json.parse.UnexpectedChar", (char)n);
					} else {
						return AFTER_ROOT;
					}
				} else {
					nameLineNumber = in.getLineNumber();
					return AFTER_VALUE;
				}
			} else {
				throw createParseException(in, "json.parse.UnexpectedChar", (char)n);
			}
		case -1:
			pop();
			if (getBeginType() == null && emptyRoot) {
				return -1;
			} else {
				throw createParseException(in, "json.parse.ObjectNotClosedError");
			}
		default:
			in.back();
			Object literal = parseLiteral(true);
			set(JSONEventType.NAME, (literal != null) ? literal.toString() : null, false);
			return AFTER_NAME;
		}
	}

	@Override
	int afterName() throws IOException {
		int n = in.next();
		switch (n) {
		case ' ':
		case '\t':
		case '\r':
		case '\n':
			in.back();
			String ws = parseWhitespace();
			if (!isIgnoreWhitespace()) {
				set(JSONEventType.WHITESPACE, ws, false);
			}
			return AFTER_NAME;
		case '/':
			in.back();
			String comment = parseComment();
			if (!isIgnoreWhitespace()) {
				set(JSONEventType.COMMENT, comment, false);
			}
			return AFTER_NAME;
		case '{':
		case '[':
			in.back();
		case ':':
			return BEFORE_VALUE;
		case -1:
			throw createParseException(in, "json.parse.ObjectNotClosedError");
		default:
			throw createParseException(in, "json.parse.UnexpectedChar", (char)n);
		}
	}

	@Override
	int beforeValue() throws IOException {
		int n = in.next();
		switch (n) {
		case ' ':
		case '\t':
		case '\r':
		case '\n':
			in.back();
			String ws = parseWhitespace();
			if (!isIgnoreWhitespace()) {
				set(JSONEventType.WHITESPACE, ws, false);
			}
			return BEFORE_VALUE;
		case '/':
			in.back();
			String comment = parseComment();
			if (!isIgnoreWhitespace()) {
				set(JSONEventType.COMMENT, comment, false);
			}
			return BEFORE_VALUE;
		case '{':
			push(JSONEventType.START_OBJECT);
			return BEFORE_NAME;
		case '[':
			push(JSONEventType.START_ARRAY);
			return BEFORE_VALUE;
		case '"':
		case '\'':
			in.back();
			set(JSONEventType.STRING, parseString(true), true);
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
			set(JSONEventType.NUMBER, parseNumber(), true);
			nameLineNumber = in.getLineNumber();
			return AFTER_VALUE;
		case ',':
			if (getBeginType() == JSONEventType.START_OBJECT) {
				set(JSONEventType.NULL, null, true);
				return BEFORE_NAME;
			} else if (getBeginType() == JSONEventType.START_ARRAY) {
				set(JSONEventType.NULL, null, true);
				return BEFORE_VALUE;
			} else {
				throw createParseException(in, "json.parse.UnexpectedChar", (char)n);
			}
		case '}':
			if (getBeginType() == JSONEventType.START_OBJECT) {
				set(JSONEventType.NULL, null, true);
				in.back();
				return AFTER_VALUE;
			} else {
				throw createParseException(in, "json.parse.UnexpectedChar", (char)n);
			}
		case ']':
			if (getBeginType() == JSONEventType.START_ARRAY) {
				if (isFirst()) {
					pop();
					if (getBeginType() == null) {
						return AFTER_ROOT;
					} else {
						nameLineNumber = in.getLineNumber();
						return AFTER_VALUE;
					}
				} else {
					set(JSONEventType.NULL, null, true);
					in.back();
					return AFTER_VALUE;
				}
			} else{
				throw createParseException(in, "json.parse.UnexpectedChar", (char)n);
			}
		case -1:
			if (getBeginType() == JSONEventType.START_OBJECT) {
				in.back();
				set(JSONEventType.NULL, null, true);
				return AFTER_VALUE;
			} else if (getBeginType() == JSONEventType.START_ARRAY) {
				throw createParseException(in, "json.parse.ArrayNotClosedError");
			} else {
				throw new IllegalStateException();
			}
		default:
			in.back();
			Object literal = parseLiteral(true);
			set(getParsedType(), literal, true);
			nameLineNumber = in.getLineNumber();
			return AFTER_VALUE;
		}
	}

	@Override
	int afterValue() throws IOException {
		int n = in.next();
		switch (n) {
		case ' ':
		case '\t':
		case '\r':
		case '\n':
			in.back();
			String ws = parseWhitespace();
			if (!isIgnoreWhitespace()) {
				set(JSONEventType.WHITESPACE, ws, false);
			}
			return AFTER_VALUE;
		case '/':
			in.back();
			String comment = parseComment();
			if (!isIgnoreWhitespace()) {
				set(JSONEventType.COMMENT, comment, false);
			}
			return AFTER_VALUE;
		case ',':
			if (getBeginType() == JSONEventType.START_OBJECT) {
				return BEFORE_NAME;
			} else if (getBeginType() == JSONEventType.START_ARRAY) {
				return BEFORE_VALUE;
			} else {
				throw createParseException(in, "json.parse.UnexpectedChar", (char)n);
			}
		case '}':
			if (getBeginType() == JSONEventType.START_OBJECT) {
				pop();
				if (getBeginType() == null) {
					if (emptyRoot) {
						throw createParseException(in, "json.parse.UnexpectedChar", (char)n);
					} else {
						return AFTER_ROOT;
					}
				} else {
					return AFTER_VALUE;
				}
			} else {
				throw createParseException(in, "json.parse.UnexpectedChar", (char)n);
			}
		case ']':
			if (getBeginType() == JSONEventType.START_ARRAY) {
				pop();
				if (getBeginType() == null) {
					return AFTER_ROOT;
				} else {
					return AFTER_VALUE;
				}
			} else {
				throw createParseException(in, "json.parse.UnexpectedChar", (char)n);
			}
		case -1:
			if (getBeginType() == JSONEventType.START_OBJECT) {
				pop();
				if (getBeginType() == null && emptyRoot) {
					return -1;
				} else {
					throw createParseException(in, "json.parse.ObjectNotClosedError");
				}
			} else if (getBeginType() == JSONEventType.START_ARRAY) {
				throw createParseException(in, "json.parse.ArrayNotClosedError");
			} else {
				throw new IllegalStateException();
			}
		default:
			if (in.getLineNumber() > nameLineNumber) {
				in.back();
				nameLineNumber = Long.MAX_VALUE;
				if (getBeginType() == JSONEventType.START_OBJECT) {
					return BEFORE_NAME;
				} else if (getBeginType() == JSONEventType.START_ARRAY) {
					return BEFORE_VALUE;
				} else {
					throw createParseException(in, "json.parse.UnexpectedChar", (char)n);
				}
			} else {
				throw createParseException(in, "json.parse.UnexpectedChar", (char)n);
			}
		}
	}

	void parseTokens(JSONEventType type, Object value) throws IOException {
		backupTokens = new LinkedList<Token>();

		loop:while (true) {
			int n = in.next();
			switch (n) {
			case ' ':
			case '\t':
			case '\r':
			case '\n':
				in.back();
				String ws = parseWhitespace();
				if (!isIgnoreWhitespace()) {
					backupTokens.add(new Token(JSONEventType.WHITESPACE, ws, false));
				}
				break;
			case '/':
				in.back();
				String comment = parseComment();
				if (!isIgnoreWhitespace()) {
					backupTokens.add(new Token(JSONEventType.COMMENT, comment, false));
				}
				break;
			case '{':
			case '[':
				if (isInterpretterMode()) {
					in.back();
					backupState = BEFORE_ROOT;
					break loop;
				}
				in.back();
			case ':':
				emptyRoot = true;
				push(JSONEventType.START_OBJECT);
				backupState = BEFORE_VALUE;
				break loop;
			case -1:
				backupState = -1;
				break loop;
			case ',':
				if (isInterpretterMode()) {
					backupState = BEFORE_ROOT;
					break loop;
				}
				throw createParseException(in, "json.parse.UnexpectedChar", (char)n);
			default:
				if (isInterpretterMode()) {
					in.back();
					backupState = BEFORE_ROOT;
					break loop;
				}
				throw createParseException(in, "json.parse.UnexpectedChar", (char)n);
			}
		}

		if (emptyRoot) {
			if (value != null) value = value.toString();
			backupTokens.addFirst(new Token(JSONEventType.NAME, value, false));
		} else {
			backupTokens.addFirst(new Token(type, value, true));
		}
	}

	@Override
	int otherState() throws IOException {
		Token token = backupTokens.removeFirst();
		set(token.type, token.value, token.isValue);
		if (backupTokens.isEmpty()) {
			backupTokens = null;
			return backupState;
		} else {
			return OTHER_STATE;
		}
	}
}
