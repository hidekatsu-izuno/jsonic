package net.arnx.jsonic;

import java.io.IOException;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.io.OutputSource;

public class JSONWriter {
	private Context context;
	private OutputSource out;
	
	JSONWriter(Context context, OutputSource out) {
		this.context = context;
		this.out = out;
	}
	
	public JSONWriter beginObject() throws IOException {
		out.append('{');
		return this;
	}
	
	public JSONWriter endObject() throws IOException {
		out.append('}');
		return this;
	}

	public JSONWriter beginArray() throws IOException {
		out.append('[');
		return this;
	}
	
	public JSONWriter endArray() throws IOException {
		out.append(']');
		return this;
	}
	
	public JSONWriter name(String name) throws IOException {
		StringFormatter.serialize(context, name, out);
		out.append(':');
		return this;
	}
	
	public JSONWriter value(Object value) throws IOException {
		value = context.preformatInternal(value);
		context.formatInternal(value, out);
		return this;
	}
}
