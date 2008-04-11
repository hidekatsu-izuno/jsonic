package sample.web.rest.service;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MemoService {
	
	// it's incorrect use. you shoud use RDBMS.
	private static int count = 0;
	private static Map<Integer, Memo> list = Collections.synchronizedMap(new LinkedHashMap<Integer, Memo>());
	
	public void init(HttpServletRequest request, HttpServletResponse response) {
		response.setHeader("X-JSON", "[\"Helo. JSONIC!\"]");
	}
	
	public Collection<Memo> find() {
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
	
	public void destroy() {
	}
}

class Memo {
	public Integer id;
	public String title;
	public String text;
	
	public String toString() {
		return "{ id: " + id + ", title: \"" + title + "\", text: \"" + text + "\" }";
	}
}
