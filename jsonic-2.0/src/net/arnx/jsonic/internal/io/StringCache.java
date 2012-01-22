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
		return new BigDecimal(toString());
	}
	
	@Override
	public String toString() {
		return new String(cbuf, 0, clen);
	}
	
	private void expand(int newLength) {
		int ncapacity = (cbuf.length + 1) * 2;
		if (ncapacity < newLength) {
			ncapacity = newLength;
		}
		cbuf = Arrays.copyOf(cbuf, ncapacity);
	}
}
