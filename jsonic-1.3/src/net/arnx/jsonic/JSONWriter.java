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
package net.arnx.jsonic;

import java.io.IOException;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.io.OutputSource;

public class JSONWriter {
	private Context context;
	private OutputSource out;
	
	private StringBuilder stack = new StringBuilder(10);
	
	JSONWriter(Context context, OutputSource out) {
		this.context = context;
		this.out = out;
	}
	
	public JSONWriter beginObject() throws IOException {
		if(stack.length() == 0) context.enter('$');
		stack.append('{');
		
		out.append('{');
		return this;
	}
	
	public JSONWriter endObject() throws IOException {
		out.append('}');
		
		stack.setLength(stack.length()-1);
		if (stack.length() == 0) context.exit();
		return this;
	}

	public JSONWriter beginArray() throws IOException {
		if(stack.length() == 0) context.enter('$');
		stack.append('[');
		
		out.append('[');
		return this;
	}
	
	public JSONWriter endArray() throws IOException {
		out.append(']');
		
		stack.setLength(stack.length()-1);
		if (stack.length() == 0) context.exit();
		return this;
	}
	
	public JSONWriter name(String name) throws IOException {
		StringFormatter.serialize(context, name, out);
		out.append(':');
		
		return this;
	}
	
	public JSONWriter value(Object value) throws IOException {
		value = context.preformatInternal(value);
		context.formatInternal(value, out);
		return this;
	}
}
