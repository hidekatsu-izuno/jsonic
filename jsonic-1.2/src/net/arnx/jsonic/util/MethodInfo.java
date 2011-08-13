package net.arnx.jsonic.util;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MethodInfo implements Comparable<MethodInfo> {
	private Class<?> beanClass;
	
	private String name;
	
	List<Member> members = new ArrayList<Member>();
	
	public MethodInfo(Class<?> beanClass, String name, Collection<Member> members) {
		this.beanClass = beanClass;
		this.name = name;
		if (members != null) this.members.addAll(members);
	}
	
	public Class<?> getBeanClass() {
		return beanClass;
	}
	
	public String getName() {
		return name;
	}
	
	public Object invoke(Object... args) {
		for (Member m : members) {
			
		}
		
		return null;
	}
	
	@Override
	public int compareTo(MethodInfo method) {
		if (!beanClass.equals(method.beanClass)) {
			return beanClass.getName().compareTo(method.beanClass.getName());			
		} else {
			return name.compareTo(method.name);
		}
	}
}
