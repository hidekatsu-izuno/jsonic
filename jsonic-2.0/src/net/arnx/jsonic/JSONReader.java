package net.arnx.jsonic;

import java.io.IOException;

import net.arnx.jsonic.internal.io.InputSource;
import net.arnx.jsonic.internal.parser.JSONParser;
import net.arnx.jsonic.internal.parser.ParseContext;
import net.arnx.jsonic.internal.parser.ScriptJSONParser;
import net.arnx.jsonic.internal.parser.StrictJSONParser;
import net.arnx.jsonic.internal.parser.TraditionalJSONParser;

public class JSONReader {
	private JSONParser parser;
	
	JSONReader(JSONMode mode, InputSource in, ParseContext context) {
		switch (mode) {
		case STRICT:
			parser = new StrictJSONParser(in, context);
			break;
		case SCRIPT:
			parser = new ScriptJSONParser(in, context);
			break;
		default:
			parser = new TraditionalJSONParser(in, context);
		}
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
}
