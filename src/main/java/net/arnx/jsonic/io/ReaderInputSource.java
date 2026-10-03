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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;

import java.math.BigDecimal;

import net.arnx.jsonic.parse.CompactNumber;

public class ReaderInputSource implements InputSource {
	private static int BACK = 20;
	
	private long lines = 1L;
	private long columns = 0L;
	private long offset = 0L;
	
	private InputStream in;
	private Reader reader;
	private final char[] buf = new char[1024];
	private int back = BACK;
	private int start = BACK;
	private int end = BACK - 1;
	private int mark = -1;
	private int plainStart = -1;
	private int plainEnd;
	private int plainQuote;
	
	public ReaderInputSource(InputStream in) {
		if (in == null) throw new NullPointerException();
		this.in = in;
	}
	
	public ReaderInputSource(Reader reader) {
		if (reader == null) throw new NullPointerException();
		this.reader = reader;
	}
	
	/** Reads a complete plain string; failed probes leave the cursor and mark unchanged. */
	public String readPlainString(int quote, net.arnx.jsonic.util.LocalCache cache) {
		if (plainStart == start && plainQuote == quote) return null;
		int cursor = start;
		while (cursor <= end) {
			char c = buf[cursor];
			if (c == quote) {
				String value = cache.getBufferedString(buf, start, cursor);
				plainStart = -1;
				mark = start;
				int consumed = cursor + 1 - start;
				start = cursor + 1;
				offset += consumed;
				columns += consumed;
				return value;
			}
			if (c == '\\' || c < 0x20 || c == 0x7F) break;
			cursor++;
		}
		// The ordinary scanner can reuse this prefix until the buffer is refilled.
		plainStart = start;
		plainEnd = cursor;
		plainQuote = quote;
		return null;
	}

	/** Advances over ordinary string characters already present in the buffer. */
	public int readPlainStringPrefix(int quote) {
		int cursor = start;
		if (plainStart == start && plainQuote == quote) {
			cursor = plainEnd;
			plainStart = -1;
		} else {
			while (cursor <= end) {
				char c = buf[cursor];
				if (c == quote || c == '\\' || c < 0x20 || c == 0x7F) break;
				cursor++;
			}
		}
		int consumed = cursor - start;
		start = cursor;
		offset += consumed;
		columns += consumed;
		return consumed;
	}

	/** Skips buffered spaces and tabs; line breaks retain the ordinary scanner. */
	public int readSpaceTabPrefix() {
		int cursor = start;
		while (cursor <= end && (buf[cursor] == ' ' || buf[cursor] == '\t')) cursor++;
		int consumed = cursor - start;
		if (consumed == 0) return 0;
		start = cursor;
		offset += consumed;
		columns += consumed;
		return consumed;
	}

	/** Checks whether the next compact value can use the buffered boolean loop. */
	public boolean hasBooleanValue(boolean afterValue) {
		int token = start;
		if (afterValue) {
			if (token > end || buf[token] != ',') return false;
			token++;
		}
		if (token > end) return false;
		char c = buf[token];
		return ((c == 't' || c == 'n') && token + 3 <= end) || (c == 'f' && token + 4 <= end);
	}

	/** Reads complete compact boolean values without refilling the input buffer. */
	public int readBooleanValues(boolean[] values, long[] nulls, int count, boolean afterValue) {
		int cursor = start;
		while (count < values.length) {
			int token = cursor;
			if (afterValue) {
				if (token > end || buf[token] != ',') break;
				token++;
			}
			if (token > end) break;
			int length;
			boolean value = false;
			boolean isNull = false;
			if (buf[token] == 't' && token + 3 <= end
					&& buf[token + 1] == 'r' && buf[token + 2] == 'u' && buf[token + 3] == 'e') {
				length = 4;
				value = true;
			} else if (buf[token] == 'f' && token + 4 <= end && buf[token + 1] == 'a'
					&& buf[token + 2] == 'l' && buf[token + 3] == 's' && buf[token + 4] == 'e') {
				length = 5;
			} else if (buf[token] == 'n' && token + 3 <= end
					&& buf[token + 1] == 'u' && buf[token + 2] == 'l' && buf[token + 3] == 'l') {
				length = 4;
				isNull = true;
			} else break;
			values[count] = value;
			if (isNull) nulls[count >>> 6] |= 1L << (count & 63);
			count++;
			cursor = token + length;
			afterValue = true;
		}
		int consumed = cursor - start;
		if (consumed != 0) {
			start = cursor;
			offset += consumed;
			columns += consumed;
		}
		return count;
	}

	/** Avoids starting scalar batching on a tiny initial buffer. */
	public boolean hasScalarRun() {
		return end + 1 - start >= 32;
	}

	/** Reads complete strings/nulls or numbers/nulls without refilling. */
	public int readScalarValues(Object[] values, int count, boolean strings,
			boolean afterValue, net.arnx.jsonic.util.LocalCache cache) {
		while (count < values.length) {
			int savedStart = start;
			long savedOffset = offset;
			long savedColumns = columns;
			if (afterValue) {
				if (start > end || buf[start] != ',') break;
				start++;
				offset++;
				columns++;
			}
			Object value = null;
			boolean matched = false;
			if (start <= end) {
				if (buf[start] == 'n') {
					matched = readLiteral("null");
				} else if (strings && buf[start] == '"') {
					start++;
					offset++;
					columns++;
					value = readPlainString('"', cache);
					matched = value != null;
				} else if (!strings) {
					value = readCompactNumber();
					matched = value != null;
				}
			}
			if (!matched) {
				start = savedStart;
				offset = savedOffset;
				columns = savedColumns;
				break;
			}
			values[count++] = value;
			afterValue = true;
		}
		return count;
	}

	/** Matches an already buffered literal; failed probes consume no input. */
	public boolean readLiteral(String expected) {
		int length = expected.length();
		if (end + 1 - start < length) return false;
		for (int i = 0; i < length; i++) {
			char c = buf[start + i];
			if (c != expected.charAt(i) || c == '\r' || c == '\n') return false;
		}
		start += length;
		offset += length;
		columns += length;
		return true;
	}

	/** Scans only a complete token already present in the character buffer. */
	public CompactNumber readCompactNumber() {
		return (CompactNumber)readNumber(true);
	}

	/** Same validated scan, retaining BigDecimal and its decimal scale. */
	public BigDecimal readDecimalNumber() {
		return (BigDecimal)readNumber(false);
	}

	private Object readNumber(boolean compact) {
		int cursor = start;
		int limit = end + 1;
		if (cursor >= limit) return null;
		boolean negative = buf[cursor] == '-';
		if (negative && ++cursor == limit) return null;
		int first = cursor;
		char c = buf[cursor];
		if (c < '0' || c > '9') return null;
		long value = c - '0';
		cursor++;
		if (c != '0') {
			while (cursor < limit) {
				c = buf[cursor];
				if (c < '0' || c > '9') break;
				if (cursor - first >= 18) return null;
				value = value * 10 + c - '0';
				cursor++;
			}
		}
		int digits = cursor - first;
		int scale = 0;
		if (cursor < limit && buf[cursor] == '.') {
			int fraction = ++cursor;
			while (cursor < limit) {
				c = buf[cursor];
				if (c < '0' || c > '9') break;
				if (++digits > 18) return null;
				value = value * 10 + c - '0';
				cursor++;
			}
			scale = cursor - fraction;
			if (scale == 0) return null;
		}
		if (cursor < limit && (buf[cursor] == 'e' || buf[cursor] == 'E')) {
			cursor++;
			boolean negativeExponent = cursor < limit && buf[cursor] == '-';
			if (cursor < limit && (buf[cursor] == '-' || buf[cursor] == '+')) cursor++;
			int exponentStart = cursor;
			int exponent = 0;
			while (cursor < limit) {
				c = buf[cursor];
				if (c < '0' || c > '9') break;
				if (cursor - exponentStart >= 3) return null;
				exponent = exponent * 10 + c - '0';
				cursor++;
			}
			if (cursor == exponentStart) return null;
			scale += negativeExponent ? exponent : -exponent;
		}
		if (cursor == limit) return null;
		c = buf[cursor];
		// Keep refills, malformed suffixes, and newline/backtracking diagnostics
		// on the original scanner. A failed probe consumes no input.
		if (c != ',' && c != ']' && c != '}' && c != ' ' && c != '\t') return null;
		long signed = negative ? -value : value;
		Object number = compact ? CompactNumber.of(signed, scale) : BigDecimal.valueOf(signed, scale);
		int consumed = cursor - start;
		start = cursor;
		offset += consumed;
		columns += consumed;
		return number;
	}

	@Override
	public int next() throws IOException {
		int n = -1;
		if ((n = get()) != -1) {
			offset++;
			if (n == '\r') {
				lines++;
				columns = 0;
			} else if (n == '\n') {
				if (start < 2 || buf[start-2] != '\r') {
					lines++;
					columns = 0;
				}
			} else {
				columns++;
			}
		}
		return n;
	}
	
	private int get() throws IOException {
		if (start > end) {
			plainStart = -1;
			if (end > BACK) {
				int len = Math.min(BACK, end - BACK  + 1);
				System.arraycopy(buf, end + 1 - len, buf, BACK - len, len);
				back = BACK - len;
			}
			if (in != null) {
				if (!in.markSupported()) in = new PushbackInputStream(in, 4);
				this.reader = new InputStreamReader(in, determineEncoding(in));
				this.in = null;
			}
			int size = reader.read(buf, BACK, buf.length-BACK);
			if (size != -1) {
				mark = (mark > end - BACK) ? BACK - (end - mark + 1) : -1;
				start = BACK;
				end = BACK + size - 1;
			} else {
				start++;
				return -1;
			}
		}
		return buf[start++];
	}
	
	@Override
	public void back() {
		if (start <= back) {
			throw new IllegalStateException("no backup charcter");
		}
		start--;
		if (start <= end) {
			offset--;
			columns--;
		}
	}
	
	@Override
	public int mark() throws IOException {
		if (start > end) {
			int c = get();
			back();
			if (c == -1) {
				mark = -1;
				return 0;
			}
		}
		
		mark = start;
		return end - mark + 1;
	}
	
	@Override
	public void copy(StringBuilder sb, int len) {
		if (mark == -1) throw new IllegalStateException("no mark");
		if (mark + len > end + 1) throw new IndexOutOfBoundsException();
		
		sb.append(buf, mark, len);
	}
	
	@Override
	public String copy(int len) {
		if (mark == -1) throw new IllegalStateException("no mark");
		if (mark + len > end + 1) throw new IndexOutOfBoundsException();
		
		return String.valueOf(buf, mark, len);
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
	
	private static String determineEncoding(InputStream in) throws IOException {
		String encoding = "UTF-8";

		if (in.markSupported()) {
			in.mark(4);
		}
		byte[] check = new byte[4];
		int size = in.read(check);
		if (size == 2) {
			if (((check[0] & 0xFF) == 0x00 && (check[1] & 0xFF) != 0x00) 
					|| ((check[0] & 0xFF) == 0xFE && (check[1] & 0xFF) == 0xFF)) {
				encoding = "UTF-16BE";
			} else if (((check[0] & 0xFF) != 0x00 && (check[1] & 0xFF) == 0x00) 
					|| ((check[0] & 0xFF) == 0xFF && (check[1] & 0xFF) == 0xFE)) {
				encoding = "UTF-16LE";
			}
		} else if (size == 4) {
			if (((check[0] & 0xFF) == 0x00 && (check[1] & 0xFF) == 0x00)) {
				encoding = "UTF-32BE";
			} else if (((check[2] & 0xFF) == 0x00 && (check[3] & 0xFF) == 0x00)) {
				encoding = "UTF-32LE";
			} else if (((check[0] & 0xFF) == 0x00 && (check[1] & 0xFF) != 0x00) 
					|| ((check[0] & 0xFF) == 0xFE && (check[1] & 0xFF) == 0xFF)) {
				encoding = "UTF-16BE";
			} else if (((check[0] & 0xFF) != 0x00 && (check[1] & 0xFF) == 0x00) 
					|| ((check[0] & 0xFF) == 0xFF && (check[1] & 0xFF) == 0xFE)) {
				encoding = "UTF-16LE";
			}
		}
		if (in.markSupported()) {
			in.reset();
		} else {
			((PushbackInputStream)in).unread(check, 0, size);
		}
		return encoding;
	}
	
	@Override
	public String toString() {
		int spos = back;
		int max = Math.min(start-1, end);
		int charCount = 0;
		for (int i = 0; i < max + 1 - back && i < BACK; i++) {
			char c = buf[max-i];
			if (c == '\r' || (c == '\n' && (max-i-1 < 0 || buf[max-i-1] != '\r'))) {
				if (charCount > 0) break;
			} else if (c != '\n') {
				spos = max-i;
				charCount++;
			}
		}
		return (spos <= max) ? String.valueOf(buf, spos, max - spos + 1) : "";
	}
}
