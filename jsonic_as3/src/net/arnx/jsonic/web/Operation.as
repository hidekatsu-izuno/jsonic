package net.arnx.jsonic.web {
	import flash.events.Event;
	import flash.events.EventDispatcher;
	
	import mx.rpc.AsyncResponder;
	import mx.rpc.AsyncToken;
	import mx.rpc.Fault;
	import mx.rpc.events.FaultEvent;
	import mx.rpc.events.ResultEvent;
	import mx.utils.ObjectProxy;
	
	import net.arnx.jsonic.JSON;
	
	[Event(name="result", type="mx.rpc.events.ResultEvent")]
	[Event(name="fault", type="mx.rpc.events.FaultEvent")]
	public class Operation extends EventDispatcher {
		private var _name:String;
		private var _service:WebService;
		private var _result:Object

		public function Operation(name:String = null) {
			super();
			_name = name;
		}
		
		internal function init(service:WebService):void {
			this._service = service;
		}
		
		public function get name():String {
			return _name;
		}
		
		public function set name(value:String):void {
			if (name != value) {
				_name = value;
			}
		}
		
		[Bindable("resultForBinding")]
		public function get lastResult():Object {
			return _result;
		}
		
		protected function get service():WebService {
			return _service;
		}
		
		public function apply(target:*, args:*):AsyncToken {
			return this.call.apply(target, args);
		}
		
		public function call(... args:Array):AsyncToken {
			var token:AsyncToken = _service.send('{'
				+ '"method":"' + _service.source + '.' + name + '",'
				+ '"params":' + JSON.encode(args) + ','
				+ '"id":' + (new Date().getTime())
				+ '}');
			
			token.addResponder(new AsyncResponder(
				function  onResult(event:ResultEvent, token:AsyncToken = null):void {
					_result = null;
					var event2:Event = null;
					
					try {
						_result = JSON.decode(String(event.result));
					} catch (error:Error) {
						var fault:Fault = new Fault(FaultEvent.FAULT, error.message, error.getStackTrace());
						event2 = FaultEvent.createEvent(fault, event.token, event.message);
					}
					
					if (event2 == null) {
						if (_result == null 
							|| !_result.hasOwnProperty("result") 
							|| !_result.hasOwnProperty("error")
							|| !_result.hasOwnProperty("id")) {
							event2 = FaultEvent.createEvent(
								new Fault(FaultEvent.FAULT, "illegal result.", null), 
								event.token, 
								event.message
							);
						} else if (_result.error != null) {
							event2 = FaultEvent.createEvent(
								new Fault(FaultEvent.FAULT, _result.error["message"], null), 
								event.token, 
								event.message
							);
						} else {
							_result = (_service.makeObjectsBindable) ? 
								new ObjectProxy(_result.result) : _result.result;
							event2 = ResultEvent.createEvent(_result, event.token, event.message);
						}
					}
					
					if (hasEventListener(event2.type)) {
						dispatchEvent(event2);
					} else {
						_service.dispatchEvent(event2);
					}
				},
				function onFault(event:FaultEvent, token:AsyncToken = null):void {
					if (hasEventListener(event.type)) {
						dispatchEvent(event);
					} else {
						_service.dispatchEvent(event);
					}
				}
			));
			
			return token;
		}
		
		public function clearResult(fireBindingEvent:Boolean = true):void {
			_result = null;
			if (fireBindingEvent) {
				dispatchEvent(new flash.events.Event("resultForBinding"));
			}	
		}
	}
}