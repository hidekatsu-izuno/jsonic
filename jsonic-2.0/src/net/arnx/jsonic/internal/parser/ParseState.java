package net.arnx.jsonic.internal.parser;

import java.io.IOException;

interface ParseState {
	public ParseState next(ParseContext context) throws IOException;
}
