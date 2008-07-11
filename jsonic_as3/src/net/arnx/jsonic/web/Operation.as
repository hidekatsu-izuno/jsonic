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

package net.arnx.jsonic.web {
	import flash.events.Event;
	import flash.events.EventDispatcher;
	
	import mx.events.PropertyChangeEvent;
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
		
		private var _result:Object;
		
		[Bindable]
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
					var response:Object = null;
					var nextEvent:Event = null;
					
					try {
						response = JSON.decode(String(event.result));
					} catch (error:Error) {
						var fault:Fault = new Fault(FaultEvent.FAULT, error.message, error.getStackTrace());
						nextEvent = FaultEvent.createEvent(fault, event.token, event.message);
					}
					
					var result:Object = null;
					if (nextEvent == null) {
						if (response == null 
							|| !response.hasOwnProperty("result") 
							|| !response.hasOwnProperty("error")
							|| !response.hasOwnProperty("id")) {
							nextEvent = FaultEvent.createEvent(
								new Fault(FaultEvent.FAULT, "illegal result.", null), 
								event.token, 
								event.message
							);
						} else if (response.error != null) {
							nextEvent = FaultEvent.createEvent(
								new Fault(FaultEvent.FAULT, response.error.message, null), 
								event.token, 
								event.message
							);
							result = new Error(response.error.message, response.error.code);
						} else {
							result = (_service.makeObjectsBindable) ? 
								new ObjectProxy(response.result) : response.result;
								
							nextEvent = ResultEvent.createEvent(
								result, 
								event.token, 
								event.message
							);
						}
					}
					
					var old:Object = _result;
					_result = result;
					this.dispatchEvent(PropertyChangeEvent.createUpdateEvent(this, "lastResult", old, _result));

					if (hasEventListener(nextEvent.type)) {
						dispatchEvent(nextEvent);
					} else {
						_service.dispatchEvent(nextEvent);
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
			var old:Object = _result;
			_result = null;
			
			if (fireBindingEvent) {
				this.dispatchEvent(PropertyChangeEvent.createUpdateEvent(this, "lastResult", old, _result));
			}
		}
	}
}