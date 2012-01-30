package net.arnx.jsonic.util;

import java.math.BigDecimal;

public class ValueCache {
	private static final int CACHE_SIZE = 256;
	
	private String[] stringCache = new String[CACHE_SIZE];
	private BigDecimal[] numberCache = new BigDecimal[CACHE_SIZE];
	
	public ValueCache() {
	}
	
	public String getString(char[] cs, int offset, int len) {
		if (len == 0) return "";
		
		if (len < 32) {
			int index = getCacheIndex(cs, offset, len);
			if (index < 0) {
				return new String(cs, offset, len);
			}
			
			String str = stringCache[index];
			if (str == null || str.length() != len) {
				str = new String(cs, offset, len);
				stringCache[index] = str;
				numberCache[index] = null;
				return str;
			}
			
			for (int i = 0; i < len; i++) {
				if (str.charAt(i) != cs[offset + i]) {
					str = new String(cs, offset, len);
					stringCache[index] = str;
					numberCache[index] = null;
					return str;
				}
			}
			return str;
		}
		
		return new String(cs, offset, len);
	}
	
	public BigDecimal getBigDecimal(char[] cs, int offset, int len) {
		if (len == 1) {
			if (cs[offset] == '0') {
				return BigDecimal.ZERO;
			} else if (cs[offset] == '1') {
				return BigDecimal.ONE;
			}
		}
		
		if (len < 32) {
			int index = getCacheIndex(cs, offset, len);
			if (index < 0) {
				return new BigDecimal(cs, offset, len);
			}
			
			String str = stringCache[index];
			BigDecimal num = numberCache[index];
			if (str == null || str.length() != len) {
				str = new String(cs, offset, len);
				num = new BigDecimal(str);
				stringCache[index] = str;
				numberCache[index] = num;
				return num;
			}
			
			for (int i = 0; i < len; i++) {
				if (str.charAt(i) != cs[offset + i]) {
					str = new String(cs, offset, len);
					num = new BigDecimal(str);
					stringCache[index] = str;
					numberCache[index] = num;
					return num;
				}
			}
			
			if (num == null) {
				num = new BigDecimal(str);
				numberCache[index] = num;
			}
			return num;
		}
		
		return new BigDecimal(cs, offset, len);
	}
	
	private static int getCacheIndex(char[] cs, int offset, int len) {
		int h = 0;
		for (int i = 0; i < len; i++) {
			char c = cs[offset + i];
			if (c < 128) {
				h = h * 32 + c;
			} else {
				return -1;
			}
		}
		h ^= (h >>> 20) ^ (h >>> 12);
		h ^= (h >>> 7) ^ (h >>> 4);
		
		return h & (CACHE_SIZE-1);
	}
	
	public String getString(StringBuilder sb, int offset, int len) {
		if (len == 0) return "";
		
		if (len < 32) {
			int index = getCacheIndex(sb, offset, len);
			if (index < 0) {
				return sb.substring(offset, offset + len);
			}
			
			String str = stringCache[index];
			if (str == null || str.length() != len) {
				str = sb.substring(offset, offset + len);
				stringCache[index] = str;
				numberCache[index] = null;
				return str;
			}
			
			for (int i = 0; i < len; i++) {
				if (str.charAt(i) != sb.charAt(offset + i)) {
					str = sb.substring(offset, offset + len);
					stringCache[index] = str;
					numberCache[index] = null;
					return str;
				}
			}
			return str;
		}
		
		return sb.substring(offset, offset + len);
	}
	
	public BigDecimal getBigDecimal(StringBuilder sb, int offset, int len) {
		if (len == 1) {
			if (sb.charAt(offset) == '0') {
				return BigDecimal.ZERO;
			} else if (sb.charAt(offset) == '1') {
				return BigDecimal.ONE;
			}
		}
		
		if (len < 32) {
			int index = getCacheIndex(sb, offset, len);
			if (index < 0) {
				return new BigDecimal(sb.substring(offset, offset + len));
			}
			
			String str = stringCache[index];
			BigDecimal num = numberCache[index];
			if (str == null || str.length() != len) {
				str = sb.substring(offset, offset + len);
				num = new BigDecimal(str);
				stringCache[index] = str;
				numberCache[index] = num;
				return num;
			}
			
			for (int i = 0; i < len; i++) {
				if (str.charAt(i) != sb.charAt(offset + i)) {
					str = sb.substring(offset, offset + len);
					num = new BigDecimal(str);
					stringCache[index] = str;
					numberCache[index] = num;
					return num;
				}
			}
			
			if (num == null) {
				num = new BigDecimal(str);
				numberCache[index] = num;
			}
			return num;
		}
		
		return new BigDecimal(sb.substring(offset, offset + len));
	}
	
	public String getString(StringBuffer sb, int offset, int len) {
		if (len == 0) return "";
		
		if (len < 32) {
			int index = getCacheIndex(sb, offset, len);
			if (index < 0) {
				return sb.substring(offset, offset + len);
			}
			
			String str = stringCache[index];
			if (str == null || str.length() != len) {
				str = sb.substring(offset, offset + len);
				stringCache[index] = str;
				numberCache[index] = null;
				return str;
			}
			
			for (int i = 0; i < len; i++) {
				if (str.charAt(i) != sb.charAt(offset + i)) {
					str = sb.substring(offset, offset + len);
					stringCache[index] = str;
					numberCache[index] = null;
					return str;
				}
			}
			return str;
		}
		
		return sb.substring(offset, offset + len);
	}
	
	public BigDecimal getBigDecimal(StringBuffer sb, int offset, int len) {
		if (len == 1) {
			if (sb.charAt(offset) == '0') {
				return BigDecimal.ZERO;
			} else if (sb.charAt(offset) == '1') {
				return BigDecimal.ONE;
			}
		}
		
		if (len < 32) {
			int index = getCacheIndex(sb, offset, len);
			if (index < 0) {
				return new BigDecimal(sb.substring(offset, offset + len));
			}
			
			String str = stringCache[index];
			BigDecimal num = numberCache[index];
			if (str == null || str.length() != len) {
				str = sb.substring(offset, offset + len);
				num = new BigDecimal(str);
				stringCache[index] = str;
				numberCache[index] = num;
				return num;
			}
			
			for (int i = 0; i < len; i++) {
				if (str.charAt(i) != sb.charAt(offset + i)) {
					str = sb.substring(offset, offset + len);
					num = new BigDecimal(str);
					stringCache[index] = str;
					numberCache[index] = num;
					return num;
				}
			}
			
			if (num == null) {
				num = new BigDecimal(str);
				numberCache[index] = num;
			}
			return num;
		}
		
		return new BigDecimal(sb.substring(offset, offset + len));
	}
	
	public String getString(CharSequence cs, int offset, int len) {
		if (len == 0) return "";
		
		if (len < 32) {
			int index = getCacheIndex(cs, offset, len);
			if (index < 0) {
				return cs.subSequence(offset, offset + len).toString();
			}
			
			String str = stringCache[index];
			if (str == null || str.length() != len) {
				str = cs.subSequence(offset, offset + len).toString();
				stringCache[index] = str;
				numberCache[index] = null;
				return str;
			}
			
			for (int i = 0; i < len; i++) {
				if (str.charAt(i) != cs.charAt(offset + i)) {
					str = cs.subSequence(offset, offset + len).toString();
					stringCache[index] = str;
					numberCache[index] = null;
					return str;
				}
			}
			return str;
		}
		
		return cs.subSequence(offset, offset + len).toString();
	}
	
	public BigDecimal getBigDecimal(CharSequence cs, int offset, int len) {
		if (len == 1) {
			if (cs.charAt(offset) == '0') {
				return BigDecimal.ZERO;
			} else if (cs.charAt(offset) == '1') {
				return BigDecimal.ONE;
			}
		}
		
		if (len < 32) {
			int index = getCacheIndex(cs, offset, len);
			if (index < 0) {
				return new BigDecimal(cs.subSequence(offset, offset + len).toString());
			}
			
			String str = stringCache[index];
			BigDecimal num = numberCache[index];
			if (str == null || str.length() != len) {
				str = cs.subSequence(offset, offset + len).toString();
				num = new BigDecimal(str);
				stringCache[index] = str;
				numberCache[index] = num;
				return num;
			}
			
			for (int i = 0; i < len; i++) {
				if (str.charAt(i) != cs.charAt(offset + i)) {
					str = cs.subSequence(offset, offset + len).toString();
					num = new BigDecimal(str);
					stringCache[index] = str;
					numberCache[index] = num;
					return num;
				}
			}
			
			if (num == null) {
				num = new BigDecimal(str);
				numberCache[index] = num;
			}
			return num;
		}
		
		return new BigDecimal(cs.subSequence(offset, offset + len).toString());
	}

	private static int getCacheIndex(CharSequence cs, int offset, int len) {
		int h = 0;
		for (int i = 0; i < cs.length(); i++) {
			char c = cs.charAt(i);
			if (c < 128) {
				h = h * 32 + c;
			} else {
				return -1;
			}
		}
		h ^= (h >>> 20) ^ (h >>> 12);
		h ^= (h >>> 7) ^ (h >>> 4);
		
		return h & (CACHE_SIZE-1);
	}
}
