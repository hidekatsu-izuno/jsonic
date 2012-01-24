package net.arnx.jsonic.internal.parser;

import java.io.IOException;

import net.arnx.jsonic.JSONEventType;

public interface JSONParser {
	public JSONEventType next() throws IOException;
	
	public Object getValue();
	
	public int getDepth();
}
