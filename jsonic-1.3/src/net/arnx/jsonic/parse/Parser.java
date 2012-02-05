package net.arnx.jsonic.parse;

import java.io.IOException;

import net.arnx.jsonic.JSONEventType;

public interface Parser {
	public ParseContext getContext();
	
	public JSONEventType next() throws IOException;
	
	public Object getValue();
	
	public int getDepth();
}
