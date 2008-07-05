package net.arnx.jsonic {
	public class JSONParseError extends Error {
		private var _lineNumber:int = -1;
		private var _columnNumber:int = -1;
		private var _offset:int = -1;
		
		public function JSONParseError(message:String="", lineNumber:int=-1, columnNumber:int=-1, offset:int=-1) {
			super(message);
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