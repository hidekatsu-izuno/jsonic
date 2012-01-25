package net.arnx.jsonic.internal.formatter;

import net.arnx.jsonic.JSON.Context;
import net.arnx.jsonic.internal.io.OutputSource;
import net.arnx.jsonic.internal.util.ClassUtil;

public class InetAddressFormatter implements Formatter {
	public static final InetAddressFormatter INSTANCE = new InetAddressFormatter();

	public boolean format(final Context context, final Object src,
			final Object o, final OutputSource out) throws Exception {
		Class<?> inetAddressClass = ClassUtil.findClass("java.net.InetAddress");
		try {
			String text = (String) inetAddressClass.getMethod("getHostAddress")
					.invoke(o);
			return StringFormatter.INSTANCE.format(context, src, text, out);
		} catch (Exception e) {
			return NullFormatter.INSTANCE.format(context, src, null, out);
		}
	}
}