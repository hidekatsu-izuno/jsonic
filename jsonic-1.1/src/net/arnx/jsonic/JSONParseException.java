/*
 * Copyright 2007-2008 Hidekatsu Izuno
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
package net.arnx.jsonic;

public class JSONParseException extends RuntimeException {
	private static final long serialVersionUID = -8323989588488596436L;

	public static final int ERROR_PARSE_FAILED = 0;
	public static final int ERROR_CONVERT_FAILED = 1;
	
	private int errorCode = ERROR_PARSE_FAILED;
	private long lineNumber = -1l;
	private long columnNumber = -1l;
	private long offset = -1l;
	
	JSONParseException(String message, long lineNumber, long columnNumber, long offset) {
		super(message);
		this.lineNumber = lineNumber;
		this.columnNumber = columnNumber;
		this.offset = offset;
	}
	
	public JSONParseException(int errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}
	
	public JSONParseException(int errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
	
	public int getErrorCode() {
		return errorCode;
	}
	
	public long getLineNumber() {
		return lineNumber;
	}
	
	public long getColumnNumber() {
		return columnNumber;
	}
	
	public long getErrorOffset() {
		return offset;
	}
}
