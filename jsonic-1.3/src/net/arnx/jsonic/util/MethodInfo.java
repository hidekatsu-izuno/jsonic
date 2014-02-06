/* 
 * Copyright 2014 Hidekatsu Izuno
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.arnx.jsonic.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class MethodInfo implements Iterable<Method>, Comparable<MethodInfo> {
	private Class<?> beanClass;
	private String name;
	private boolean isStatic;
	
	List<Method> methods = new ArrayList<Method>();
	
	public MethodInfo(Class<?> beanClass, String name, Collection<Method> methods, boolean isStatic) {
		this.beanClass = beanClass;
		this.name = name;
		this.isStatic = isStatic;
		if (methods != null) this.methods.addAll(methods);
	}
	
	public Class<?> getBeanClass() {
		return beanClass;
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isStatic() {
		return isStatic;
	}
	
	public Object invoke(Object instance, Object... args) {
		Method method = findMethod(args);
		try {
			return method.invoke(instance, args);
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
	
	public Method findMethod(Object... args) {
		Method method = null;
		Class<?>[] types = null;
		
		Method vmethod = null;
		Class<?>[] vtypes = null;		
		
		for (Method cmethod : methods) {
			Class<?>[] cparams = cmethod.getParameterTypes();
			
			if (cmethod.isVarArgs()) {
				if (args.length < cparams.length-1) {
					continue;
				}
				
				if (vmethod == null) {
					Class<?> vtype = cparams[cparams.length-1].getComponentType();
					Class<?>[] tmp = new Class<?>[args.length];
					System.arraycopy(cparams, 0, tmp, 0, cparams.length-1);
					for (int i = cparams.length-1; i < tmp.length; i++) {
						tmp[i] = vtype;
					}
					vmethod = cmethod;
					vtypes = tmp;
				} else {
					int vpoint = BeanInfo.calcurateDistance(vtypes, args);
					int cpoint = BeanInfo.calcurateDistance(cparams, args);
					if (cpoint > vpoint) {
						vmethod = cmethod;
						vtypes = cparams;
					} else if (cpoint == vpoint) {
						cmethod = null;
						cparams = null;
					}
				}
			} else {
				if (args.length != cparams.length) {
					continue;
				}
				
				if (method == null) {
					method = cmethod;
					types = cparams;
				} else {
					int point = BeanInfo.calcurateDistance(types, args);
					int cpoint = BeanInfo.calcurateDistance(cparams, args);
					if (cpoint > point) {
						method = cmethod;
						types = cparams;
					} else if (cpoint == point) {
						cmethod = null;
						cparams = null;
					}
				}
			}			
		}
		
		if (vmethod != null) {
			if (method == null) {
				method = vmethod;
			} else {
				int point = BeanInfo.calcurateDistance(types, args);
				int vpoint = BeanInfo.calcurateDistance(vtypes, args);
				if (vpoint > point) {
					method = vmethod;
				}
			}
		}
		
		if (method == null) {
			throw new IllegalStateException("suitable method is not found: " + name);
		}
		
		return method;		
	}

	@Override
	public Iterator<Method> iterator() {
		return methods.iterator();
	}
	
	public int size() {
		return methods.size();
	}
}
