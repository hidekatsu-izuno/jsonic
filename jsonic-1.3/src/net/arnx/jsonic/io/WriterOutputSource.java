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
import java.io.Writer;

public class WriterOutputSource implements OutputSource {
	private final Writer writer;
	
	private final char[] buf = new char[1024];
	private int pos = 0;
	
	public WriterOutputSource(Writer writer) {
		this.writer = writer;
	}
	
	@Override
	public void append(String text) throws IOException {
		append(text, 0, text.length());
	}
	
	@Override
	public void append(String text, int start, int end) throws IOException {
		int length = end-start;
		if (pos + length < buf.length) {
			text.getChars(start, end, buf, pos);
			pos += length;
			return;
		}
	
		if (pos > 0) {
			writer.write(buf, 0, pos);
			pos = 0;
		}
			
		if (length < buf.length) {
			text.getChars(start, end, buf, 0);
			pos = length;
		} else {
			writer.write(text, start, length);
		}
	}
	
	@Override
	public void append(char c) throws IOException {
		if (pos + 1 >= buf.length) {
			writer.write(buf, 0, pos);
			pos = 0;
		}
		buf[pos++] = c;
	}
	
	public void flush() throws IOException {
		if (pos > 0) {
			writer.write(buf, 0, pos);
			pos = 0;
		}
		writer.flush();
	}
}

