package net.arnx.jsonic.internal.parser;

import java.io.IOException;

public interface JSONParser {
	public TokenType next() throws IOException;
	
	public Object getValue();
}
