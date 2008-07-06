package net.arnx.jsonic {
	import flexunit.framework.AssertionFailedError;
	import flexunit.framework.TestCase;
	import flexunit.framework.TestSuite;
	import flexunit.framework.Assert;
	
	import mx.resources.Locale;
	import mx.utils.ObjectUtil;

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
			assertEquals('{"publicValue":1}', JSON.encode(new EncodeTestClass2()));
		}
		
		public function testDecode():void {
			var list:Array = [{}, [], 1, "str'ing", "", true, false, null];
			assertEquals(list, JSON.decode('[{}, [], 1, "str\'ing", "", true, false, null]'));
			assertEquals(list, JSON.decode('\r[\t{\r}\n, [\t]\r,\n1 ,\t \r"str\'ing"\n, "", true\t,\rfalse\n,\tnull\r]\n'));
		}
		
		public static function assertEquals(... rest):void {
			var message:String;
			var expected:Object;
			var actual:Object;
			
			if ( rest.length == 3 ) {
				message = rest[0];
				expected = rest[1];
				actual = rest[2];
			} else {
				message = "";
				expected = rest[0];
				actual = rest[1];
			}
			
			if (ObjectUtil.compare(expected, actual) != 0) {
				if (message.length > 0) {
					message = message + " - ";
				}
				throw new AssertionFailedError(message + "expected:<" + expected + "> but was:<" + actual + ">");
			}
		}
	}
}
