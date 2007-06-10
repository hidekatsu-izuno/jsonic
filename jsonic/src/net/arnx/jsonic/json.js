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

JSON = new Object();

JSON.encode = function (value) {
	var i = 0;
	var text = 'null';
	var type = typeof(value);

	if (type == 'boolean' || value instanceof Boolean) {
		text = (value) ? 'true' : 'false';
	} else if (!value && value != false) {
		text = 'null';
	} else if (type == 'string' || value instanceof String) {
		var data = ['"'];
		for (i = 0; i < value.length; i++) {
			var c = value.charAt(i);
			switch (c) {
			case '"':
				data.push('\\"');
				break;
			case '\\':
				data.push('\\\\');
				break;
			case '\b':
				data.push('\\b');
				break;
			case '\f':
				data.push('\\f');
				break;
			case '\n':
				data.push('\\n');
				break;
			case '\r':
				data.push('\\r');
				break;
			case '\t':
				data.push('\\t');
				break;
			default:
				data.push(c);
			}
		}
		data.push('"');
		text = data.join('');
	} else if (type == 'number' || value instanceof Number) {
		if (isNaN(value) || value == Number.POSITIVE_INFINITY || value == Number.NEGATIVE_INFINITY) {
			text = '"' + value.toString() + '"';
		} else {
			text = value.toString();
		}
	} else if (value instanceof Array) {
		var data = [];
		for (var i = 0; i < value.length; i++)
			data.push(JSON.encode(value[i]));
		text = '[' + data.join(',') + ']';
	} else if (type == 'object') {
		var data = [];
		for (var key in value) {
			data.push(JSON.encode(key) + ':' + JSON.encode(value[key]));
		}
		text = '{' + data.join(',') + '}';
	}
	
	return text;
}

JSON.decode = function (value) {
	if (/^("(\\.|[^"\\\n\r])*"|[,:{}\[\]0-9.\-+Eaeflnr-u \n\r\t])+?$/.test(value)) {
		return eval("(" + value + ")");
	}
	throw new Error(1000, 'JSON syntax error.');
}