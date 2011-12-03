package net.arnx.jsonic;

public enum CaseStyle {
	LOWER_CASE {
		@Override
		public String to(String value) {
			return toSimpleCase(value, false);
		}
	},
	
	LOWER_CAMEL {
		@Override
		public String to(String value) {
			return toCamelCase(value, false);
		}
	},
	
	LOWER_SPACE {
		@Override
		public String to(String value) {
			return toSeparatedCase(value, false, ' ');
		}
	},
	
	LOWER_HYPHEN {
		@Override
		public String to(String value) {
			return toSeparatedCase(value, false, '-');
		}
	},
	
	LOWER_UNDERSCORE {
		@Override
		public String to(String value) {
			return toSeparatedCase(value, false, '_');
		}
	},
	
	UPPER_CASE {
		@Override
		public String to(String value) {
			return toSimpleCase(value, true);
		}
	},
	
	UPPER_CAMEL {
		@Override
		public String to(String value) {
			return toCamelCase(value, true);
		}
	},
	
	UPPER_SPACE {
		@Override
		public String to(String value) {
			return toSeparatedCase(value, true, ' ');
		}
	},
	
	UPPER_HYPHEN {
		@Override
		public String to(String value) {
			return toSeparatedCase(value, true, '-');
		}
	},
	
	UPPER_UNDERSCORE {
		@Override
		public String to(String value) {
			return toSeparatedCase(value, true, '_');
		}
	};
	
	public abstract String to(String value);
	
	private static final int SEPARATOR = 1;
	private static final int LOWER = 2;
	private static final int UPPER = 3;
	private static final int NUMBER = 4;
	private static final int OTHER = 9;
	private static final int[] MAP = new int[128];
	
	static {
		for (int i = 0; i < MAP.length; i++) {
			if (i >= 'A' && i <= 'Z') {
				MAP[i] = UPPER;
			} else if (i >= 'a' && i <= 'z') {
				MAP[i] = LOWER;
			} else if (i >= '0' && i <= '9') {
				MAP[i] = NUMBER;
			} else if (i == ' ' || i == '+'|| i == ',' || i == '-' || i == '.' || i == '_') {
				MAP[i] = SEPARATOR;
			} else {
				MAP[i] = OTHER;
			}
		}
	}
	
	private static String toSimpleCase(String value, boolean upper) {
		if (value == null || value.isEmpty()) {
			return value;
		}
		char[] ca = value.toCharArray();
		for (int i = 0; i < ca.length; i++) {
			int type = getType(ca[i]);
			if (upper && type == LOWER) {
				ca[i] = (char)(ca[i] - 32);
			} else if (!upper && type == UPPER) {
				ca[i] = (char)(ca[i] + 32);
			}
		}
		return String.valueOf(ca);
	}
	
	private static String toCamelCase(String value, boolean upper) {
		if (value == null || value.isEmpty()) {
			return value;
		}
		
		int start;
		int end;
		for (start = 0; start < value.length(); start++) {
			int type = getType(value.charAt(start));
			if (type != SEPARATOR && type != OTHER) break;
		}
		for (end = 0; end < value.length()-start; end++) {
			int type = getType(value.charAt(value.length()-end-1));
			if (type != SEPARATOR && type != OTHER) break;
		}
		
		int index = -1;
		for (int i = start; i < value.length()-end; i++) {
			if (getType(value.charAt(i)) == SEPARATOR) {
				index = i;
				break;
			}
		}
		
		if (index == -1) {
			int type = getType(value.charAt(start));
			if (type == UPPER) {
				char[] ca = value.toCharArray();
				if (!upper) ca[start] = (char)(ca[start] + 32);
				for (int i = start+1; i < ca.length - end; i++) {
					if (getType(ca[i]) != UPPER) {
						break;
					} else if (i+1 < ca.length - end) {
						int next = getType(ca[i+1]);
						if (next != UPPER && next != NUMBER) {
							break;
						}
					}
					ca[i] = (char)(ca[i] + 32);	
				}
				return String.valueOf(ca);
			} else if (upper && type == LOWER) {
				char[] ca = value.toCharArray();
				ca[start] = (char)(ca[start] - 32);
				return String.valueOf(ca);
			} else {
				return value;
			}
		} else {
			char[] ca = value.toCharArray();
			int pos = start;
			for (int i = start; i < ca.length - end; i++) {
				if (getType(ca[i]) == SEPARATOR) {
					upper = true;
				} else if (upper) {
					ca[pos++] = (getType(ca[i]) == LOWER) ? (char)(ca[i] - 32) : ca[i];
					upper = false;
				} else {
					ca[pos++] = (getType(ca[i]) == UPPER) ? (char)(ca[i] + 32) : ca[i];
					upper = false;
				}
			}
			for (int i = 0; i < end; i++) {
				ca[pos++] = ca[ca.length-end];
			}
			return String.valueOf(ca, 0, pos);
		}		
	}
	
	private static String toSeparatedCase(String value, boolean upper, char sep) {
		if (value == null || value.isEmpty()) {
			return value;
		}
		
		int start;
		int end;
		for (start = 0; start < value.length(); start++) {
			int type = getType(value.charAt(start));
			if (type != SEPARATOR && type != OTHER) break;
		}
		for (end = 0; end < value.length()-start; end++) {
			int type = getType(value.charAt(value.length()-end-1));
			if (type != SEPARATOR && type != OTHER) break;
		}
		
		StringBuilder sb = new StringBuilder((int)(value.length() * 1.5));
		if (start > 0) sb.append(value, 0, start);
		int prev = -1;
		for (int i = start; i < value.length() - end; i++) {
			char c = value.charAt(i);
			int type = getType(c);
			if (type == UPPER && prev != -1) {
				if (prev != UPPER && prev != SEPARATOR) {
					sb.append(sep);
				} else if (i+1 < value.length() - end) {
					int next = getType(value.charAt(i+1));
					if (next != UPPER && next != NUMBER && next != SEPARATOR) {
						sb.append(sep);
					}
				}
			}
			if (type == SEPARATOR) {
				sb.append(sep);
			} else if (upper && type == LOWER) {
				sb.append((char)(c - 32));
			} else if (!upper && type == UPPER) {
				sb.append((char)(c + 32));
			} else {
				sb.append(c);
			}
			prev = type;
		}
		if (end > 0) sb.append(value, value.length()-end, value.length());
		return sb.toString();
	}
	
	private static int getType(char c) {
		return (c < MAP.length) ? MAP[c] : 0;
	}
}
