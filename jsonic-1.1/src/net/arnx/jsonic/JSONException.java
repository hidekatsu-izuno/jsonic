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

public class JSONException extends RuntimeException {
	private static final long serialVersionUID = -8323989588488596436L;

	public static final int PARSE_ERROR = 0;
	public static final int FORMAT_ERROR = 1;
	public static final int CONVERT_ERROR = 2;
	
	private int errorCode = PARSE_ERROR;
	private long lineNumber = -1l;
	private long columnNumber = -1l;
	private long offset = -1l;
	
	JSONException(String message, long lineNumber, long columnNumber, long offset) {
		super(message);
		this.lineNumber = lineNumber;
		this.columnNumber = columnNumber;
		this.offset = offset;
	}
	
	public JSONException(int errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}
	
	public JSONException(int errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
	
	public int getErrorCode() {
		return errorCode;
	}
	
	/**
	 * Returns the line number where the error was found.
	 */
	public long getLineNumber() {
		return lineNumber;
	}
	
	/**
	 * Returns the column number where the error was found.
	 */
	public long getColumnNumber() {
		return columnNumber;
	}
	
	/**
	 * Returns the offset in line where the error was found.
	 */
	public long getErrorOffset() {
		return offset;
	}
}
