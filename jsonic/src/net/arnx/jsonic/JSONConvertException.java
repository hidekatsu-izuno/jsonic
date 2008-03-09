/*
 * Copyright 2007-2008 Hidekatsu Izuno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package net.arnx.jsonic;

import java.util.ArrayList;
import java.util.List;

public class JSONConvertException extends RuntimeException {
	private static final long serialVersionUID = -6173125387096087580L;
	private List<Object> keys = new ArrayList<Object>();
	
	JSONConvertException(String message, Throwable cause) {
		super(message, cause);
	}
	
	void add(Object key) {
		keys.add(key);
	}

	@Override
	public String getMessage() {
		StringBuilder sb = new StringBuilder(super.getMessage());
		for (int i = 0; i < keys.size(); i++) {
			Object key = keys.get(keys.size()-i-1);
			if (key instanceof Number) {
				sb.append('[').append(key).append(']');
			} else {
				if (i != 0) sb.append('.');
				sb.append(key);
			}
		}
		return sb.toString();
	}
}
