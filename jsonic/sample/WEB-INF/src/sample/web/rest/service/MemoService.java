package sample.web.rest.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class MemoService {
	
	// it's incorrect program for sample. you should use RDBMS.
	private static int count = 0;
	private static Map<Integer, Memo> list = new LinkedHashMap<Integer, Memo>();
	
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
}

class Memo {
	public Integer id;
	public String title;
	public String text;
	
	public String toString() {
		return "{ id: " + id + ", title: \"" + title + "\", text: \"" + text + "\" }";
	}
}
