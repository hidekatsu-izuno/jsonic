/*
 * Copyright 2007 Hidekatsu Izuno
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
package sample.guice.web.rest.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;

public class MemoService {
	
	// it's incorrect use. you should use RDBMS.
	private int count = 0;
	private Map<Integer, Memo> list;
	
	// injects context object.
	
	@Inject
	public ServletContext application;
	
	@Inject
	public HttpServletRequest request;
	
	@Inject
	public HttpServletResponse response;
	
	@SuppressWarnings("unchecked")
	public void init() {
		// response.setHeader("X-JSON", "[\"Hello. JSONIC!\"]");
		
		ObjectInputStream oin = null;
		synchronized(MemoService.class) {
			try {
				InputStream in = application.getResourceAsStream("/WEB-INF/database.dat");
				if (in != null) {
					oin = new ObjectInputStream(in);
					count = oin.readInt();
					list = (Map<Integer, Memo>)oin.readObject();
				} else {
					list = new LinkedHashMap<Integer, Memo>();
				}
			} catch (Exception e) {
				throw new IllegalStateException(e);
			} finally {
				try {
					if (oin != null) oin.close();
				} catch (IOException e) {}
			}
		}
	}
	
	// This method is ignored. Because init method has no arguments.
	public void init(String str) {
		throw new UnsupportedOperationException();
	}
	
	public Collection<Memo> find() {
		return list.values();
	}
	
	public Collection<Memo> find(Memo memo) {
		return list.values();
	}
	
	public Collection<Memo> find(Memo memo, String str) {
		return list.values();
	}
	
	public void create(Memo memo) {
		if (memo.id != null)
			throw new IllegalArgumentException();
		
		memo.id = count++;
		list.put(memo.id, memo);
	}
	
	public void update(Memo memo) {
		if (!list.containsKey(memo.id)) 
			throw new IllegalStateException();

		Memo target = list.get(memo.id);
		target.title = memo.title;
		target.text = memo.text + "aaa";
	}
	
	public void delete(Memo memo) {
		if (!list.containsKey(memo.id)) 
			throw new IllegalStateException();
		
		list.remove(memo.id);
	}
	
	public void destroy() {
		ObjectOutputStream oout = null;
		synchronized(MemoService.class) {
			try {
				oout = new ObjectOutputStream(new FileOutputStream(application.getRealPath("/WEB-INF/database.dat")));
				oout.writeInt(count);
				oout.writeObject(list);
				oout.flush();
			} catch (Exception e) {
				throw new IllegalStateException(e);
			} finally {
				try {
					if (oout != null) oout.close();
				} catch (IOException e) {}
			}
		}
	}
}

class Memo implements Serializable {
	public Integer id;
	public String title;
	public String text;
	
	public String toString() {
		return "{ id: " + id + ", title: \"" + title + "\", text: \"" + text + "\" }";
	}
}