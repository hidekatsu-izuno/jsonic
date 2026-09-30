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
package sample.basic.service;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sample.basic.web.rpc.service.CalcService;
import sample.basic.web.rpc.service.EditService;

public class RpcInfoService {
	static List<Class<?>> rpcList = new ArrayList<Class<?>>();
	static {
		rpcList.add(CalcService.class);
		rpcList.add(EditService.class);
	}
	
	public List<Map<String, Object>> find() {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		
		for (Class<?> c : rpcList) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("name", toComponentName(c.getName()));
			
			List<Map<String, Object>> methods = new ArrayList<Map<String, Object>>();
			for (Method m : c.getMethods()) {
				if (m.getDeclaringClass().equals(Object.class)) continue;
				
				Map<String, Object> method = new HashMap<String, Object>();
				method.put("name", m.getName());
				method.put("code", m.toGenericString());
				method.put("example", getParameterExample(m.getParameterTypes()));
				
				methods.add(method);
			}
			map.put("methods", methods);
			list.add(map);
		}
		
		return list;
	}
	
	private String toComponentName(String name) {
		name = name.substring(0, name.length()-"Service".length());
		int last = name.lastIndexOf('.');
		if (last != -1 && last+1 < name.length()) name = name.substring(last+1);
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (Character.isUpperCase(c)) {
				if (i != 0) sb.append('_');
				sb.append(Character.toLowerCase(c));
			} else {
				sb.append(c);
			}
		}
		
		return sb.toString();
	}
	
	private String getParameterExample(Class<?>[] paramTypes) {
		StringBuilder sb = new StringBuilder("[");
		for (Class<?> c : paramTypes) {
			if (c.equals(boolean.class) || c.equals(Boolean.class)) {
				sb.append("true");
			} else if (c.equals(byte.class) || c.equals(short.class) || c.equals(int.class) || c.equals(long.class)
					|| c.equals(Byte.class) || c.equals(Short.class) || c.equals(Integer.class) || c.equals(Long.class)
					|| c.equals(BigInteger.class)) {
				sb.append("100");
			} else if (Number.class.isAssignableFrom(c)) {
				sb.append("33.3");
			} else if (CharSequence.class.isAssignableFrom(c)) {
				sb.append("\"abc\"");
			} else if (c.isArray()) {
				Class<?>[] array = new Class[5];
				for (int i = 0; i < array.length; i++) {
					array[i] = c.getComponentType();
				}
				sb.append(getParameterExample(array));
			} else if (List.class.isAssignableFrom(c)) {
				sb.append("[]");
			} else {
				sb.append("{}");
			}
			sb.append(", ");
		}
		if (paramTypes.length > 0) sb.setLength(sb.length()-2);
		sb.append(']');
		
		return sb.toString();
	}
}
