package net.arnx.jsonic.internal.io;

import java.math.BigDecimal;

public class StringCache {
	public static final StringCache EMPTY_CACHE = new StringCache() {
		@Override
		public void append(char c) {
		}
		
		@Override
		public void append(CharSequence cs, int start, int end) {
		}
		
		@Override
		public void append(char[] buf, int offset, int len) {
		}
		
		@Override
		public void setLength(int len) {
		}
		
		public BigDecimal toBigDecimal() {
			return null;
		};
		
		@Override
		public String toString() {
			return null;
		}
	};
	
	private StringBuilder sb;
	
	private StringCache() {
	}
	
	public StringCache(int size) {
		sb = new StringBuilder(size);
	}
	
	public void append(char c) {
		sb.append(c);
	}
	public void append(CharSequence cs, int start, int end) {
		sb.append(cs, start, end);
	}
	
	public void append(char[] buf, int offset, int len) {
		sb.append(buf, offset, len);
	}
	
	public void setLength(int len) {
		sb.setLength(len);
	}
	
	public BigDecimal toBigDecimal() {
		return new BigDecimal(toString());
	}
	
	@Override
	public String toString() {
		return sb.toString();
	}
}
