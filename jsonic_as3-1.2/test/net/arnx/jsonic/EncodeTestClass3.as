package net.arnx.jsonic
{
	public dynamic class EncodeTestClass3
	{
		public function EncodeTestClass3() {
		}
		public var publicValue:int = 1;
		
		[Transient]
		public var transientValue:int = 1;
		
		protected var protectedValue:int = 1;
		
		internal var friendlyValue:int = 1;
		
		private var privateValue:int = 1;
		
		public var mine:EncodeTestClass2;
	}
}