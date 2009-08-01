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
package sample.basic.web.rest.service;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MemoService {
	
	// it's incorrect use. you should use RDBMS.
	// and it's not work in a hot deploy.
	private static int count = 0;
	private static Map<Integer, Memo> list = Collections.synchronizedMap(new LinkedHashMap<Integer, Memo>());
	
	// injects request/response. but default container only.
	public HttpServletRequest request;
	public HttpServletResponse response;
	
	public void init() {
		// response.setHeader("X-JSON", "[\"Hello. JSONIC!\"]");
	}
	
	// This method ignored. Because init method has no arguments.
	public void init(String str) {
		throw new UnsupportedOperationException();
	}
	
	public Collection<Memo> find() {
		return list.values();
	}
	
	public Collection<Memo> find(Memo memo) {
		return list.values();
	}
	
	public Collection<Memo> find(Memo memo, String str) {
		return list.values();
	}
	
	public void create(Memo memo) {
		if (memo.id != null)
			throw new IllegalArgumentException();
		
		memo.id = count++;
		list.put(memo.id, memo);
	}
	
	public void update(Memo memo) {
		if (!list.containsKey(memo.id)) 
			throw new IllegalStateException();

		Memo target = list.get(memo.id);
		target.title = memo.title;
		target.text = memo.text;
	}
	
	public void delete(Memo memo) {
		if (!list.containsKey(memo.id)) 
			throw new IllegalStateException();
		
		list.remove(memo.id);
	}
	
	public void destroy() {
	}
}

class Memo {
	public Integer id;
	public String title;
	public String text;
	
	public String toString() {
		return "{ id: " + id + ", title: \"" + title + "\", text: \"" + text + "\" }";
	}
}
