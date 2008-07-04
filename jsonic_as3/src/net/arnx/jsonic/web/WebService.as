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
	import flash.events.IEventDispatcher;
	import flash.utils.Proxy;
	import flash.utils.flash_proxy;
	
	import mx.messaging.ChannelSet;
	import mx.messaging.messages.HTTPRequestMessage;
	import mx.rpc.AsyncToken;
	import mx.rpc.events.FaultEvent;
	import mx.rpc.events.ResultEvent;
	import mx.rpc.http.HTTPService;

	[Event(name="result", type="mx.rpc.events.ResultEvent")]
	[Event(name="fault", type="mx.rpc.events.FaultEvent")]
	[Event(name="invoke", type="mx.rpc.events.InvokeEvent")]
	[Bindable(event="operationsChange")]
	public dynamic class WebService extends Proxy implements IEventDispatcher {
		private var _service:HTTPService;
		private var _eventDispatcher:EventDispatcher;
		private var _source:String;
		
		public function WebService(destination:String = null, service:HTTPService = null) {
			super();
	        _eventDispatcher = new EventDispatcher(this);
			_service = (service) ? service : new HTTPService();
			_service.contentType = "application/json";
			_service.method = HTTPRequestMessage.POST_METHOD;
			_service.resultFormat = HTTPService.RESULT_FORMAT_TEXT;
			if (destination != null) _service.destination = destination;
			
			_operations = {};
		}
		
		protected function get service():HTTPService {
			return _service;
		}
		
		[Inspectable(category="General", defaultValue="true")]
    	/**
		 * When this value is true, anonymous objects returned are forced to bindable objects.
		 */
		public function get makeObjectsBindable():Boolean {
			return _service.makeObjectsBindable;
		}

		public function set makeObjectsBindable(b:Boolean):void {
			_service.makeObjectsBindable = b;
		}
		
		public function get channelSet():ChannelSet {
			return _service.channelSet;
		}
    
		public function set channelSet(value:ChannelSet):void {
			_service.channelSet = value;
		}
		
		[Inspectable(defaultValue="DefaultHTTP", category="General")]
		public function get destination():String {
			return _service.destination;
		}
		
		public function set destination(value:String):void {
			_service.destination = value;
		}
		
		[Inspectable(category="General")]
		public function get source():String {
			return _source;
		}
		
		public function set source(source:String):void {
			_source = source;
		}
		
		[Inspectable(category="General")]
		public function get requestTimeout():int {
			return _service.requestTimeout;
		}
		
		public function set requestTimeout(value:int):void {
			_service.requestTimeout = value;
		}
		
		public function get rootURL():String {
			return _service.rootURL;
		}
		
		public function set rootURL(value:String):void {
			_service.rootURL = value;
		}
		
		[Inspectable(defaultValue="undefined", category="General")]
		public function get url():String {
			return _service.url;
		}
		
		public function set url(value:String):void {
			_service.url = value;
		}

		[Inspectable(defaultValue="false", category="General")]
		public function get useProxy():Boolean {
			return _service.useProxy;
		}
		
		public function set useProxy(value:Boolean):void {
			_service.useProxy = value;
		}

		//----------------------------------
		//  operations
		//----------------------------------
		
		private var _operations:Object;

		[ArrayElementType("net.arnx.jsonic.web.Operation")]
		public function set operations(ops:Array):void {
			for (var i:Number = 0; i < ops.length; i++) {
				var op:Operation = Operation(ops[i]);
				op.init(this);
				_operations[op.name] = op;
			}
		}
		
		internal function send(... args:Array):AsyncToken {
			return _service.send(args);
		}
		
	    //--------------------------------------------------------------------------
	    // EventDispatcher methods
	    //--------------------------------------------------------------------------
	
	    /**
	     *  @inheritDoc
	     */
		public function addEventListener(type:String, listener:Function, useCapture:Boolean = false, 
			priority:int = 0, useWeakReference:Boolean = false):void {
			_eventDispatcher.addEventListener(type, listener, useCapture, priority, useWeakReference);
		}
		
	    /**
	     *  @inheritDoc
	     */
		public function dispatchEvent(event:Event):Boolean {
			return _eventDispatcher.dispatchEvent(event);
		}
		
	    /**
	     *  @inheritDoc
	     */
		public function hasEventListener(type:String):Boolean {
			return _eventDispatcher.hasEventListener(type);
		}
		
	    /**
	     *  @inheritDoc
	     */
		public function removeEventListener(type:String, listener:Function, useCapture:Boolean = false):void {
			_eventDispatcher.removeEventListener(type, listener, useCapture);
		}
		
	    /**
	     *  @inheritDoc
	     */
		public function willTrigger(type:String):Boolean {
			return _eventDispatcher.willTrigger(type);
		}
		
	    //--------------------------------------------------------------------------
	    // Proxy methods
	    //--------------------------------------------------------------------------
	    
		/**
		 * @private
		 */
		override flash_proxy function getProperty(name:*):* {
	        return getOperation(getLocalName(name));
		}
		
	    /**
	     * @private
	     */
	    override flash_proxy function setProperty(name:*, value:*):void {
			throw new Error("Not supported operation.");
	    }
		
		/**
		 * @private
		 */
		override flash_proxy function callProperty(name:*, ... args:Array):* {
			return getOperation(getLocalName(name)).apply(null, args);
		}
	
		private var _nextNameArray:Array;
	    
		/**
		 * @private
		 */
	    override flash_proxy function nextNameIndex(index:int):int {
			if (index == 0) {
				_nextNameArray = [];
				for (var op:String in _operations) {
					_nextNameArray.push(op);    
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
	        return _operations[_nextNameArray[index-1]];
	    }
	    
	    private function getLocalName(name:Object):String {
	    	return (name is QName) ? QName(name).localName : String(name);
		}
		
	    //---------------------------------
	    //   Public methods
	    //---------------------------------
	
		public function getOperation(name:String):Operation {
			var op:Operation = _operations[name];
			if (!op) {
				op = new Operation(name);
				op.init(this);
				_operations[name] = op;
			}
			return op;
		}
	
	    public function disconnect():void {
	        _service.disconnect();
	    }

	    public function setCredentials(username:String, password:String, charset:String=null):void {
	        _service.setCredentials(username, password, charset);
	    }

	    public function logout():void {
	        _service.logout();
	    }

	    public function setRemoteCredentials(remoteUsername:String, remotePassword:String, charset:String=null):void {
	        _service.setRemoteCredentials(remoteUsername, remotePassword, charset);
	    }
	}
}