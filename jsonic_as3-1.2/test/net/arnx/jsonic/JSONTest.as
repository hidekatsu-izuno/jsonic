package net.arnx.jsonic {
	import flash.utils.Dictionary;
	
	import flexunit.framework.AssertionFailedError;
	import flexunit.framework.TestCase;
	import flexunit.framework.TestSuite;
	
	import mx.collections.ArrayCollection;
	import mx.resources.Locale;
	import mx.utils.ObjectProxy;
	import mx.utils.ObjectUtil;

	public class JSONTest extends TestCase {
		public function JSONTest(methodName:String=null) {
			super(methodName);
		}
		
		public static function suite():TestSuite {
			var suite:TestSuite = new TestSuite();
			suite.addTest(new JSONTest("testEncode"));	
			suite.addTest(new JSONTest("testDecode"));
			suite.addTest(new JSONTest("testFormat"));	
			suite.addTest(new JSONTest("testParse"));
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
			
			var etc3:EncodeTestClass3 = new EncodeTestClass3();
			etc3.dynamicValue = 1;
			assertEquals('{"dynamicValue":1,"publicValue":1}', JSON.encode(etc3));
			
			var array:ArrayCollection = new ArrayCollection([10, 20, 30]);
			assertEquals('[10,20,30]', JSON.encode(array));
						
			var dic:Dictionary = new Dictionary();
			dic["value"] = 1;
			assertEquals('{"value":1}', JSON.encode(dic));
			
			var proxyObject:Object = new ObjectProxy(dic);
			assertEquals('{"value":1}', JSON.encode(proxyObject));
		}
		
		public function testDecode():void {
			var list:Array = [{}, [], 1, "str'ing", "", true, false, null];
			assertEquals(list, JSON.decode('[{}, [], 1, "str\'ing", "", true, false, null]'));
			assertEquals(list, JSON.decode('\r[\t{\r}\n, [\t]\r,\n1 ,\t \r"str\'ing"\n, "", true\t,\rfalse\n,\tnull\r]\n'));
			
			list = [-1.1, 1.11e1, 1.11E+1, 11.1e-1];
			assertEquals(list, JSON.decode("[-1.1, 1.11e1, 1.11E+1, 11.1e-1]"));
			
			var map1:Object = {
				map2: {
					"'2'": 2,
					map3: {
						"'3": 3
					}
				},
				'1': 1
			};
			assertEquals(map1, JSON.decode('{"map2": {"\'2\'": 2, "map3": {"\'3": 3}}, "1": 1}'));
		}
		
		public function testFormat():void {
			var json:JSON = new JSON();
			var obj:Object = {
				a: 100,
				b: null,
				list: [100, null]
			};
			json.suppressNull = true;
			assertEquals('{"a":100,"list":[100,null]}', json.format(obj));
		}
		
		public function testParse():void {
			var json:JSON = new JSON();
			var obj:Object = {
				a: 100,
				list: [100, null]
			};
			json.suppressNull = true;
			assertEquals(obj, json.parse('{"a":100,"b":null,"list":[100,null]}'));
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
				throw new AssertionFailedError(message + "expected:<" + ObjectUtil.toString(expected) + "> but was:<" + ObjectUtil.toString(actual) + ">");
			}
		}
	}
}
