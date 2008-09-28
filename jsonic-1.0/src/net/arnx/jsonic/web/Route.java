package net.arnx.jsonic.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

public class Route {
	private static final Pattern REPLACE_PATTERN = Pattern.compile("\\$\\{(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)\\}");
	private static final Pattern OPTIONS_PATTERN = Pattern.compile(";\\s*([\\w!#$%&'*+.^_`|~-]+)\\s*=\\s*(?:([\\w!#$%&'*+.^_`|~-]+)|\"((?>[!-~&&[^\\\\\"]]*|\\\\\\p{ASCII}))\")\\s*");

	private String target;
	private String method;
	private String contentType = "";
	private int contentLength = 0;
	private Map<String, Object> params;
	
	private boolean isRpcMode;
	
	public Route(HttpServletRequest request, String target, Map<String, Object> params) throws IOException {
		this.target = target;
		this.params = params;
		
		if ("rpc".equalsIgnoreCase(getParameter("class"))) {
			isRpcMode = true;
			
			this.method = request.getMethod().toUpperCase();
		} else {
			if (request.getQueryString() != null) {
				parseQueryString(new ByteArrayInputStream(request.getQueryString().getBytes("US-ASCII")), request.getCharacterEncoding());
			}
			if (!request.getMethod().equalsIgnoreCase("GET")) {
				contentLength = request.getContentLength();
				
				String type = request.getContentType();
				Map<String, String> options = new HashMap<String, String>();
				
				if (type != null) {
					int index = type.indexOf(';');
					contentType = ((index != -1) ? type.substring(0, index) : type).trim().toLowerCase();
					if (index != -1) parseContentTypeOptions(contentType.substring(index), options);
				}
				
				if (contentLength > 0) {
					if (contentType.equals("application/x-www-form-urlencoded")) {
						parseQueryString(request.getInputStream(), request.getCharacterEncoding());
						contentLength = 0;
					} else if (contentType.startsWith("multipart/")) {
						String boundary = options.get("boundary");
						parseMultipart(request.getInputStream(), request.getCharacterEncoding(), boundary);
						contentLength = 0;
					}
				}
			}
			
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
		
		if (o instanceof Map && ((Map)o).containsKey(null)) {
			o = ((Map)o).get(null);
		}
		
		if (o instanceof List && !((List)o).isEmpty()) {
			o = ((List)o).get(0);
		}
		
		return (o instanceof String) ? (String)o : null;
	}
	
	public Map<String, Object> getParameterMap() {
		return params;
	}
	
	public boolean hasJSONContent() {
		return contentLength > 0;
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
	
	private void parseContentTypeOptions(String text, Map<String, String> options) throws IOException {
		Matcher m = OPTIONS_PATTERN.matcher(text);
		while (m.find()) {
			options.put(m.group(1), (m.group(2) != null) ? m.group(2) : m.group(3));
		}
	}
	
	private void parseQueryString(InputStream in, String encoding) throws IOException {
		List<String> pairs = new ArrayList<String>();

		int state = 0; // 0 '%' 1 'N' 2 ('N' | '=' | '&')
		
		byte[] buf = new byte[128];
		int length = 0;
		
		int before = 0;
		while (true) {
			int c = in.read();
			if (c == -1) {
				if (state == 2) buf = byteAppend(buf, length++, before);
				if (pairs.size()%2 == 1){
					pairs.add(new String(buf, 0, length, encoding));
				} else {
					pairs.add(new String(buf, 0, length, encoding));
					pairs.add("");
				}
				break;
			}
			
			if (c == '&') {
				if (state == 2) buf = byteAppend(buf, length++, before);
				
				if (pairs.size()%2 == 1){
					pairs.add(new String(buf, 0, length, encoding));
				} else {
					pairs.add(new String(buf, 0, length, encoding));
					pairs.add("");
				}
				length = 0;
				state = 0;
			} else if (c == '=') {
				if (state == 2) buf = byteAppend(buf, length++, before);
				
				if (pairs.size()%2 == 1){
					buf = byteAppend(buf, length++, c);
				} else {
					pairs.add(new String(buf, 0, length, encoding));
					length = 0;
				}
				state = 0;
			} else if (state == 2){
				int d1 = Character.digit(before, 16);
				int d2 = Character.digit(c, 16);
					
				if (d1 != -1 && d2 != -1) {
					buf = byteAppend(buf, length++, (d1 << 4) | d2);
				} else {
					buf = byteAppend(buf, length++, before);
					buf = byteAppend(buf, length++, c);
				}
				state = 0;
			} else if (state == 1) {
				state = 2;
			} else {
				if (c == '+') {
					buf = byteAppend(buf, length++, ' ');
					state = 0;
				} else if (c == '%') {
					state = 1;
				} else {
					buf = byteAppend(buf, length++, c);
					state = 0;
				}
			}
			
			before = c;
		}
		
		parseParameter(pairs, params);
	}
	
	private void parseMultipart(InputStream in, String encoding, String boundary) throws IOException {
		if (boundary == null || boundary.length() == 0) {
			return; 
		}
		
		List<String> pairs = new ArrayList<String>();
		
		// TODO
		
		parseParameter(pairs, params);
	}
	
	private static void parseParameter(List<String> pairs, Map<String, Object> params) {
		for (int i = 0; i < pairs.size(); i+= 2) {
			String name = pairs.get(i);
			String value = pairs.get(i+1);
			
			int start = 0;
			char old = '\0';
			Map<String, Object> current = params;
			for (int j = 0; j < name.length(); j++) {
				char c = name.charAt(j);
				if (c == '.' || c == '[') {
					String key = name.substring(start, (old == ']') ? j-1 : j);
					Object target = current.get(key);
					
					if (!(target instanceof Map)) {
						Map<String, Object> map = new LinkedHashMap<String, Object>();
						if (target != null) map.put(null, target);
						current.put(key, map);
						current = map;
					} else {
						current = (Map<String, Object>)target;
					}
					start = j+1;
				}
				old = c;
			}
			
			name = name.substring(start, (old == ']') ? name.length()-1 : name.length());

			if (current.containsKey(name)) {
				Object target = current.get(name);
				
				if (target instanceof Map) {
					Map map = (Map)target;
					if (map.containsKey(null)) {
						target = map.get(null);
						if (target instanceof List) {
							((List)target).add(value);
						} else {
							List list = new ArrayList();
							list.add(target);
							list.add(value);
							map.put(null, list);
						}
					} else {
						map.put(null, value);
					}
				} else if (target instanceof List) {
					((List)target).add(value);
				} else {
					List list = new ArrayList();
					list.add(target);
					list.add(value);
					current.put(name, list);
				}
			} else {
				current.put(name, value);
			}
		}		
	}
	
	private static byte[] byteAppend(byte[] array, int pos, int b) {
		if (pos >= array.length) {
			byte[] newArray = new byte[array.length*2];
			System.arraycopy(array, 0, newArray, 0, array.length);
			array = newArray;
		}
		array[pos] = (byte)b;
		return array;
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