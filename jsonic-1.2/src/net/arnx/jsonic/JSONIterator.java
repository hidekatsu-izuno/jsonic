package net.arnx.jsonic;

import java.io.IOException;
import java.lang.reflect.Type;

public interface JSONIterator {
	public boolean hasNext() throws IOException, JSONException;
	
	public <T> T next() throws IOException, JSONException;
	
	public <T> T next(Class<? extends T> cls) throws IOException, JSONException;
	
	public <T> T next(Type type) throws IOException, JSONException;
}
