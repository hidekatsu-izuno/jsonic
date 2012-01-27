package net.arnx.jsonic.parser;

import java.io.IOException;

import net.arnx.jsonic.JSONEventType;

public interface Parser {
	public JSONEventType next() throws IOException;
	
	public Object getValue();
	
	public int getDepth();
}
