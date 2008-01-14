package net.arnx.jsonic.web;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpClient {
	private URI uri;
	private String method = "GET";
	private Map<String, String> requestHeaders = new LinkedHashMap<String, String>();
	private String requestContent;
	
	private int responseCode = 0;
	private String responseMessage;
	private Map<String, String> responseHeaders = new LinkedHashMap<String, String>();
	private String responseContent = null;
	
	public HttpClient() {
		
	}
	
	public HttpClient(String url) {
		this.setURL(url);
	}
	
	public void setRequestMethod(String method) {
		this.method = method;
	}
	
	public void setURL(String url) {
		try {
			this.uri = new URI(url);
		} catch (Exception e) {
			throw new IllegalArgumentException("url is invalid.");
		}
	}
	
	public void setRequestHeader(String name, String value) {
		requestHeaders.put(name, value);
	}
	
	public void setRequestContent(String content) {
		try {
			setRequestHeader("Content-Length", Integer.toString(content.getBytes("UTF-8").length));
		} catch (UnsupportedEncodingException e) {
			// no handle
		}
		this.requestContent = content;
	}
	
	public int getResponseCode() {
		return responseCode;
	}
	
	public String getResponseMessage() {
		return responseMessage;
	}
	
	public String getResponseHeader(String name) {
		return responseHeaders.get(name);
	}
	
	public String getResponseContent() {
		return responseContent;
	}
	
	public void connect() throws Exception {
		responseCode = 0;
		responseMessage = null;
		responseHeaders.clear();
		responseContent = null;
		
		String host = (uri.getHost() != null) ? uri.getHost() : "localhost";
		int port = (uri.getPort() >= 0) ? uri.getPort(): 80;
		
		Socket socket = new Socket(host, port);
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
		BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
		try {
			StringBuilder sb = new StringBuilder();
			sb.append(method).append(' ');
			sb.append(uri.getRawPath());
			if (uri.getRawQuery() != null) sb.append('?').append(uri.getRawQuery());
			sb.append(" HTTP/1.0\r\n");
			for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
				sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
			}
			sb.append("\r\n");
			if (requestContent != null) sb.append(requestContent);
			writer.write(sb.toString());
			writer.flush();
			
			String line = reader.readLine();
			int index1 = line.indexOf(' ');
			int index2 = line.indexOf(' ', index1+1);
			responseCode = Integer.parseInt(line.substring(index1+1, index2));
			responseMessage = line.substring(index2+1);
			
			while ((line = reader.readLine()) != null && line.length() > 0) {
				int index = line.indexOf(':');
				responseHeaders.put(line.substring(0, index), line.substring(index+1));
			}
			
			sb.setLength(0);
			while ((line = reader.readLine()) != null) {
				sb.append(line).append('\n');
			}
			responseContent = sb.toString();
		} finally {
			socket.close();
		}
	}
	
	public void clear() {
		method = "GET";
		requestHeaders.clear();
		responseCode = 0;
		responseMessage = null;
		responseHeaders.clear();
		responseContent = null;
	}
}
