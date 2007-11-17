package sample;
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

import java.util.HashMap;
import java.util.Map;

public class EditService {
	public Map get(Map params) throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("url", "edit.json");
		
		Map<String, Object> concatMethod = new HashMap<String, Object>();
		concatMethod.put("name", "concat");
		concatMethod.put("example", "[\"blue\", \"bird\"]");
		concatMethod.put("code", this.getClass().getMethod("concat", new Class[] {String.class, String.class}));
		
		Map<String, Object> splitMethod = new HashMap<String, Object>();
		splitMethod.put("name", "split");
		splitMethod.put("example", "[\"abc-efg-hij\", \"-\"]");
		splitMethod.put("code", this.getClass().getMethod("split", new Class[] {String.class, String.class}));
		
		map.put("methods", new Map[] {concatMethod, splitMethod});
		
		return map;
	}
	
	public String concat(String a, String b) {
		return a + b;
	}
	
	public String[] split(String a, String b) {
		return a.split(b);
	}
}
