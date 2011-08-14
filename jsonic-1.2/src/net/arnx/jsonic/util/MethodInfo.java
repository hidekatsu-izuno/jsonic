package net.arnx.jsonic.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
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
	
	public Object invoke(Object instance, Object... args) {
		Member m = findMember(args);
		try {
			if (m instanceof Constructor<?>) {
				Constructor<?> con = (Constructor<?>)m;
				con.setAccessible(true);
				return con.newInstance(args);
			} else {
				Method method = (Method)m;
				method.setAccessible(true);
				return method.invoke(instance, args);
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	@Override
	public int compareTo(MethodInfo method) {
		if (!beanClass.equals(method.beanClass)) {
			return beanClass.getName().compareTo(method.beanClass.getName());			
		} else {
			return name.compareTo(method.name);
		}
	}
	
	private Member findMember(Object... args) {
		Member member = null;
		Class<?>[] types = null;
		
		Member vmember = null;
		Class<?>[] vtypes = null;		
		
		for (Member cmember : members) {
			Class<?>[] cparams;
			boolean cvarArgs;
			if (cmember instanceof Constructor<?>) {
				Constructor<?> con = (Constructor<?>)cmember;
				cparams = con.getParameterTypes();
				cvarArgs = con.isVarArgs();
			} else {
				Method m = (Method)cmember;
				cparams = m.getParameterTypes();
				cvarArgs = m.isVarArgs();
			}
			
			if (cvarArgs) {
				if (args.length < cparams.length-1) {
					continue;
				}
				
				if (vmember == null) {
					Class<?> vtype = cparams[cparams.length-1].getComponentType();
					Class<?>[] tmp = new Class<?>[args.length];
					System.arraycopy(cparams, 0, tmp, 0, cparams.length-1);
					for (int i = cparams.length-1; i < tmp.length; i++) {
						tmp[i] = vtype;
					}
					vmember = cmember;
					vtypes = tmp;
				} else {
					int vpoint = calcurateDistance(vtypes, args);
					int cpoint = calcurateDistance(cparams, args);
					if (cpoint > vpoint) {
						vmember = cmember;
						vtypes = cparams;
					} else if (cpoint == vpoint) {
						cmember = null;
						cparams = null;
					}
				}
			} else {
				if (args.length != cparams.length) {
					continue;
				}
				
				if (member == null) {
					member = cmember;
					types = cparams;
				} else {
					int point = calcurateDistance(types, args);
					int cpoint = calcurateDistance(cparams, args);
					if (cpoint > point) {
						member = cmember;
						types = cparams;
					} else if (cpoint == point) {
						cmember = null;
						cparams = null;
					}
				}
			}			
		}
		
		if (vmember != null) {
			if (member == null) {
				member = vmember;
			} else {
				int point = calcurateDistance(types, args);
				int vpoint = calcurateDistance(vtypes, args);
				if (vpoint > point) {
					member = vmember;
				}
			}
		}
		
		if (member == null) {
			throw new IllegalStateException("suitable member is not found: " + name);
		}
		
		return member;		
	}
	
	private static int calcurateDistance(Class<?>[] params, Object[] args) {
		int point = 0;
		for (int i = 0; i < args.length; i++) {
			if (args[i] == null) {
				if (!params[i].isPrimitive()) point += 5;
			} else if (params[i].equals(args[i].getClass())) {
				point += 10;
			} else if (params[i].isAssignableFrom(args[i].getClass())) {
				point += 8;
			} else if (boolean.class.equals(args[i].getClass()) || Boolean.class.equals(args[i].getClass())) {
				if (boolean.class.equals(params[i]) || Boolean.class.equals(params[i].getClass())) {
					point += 10;
				}
			} else if (byte.class.equals(args[i].getClass()) || Byte.class.equals(args[i].getClass())) {
				if (byte.class.equals(params[i])
						|| short.class.equals(params[i]) || char.class.equals(params[i])
						|| int.class.equals(params[i]) || long.class.equals(params[i])
						|| float.class.equals(params[i]) || double.class.equals(params[i])
						|| Byte.class.equals(params[i])
						|| Short.class.equals(params[i]) || Character.class.equals(params[i])
						|| Integer.class.equals(params[i]) || Long.class.equals(params[i])
						|| Float.class.equals(params[i]) || Double.class.equals(params[i])) {
					point += 10;
				}
			} else if (short.class.equals(args[i].getClass()) || Short.class.equals(args[i].getClass())
					|| char.class.equals(args[i].getClass()) || Character.class.equals(args[i].getClass())) {
				if (short.class.equals(params[i]) || char.class.equals(params[i])
						|| int.class.equals(params[i]) || long.class.equals(params[i])
						|| float.class.equals(params[i]) || double.class.equals(params[i])
						|| Short.class.equals(params[i]) || Character.class.equals(params[i])
						|| Integer.class.equals(params[i]) || Long.class.equals(params[i])
						|| Float.class.equals(params[i]) || Double.class.equals(params[i])) {
					point += 10;
				}
			} else if (int.class.equals(args[i].getClass()) || Integer.class.equals(args[i].getClass())) {
				if (int.class.equals(params[i]) || long.class.equals(params[i])
						|| float.class.equals(params[i]) || double.class.equals(params[i])
						|| Integer.class.equals(params[i]) || Long.class.equals(params[i])
						|| Float.class.equals(params[i]) || Double.class.equals(params[i])) {
					point += 10;
				}
			} else if (long.class.equals(args[i].getClass()) || Long.class.equals(args[i].getClass())) {
				if (long.class.equals(params[i])
						|| float.class.equals(params[i]) || double.class.equals(params[i])
						|| Long.class.equals(params[i])
						|| Float.class.equals(params[i]) || Double.class.equals(params[i])) {
					point += 10;
				}
			} else if (float.class.equals(args[i].getClass()) || Float.class.equals(args[i].getClass())) {
				if (float.class.equals(params[i]) || double.class.equals(params[i])
						|| Float.class.equals(params[i]) || Double.class.equals(params[i])) {
					point += 10;
				}
			} else if (double.class.equals(args[i].getClass()) || Double.class.equals(args[i].getClass())) {
				if (double.class.equals(params[i]) || Double.class.equals(params[i])) {
					point += 10;
				}
			}
		}
		return point;
	}
}
