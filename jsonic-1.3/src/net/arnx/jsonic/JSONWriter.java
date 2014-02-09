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
import java.util.Arrays;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.io.OutputSource;

public class JSONWriter {
	private Context context;
	private OutputSource out;
	
	private Stack stack = new Stack();
	
	JSONWriter(Context context, OutputSource out) {
		this.context = context;
		this.out = out;
	}
	
	public JSONWriter beginObject() throws IOException {
		if(stack.size() == 0) {
			context.enter(JSON.ROOT, null);
		}
		stack.push(JSONDataType.OBJECT);
		
		out.append('{');
		return this;
	}
	
	public JSONWriter endObject() throws IOException {
		if(stack.size() == 0) {
			throw new IllegalStateException();
		}
		State state = stack.pop();
		if (state.type != JSONDataType.OBJECT) {
			throw new IllegalStateException();
		}
		
		out.append('}');
		out.flush();
		
		if (stack.size() == 0) context.exit();
		return this;
	}

	public JSONWriter beginArray() throws IOException {
		if(stack.size() == 0) {
			context.enter(JSON.ROOT, null);
		}
		stack.push(JSONDataType.ARRAY);
		
		out.append('[');
		return this;
	}
	
	public JSONWriter endArray() throws IOException {
		if(stack.size() == 0) {
			throw new IllegalStateException();
		}
		State state = stack.pop();
		if (state.type != JSONDataType.ARRAY) {
			throw new IllegalStateException();
		}
		
		out.append(']');
		out.flush();
		
		if (stack.size() == 0) context.exit();
		return this;
	}
	
	public JSONWriter name(String name) throws IOException {
		if(stack.size() == 0) {
			throw new IllegalStateException();
		}
		State state = stack.peek();
		if (state.type == JSONDataType.OBJECT) {
			state.name = name;
		} else {
			throw new IllegalStateException();
		}
		
		StringFormatter.serialize(context, name, out);
		out.append(':');
		
		return this;
	}
	
	public JSONWriter value(Object value) throws IOException {
		if(stack.size() == 0) {
			throw new IllegalStateException();
		}
		State state = stack.peek();
		if (state.type == JSONDataType.OBJECT) {
			if (state.name != null) {
				context.enter(state.name);
			} else {
				throw new IllegalStateException();
			}
		} else if (state.type == JSONDataType.ARRAY) {
			context.enter(state.index++);
		} else {
			throw new IllegalStateException();
		}
		
		value = context.preformatInternal(value);
		context.formatInternal(value, out);
		context.exit();
		return this;
	}
	
	public JSONWriter flush() throws IOException {
		out.flush();
		return this;
	}
	
	static final class Stack {
		private int size = 0;
		private State[] list = new State[8];
		
		public State push(JSONDataType type) {
			size++;
			if (size >= list.length) {
				list = Arrays.copyOf(list, Math.max(size, list.length) * 2);
			}
			State state;
			if (list[size] != null) {
				state = list[size];
				state.name = null;
				state.index = 0;
			} else {
				state = new State();
				list[size] = state;
			}
			state.type = type;
			return state;
		}
		
		public State peek() {
			return list[size];
		}
		
		public State pop() {
			return list[size--];
		}
		
		public int size() {
			return size;
		}
	}
	
	static final class State {
		public JSONDataType type;
		public String name;
		public int index = 0;
	}
}
