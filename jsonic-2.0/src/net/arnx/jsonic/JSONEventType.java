package net.arnx.jsonic;

public enum JSONEventType {
	START_OBJECT(true),
	END_OBJECT(true),
	START_ARRAY(true),
	END_ARRAY(true),
	NAME(true),
	STRING(true),
	NUMBER(true),
	TRUE(true),
	FALSE(true),
	NULL(true),
	WHITESPACE(false),
	COMMENT(false);
	
	private boolean isValue;
	
	JSONEventType(boolean isValue) {
		this.isValue = isValue;
	}
	
	boolean isValue() {
		return isValue;
	}
}
