package net.arnx.jsonic.internal.formatter;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

import net.arnx.jsonic.JSONHint;
import net.arnx.jsonic.NamingStyle;
import net.arnx.jsonic.internal.io.OutputSource;
import net.arnx.jsonic.internal.util.StringCache;

public class FormatContext {
	private final Locale locale;
	private final int maxDepth;
	private final boolean prettyPrint;
	private final boolean suppressNull;
	private final String numberFormat;
	private final String dateFormat;
	private final NamingStyle propertyStyle;
	private final NamingStyle enumStyle;
	
	private Object[] path;
	private int depth = 0;
	private Map<Class<?>, Object> memberCache;
	private Map<String, DateFormat> dateFormatCache;
	private Map<String, NumberFormat> numberFormatCache;
	private StringCache cache;
	
	public FormatContext(
			Locale locale,
			int maxDepth,
			boolean prettyPrint,
			boolean suppressNull,
			String numberFormat,
			String dateFormat,
			NamingStyle propertyStyle,
			NamingStyle enumStyle) {
		this.locale = locale;
		this.maxDepth = maxDepth;
		this.prettyPrint = prettyPrint;
		this.suppressNull = suppressNull;
		this.numberFormat = numberFormat;
		this.dateFormat = dateFormat;
		this.propertyStyle = propertyStyle;
		this.enumStyle = enumStyle;
	}
	
	public Locale getLocale() {
		return locale;
	}
	
	public int getMaxDepth() {
		return maxDepth;
	}
	
	public boolean isPrettyPrint() {
		return prettyPrint;
	}
	
	public boolean isSuppressNull() {
		return suppressNull;
	}
	
	public NumberFormat getNumberFormat() {
		return null;
	}
	
	public DateFormat getDateFormat() {
		return null;
	}
	
	public NamingStyle getPropertyStyle() {
		return propertyStyle;
	}
	
	public NamingStyle getEnumStyle() {
		return enumStyle;
	}
	
	public void enter(Object key, JSONHint hint) {
		depth++;
		if (path == null) path = new Object[8];
		if (path.length < depth*2+2) {
			Object[] newPath = new Object[Math.max(path.length*2, depth*2+2)];
			System.arraycopy(path, 0, newPath, 0, path.length);
			path = newPath;
		}
		path[depth*2] = key;
		path[depth*2+1] = hint;
	}
	
	public void exit() {
		depth--;
	}
	
	/**
	 * Returns the current depth.
	 * 
	 * @return depth number. 0 is root node.
	 */
	public int getDepth() {
		return depth;
	}
	
	/**
	 * Returns the current key object.
	 * 
	 * @return Root node is '$'. When the parent is a array, the key is Integer, otherwise String. 
	 */
	public Object getKey() {
		return path[depth*2];
	}
	
	/**
	 * Returns the key object in any level. the negative value means relative to current level.
	 * 
	 * @return Root node is '$'. When the parent is a array, the key is Integer, otherwise String. 
	 */
	public Object getKey(int depth) {
		if (depth < 0) depth = this.depth + depth;
		return path[depth*2];
	}
	
	/**
	 * Returns the current hint annotation.
	 * 
	 * @return the current annotation if present on this context, else null.
	 */
	public JSONHint getHint() {
		return (JSONHint)path[depth*2+1];
	}
	
	public void format(String obj, OutputSource out) {
		
	}
}
