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

public interface InputSource {
	int next() throws IOException;
	void back();
	
	long getLineNumber();
	long getColumnNumber();
	long getOffset();
	
	int mark() throws IOException;
	void copy(StringBuilder sb, int len);
	String copy(int len);
}
