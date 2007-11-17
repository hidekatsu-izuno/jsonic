package s2jsonrpc_sample.service;
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
import java.util.Map;
import java.util.HashMap;

public class CalcService {
	public Map get(Map params) throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("url", "calc.json");
		
		Map<String, Object> plusMethod = new HashMap<String, Object>();
		plusMethod.put("name", "plus");
		plusMethod.put("example", "[1, 2]");
		plusMethod.put("code", this.getClass().getMethod("plus", new Class[] {int.class, int.class}));
		
		Map<String, Object> sumMethod = new HashMap<String, Object>();
		sumMethod.put("name", "sum");
		sumMethod.put("example", "[[1, 2, 3, 4, 5]]");
		sumMethod.put("code", this.getClass().getMethod("sum", new Class[] {int[].class}));
		
		map.put("methods", new Map[] {plusMethod, sumMethod});
		
		return map;
	}
	
	public int plus(int a, int b) {
		return a + b;
	}
	
	public int sum(int[] a) {
		int result = 0;
		for (int n : a) {
			result += n;
		}
		return result;
	}
}
