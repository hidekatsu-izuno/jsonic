/* 
 * Copyright 2014 Hidekatsu Izuno
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.arnx.jsonic.io;

import java.math.BigDecimal;

import net.arnx.jsonic.parse.CompactNumber;
import net.arnx.jsonic.util.LocalCache;

public class CharSequenceInputSource implements InputSource {
	private int lines = 1;
	private int columns = 0;
	private int offset = 0;
	
	private int start = 0;
	int mark = -1;
	
	private final CharSequence cs;
	private final boolean plainStrings;
	
	public CharSequenceInputSource(CharSequence cs) {
		if (cs == null) {
			throw new NullPointerException();
		}
		this.cs = cs;
		// String.indexOf uses a bulk scan. Escaped documents keep the full parser
		// throughout, avoiding repeated probes before escape decoding.
		this.plainStrings = cs instanceof String && ((String)cs).indexOf('\\') < 0;
	}
	
	/**
	 * Reads an unescaped quoted string after its opening quote. On failure,
	 * returns null without moving the cursor, so the full parser can take over.
	 */
	public String readPlainString(int quote, LocalCache cache) {
		if (!plainStrings) return null;
		for (int end = start; end < cs.length(); end++) {
			char c = cs.charAt(end);
			if (c == quote) {
				String value = cache.getString(cs, start, end);
				int consumed = end + 1 - start;
				start += consumed;
				offset += consumed;
				columns += consumed;
				return value;
			}
			if (c == '\\' || c < 0x20 || c == 0x7F) return null;
		}
		return null;
	}

	/** Reads a short decimal, or leaves the cursor untouched for the full parser. */
	public CompactNumber readCompactNumber() {
		return (CompactNumber)readNumber(true);
	}

	/** Same validated scan, retaining BigDecimal and its decimal scale. */
	public BigDecimal readDecimalNumber() {
		return (BigDecimal)readNumber(false);
	}

	private Object readNumber(boolean compact) {
		if (!(cs instanceof String)) return null;
		int end = start;
		int limit = cs.length();
		if (end >= limit) return null;
		boolean negative = cs.charAt(end) == '-';
		if (negative && ++end == limit) return null;
		int first = end;
		long value = 0;
		int digits = 0;
		int fraction = -1;
		while (end < limit) {
			char c = cs.charAt(end);
			if (c >= '0' && c <= '9') {
				if (++digits > 18 || (fraction < 0 && end > first && cs.charAt(first) == '0')) return null;
				value = value * 10 + c - '0';
			} else if (c == '.' && fraction < 0 && end > first) {
				fraction = end + 1;
			} else {
				// Retain the legacy scanner for invalid syntax, exponents, EOF,
				// and line breaks (including its existing error position handling).
				if (digits == 0 || fraction == end
						|| (c != ',' && c != ']' && c != '}' && c != ' ' && c != '\t')) return null;
				long signed = negative ? -value : value;
				int scale = fraction < 0 ? 0 : end - fraction;
				Object number = compact ? CompactNumber.of(signed, scale) : BigDecimal.valueOf(signed, scale);
				int consumed = end - start;
				start = end;
				offset += consumed;
				columns += consumed;
				return number;
			}
			end++;
		}
		return null;
	}

	@Override
	public int next() {
		int n = -1;
		if (start < cs.length()) {
			n = cs.charAt(start++);
			offset++;
			if (n == '\r') {
				lines++;
				columns = 0;
			} else if (n == '\n') {
				if (offset < 2 || cs.charAt(offset-2) != '\r') {
					lines++;
					columns = 0;
				}
			} else {
				columns++;
			}
		} else {
			start++;
			return -1;
		}
		return n;
	}
	
	@Override
	public void back() {
		if (start == 0) {
			throw new IllegalStateException("no backup charcter");
		}
		start--;
		if (start < cs.length()) {
			offset--;
			columns--;
		}
	}
	
	@Override
	public int mark() {
		mark = start;
		return cs.length() - mark;
	}
	
	@Override
	public void copy(StringBuilder sb, int len) {
		if (mark == -1) throw new IllegalStateException("no mark");
		if (mark + len > cs.length()) throw new IndexOutOfBoundsException();
		
		sb.append(cs, mark, mark + len);
	}
	
	@Override
	public String copy(int len) {
		if (mark == -1) throw new IllegalStateException("no mark");
		if (mark + len > cs.length()) throw new IndexOutOfBoundsException();
		
		char[] array = new char[len];
		for (int i = 0; i < len; i++) {
			array[i] = cs.charAt(mark + i);
		}
		return String.valueOf(array);
	}
	
	@Override
	public long getLineNumber() {
		return lines;
	}
	
	@Override
	public long getColumnNumber() {
		return columns;
	}
	
	@Override
	public long getOffset() {
		return offset;
	}
	
	@Override
	public String toString() {
		int spos = 0;
		int max = Math.min(start-1, cs.length()-1);
		int charCount = 0;
		for (int i = 0; i < max + 1 && i < 20; i++) {
			char c = cs.charAt(max-i);
			if (c == '\r' || (c == '\n' && (max-i-1 < 0 || cs.charAt(max-i-1) != '\r'))) {
				if (charCount > 0) break;
			} else if (c != '\n') {
				spos = max-i;
				charCount++;
			}
		}
		return (spos <= max) ? cs.subSequence(spos, max+1).toString() : "";
	}
}