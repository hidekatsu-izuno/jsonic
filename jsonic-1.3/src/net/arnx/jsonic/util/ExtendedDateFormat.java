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
package net.arnx.jsonic.util;

import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ExtendedDateFormat extends SimpleDateFormat {
	boolean escape = false;
	
	public ExtendedDateFormat(String pattern, Locale locale) {
		super(escape(pattern), locale);
		escape = !pattern.equals(this.toPattern());
	}
	
	public ExtendedDateFormat(String pattern) {
		super(escape(pattern));
		escape = !pattern.equals(this.toPattern());
	}
	
	static String escape(String pattern) {
		boolean skip = false;
		int count = 0;
		StringBuilder sb = null;
		int last = 0;
		for (int i = 0; i < pattern.length(); i++) {
			char c = pattern.charAt(i);
			if (c == '\'') {
				skip = !skip;
			} else if (c == 'Z' && !skip) {
				count++;
				if (count == 2) {
					if (sb == null) sb = new StringBuilder(pattern.length() + 4);
					sb.append(pattern, last, i-1);
					sb.append("Z\0");
					last = i+1;
				}
			} else {
				count = 0;
			}
		}
		if (sb != null) {
			if (last < pattern.length()) sb.append(pattern, last, pattern.length());
			return sb.toString();
		} else {
			return pattern;
		}
	}

	@Override
	public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos) {
		super.format(date, toAppendTo, pos);
		if (escape) {
			for (int i = 5; i < toAppendTo.length(); i++) {
				if (toAppendTo.charAt(i) == '\0') {
					toAppendTo.setCharAt(i, toAppendTo.charAt(i-1));
					toAppendTo.setCharAt(i-1, toAppendTo.charAt(i-2));
					toAppendTo.setCharAt(i-2, ':');
				}
			}
		}
		return toAppendTo;
	}
}
