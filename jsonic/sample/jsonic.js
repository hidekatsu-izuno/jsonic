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
	if (!value && value != false) {
		return 'null';
	}
	
	var text = null;
	
	switch (typeof(value)) {
	case 'object':
		if (value instanceof String) {
		} else if (value instanceof Date) {
		} else if (value instanceof Array) {
		} else {
			text = '{';
			for (var key in value) {
				text += key + ':' + JSON.encode(value) + ',';
			}
			text += '}';
		}
		break;
	case 'string':
		text = '"' + value + '"';
		break;
	case 'number':
		text = value.toString();
		break;
	case 'boolean':
		text = (value) ? 'true' : 'false';
		break;
	default:
		text = value.toString();
	}
	
	return text;
}