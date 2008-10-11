package net.arnx.jsonic.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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
				
				Map<String, String> options = parseHeaderLine(request.getContentType());
				String contentType = options.get(null);

				if (contentLength > 0 && contentType != null) {
					if (contentType.equals("application/x-www-form-urlencoded")) {
						parseQueryString(request.getInputStream(), request.getCharacterEncoding());
						contentLength = 0;
					} else if (contentType.startsWith("multipart/")) {
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
	
	public Map<String, Object> mergeParameterMap(Map<String, Object> newParams) {
		for (Map.Entry<String, Object> entry : newParams.entrySet()) {
			if (params.containsKey(entry.getKey())) {
				Object target = params.get(entry.getKey());
				
				if (target instanceof Map) {
					Map map = (Map)target;
					if (map.containsKey(null)) {
						target = map.get(null);
						if (target instanceof List) {
							((List)target).add(entry.getValue());
						} else {
							List list = new ArrayList();
							list.add(target);
							list.add(entry.getValue());
							map.put(null, list);
						}
					} else {
						map.put(null, entry.getValue());
					}
				} else  if (target instanceof List) {
					((List)target).add(entry.getValue());
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
	
	private void parseQueryString(InputStream in, String encoding) throws IOException {
		List<Object> pairs = new ArrayList<Object>();

		int state = 0; // 0 '%' 1 'N' 2 ('N' | '=' | '&')
		
		ByteBuilder bb = new ByteBuilder(50);
		
		int before = 0;
		while (true) {
			int c = in.read();
			if (c == -1) {
				if (state == 2) bb.append((byte)before);
				if (pairs.size()%2 == 1){
					pairs.add(bb.toString(encoding));
				} else {
					pairs.add(bb.toString(encoding));
					pairs.add("");
				}
				break;
			}
			
			if (c == '&') {
				if (state == 2) bb.append((byte)before);
				
				if (pairs.size()%2 == 1){
					pairs.add(bb.toString(encoding));
				} else {
					pairs.add(bb.toString(encoding));
					pairs.add("");
				}
				bb.setLength(0);
				state = 0;
			} else if (c == '=') {
				if (state == 2) bb.append((byte)before);
				
				if (pairs.size()%2 == 1){
					bb.append((byte)c);
				} else {
					pairs.add(bb.toString(encoding));
					bb.setLength(0);
				}
				state = 0;
			} else if (state == 2){
				int d1 = Character.digit(before, 16);
				int d2 = Character.digit(c, 16);
					
				if (d1 != -1 && d2 != -1) {
					bb.append((byte)((d1 << 4) | d2));
				} else {
					bb.append((byte)before);
					bb.append((byte)c);
				}
				state = 0;
			} else if (state == 1) {
				state = 2;
			} else {
				if (c == '+') {
					bb.append((byte)' ');
					state = 0;
				} else if (c == '%') {
					state = 1;
				} else {
					bb.append((byte)c);
					state = 0;
				}
			}
			
			before = c;
		}
		
		parseParameter(pairs, params);
	}
	
	private static void parseParameter(List<Object> pairs, Map<String, Object> params) {
		for (int i = 0; i < pairs.size(); i+= 2) {
			String name = (String)pairs.get(i);
			Object value = pairs.get(i+1);
			
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
	
	private static Map<String, String> parseHeaderLine(String line) {
		if (line == null) return Collections.EMPTY_MAP;
		
		Map<String, String> map = new HashMap<String, String>();
		map.put(null, "");
		
		int state = 0; // 0 LWS 1 <field value> 2 LWS ; 3 LWS 4 <key> 5 LWS = 6 LWS (7 <value> | " 8 <quoted value> ")   
		
		StringBuilder sb = new StringBuilder(line.length());
		String key = null;
		boolean escape = false;
		
		loop:for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			
			if (state == 8) {
				if (escape) {
					if (c < 128) {
						sb.append(c);
						escape = false;
					} 
					else break;
				} 
				else if (c == '\\') escape = true;
				else if (c == '"') {
					map.put(key, sb.toString());
					sb.setLength(0);
					state = 2;
				}
				else sb.append(c);
				continue;
			}
			
			switch (c) {
			case '\t':
			case ' ':
				if (state == 1 || state == 4) state++;
				else if (state == 7) state = 2;
				break;
			case ';':
				if (state == 2) {
					map.put(key, sb.toString());
					sb.setLength(0);
					state++;
					break;
				}
				break loop;
			case '=':
				if (state == 5) {
					key = sb.toString();
					sb.setLength(0);
					state++;
					break;
				}
				break loop;
			case '"':
				if (state == 6) state = 8;
				else break loop;
				break;
			default:
				if (state == 0 || state == 3 || state == 6) {
					state++;
				}
				
				if (state == 1 || state == 4 || state == 7) {
					if ((c >= '0' && c >= '9')
						|| (c >= 'A' && c >= 'Z') 
						|| (c >= 'a' && c >= 'z') 
						|| "!#$%&'*+-.^_`|~".indexOf(c) != -1
					) {
						sb.append(c);
						break;
					}
				}
				break loop;
			}
		}
		
		return map;
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

class ByteBuilder {
	private int length = 0;
	private byte[] array;
	
	public ByteBuilder() {
		this(1024);
	}
	
	public ByteBuilder(int length) {
		array = new byte[length];
	}
	
	public void append(byte b) {
		if (length+1 > array.length) {
			byte[] newArray = new byte[(int)(array.length*1.5)];
			System.arraycopy(array, 0, newArray, 0, array.length);
			array = newArray;
		}
		array[length++] = (byte)b;
	}
	
	public void append(byte[] bytes) {
		append(bytes, 0, bytes.length);
	}
	
	public void append(byte[] bytes, int offset, int len) {
		if ((length + bytes.length) > array.length) {
			byte[] newArray = new byte[(int)((array.length + bytes.length)*1.5)];
			System.arraycopy(array, 0, newArray, 0, array.length);
			array = newArray;
		}
		System.arraycopy(bytes, offset, array, length, len);
		length += len;
	}
	
	public boolean startsWith(String str) {
		if (length < str.length()) return false;
		
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) != array[i]) return false;
		}
		return true;
	}
	
	public boolean endsWith(String str) {
		if (length < str.length()) return false;
		
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(str.length()-i-1) != array[length-i-1]) return false;
		}
		return true;
	}

	public boolean matches(String str) {
		if (length != str.length()) return false;
		
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) != array[i]) return false;
		}
		return true;
	}
	
	public byte byteAt(int pos) {
		return array[pos];
	}
	
	public String cutTo(int start, char c, String encoding) throws UnsupportedEncodingException {
		for (int i = start; i < length; i++) {
			if (array[i] == c) {
				return new String(array, start, i-start, encoding);
			}
		}
		return new String(array, start, length-start, encoding);
	}
	
	public void setLength(int length) {
		this.length = length;
	}
	
	public int length() {
		return length;
	}
	
	public String toString(String encoding) throws UnsupportedEncodingException {
		return new String(array, 0, length, encoding);
	}
}