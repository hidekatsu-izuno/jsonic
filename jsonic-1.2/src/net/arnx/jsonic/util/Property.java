package net.arnx.jsonic.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class Property implements Comparable<Property> {
	BeanInfo parent;
	
	String name;
	Member reader;
	Class<?> readType;
	Type readGtype;
	
	Member writer;
	Class<?> writeType;
	Type writeGtype;	
	
	Property(BeanInfo parent, String name) {
		this.parent = parent;
		this.name = name;
	}
	
	void init() {
		if (reader instanceof Method) {
			readType = ((Method)reader).getReturnType();
			readGtype = ((Method)reader).getGenericReturnType();
		} else if (reader instanceof Field) {
			readType = ((Field)reader).getType();
			readGtype = ((Field)reader).getGenericType();
		}
		
		if (writer instanceof Method) {
			writeType = ((Method)writer).getParameterTypes()[0];
			writeGtype = ((Method)writer).getGenericParameterTypes()[0];
		} else if (writer instanceof Field) {
			writeType = ((Field)writer).getType();
			writeGtype = ((Field)writer).getGenericType();
		}
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isReadable() {
		return (reader != null);
	}
	
	public Class<?> getReadType() {
		return readType;
	}
	
	public Type getReadGenericType() {
		return readGtype;
	}
	
	public Member getReadMember() {
		return reader;
	}
	
	public <T extends Annotation> T getReadAnnotation(Class<T> annotationClass) {
		return ((AnnotatedElement)reader).getAnnotation(annotationClass);
	}
	
	public Object get(Object target) {
		try {
			if (reader instanceof Method) {
				try {
					return ((Method)reader).invoke(target);
				} catch (InvocationTargetException e) {
					if (e.getCause() instanceof Error) {
						throw (Error)e.getCause();
					} else if (e.getCause() instanceof RuntimeException) {
						throw (RuntimeException)e.getCause();
					} else {
						throw new IllegalStateException(e.getCause());
					}
				}
			} else if (reader instanceof Field) {
				return ((Field)reader).get(target);
			} else {
				throw new IllegalStateException(name + " property is not readable.");
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	
	public boolean isWritable() {
		return (writer != null);
	}
	
	public Class<?> getWriteType() {
		return writeType;
	}
	
	public Type getWriteGenericType() {
		return writeGtype;
	}
	
	public Member getWriteMember() {
		return writer;
	}
	
	public <T extends Annotation> T getWriteAnnotation(Class<T> annotationClass) {
		return ((AnnotatedElement)writer).getAnnotation(annotationClass);
	}
	
	public void set(Object target, Object value) {
		try {
			if (writer instanceof Method) {
				try {
					((Method)writer).invoke(target, value);
				} catch (InvocationTargetException e) {
					if (e.getCause() instanceof Error) {
						throw (Error)e.getCause();
					} else if (e.getCause() instanceof RuntimeException) {
						throw (RuntimeException)e.getCause();
					} else {
						throw new IllegalStateException(e.getCause());
					}
				}
			} else if (writer instanceof Field) {
				((Field)writer).set(target, value);
			} else {
				throw new IllegalStateException(name + " property is not writable.");
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	public Property alias(String name) {
		Property alias = new Property(parent, name);
		synchronized (this) {
			alias.reader = reader;
			alias.readType = readType;
			alias.readGtype = readGtype;
			
			alias.writer = writer;
			alias.writeType = writeType;
			alias.writeGtype = writeGtype;
		}
		return alias;
	}

	@Override
	public int compareTo(Property property) {
		if (!parent.getType().equals(property.parent.getType())) {
			return parent.getType().getName().compareTo(property.parent.getType().getName());			
		} else {
			return name.compareTo(property.name);
		}
	}
}
