/* 
 * Copyright 2014 Hidekatsu Izuno
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.arnx.jsonic;

/**
 * JSON event types for Pull Parser (getReader). 
 */
public enum JSONEventType {
	/**
	 * Starts JSON object.
	 */
	START_OBJECT,
	
	/**
	 * Ends JSON object.
	 */
	END_OBJECT,
	
	/**
	 * Starts JSON array.
	 */	
	START_ARRAY,
	
	/**
	 * Ends JSON array.
	 */	
	END_ARRAY,
	
	/**
	 * JSON object name.
	 */
	NAME,
	
	/**
	 * JSON string.
	 */
	STRING,
	
	/**
	 * JSON number.
	 */
	NUMBER,
	
	/**
	 * JSON true or false
	 */
	BOOLEAN,
	
	/**
	 * JSON null
	 */
	NULL,
	
	/**
	 * White spaces
	 */
	WHITESPACE,
	
	/**
	 * Single line or Multi line comment.
	 */
	COMMENT
}
