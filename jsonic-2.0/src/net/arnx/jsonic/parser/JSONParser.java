package net.arnx.jsonic.parser;

import java.io.IOException;

public interface JSONParser {
	public TokenType next() throws IOException;
	public Object getValue();
}
