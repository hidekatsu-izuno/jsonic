/*
 * Copyright 2008 Hidekatsu Izuno
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
	import flash.utils.Proxy;
	import flash.utils.flash_proxy;
	
	import mx.collections.ArrayCollection;
	import mx.resources.IResourceManager;
	import mx.resources.Locale;
	import mx.resources.ResourceManager;
	import mx.utils.ObjectUtil;
	
	use namespace flash_proxy;
	
	[ResourceBundle("jsonic")]
	public class JSON extends Proxy {
		public static function encode(source:Object, prettyPrint:Boolean = false):String {
			return new JSON().format(source, new ByteArray(), prettyPrint, 0).toString();
		}
		
		public static function decode(source:String):Object {
			return new JSON().load(source)._root;
		}
		
		private static var _resourceManager:IResourceManager = ResourceManager.getInstance();
		
		private var _maxDepth:int;
		private var _root:Object;
		
		public function JSON(maxDepth:int = 32) {
			_maxDepth = maxDepth;
		}
	    
	    public function load(source:String):JSON {
	    	_root = parse(new StringParserSource(source));
	    	return this;
	    }
	    	
		public function toString(prettyPrint:Boolean = false):String {
			return format(_root, new ByteArray(), prettyPrint, 0).toString();
		} 
		
	    //--------------------------------------------------------------------------
	    // Proxy methods
	    //--------------------------------------------------------------------------
	    
		/**
		 * @private
		 */
		override flash_proxy function getProperty(name:*):* {
	        return _root[name];
		}
		
	    /**
	     * @private
	     */
	    override flash_proxy function setProperty(name:*, value:*):void {
			throw _root[name] = value;
	    }
		
		/**
		 * @private
		 */
		override flash_proxy function callProperty(name:*, ... args:Array):* {
			return _root[name].apply(null, args);
		}
	
		private var _nextNameArray:Array;
	    
		/**
		 * @private
		 */
	    override flash_proxy function nextNameIndex(index:int):int {
			if (index == 0) {
				_nextNameArray = [];
				for (var name:String in _root) {
					_nextNameArray.push(name);    
				}    
			}
			return (index < _nextNameArray.length) ? index + 1 : 0;
		}
	
		/**
		 * @private
		 */
		override flash_proxy function nextName(index:int):String {
			return _nextNameArray[index-1];
		}
	
	    /**
	     * @private
	     */
	    override flash_proxy function nextValue(index:int):*
	    {
	        return _root[_nextNameArray[index-1]];
	    }
	    		
		private function format(o:Object, array:ByteArray, prettyPrint:Boolean, level:int):ByteArray {
			if (level > _maxDepth) {
				o = null;
			}
			
			if (o is Date) {
				o = (o as Date).getTime();
			} else if (o is RegExp) {
				o = RegExp(o).source;
			} else if (o is Locale) {
				o = o.toString().replace(/_/g, '-');
			}
			
			if (level == 0) {
				var type:String = typeof(o);
				if (type == "number" || type == "boolean" || type == "string" || o is Date) {
					throw new ArgumentError(getMessage("json.format.IllegalRootTypeError"));
				}
			}
			
			var escape:Boolean = true;
			
			if (o == null) {
				array.writeUTFBytes("null");
			} else if (o is String) {
				if (escape) {
					formatString(o as String, array);
				} else {
					array.writeUTFBytes(o as String);
				}
			} else if (o is Number) {
				if (isNaN(o as Number) || !isFinite(o as Number)) {
					formatString(o.toString(), array);
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
					if (prettyPrint) tabs(array, level+1);
					format(o[i], array, prettyPrint, level+1);
				}
				if (prettyPrint && o.length > 0) tabs(array, level+1);
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
					if (o[key] === o) continue;
					
					if (first) {
						first = false;
					} else {
						array.writeUTFBytes(',');
					}
					
					if (prettyPrint) tabs(array, level+1);
					formatString(key.toString(), array);
					array.writeUTFBytes(':');
					if (prettyPrint) array.writeUTFBytes(' ');
					format(o[key], array, prettyPrint, level+1);
				}	
				if (prettyPrint && first) tabs(array, level+1);
				array.writeUTFBytes('}');
			}
			
			return array;
		}
		
		private function formatString(s:String, array:ByteArray):ByteArray {
			array.writeUTFBytes('"');
			for (var i:int = 0; i < s.length; i++) {
				var c:String = s.charAt(i);
				switch (c) {
					case '"':
					case '\\': 
						array.writeUTFBytes('\\');
						array.writeUTFBytes(c);
						break;
					case '\b':
						array.writeUTFBytes('\\b');
						break;
					case '\f':
						array.writeUTFBytes('\\f');
						break;
					case '\n':
						array.writeUTFBytes('\\n');
						break;
					case '\r':
						array.writeUTFBytes('\\r');
						break;
					case '\t':
						array.writeUTFBytes('\\t');
						break;
					default:
						array.writeUTFBytes(c);	
				}
			}
			array.writeUTFBytes('"');
			
			return array;
		}
		
		private function parse(s:IParserSource):Object {
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
						o = parseArray(s, 1);
						break;
					}
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				case '/':
				case '#':
					s.back();
					skipComment(s);
					break;
				default:
					if (o == null) {
						s.back();
						o = parseObject(s, 1);
						break;
					}
					throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
				}
			}
			
			return o;
		}
		
		private function parseObject(s:IParserSource, level:int):Object {
			var point:int = 0; // 0 '{' 1 'key' 2 ':' 3 '\n'? 4 'value' 5 '\n'? 6 ',' ... '}' E
			var map:Object = {};
			var key:String = null;
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
						value = parseObject(s, level+1);
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
						if (level < _maxDepth) map[key] = null;
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
							if (level < _maxDepth) map[key] = null;
						}
						break loop;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				case '\'':
				case '"':
					if (point == 0) {
						s.back();
						point = 1;
					} else if (point == 1 || point == 6) {
						s.back();
						key = parseString(s);
						point = 2;
					} else if (point == 3) {
						s.back();
						value = parseString(s);
						if (level < _maxDepth) map[key] = value;
						point = 5;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				case '[':
					if (point == 3) {
						s.back();
						value = parseArray(s, level+1);
						if (level < _maxDepth) map[key] = value;
						point = 5;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				case '/':
				case '#':
					s.back();
					skipComment(s);
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
						key = parseLiteral(s);
						point = 2;
					} else if (point == 3) {
						if ((c == '-') || (c >= '0' && c <= '9')) {
							s.back();
							value = parseNumber(s);
							if (level < _maxDepth) map[key] = value;
						} else {
							s.back();
							var literal:String = parseLiteral(s);
							if (literal == "null") {
								map[key] = null;
							} else if (literal == "true") {
								map[key] = true;
							} else if (literal == "false") {
								map[key] = false;
							} else {
								map[key] = literal;
							}
						}
						point = 5;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
				}
			}
			
			if ((c == null) ? (start != '\0') : (c != '}')) {
				throw createParseException(getMessage("json.parse.ObjectNotClosedError"), s);
			}
			return map;
		}
		
		private function parseArray(s:IParserSource, level:int):Array {
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
						value = parseArray(s, level+1);
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
					if (point == 1) {
						if (list.length != 0 && level < _maxDepth) list.push(null);
						break loop;					
					} else if (point == 2 || point == 3) {
						break loop;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				case '{':
					if (point == 1 || point == 3){
						s.back();
						value = parseObject(s, level+1);
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
						value = parseString(s);
						if (level < _maxDepth) list.push(value);
						point = 2;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
					break;
				case '/':
				case '#':
					s.back();
					skipComment(s);
					if (point == 2) {
						point = 3;
					}
					break;
				default:
					if (point == 1 || point == 3) {
						if ((c == '-') || (c >= '0' && c <= '9')) {
							s.back();
							value = parseNumber(s);
							if (level < _maxDepth) list.push(value);
						} else {
							s.back();
							var literal:String = parseLiteral(s);
							if (level < _maxDepth) {
								if (literal == "null") {
									list.push(null);
								} else if (literal == "true") {
									list.push(true);
								} else if (literal == "false") {
									list.push(false);
								} else {
									list.push(literal);
								}
							}
						}
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
		
		private function parseString(s:IParserSource):String {
			var point:int = 0; // 0 '"|'' 1 'c' ... '"|'' E
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
							sb.writeUTFBytes(parseEscape(s));
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
		
		private function parseLiteral(s:IParserSource):String {
			var point:int = 0; // 0 'IdStart' 1 'IdPart' ... !'IdPart' E
			var sb:ByteArray = s.getCachedBuilder();
			
			var c:String = null;
			loop:while ((c = s.next()) != null) {
				switch(c) {
				case '\uFEFF': // BOM
					break;
				case '\\':
					s.back();
					c = parseEscape(s);
					if (point == 0 && /[a-zA-Z$_]/.test(c)) {
						sb.writeUTFBytes(c);
						point = 1;
					} else if (point == 1 && /[a-zA-Z0-9$_]/.test(c)){
						sb.writeUTFBytes(c);
					} else {
						s.back();
						break loop;
					}
					break;
				default:
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
			}
			return sb.toString();
		}	
		
		private function parseNumber(s:IParserSource):Number {
			var point:int = 0; // 0 '(-)' 1 '0' | ('[1-9]' 2 '[0-9]*') 3 '(.)' 4 '[0-9]' 5 '[0-9]*' 6 'e|E' 7 '[+|-]' 8 '[0-9]' E
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
						} else if (point == 2 || point == 5) {
							sb.writeUTFBytes(c);
						} else if (point == 4) {
							sb.writeUTFBytes(c);
							point = 5;
						} else if (point == 7 || point == 8) {
							sb.writeUTFBytes(c);
							break loop;
						} else {
							throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
						}
					} else if (point == 2 || point == 3 || point == 5 || point == 6) {
						s.back();
						break loop;
					} else {
						throw createParseException(getMessage("json.parse.UnexpectedChar", c), s);
					}
				}
			}
			
			return new Number(sb.toString());
		}
		
		private function parseEscape(s:IParserSource):String {
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
					case '"':
					case '\\':
					case '/':
						escape = c;
						break loop;
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
		
		private function skipComment(s:IParserSource):void {
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
			return new JSONParseError("" + s.lineNumber + ": " + message + "\n" + s.toString() + " <- ?");
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
