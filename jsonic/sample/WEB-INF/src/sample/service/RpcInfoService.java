package sample.service;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RpcInfoService {
	public List find(Map params) {
		Class[] classes = new Class[] {
				sample.web.basic.service.CalcService.class,
				sample.web.basic.service.EditService.class
			};
		
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		
		for (Class c : classes) {
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
