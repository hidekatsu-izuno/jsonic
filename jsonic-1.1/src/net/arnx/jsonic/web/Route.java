package net.arnx.jsonic.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

public class Route {
	private static final Pattern REPLACE_PATTERN = Pattern.compile("\\$\\{(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)\\}");

	private String target;
	private String method;
	private Map<Object, Object> params;
	
	private boolean isRpcMode;
	private String contentType;
	
	@SuppressWarnings("unchecked")
	public Route(HttpServletRequest request, String target, Map<String, Object> params) throws IOException {
		this.target = target;
		this.params = (Map)params;
		this.contentType = request.getContentType();
		
		if ("rpc".equalsIgnoreCase(getParameter("class"))) {
			isRpcMode = true;
			
			this.method = request.getMethod().toUpperCase();
		} else {
			Map<String, String[]> pmap = Collections.emptyMap();
			
			if (hasJSONContent()) {
				if (request.getQueryString() != null 
						&& request.getQueryString().trim().length() != 0) {
					pmap = parseQueryString(request.getQueryString(), request.getCharacterEncoding());
				}
			} else {
				pmap = request.getParameterMap();
				
				if (!"application/x-www-form-urlencoded".equals(contentType)
						&& request.getQueryString() != null 
						&& request.getQueryString().trim().length() != 0) {
						
					Map<String, String[]> pairs = parseQueryString(request.getQueryString(), request.getCharacterEncoding());
					
					for (Map.Entry<String, String[]> entry : pairs.entrySet()) {
						String[] values = pmap.get(entry.getKey());
						if (values.length <= entry.getValue().length) continue;
						
						int size = values.length;
						for (String estr : entry.getValue()) {
							for (int i = 0; i < values.length; i++) {
								if (estr.equals(values[i])) {
									values[i] = null;
									size--;
									break;
								}
							}
						}
						
						String[] newValues = new String[size];
						int pos = 0;
						for (String pstr : values) {
							if (pstr != null) newValues[pos++] = pstr;
						}
						pmap.put(entry.getKey(), newValues);
					}
				}
			}
			
			parseParameter(pmap, this.params);
			
			String m = getParameter("_method");
			if (m == null) m = request.getMethod();
			this.method = m.toUpperCase();
		}
	}
	
	public String getMethod() {
		return method;
	}
	
	public boolean isRpcMode() {
		return isRpcMode;
	}
	
	public String getParameter(String name) {
		Object o = params.get(name);
		
		if (o instanceof Map) {
			Map<?, ?> map = (Map<?, ?>)o;
			if (map.containsKey(null)) o = map.get(null); 
		}
		
		if (o instanceof List) {
			List<?> list = (List<?>)o;
			if (!list.isEmpty()) o = list.get(0);
		}
		
		return (o instanceof String) ? (String)o : null;
	}
	
	public Map<?, ?> getParameterMap() {
		return params;
	}
	
	@SuppressWarnings("unchecked")
	public Map<?, ?> mergeParameterMap(Map<?, ?> newParams) {
		for (Map.Entry<?, ?> entry : newParams.entrySet()) {
			if (params.containsKey(entry.getKey())) {
				Object target = params.get(entry.getKey());
				
				if (target instanceof Map) {
					Map<Object, Object> map = (Map<Object, Object>)target;
					if (map.containsKey(null)) {
						target = map.get(null);
						if (target instanceof List) {
							((List<Object>)target).add(entry.getValue());
						} else {
							List<Object> list = new ArrayList<Object>();
							list.add(target);
							list.add(entry.getValue());
							map.put(null, list);
						}
					} else {
						map.put(null, entry.getValue());
					}
				} else  if (target instanceof List) {
					((List<Object>)target).add(entry.getValue());
				} else {
					List<Object> list = new ArrayList<Object>();
					list.add(target);
					list.add(entry.getValue());
					params.put(entry.getKey(), list);
				}
			} else {
				params.put(entry.getKey(), entry.getValue());
			}
		}
		return params;
	}
	
	public boolean hasJSONContent() {
		return "application/json".equals(contentType);
	}
	
	public String getComponentClass(String sub) {
		Matcher m = REPLACE_PATTERN.matcher(target);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String key = m.group(1);
			String value = getParameter(key);
			
			if (key.equals("class")) {
				value = toUpperCamel((sub != null) ? sub 
					: (value != null) ? value : "");
			} else if (value == null) {
				value = "";
			} else if (key.equals("package")) {
				value = value.replace('/', '.');
			}
			m.appendReplacement(sb, value);
		}
		m.appendTail(sb);
		return sb.toString();
	}
	
	private Map<String, String[]> parseQueryString(String qs, String encoding) throws UnsupportedEncodingException {
		Map<String, String[]> pairs = new HashMap<String, String[]>();
		
		int start = 0;
		String key = null;
		
		for (int i = 0; i <= qs.length(); i++) {
			if (i == qs.length() || qs.charAt(i) == '&') {
				String value = null;
				String[] values = null;
				
				if (key == null) {
					key = URLDecoder.decode(qs.substring(start, i), encoding);
					value = "";
				} else {
					value = URLDecoder.decode(qs.substring(start, i), encoding);
				}
				
				if (pairs.containsKey(key)) {
					String[] tmp = pairs.get(key);
					values = new String[tmp.length+1];
					System.arraycopy(tmp, 0, values, 0, tmp.length);
					values[tmp.length] = value;
				} else {
					values = new String[] { value };
				}
				
				pairs.put(key, values);
				key = null;
				
				start = i+1;
			} else if (qs.charAt(i) == '=') {
				key = URLDecoder.decode(qs.substring(start, i), encoding);
				start = i+1;
			}
		}
		
		return pairs;
	}
	
	@SuppressWarnings("unchecked")
	private static void parseParameter(Map<String, String[]> pairs, Map<Object, Object> params) {
		for (Map.Entry<String, String[]> entry : pairs.entrySet()) {
			String name = entry.getKey();
			String[] values = entry.getValue();
			
			int start = 0;
			char old = '\0';
			Map<Object, Object> current = params;
			for (int i = 0; i < name.length(); i++) {
				char c = name.charAt(i);
				if (c == '.' || c == '[') {
					String key = name.substring(start, (old == ']') ? i-1 : i);
					Object target = current.get(key);
					
					if (target instanceof Map) {
						current = (Map<Object, Object>)target;
					} else {
						Map<Object, Object> map = new LinkedHashMap<Object, Object>();
						if (target != null) map.put(null, target);
						current.put(key, map);
						current = map;
					}
					start = i+1;
				}
				old = c;
			}
			
			name = name.substring(start, (old == ']') ? name.length()-1 : name.length());

			if (current.containsKey(name)) {
				Object target = current.get(name);
				
				if (target instanceof Map) {
					Map<Object, Object> map = (Map<Object, Object>)target;
					if (map.containsKey(null)) {
						target = map.get(null);
						if (target instanceof List) {
							List<Object> list = ((List<Object>)target);
							for (String value : values) list.add(value);
						} else {
							List<Object> list = new ArrayList<Object>();
							list.add(target);
							for (String value : values) list.add(value);
							map.put(null, list);
						}
					} else if (values.length > 1) {
						List<Object> list = new ArrayList<Object>();
						for (String value : values) list.add(value);
						map.put(null, list);
					} else {
						map.put(null, values[0]);						
					}
				} else if (target instanceof List) {
					List<Object> list = ((List<Object>)target);
					for (String value : values) list.add(value);
				} else {
					List<Object> list = new ArrayList<Object>();
					list.add(target);
					for (String value : values) list.add(value);
					current.put(name, list);
				}
			} else if (values.length > 1) {
				List<Object> list = new ArrayList<Object>();
				for (String value : values) list.add(value);
				current.put(name, list);
			} else {
				current.put(name, values[0]);						
			}
		}
	}
		
	private String toUpperCamel(String name) {
		StringBuilder sb = new StringBuilder(name.length());
		boolean toUpperCase = true;
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (c == ' ' || c == '_' || c == '-') {
				toUpperCase = true;
			} else if (toUpperCase) {
				sb.append(Character.toUpperCase(c));
				toUpperCase = false;
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}