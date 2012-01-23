package net.arnx.jsonic.internal.parser;

public enum JSONEventType {
	START_OBJECT,
	END_OBJECT,
	START_ARRAY,
	END_ARRAY,
	NAME,
	STRING,
	NUMBER,
	TRUE,
	FALSE,
	NULL
}
