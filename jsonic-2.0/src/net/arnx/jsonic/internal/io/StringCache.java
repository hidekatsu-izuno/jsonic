package net.arnx.jsonic.internal.io;

import java.math.BigDecimal;
import java.util.Arrays;

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
		public void clear() {
		}
		
		public BigDecimal toBigDecimal() {
			return null;
		};
		
		@Override
		public String toString() {
			return null;
		}
	};
	
	private char[] cbuf;
	private int clen = 0;
	
	private String[] scache = new String[256];
	private BigDecimal[] dcache = new BigDecimal[256];
	
	private StringCache() {
	}
	
	public StringCache(int size) {
		cbuf = new char[size];
	}
	
	public void append(char c) {
        int nlen = clen + 1;
        if (nlen > cbuf.length) expand(nlen);
        cbuf[clen++] = c;
	}
	
	public void append(String str, int start, int end) {
		int len = end - start;
		if (len == 0) return;
		
        int nlen = clen + len;
        if (nlen > cbuf.length) expand(nlen);
        str.getChars(start, end, cbuf, clen);
        clen = nlen;
	}
	
	public void append(StringBuilder sb, int start, int end) {
		int len = end - start;
		if (len == 0) return;
		
        int nlen = clen + len;
        if (nlen > cbuf.length) expand(nlen);
        sb.getChars(start, end, cbuf, clen);
        clen = nlen;
	}
	
	public void append(StringBuffer sb, int start, int end) {
		int len = end - start;
		if (len == 0) return;
		
        int nlen = clen + len;
        if (nlen > cbuf.length) expand(nlen);
        sb.getChars(start, end, cbuf, clen);
        clen = nlen;
	}
	
	public void append(CharSequence cs, int start, int end) {
		int len = end - start;
		if (len == 0) return;
		
        int nlen = clen + len;
        if (nlen > cbuf.length) expand(nlen);
        for (int i = start; i < end; i++) {
        	cbuf[clen++] = cs.charAt(i);
        }
        clen = nlen;
	}
	
	public void append(char[] buf, int offset, int len) {
		if (len == 0) return;
		
        int nlen = clen + len;
        if (nlen > cbuf.length) expand(nlen);
        System.arraycopy(buf, offset, cbuf, clen, len);
        clen = nlen;
	}
	
	public void clear() {
		clen = 0;
	}
	
	public BigDecimal toBigDecimal() {
		if (clen == 1) {
			if (cbuf[0] == '0') {
				return BigDecimal.ZERO;
			} else if (cbuf[0] == '1') {
				return BigDecimal.ONE;
			}
		}
		
		if (clen < 32) {
			int index = index();
			if (index < 0) {
				return new BigDecimal(cbuf, 0, clen);
			}
						
			String str = scache[index];
			BigDecimal num = dcache[index];
			if (str == null || str.length() != clen) {
				str = new String(cbuf, 0, clen);
				num = new BigDecimal(str);
				scache[index] = str;
				dcache[index] = num;
				return num;
			}
			
			for (int i = 0; i < clen; i++) {
				if (str.charAt(i) != cbuf[i]) {
					str = new String(cbuf, 0, clen);
					num = new BigDecimal(str);
					scache[index] = str;
					dcache[index] = num;
					return num;
				}
			}
			
			if (num == null) {
				num = new BigDecimal(str);
				dcache[index] = num;
			}
			return num;
		}
		
		return new BigDecimal(cbuf, 0, clen);
	}
	
	@Override
	public String toString() {
		if (clen == 0) return "";
		
		if (clen < 32) {
			int index = index();
			if (index < 0) {
				return new String(cbuf, 0, clen);
			}
			
			String str = scache[index];
			if (str == null || str.length() != clen) {
				str = new String(cbuf, 0, clen);
				scache[index] = str;
				dcache[index] = null;
				return str;
			}
			
			for (int i = 0; i < clen; i++) {
				if (str.charAt(i) != cbuf[i]) {
					str = new String(cbuf, 0, clen);
					scache[index] = str;
					dcache[index] = null;
					return str;
				}
			}
			return str;
		}
		
		return new String(cbuf, 0, clen);
	}
	
	private int index() {
		int h = 0;
		for (int i = 0; i < clen; i++) {
			if (cbuf[i] < 128) {
				h = h * 32 + cbuf[i];
			} else {
				return -1;
			}
		}
		h ^= (h >>> 20) ^ (h >>> 12);
		h ^= (h >>> 7) ^ (h >>> 4);
		
		return h & (scache.length-1);		
	}
	
	private void expand(int newLength) {
		int ncapacity = (cbuf.length + 1) * 2;
		if (ncapacity < newLength) {
			ncapacity = newLength;
		}
		cbuf = Arrays.copyOf(cbuf, ncapacity);
	}
}
