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
package sample.spring.web.rest.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.ServletContextAware;

public class MemoService implements ServletContextAware {
	
	// it's incorrect use. you should use RDBMS.
	private int count = 0;
	private Map<Integer, Memo> list;

	private ServletContext context;
	private HttpServletRequest request;
	private HttpServletResponse response;

	@Override
	public void setServletContext(ServletContext context) {
		this.context = context;
	}
	
	
	// JSONIC original injection
	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}
	
	// JSONIC original injection
	public void setResponse(HttpServletResponse response) {
		this.response = response;
	}
	
	@SuppressWarnings("unchecked")
	public void init() {
		response.setHeader("X-JSON", request.getHeader("X-JSON"));
		
		ObjectInputStream oin = null;
		synchronized(MemoService.class) {
			try {
				File file = new File(context.getRealPath("/WEB-INF/database.dat"));
				if (file.exists()) {
					oin = new ObjectInputStream(new FileInputStream(file));
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
		
		request.getContentType();
		response.getContentType();
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
		target.text = memo.text;
	}
	
	public void delete(Memo memo) {
		if (!list.containsKey(memo.id)) 
			throw new IllegalStateException();
		
		list.remove(memo.id);
	}
	
	public void print() throws IOException {
		response.setCharacterEncoding("MS932");
		response.setHeader("Content-Disposition", "attachment; filename=\"memos.csv\"");
		PrintWriter writer = response.getWriter();
		
		for (Memo memo : list.values()) {
			writer.print(memo.id);
			writer.print(",");
			writer.print(memo.title);
			writer.print(",");
			writer.print(memo.text);
			writer.print("\r\n");
		}
		
		response.flushBuffer();
	}
	
	public void exception() {
		throw new MemoException("memo error!");
	}
	
	public void destroy() {
		ObjectOutputStream oout = null;
		synchronized(MemoService.class) {
			try {
				oout = new ObjectOutputStream(new FileOutputStream(context.getRealPath("/WEB-INF/database.dat")));
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