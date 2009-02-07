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

package net.arnx.jsonic.web.mxml {
	import flash.events.ErrorEvent;
	import flash.events.Event;
	
	import mx.core.IMXMLObject;
	import mx.rpc.events.AbstractEvent;
	import mx.rpc.events.FaultEvent;
	import mx.rpc.http.mxml.HTTPService;
	import mx.rpc.mxml.IMXMLSupport;
	
	import net.arnx.jsonic.web.WebService;
	
	[DefaultProperty("operations")]
	public dynamic class WebService extends net.arnx.jsonic.web.WebService implements IMXMLSupport, IMXMLObject {
		private var _document:Object;
		private var _id:String;
		
		private var _concurrency:String;
		private var _showBusyCursor:Boolean;
		
		public function WebService(destination:String=null) {
			super(destination, new HTTPService());
		}
		
		public function initialized(document:Object, id:String):void {
			_document = document;
			_id = id;
		}
		
		[Inspectable(enumeration="multiple,single,last", defaultValue="multiple", category="General")]
		public function get concurrency():String {
			return HTTPService(service).concurrency;
		}
		
		public function set concurrency(value:String):void {
			HTTPService(service).concurrency = value;
		}
		
		[Inspectable(defaultValue="false", category="General")]
		public function get showBusyCursor():Boolean {
			return HTTPService(service).showBusyCursor;
		}
		
		public function set showBusyCursor(value:Boolean):void {
			HTTPService(service).showBusyCursor = value;
		}
		
		[Inspectable(category="General")]
		public function get endpoint():String {
			return this.url;
		}
    
		public function set endpoint(value:String):void {
        	if (this.url != value || value == null) {
				this.url = value;
			}
		}
		
		override public function dispatchEvent(event:Event):Boolean {
			if (hasEventListener(event.type)) {
				return super.dispatchEvent(event);
			} else if ((event is FaultEvent && !hasTokenResponders(event)) || event is ErrorEvent) {
				var reason:String = (event is FaultEvent) ? 
					FaultEvent(event).fault.faultString :
					ErrorEvent(event).text;

				if (_document && _document.willTrigger(ErrorEvent.ERROR)) {
					var errorEvent:ErrorEvent = new ErrorEvent(ErrorEvent.ERROR, true, true);
                	errorEvent.text = reason;
					return _document.dispatchEvent(errorEvent);
				} else if (event is FaultEvent) {
					throw FaultEvent(event).fault;
				} else {
					throw new Error("No listener for event: " + reason);
				}
			}
			return false;
		}
		
		private function hasTokenResponders(event:Event):Boolean {
			if (event is AbstractEvent) {
            	var rpcEvent:AbstractEvent = event as AbstractEvent;
            	if (rpcEvent.token != null && rpcEvent.token.hasResponder()) {
					return true;
				}
			}

			return false;
		}
	}
}