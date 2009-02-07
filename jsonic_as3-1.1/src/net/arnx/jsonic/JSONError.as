package net.arnx.jsonic {
	public class JSONError extends Error {
		public static const FORMAT_ERROR:int = 10000;
		public static const PARSE_ERROR:int = 20000;
		
		private var _lineNumber:int = -1;
		private var _columnNumber:int = -1;
		private var _offset:int = -1;
		
		public function JSONError(message:String, id:int, lineNumber:int=-1, columnNumber:int=-1, offset:int=-1) {
			super(message, id);
			_lineNumber = lineNumber;
			_columnNumber = columnNumber;
			_offset = offset;
		}
		
		public function get lineNumber():int {
			return _lineNumber;
		}
		
		public function get columnNumber():int {
			return _columnNumber;
		}
		
		public function get offset():int {
			return _offset;
		}
	}
}