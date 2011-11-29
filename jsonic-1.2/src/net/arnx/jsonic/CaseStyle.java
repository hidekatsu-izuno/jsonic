package net.arnx.jsonic;

public enum CaseStyle {
	LOWER_CASE,
	LOWER_CAMEL,
	LOWER_UNDERSCORE,
	LOWER_HYPHEN,
	UPPER_CASE,
	UPPER_CAMEL,
	UPPER_UNDERSCORE,
	UPPER_HYPHEN;
	
	public String to(String value) {
		if (value == null) {
			return value;
		}
		
		switch (this) {
		case LOWER_CASE:
			return value.toLowerCase();
		case LOWER_CAMEL:
			return null;
		case LOWER_UNDERSCORE:
			return null;
		case LOWER_HYPHEN:
			return null;
		case UPPER_CASE:
			return value.toUpperCase();
		case UPPER_CAMEL:
			return null;
		case UPPER_UNDERSCORE:
			return null;
		case UPPER_HYPHEN:
			return null;
		default:
			throw new UnsupportedOperationException();
		}
	}
}
