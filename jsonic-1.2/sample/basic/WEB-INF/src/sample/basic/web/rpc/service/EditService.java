/*
 * Copyright 2007 Hidekatsu Izuno
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
package sample.basic.web.rpc.service;

public class EditService {
	
	public String concat(String a, String b) {
		return a + b;
	}
	
	public String concat(String... text) {
		StringBuilder sb = new StringBuilder();
		for (String str : text) {
			sb.append(str);
		}
		return sb.toString();
	}
	
	public String[] split(String a, String b) {
		return a.split(b);
	}
}
