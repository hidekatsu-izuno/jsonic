package sample.web.rest.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

public class MemoService {
	
	// auto binding
	public HttpSession session;
	
	// it's incorrect program for sample. you should use RDBMS.
	private static int count = 0;
	private static Map<Integer, Memo> list;
	
	public void init() {
		Map map = (Map)session.getAttribute(this.getClass().getName());
		if (map == null) {
			count = 0;
			list = new LinkedHashMap<Integer, Memo>();
		} else {
			count = (Integer)map.get("count");
			list = (Map<Integer, Memo>)map.get("list");
		}
	}
	
	public Collection<Memo> find() {
		return list.values();
	}
	
	public void create(Memo memo) {
		memo.id = count++;
		list.put(memo.id, memo);
	}
	
	public void update(Memo memo) {
		Memo target = list.get(memo.id);
		target.title = memo.title;
		target.text = memo.text;
	}
	
	public void delete(Memo memo) {
		list.remove(memo.id);
	}
	
	public void destroy() {
		Map map = new HashMap();
		map.put("count", count);
		map.put("list", list);
		session.setAttribute(this.getClass().getName(), map);
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
