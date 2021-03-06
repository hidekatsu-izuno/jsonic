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

public class StringInputSource extends CharSequenceInputSource {
	private final String str;
	
	public StringInputSource(String str) {
		super(str);
		this.str = str;
	}
	
	@Override
	public void copy(StringBuilder sb, int len) {
		if (mark == -1) {
			throw new IllegalStateException("no mark");
		}
		sb.append(str, mark, mark + len);
	}
}
