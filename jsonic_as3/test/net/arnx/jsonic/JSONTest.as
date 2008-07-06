package net.arnx.jsonic {
	import flexunit.framework.TestCase;
	import flexunit.framework.TestSuite;
	
	import mx.resources.Locale;

	public class JSONTest extends TestCase {
		public function JSONTest(methodName:String=null) {
			super(methodName);
		}
		
		public static function suite():TestSuite {
			var suite:TestSuite = new TestSuite();
			suite.addTest(new JSONTest("testEncode"));	
			suite.addTest(new JSONTest("testDecode"));	
			return suite;
		}
		
		public function testEncode():void {
			var list:Array = [];
			assertEquals("[]", JSON.encode(list));
			
			list.push("", 1, 1.0, "c", "string", true, false, null, new Object(), new Array(), /\.*/);
			assertEquals('["",1,1,"c","string",true,false,null,{},[],"\\\\.*"]', JSON.encode(list));
			
			assertEquals('[1,2,3]', JSON.encode([1,2,3]));
			assertEquals('[1,2,3,"NaN","Infinity","-Infinity"]', 
					JSON.encode([1.0,2.0,3.0,NaN,Number.POSITIVE_INFINITY,Number.NEGATIVE_INFINITY])
			);
			
			assertEquals('["en-US"]', JSON.encode([new Locale("en_US")]));
			assertEquals('["ja-JP"]', JSON.encode([new Locale("ja_JP")]));
			
			var date:Date = new Date();
			assertEquals('[' + date.getTime() + ']', JSON.encode([date]));
			
			assertEquals('{}', JSON.encode({}));
			assertEquals('{"value":1}', JSON.encode({value:1}));
			
			assertEquals('{"publicValue":1}', JSON.encode(new EncodeTestClass1()));
		}
		
		public function testDecode():void {
			
		}
	}
}
