package net.arnx.jsonic;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Locale;

import net.arnx.jsonic.internal.io.CharSequenceInputSource;
import net.arnx.jsonic.internal.io.InputSource;
import net.arnx.jsonic.internal.io.ReaderInputSource;
import net.arnx.jsonic.internal.io.StringBufferInputSource;
import net.arnx.jsonic.internal.io.StringBuilderInputSource;
import net.arnx.jsonic.internal.io.StringInputSource;
import net.arnx.jsonic.internal.parser.JSONParser;

public class JSONReader {
	private JSONMode mode = JSONMode.TRADITIONAL;
	private Locale locale = Locale.getDefault();
	private int maxDepth = 32;
	private boolean ignoreWhirespace = true;
	private InputSource in;
	
	private JSONParser parser;

	public JSONReader(CharSequence cs) {
		this(getInputSource(cs));
	}
	
	public JSONReader(InputStream in) throws IOException {
		this(new ReaderInputSource(in));
	}
	
	public JSONReader(Reader reader) throws IOException {
		this(new ReaderInputSource(reader));
	}
	
	private static InputSource getInputSource(CharSequence cs) {
		if (cs instanceof String) {
			return new StringInputSource((String)cs);
		} else if (cs instanceof StringBuilder) {
			return new StringBuilderInputSource((StringBuilder)cs);
		} else if (cs instanceof StringBuffer) {
			return new StringBufferInputSource((StringBuffer)cs);
		} else {
			return new CharSequenceInputSource(cs);
		}
	}
	
	private JSONReader(InputSource in) {
		this.in = in;
	}
	
	public void setMode(JSONMode mode) {
		this.mode = mode;
	}
	
	public void setLocale(Locale locale) {
		this.locale = locale;
	}
	
	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}
	
	public void setIgnoreWhitespace(boolean ignore) {
		this.ignoreWhirespace = ignore;
	}
	
	public JSONEventType next() throws IOException {
		return parser.next();
	}
	
	public Object getValue() {
		return parser.getValue();
	}
	
	public int getDepth() {
		return parser.getDepth();
	}
	
	public void close() {
		
	}
}
