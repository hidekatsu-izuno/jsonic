/*
 * Copyright 2009 Hidekatsu Izuno
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

package net.arnx.jsonic {
	import flash.utils.ByteArray;
	
	import mx.collections.ArrayCollection;
	import mx.resources.IResourceManager;
	import mx.resources.Locale;
	import mx.resources.ResourceManager;
	import mx.utils.ObjectUtil;
	
	[ResourceBundle("jsonic")]
	public class JSON {
		public static function encode(source:Object, prettyPrint:Boolean = false):String {
			var json:JSON = new JSON();
			json.prettyPrint = prettyPrint;
			return json.format(source);
		}
		
		public static function decode(source:String):Object {
			return new JSON().parse(source);
		}
		
		private static var _resourceManager:IResourceManager = ResourceManager.getInstance();
		
		private static var CONTROL_CHARS:Object = {
			"\u0000": "\\u0000", 
			"\u0001": "\\u0001", 
			"\u0002": "\\u0002", 
			"\u0003": "\\u0003",
			"\u0004": "\\u0004", 
			"\u0005": "\\u0005", 
			"\u0006": "\\u0006", 
			"\u0007": "\\u0007",
			"\u0008": "\u0008",
			"\u0009": "\\t", 
			"\u000A": "\\n", 
			"\u000B": "\\u000B", 
			"\u000C": "\\f",
			"\u000D": "\\r", 
			"\u000E": "\\u000E", 
			"\u000F": "\\u000F",
			"\u0010": "\\u0010", 
			"\u0011": "\\u0011", 
			"\u0012": "\\u0012", 
			"\u0013": "\\u0013",
			"\u0014": "\\u0014", 
			"\u0015": "\\u0015", 
			"\u0016": "\\u0016", 
			"\u0017": "\\u0017",
			"\u0018": "\\u0018",
			"\u0019": "\\u0019",
			"\u001A": "\\u001A", 
			"\u001B": "\\u001B", 
			"\u001C": "\\0001C",
			"\u001D": "\\0001D", 
			"\u001E": "\\u001E", 
			"\u001F": "\\u001F",
			"\u0022": "\\\"",
			"\u005C": "\\\\",
			"\u007F": "\\u007F"
		};
		
		private var _prettyPrint:Boolean = false;
		private var _maxDepth:int;
		private var _suppressNull:Boolean = false;
		
		public function JSON(maxDepth:int = 32) {
			this.maxDepth = maxDepth;
		}
		
		public function set prettyPrint(value:Boolean):void {
			_prettyPrint = value;	
		}
		
		public function set maxDepth(value:int):void {
			_maxDepth = value;
		}
		
		public function set suppressNull(value:Boolean):void {
			_suppressNull = value;
		}
		
		public function format(o:Object):String {
			return _format(o, new ByteArray(), 0).toString();
		}
		
		public function parse(s:String):Object {
			return _parse(new StringParserSource(s));
		}
	    		
		private function _format(o:Object, array:ByteArray, level:int):ByteArray {
			if (level > _maxDepth) {
				o = null;
			}
			
			if (o is Date) {
				o = (o as Date).getTime();
			} else if (o is RegExp) {
				o = RegExp(o).source;
			} else if (o is Locale) {
				o = o.toString().replace(/_/g, '-');
			} else if (o is ByteArray) {
				o = Base64.encode(ByteArray(o));
			}
			
			if (level == 0) {
				var type:String = typeof(o);
				if (type == "number" || type == "boolean" || type == "string" || o is Date) {
					throw new JSONError(getMessage("json.format.IllegalRootTypeError"), JSONError.FORMAT_ERROR);
				}
			}
			
			var escape:Boolean = true;
			
			if (o == null) {
				array.writeUTFBytes("null");
			} else if (o is String) {
				if (escape) {
					_formatString(o as String, array);
				} else {
					array.writeUTFBytes(o as String);
				}
			} else if (o is Number) {
				if (isNaN(o as Number) || !isFinite(o as Number)) {
					_formatString(o.toString(), array);
				} else {
					array.writeUTFBytes(o.toString());
				}
			} else if (o is Boolean) {
				array.writeUTFBytes(o.toString());
			} else if (o is Array || o is ArrayCollection) {
				array.writeUTFBytes('[');
				for (var i:int = 0; i < o.length; i++) {
					if (o[i] === o) continue;
					
					if (i != 0) array.writeUTFBytes(',');
					if (_prettyPrint) tabs(array, level+1);
					_format(o[i], array, level+1);
				}
				if (_prettyPrint && o.length > 0) tabs(array, level+1);
				array.writeUTFBytes(']');
			} else {
				var classInfo:Object = ObjectUtil.getClassInfo(o, null, {
					includeReadOnly: true,
					includeTransient: false,
					uris: null
				});
				
				array.writeUTFBytes('{');
				
				var first:Boolean = true;
				for each (var key:Object in classInfo.properties) {
					if (key == 'mine' || o[key] === o || (_suppressNull && !o[key])) continue;
					
					if (first) {
						first = false;
					} else {
						array.writeUTFBytes(',');
					}
					
					if (_prettyPrint) tabs(array, level+1);
					_formatString(key.toString(), array);
					array.writeUTFBytes(':');
					if (_prettyPrint) array.writeUTFBytes(' ');
					_format(o[key], array, level+1);
				}	
				if (_prettyPrint && first) tabs(array, level+1);
				array.writeUTFBytes('}');
			}
			
			return array;
		}
		
		private function _formatString(s:String, array:ByteArray):ByteArray {
			array.writeUTFBytes('"');
			for (var i:int = 0; i < s.length; i++) {
				var c:String = s.charAt(i);
				var result:String = CONTROL_CHARS[c];
				if (result !== null) {
					array.writeUTFBytes(result);
				} else {
					array.writeUTFBytes(c);	
				}
			}
			array.writeUTFBytes('"');
			
			return array;
		}
		
		private function _parse(s:IParserSource):Object {
			var o:Object = null;

			var c:String = null;
			while ((c = s.next()) != null) {
				switch(c) {
				case '\r':
				case '\n':
				case ' ':
				case '\t':
				case '\uFEFF': // BOM
					break;
				case '[':
					if (o == null) {
						s.back();
						o = _parseArray(s, 1);
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				case '/':
				case '#':
					s.back();
					_skipComment(s);
					break;
				default:
					if (o == null) {
						s.back();
						o = _parseObject(s, 1);
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
				}
			}
			
			return (!o) ? {} : o;
		}
		
		private function _parseObject(s:IParserSource, level:int):Object {
			var point:int = 0; // 0 '{' 1 'key' 2 ':' 3 '\n'? 4 'value' 5 '\n'? 6 ',' ... '}' E
			var map:Object = (level <= _maxDepth) ? {} : null;
			var key:Object = null;
			var value:Object = null;
			var start:String = '\0';
			
			var c:String = null;
			loop:while ((c = s.next()) != null) {
				switch(c) {
				case '\r':
				case '\n':
					if (point == 5) {
						point = 6;
					}
					break;
				case ' ':
				case '\t':
				case '\uFEFF': // BOM
					break;
				case '{':
					if (point == 0) {
						start = '{';
						point = 1;
					} else if (point == 2 || point == 3){
						s.back();
						value = _parseObject(s, level+1);
						if (level < _maxDepth) map[key] = value;
						point = 5;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				case ':':
					if (point == 2) {
						point = 3;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				case ',':
					if (point == 3) {
						if (level < _maxDepth && !_suppressNull) map[key] = null;
						point = 1;
					} else if (point == 5 || point == 6) {
						point = 1;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				case '}':
					if (start == '{' && (point == 1 || point == 3 || point == 5 || point == 6)) {
						if (point == 3) {
							if (level < _maxDepth && !_suppressNull) map[key] = null;
						}
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break loop;
				case '\'':
				case '"':
					if (point == 0) {
						s.back();
						point = 1;
					} else if (point == 1 || point == 6) {
						s.back();
						key = _parseString(s);
						point = 2;
					} else if (point == 3) {
						s.back();
						value = _parseString(s);
						if (level < _maxDepth) map[key] = value;
						point = 5;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				case '[':
					if (point == 3) {
						s.back();
						value = _parseArray(s, level+1);
						if (level < _maxDepth) map[key] = value;
						point = 5;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				case '/':
				case '#':
					s.back();
					_skipComment(s);
					if (point == 5) {
						point = 6;
					}
					break;
				default:
					if (point == 0) {
						s.back();
						point = 1;
					} else if (point == 1 || point == 6) {
						s.back();
						key = ((c == '-') || (c >= '0' && c <= '9')) ? _parseNumber(s) : _parseLiteral(s);
						point = 2;
					} else if (point == 3) {
						s.back();
						value = ((c == '-') || (c >= '0' && c <= '9')) ? _parseNumber(s) : _parseLiteral(s);
						if (level < _maxDepth && (value != null || !_suppressNull)) {
							map[key] = value;
						}
						point = 5;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
				}
			}
			
			if (c == null) {
				if (point == 3 || point == 4) {
					if (level < _maxDepth && !_suppressNull) map[key] = null;
				} else if (point == 2) {
					throw createParseException(getMessage("json.parse.ObjectNotClosedError"), s);
				}
			}
			
			if ((c == null) ? (start != '\0') : (c != '}')) {
				throw createParseException(getMessage("json.parse.ObjectNotClosedError"), s);
			}
			return map;
		}
		
		private function _parseArray(s:IParserSource, level:int):Array {
			var point:int = 0; // 0 '[' 1 'value' 2 '\n'? 3 ',' ... ']' E
			var list:Array = [];
			var value:Object = null;
			
			var c:String = null;
			loop:while ((c = s.next()) != null) {
				switch(c) {
				case '\r':
				case '\n':
					if (point == 2) {
						point = 3;
					}
					break;
				case ' ':
				case '\t':
				case '\uFEFF': // BOM
					break;
				case '[':
					if (point == 0) {
						point = 1;
					} else if (point == 1 || point == 3) {
						s.back();
						value = _parseArray(s, level+1);
						if (level < _maxDepth) list.push(value);
						point = 2;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				case ',':
					if (point == 1) {
						if (level < _maxDepth) list.push(null);
					} else if (point == 2 || point == 3) {
						point = 1;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				case ']':
					if (point == 1 || point == 2 || point == 3) {
						if (point == 1 && list.length != 0 && level < _maxDepth) {
							list.push(null);
						}
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break loop;
				case '{':
					if (point == 1 || point == 3){
						s.back();
						value = _parseObject(s, level+1);
						if (level < _maxDepth) list.push(value);
						point = 2;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				case '\'':
				case '"':
					if (point == 1 || point == 3) {
						s.back();
						value = _parseString(s);
						if (level < _maxDepth) list.push(value);
						point = 2;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				case '/':
				case '#':
					s.back();
					_skipComment(s);
					if (point == 2) {
						point = 3;
					}
					break;
				default:
					if (point == 1 || point == 3) {
						s.back();
						value = ((c == '-') || (c >= '0' && c <= '9')) ? _parseNumber(s) : _parseLiteral(s);
						if (level < _maxDepth) list.push(value);
						point = 2;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);						
					}
				}
			}
			
			if (c != ']') {
				throw createParseException(getMessage("json.parse.ArrayNotClosedError"), s);
			}
			return list;
		}
		
		private function _parseString(s:IParserSource):String {
			var point:int = 0; // 0 '"|'' 1 'c' '\' 2 'c' ... '"|'' E
			var sb:ByteArray = s.getCachedBuilder();
			var start:String = '\0';
			
			var c:String = null;
			loop:while ((c = s.next()) != null) {
				switch(c) {
				case '\uFEFF': // BOM
					break;
				case '\\':
					if (point == 1) {
						if (start == '"') {
							s.back();
							sb.writeUTFBytes(_parseEscape(s));
						} else {
							sb.writeUTFBytes(c);
						}
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				case '\'':
				case '"':
					if (point == 0) {
						start = c;
						point = 1;
					} else if (point == 1) {
						if (start == c) {
							break loop;
						} else {
							sb.writeUTFBytes(c);
						}
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				default:
					if (point == 1) {
						sb.writeUTFBytes(c);
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
				}
			}
			
			if (c != start) {
				throw createParseException(getMessage("json.parse.StringNotClosedError"), s);
			}
			return sb.toString();
		}
		
		private function _parseLiteral(s:IParserSource):Object {
			var point:int = 0; // 0 'IdStart' 1 'IdPart' ... !'IdPart' E
			var sb:ByteArray = s.getCachedBuilder();
			
			var c:String = null;
			loop:while ((c = s.next()) != null) {
				if (c == '\uFEFF') continue;
				
				if (c == '\\') {
					s.back();
					c = _parseEscape(s);
				}
				
				if (point == 0 && /[a-zA-Z$_]/.test(c)) {
					sb.writeUTFBytes(c);
					point = 1;
				} else if (point == 1 && /[a-zA-Z0-9$_]/.test(c)){
					sb.writeUTFBytes(c);
				} else {
					s.back();
					break loop;
				}
			}
			
			var str:String = sb.toString();
			
			if ("null" == str) return null;
			if ("true" == str) return true;
			if ("false" == str) return false;
			
			return str;
		}	
		
		private function _parseNumber(s:IParserSource):Number {
			var point:int = 0; // 0 '(-)' 1 '0' | ('[1-9]' 2 '[0-9]*') 3 '(.)' 4 '[0-9]' 5 '[0-9]*' 6 'e|E' 7 '[+|-]' 8 '[0-9]' 9 '[0-9]*' E
			var sb:ByteArray = s.getCachedBuilder();
			
			var c:String = null;
			loop:while ((c = s.next()) != null) {
				switch(c) {
				case '\uFEFF': // BOM
					break;
				case '+':
					if (point == 7) {
						sb.writeUTFBytes(c);
						point = 8;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				case '-':
					if (point == 0) {
						sb.writeUTFBytes(c);
						point = 1;
					} else if (point == 7) {
						sb.writeUTFBytes(c);
						point = 8;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				case '.':
					if (point == 2 || point == 3) {
						sb.writeUTFBytes(c);
						point = 4;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				case 'e':
				case 'E':
					if (point == 2 || point == 3 || point == 5 || point == 6) {
						sb.writeUTFBytes(c);
						point = 7;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				default:
					if (c >= '0' && c <= '9') {
						if (point == 0 || point == 1) {
							sb.writeUTFBytes(c);
							point = (c == '0') ? 3 : 2;
						} else if (point == 2 || point == 5 || point == 9) {
							sb.writeUTFBytes(c);
						} else if (point == 4) {
							sb.writeUTFBytes(c);
							point = 5;
						} else if (point == 7 || point == 8) {
							sb.writeUTFBytes(c);
							point = 9;
						} else {
							throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
						}
					} else if (point == 2 || point == 3 || point == 5 || point == 6 || point == 9) {
						s.back();
						break loop;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
				}
			}
			
			return new Number(sb.toString());
		}
		
		private function _parseEscape(s:IParserSource):String {
			var point:int = 0; // 0 '\' 1 'u' 2 'x' 3 'x' 4 'x' 5 'x' E
			var escape:String = '\0';
			
			var c:String = null;
			loop:while ((c = s.next()) != null) {
				if (c == '\uFEFF') continue; // BOM
				
				if (point == 0) {
					if (c == '\\') {
						point = 1;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
				} else if (point == 1) {
					switch(c) {
					case 'b':
						escape = '\b';
						break loop;
					case 'f':
						escape = '\f';
						break loop;
					case 'n':
						escape = '\n';
						break loop;
					case 'r':
						escape = '\r';
						break loop;
					case 't':
						escape = '\t';
						break loop;
					case 'u':
						var n:Number = parseInt(s.next() + s.next() + s.next() + s.next(), 16);
						if (n && !isNaN(n)) {
							escape = String.fromCharCode(n);
						} else {
							throw createParseException(getMessage("json.parse.IllegalUnicodeEscape", c), s);
						}
						break loop;
					default:
						escape = c;
						break loop;
					}
				}
			}
			
			return escape;
		}
		
		private function _skipComment(s:IParserSource):void {
			var point:int = 0; // 0 '/' 1 '*' 2  '*' 3 '/' E or  0 '/' 1 '/' 4  '\r|\n|\r\n' E
			
			var c:String = null;
			loop:while ((c = s.next()) != null) {
				switch(c) {
				case '\uFEFF':
					break;
				case '/':
					if (point == 0) {
						point = 1;
					} else if (point == 1) {
						point = 4;
					} else if (point == 3) {
						break loop;
					} else if (!(point == 2 || point == 4)) {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				case '*':
					if (point == 1) {
						point = 2;
					} else if (point == 2) {
						point = 3;
					} else if (!(point == 3 || point == 4)) {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				case '\n':
				case '\r':
					if (point == 2 || point == 3) {
						point = 2;
					} else if (point == 4) {
						break loop;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				case '#':
					if (point == 0) {
						point = 4;
					} else if (point == 3) {
						point = 2;
					} else if (!(point == 2 || point == 4)) {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				default:
					if (point == 3) {
						point = 2;
					} else if (!(point == 2 || point == 4)) {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
				}
			}	
		}
				
		private function createParseException(message:String, s:IParserSource):Error {
			return new JSONError("" + s.lineNumber + ": " + message + "\n" + s.toString() + " <- ?",
				JSONError.PARSE_ERROR, s.lineNumber, s.columnNumber, s.offset);
		}
	
		private function getMessage(id:String, ... args:Array):String {
			return _resourceManager.getString("jsonic", id, args);
		}
		
		private static function tabs(array:ByteArray, count:int):void {
			array.writeUTFBytes('\n');
			for (var i:int = 0; i < count; i++) {
				array.writeUTFBytes('\t');
			}
		}
	}
}

import flash.utils.ByteArray;

interface IParserSource {
	function next():String;
	function back():void;
	function get lineNumber():int;
	function get columnNumber():int;
	function get offset():int;
	function getCachedBuilder():ByteArray;
	function toString():String;
}

class StringParserSource implements IParserSource {
	private var _lines:int = 1;
	private var _columns:int = 1;
	private var _offset:int = 0;
	
	private var _cs:String;
	private var _cache:ByteArray = new ByteArray();

	public function StringParserSource(cs:String) {
		if (cs == null) throw ArgumentError("Invalid argument:" + cs);
		_cs = cs;
	}

	public function next():String {
		if (_offset < _cs.length) {
			var c:String = _cs.charAt(_offset++);
			if (c == '\r' || (c == '\n' && _offset > 1 && _cs.charAt(offset-2) != '\r')) {
				_lines++;
				_columns = 0;
			} else {
				_columns++;
			}
			return c;
		}
		return null;
	}
	
	public function back():void {
		_offset--;
		_columns--;
	}
	
	public function get lineNumber():int {
		return _lines;
	}
	
	public function get columnNumber():int {
		return _columns;
	}
	
	public function get offset():int {
		return _offset;
	}
	
	public function getCachedBuilder():ByteArray {
		_cache.length = 0;
		return _cache;
	}
	
	public function toString():String {
		return _cs.substring(_offset-_columns+1, _offset);
	}
}

class Base64 {
	public static const BASE64_MAP:String = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
	
	public static function encode(data:ByteArray):String {
		if (!data) return null;
				
		var buffer:ByteArray = new ByteArray();
		var length:int = data.length;
		var buf:int = 0;
		for (var i:int = 0; i < length; i++) {
			var value:int = data.readByte();
			switch (i % 3) {
				case 0 :
					buffer.writeUTFBytes(BASE64_MAP.charAt((value & 0xFC) >> 2));
					buf = (value & 0x03) << 4;
					if (i + 1 == length) {
						buffer.writeUTFBytes(BASE64_MAP.charAt(buf));
						buffer.writeUTFBytes('=');
						buffer.writeUTFBytes('=');
					}
					break;
				case 1 :
					buf += (value & 0xF0) >> 4;
					buffer.writeUTFBytes(BASE64_MAP.charAt(buf));
					buf = (value & 0x0F) << 2;
					if (i + 1 == length) {
						buffer.writeUTFBytes(BASE64_MAP.charAt(buf));
						buffer.writeUTFBytes('=');
					}
					break;
				case 2 :
					buf += (value & 0xC0) >> 6;
					buffer.writeUTFBytes(BASE64_MAP.charAt(buf));
					buffer.writeUTFBytes(BASE64_MAP.charAt(value & 0x3F));
					break;
			}
		}
		
		return buffer.toString();
	}
	
	public static function decode(cs:String):ByteArray {
		var c:String;		
		var buffer:ByteArray = new ByteArray();
		
		var pos:int = 0;
		for (var i:int = 0; i < cs.length; i++) {
			c = cs.charAt(i);
			var n:Number = cs.charCodeAt(i);
			
			var data:int = 0;
			if (c >= 'A' && c <= 'Z') {
				data = n - 65;
			} else if (c >= 'a' && c <= 'z') {
				data = n - 97 + 26;
			} else if (c >= '0' && c <= '9') {
				data = n - 48 + 52;
			} else if (c == '+') {
				data = 62;
			} else if (c == '/') {
				data = 63;
			} else if (c == '=') {
				break;
			} else {
				continue;
			}
			
			switch (pos % 4) {
			case 0:
				buffer.writeByte(data << 2);
				break;
			case 1:
				buffer[pos / 4 * 3] += (data >> 4);
				buffer.writeByte((data & 0xF) << 4);
				break;
			case 2:
				buffer[pos / 4 * 3 + 1] += (data >> 2);
				buffer.writeByte((data & 0x3) << 6);
				break;
			case 3:
				buffer[pos / 4 * 3 + 2] += data;
				break;
			}
			pos++;
		}
		
		return buffer;
	}
}
