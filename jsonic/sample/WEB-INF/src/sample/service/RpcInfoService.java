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
package sample.service;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import sample.web.basic.service.*;

public class RpcInfoService {
	static Map<Integer, RpcInfo> rpcList = new LinkedHashMap<Integer, RpcInfo>();
	static {
		rpcList.put(rpcList.size(), new RpcInfo(rpcList.size(), CalcService.class));
		rpcList.put(rpcList.size(), new RpcInfo(rpcList.size(), EditService.class));
	}
	
	static class RpcInfo {
		public Integer id;
		public Class class_;
		
		public RpcInfo(Integer id, Class class_) {
			this.id = id;
			this.class_ = class_;
		}

		@Override
		public int hashCode() {
			return class_.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			final RpcInfo other = (RpcInfo) obj;
			return id.equals(other.id);
		}
	}
	
	public List find(RpcInfo info) {
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		
		for (RpcInfo ri : rpcList.values()) {
			Class c = ri.class_;
			
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
	
	public void create(RpcInfo info) {
		if (info == null || rpcList.containsKey(info.id)) {
			throw new IllegalArgumentException();
		}

		info.id = rpcList.size();
		rpcList.put(rpcList.size(), info);
	}
	
	public void update(RpcInfo info) {
		if (info == null || !rpcList.containsKey(info.id)) {
			throw new IllegalArgumentException();
		}
		
		RpcInfo ri = rpcList.get(info.id);
		ri.class_ = info.class_;
	}
	
	public void delete(RpcInfo info) {
		if (info == null || !rpcList.containsKey(info.id)) {
			throw new IllegalArgumentException();
		}
		
		rpcList.remove(info);
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
	
	private String getParameterExample(Class[] paramTypes) {
		StringBuilder sb = new StringBuilder("[");
		for (Class c : paramTypes) {
			if (c.equals(boolean.class) || c.equals(Boolean.class)) {
				sb.append("true");
			} else if (c.equals(byte.class) || c.equals(short.class) || c.equals(int.class) || c.equals(long.class)
					|| c.equals(Byte.class) || c.equals(Short.class) || c.equals(Integer.class) || c.equals(Long.class)
					|| c.equals(BigInteger.class)) {
				sb.append("100");
			} else if (c.isAssignableFrom(Number.class)) {
				sb.append("33.3");
			} else if (CharSequence.class.isAssignableFrom(c)) {
				sb.append("\"abc\"");
			} else if (c.isArray()) {
				Class[] array = new Class[5];
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
