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

public class Calc {
	public Map execute(Map params) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("url", "calc.json");
		
		Map<String, Object> sumMethod = new HashMap<String, Object>();
		sumMethod.put("name", "sum");
		sumMethod.put("example", "[1, 2]");
		sumMethod.put("code", "public int sum(int a, int b) {\n\treturn a + b;\n}\n\n"
				+ "public int sum(int[] a) {\n\tint result = 0;\n\tfor (int n : a) {\n"
				+ "\t\tresult += n;\n\t}\n\treturn result;\n}");
		
		map.put("methods", new Map[] {sumMethod});
		
		return map;
	}
	
	public int sum(int a, int b) {
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
