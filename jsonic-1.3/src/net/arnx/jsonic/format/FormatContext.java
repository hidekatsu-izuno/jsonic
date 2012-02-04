package net.arnx.jsonic.format;

import java.lang.reflect.Member;

import net.arnx.jsonic.JSON.Context;

public interface FormatContext {
	boolean ignore(Context context, Class<?> target, Member member);
	String normalize(String name);
	Object preformat(Context context, Object value) throws Exception;
}
