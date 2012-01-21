package net.arnx.jsonic.internal.parser;

public enum JSONEventType {
	BEGIN_OBJECT,
	END_OBJECT,
	BEGIN_ARRAY,
	END_ARRAY,
	NAME,
	STRING,
	NUMBER,
	TRUE,
	FALSE,
	NULL
}
