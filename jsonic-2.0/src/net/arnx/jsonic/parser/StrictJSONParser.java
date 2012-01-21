package net.arnx.jsonic.parser;

import java.io.IOException;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import net.arnx.jsonic.JSONException;
import net.arnx.jsonic.internal.io.CharSequenceInputSource;
import net.arnx.jsonic.internal.io.InputSource;
import net.arnx.jsonic.internal.io.ReaderInputSource;

public class StrictJSONParser implements JSONParser {
	private InputSource in;
	private Locale locale = Locale.getDefault();
	
	private int state = 0;
	private Object value;
	
	public StrictJSONParser(CharSequence cs) {
		in = new CharSequenceInputSource(cs);
	}
	
	public StrictJSONParser(Reader reader) {
		in = new ReaderInputSource(reader);
	}
	
	public void setLocale(Locale locale) {
		this.locale = locale;
	}
	
	@Override
	public TokenType next() throws IOException {
		value = null;
		
		switch (state) {
		case 0: return parseBeforeRoot();
		case 1: return parseAfterRoot();
		case 2: return parseBeforeObjectName();
		case 3: return parseAfterObjectName();
		case 4: return parseBeforeObjectValue();
		case 5: return parseAfterObjectValue();
		case 6: return parseBeforeArrayValue();
		case 7: return parseAfterArrayValue();
		default: throw new IllegalStateException();
		}
	}
	
	private TokenType parseBeforeRoot() throws IOException {
		int n = -1;
		while ((n = in.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case '\r':
			case '\n':
			case ' ':
			case '\t':
			case 0xFEFF: // BOM
				continue;
			case '{':
				return TokenType.BEGIN_OBJECT;
			case '[':
				return TokenType.BEGIN_ARRAY;
			default:
				throw createParseException(getMessage("json.parse.UnexpectedChar", c), in);
			}
		}
		throw createParseException(getMessage("json.parse.EmptyInputError"), in);
	}
	
	private TokenType parseAfterRoot() throws IOException {
		return null;
	}
	
	private TokenType parseBeforeObjectName() throws IOException {
		return null;
	}
	
	private TokenType parseAfterObjectName() throws IOException {
		return null;
	}
	
	private TokenType parseBeforeObjectValue() throws IOException {
		return null;
	}
	
	private TokenType parseAfterObjectValue() throws IOException {
		return null;
	}
	
	private TokenType parseBeforeArrayValue() throws IOException {
		return null;
	}
	
	private TokenType parseAfterArrayValue() throws IOException {
		
		return null;
	}

	@Override
	public Object getValue() {
		return value;
	}
	
	JSONException createParseException(String message, InputSource s) {
		return new JSONException("" + s.getLineNumber() + ": " + message + "\n" + s.toString() + " <- ?",
				JSONException.PARSE_ERROR, s.getLineNumber(), s.getColumnNumber(), s.getOffset());
	}
	
	String getMessage(String id, Object... args) {
		ResourceBundle bundle = ResourceBundle.getBundle("net.arnx.jsonic.Messages", locale);
		return MessageFormat.format(bundle.getString(id), args);
	}
}
