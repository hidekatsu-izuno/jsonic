package net.arnx.jsonic;

/**
 * JSON event types for Pull Parser (getReader). 
 */
public enum JSONEventType {
	START_OBJECT,
	END_OBJECT,
	START_ARRAY,
	END_ARRAY,
	NAME,
	STRING,
	NUMBER,
	BOOLEAN,
	NULL,
	WHITESPACE,
	COMMENT
}
