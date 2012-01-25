package net.arnx.jsonic;

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
	NULL,
	WHITESPACE,
	COMMENT
}
