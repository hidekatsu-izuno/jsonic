package net.arnx.jsonic.internal.formatter;

import java.io.Flushable;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;

import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DOMElementFormatter implements Formatter {
	public static final DOMElementFormatter INSTANCE = new DOMElementFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		Element elem = (Element) o;
		out.append('[');
		StringFormatter.serialize(context, elem.getTagName(), out);

		out.append(',');
		if (context.isPrettyPrint()) {
			out.append('\n');
			for (int j = 0; j < context.getLevel() + 1; j++)
				out.append('\t');
		}
		out.append('{');
		if (elem.hasAttributes()) {
			NamedNodeMap names = elem.getAttributes();
			for (int i = 0; i < names.getLength(); i++) {
				if (i != 0) {
					out.append(',');
				}
				if (context.isPrettyPrint() && names.getLength() > 1) {
					out.append('\n');
					for (int j = 0; j < context.getLevel() + 2; j++)
						out.append('\t');
				}
				Node node = names.item(i);
				if (node instanceof Attr) {
					StringFormatter.serialize(context, node.getNodeName(), out);
					out.append(':');
					if (context.isPrettyPrint())
						out.append(' ');
					StringFormatter
							.serialize(context, node.getNodeValue(), out);
				}
			}
			if (context.isPrettyPrint() && names.getLength() > 1) {
				out.append('\n');
				for (int j = 0; j < context.getLevel() + 1; j++)
					out.append('\t');
			}
		}
		out.append('}');
		if (elem.hasChildNodes()) {
			NodeList nodes = elem.getChildNodes();
			for (int i = 0; i < nodes.getLength(); i++) {
				Object value = nodes.item(i);
				if ((value instanceof Element)
						|| (value instanceof CharacterData && !(value instanceof Comment))) {
					out.append(',');
					if (context.isPrettyPrint()) {
						out.append('\n');
						for (int j = 0; j < context.getLevel() + 1; j++)
							out.append('\t');
					}
					context.enter(i + 2);
					value = context.preformat(value);
					context.format(value, out);
					context.exit();
					if (out instanceof Flushable)
						((Flushable) out).flush();
				}
			}
		}
		if (context.isPrettyPrint()) {
			out.append('\n');
			for (int j = 0; j < context.getLevel(); j++)
				out.append('\t');
		}
		out.append(']');
		return true;
	}
}